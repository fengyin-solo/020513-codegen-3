package com.redtourism.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.redtourism.common.Constants;
import com.redtourism.common.OrderStatus;
import com.redtourism.entity.Hotel;
import com.redtourism.entity.OrderInfo;
import com.redtourism.entity.OrderStatusLog;
import com.redtourism.entity.User;
import com.redtourism.mapper.OrderInfoMapper;
import com.redtourism.mapper.OrderStatusLogMapper;
import com.redtourism.service.HotelService;
import com.redtourism.service.MessageService;
import com.redtourism.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    /** 统一按东八区判定“入住日当天/离店之后”，避免服务器时区漂移。 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private OrderStatusLogMapper statusLogMapper;
    @Autowired
    private HotelService hotelService;
    @Autowired
    private MessageService messageService;
    /** 自注入代理，保证列表查询内部调用 autoTransition 时事务/行锁生效（避免 this 自调用绕过代理）。 */
    @Autowired
    @Lazy
    private OrderService self;

    private LocalDate today() {
        return LocalDate.now(ZONE);
    }

    // ==================== 状态流转记录 ====================

    /** 记录一次状态变更：变更时间由数据库默认值生成，触发人显式注明。 */
    private void writeLog(Long orderId, String fromStatus, String toStatus,
                          User operator, String triggerType, String remark) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setTriggerType(triggerType);
        if (operator != null) {
            log.setTriggerId(operator.getId());
            log.setTriggerName(StringUtils.hasText(operator.getNickname()) ? operator.getNickname() : operator.getUsername());
        } else if (OrderStatus.TRIGGER_SYSTEM.equals(triggerType)) {
            log.setTriggerName(OrderStatus.SYSTEM_NAME);
        }
        log.setRemark(remark);
        statusLogMapper.insert(log);
    }

    /**
     * 状态变更统一入口：更新订单状态、对应状态时间点、写流转记录。
     * 每一步状态变更都保留变更时间并注明触发人。
     */
    private void applyStatus(OrderInfo order, String toStatus, User operator,
                             String triggerType, String remark) {
        String from = order.getStatus();
        if (toStatus.equals(from)) {
            return;
        }
        Date now = new Date();
        order.setStatus(toStatus);
        switch (toStatus) {
            case OrderStatus.CONFIRMED:
                order.setConfirmedTime(now);
                break;
            case OrderStatus.CHECKED_IN:
                order.setCheckedInTime(now);
                break;
            case OrderStatus.FINISHED:
                order.setFinishedTime(now);
                break;
            case OrderStatus.CANCELLED:
                order.setCancelledTime(now);
                break;
            default:
                break;
        }
        updateById(order);
        writeLog(order.getId(), from, toStatus, operator, triggerType, remark);
    }

    private static boolean isHotel(OrderInfo order) {
        return Constants.ORDER_HOTEL.equals(order.getOrderType());
    }

    private static String displayName(User user) {
        if (user == null) return OrderStatus.SYSTEM_NAME;
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    // ==================== 下单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderInfo createOrder(OrderInfo order, User operator) {
        order.setOrderNo("ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        if (isHotel(order)) {
            // 酒店订单必须有入住/离店日期，作为后续状态自动推进与改期判定的依据
            if (order.getCheckInDate() == null || order.getCheckOutDate() == null) {
                throw new IllegalArgumentException("酒店订单必须选择入住日期和离店日期");
            }
            if (!order.getCheckOutDate().isAfter(order.getCheckInDate())) {
                throw new IllegalArgumentException("离店日期必须晚于入住日期");
            }
            if (order.getCheckInDate().isBefore(today())) {
                throw new IllegalArgumentException("入住日期不能早于今天");
            }
            if (order.getQuantity() == null || order.getQuantity() < 1) {
                order.setQuantity(1);
            }
            if (order.getRescheduleCount() == null) order.setRescheduleCount(0);
            // 订单提交之后先是“待确认”
            order.setStatus(OrderStatus.PENDING_CONFIRM);
        } else {
            order.setStatus(OrderStatus.PENDING);
        }
        save(order);
        writeLog(order.getId(), null, order.getStatus(), operator, OrderStatus.TRIGGER_USER,
                isHotel(order) ? "用户提交酒店预订，等待酒店确认" : "用户提交订单");
        return order;
    }

    // ==================== 取消 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, User operator) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(operator.getId())) {
            throw new RuntimeException("无权操作此订单");
        }
        doCancel(order, operator, OrderStatus.TRIGGER_USER, "用户取消订单");
        return true;
    }

    /**
     * 取消规则：
     * 酒店订单——待确认、已确认阶段（入住日之前）允许取消；跨过入住日（入住中/已结束）不允许取消；
     * 普通订单——待支付阶段允许取消。
     */
    private void doCancel(OrderInfo order, User operator, String triggerType, String remark) {
        if (isHotel(order)) {
            String st = order.getStatus();
            if (OrderStatus.PENDING_CONFIRM.equals(st)) {
                // 待确认阶段允许直接取消（酒店尚未确认，无履约约束）
                applyStatus(order, OrderStatus.CANCELLED, operator, triggerType, remark);
                if (order.getUserId() != null) {
                    messageService.sendMessage(order.getUserId(), "酒店订单已取消",
                            "您的订单 " + order.getOrderNo() + " 已取消。");
                }
                return;
            }
            if (OrderStatus.CONFIRMED.equals(st)) {
                // 已确认订单在入住日之前可以取消；到达入住日后系统会转为入住中，不可再取消
                if (order.getCheckInDate() != null && !order.getCheckInDate().isAfter(today())) {
                    throw new RuntimeException("已到入住日，订单不可取消");
                }
                applyStatus(order, OrderStatus.CANCELLED, operator, triggerType, remark);
                if (order.getUserId() != null) {
                    messageService.sendMessage(order.getUserId(), "酒店订单已取消",
                            "您的订单 " + order.getOrderNo() + " 已取消。");
                }
                return;
            }
            throw new RuntimeException("当前酒店订单状态不可取消（入住中或已结束的订单无法取消）");
        }
        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可取消");
        }
        applyStatus(order, OrderStatus.CANCELLED, operator, triggerType, remark);
    }

    // ==================== 支付 / 退款（普通订单） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(Long orderId, String payMethod, User operator) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(operator.getId())) {
            throw new RuntimeException("无权操作此订单");
        }
        if (isHotel(order)) {
            throw new RuntimeException("酒店预订无需支付，提交后等待酒店确认即可");
        }
        if (!OrderStatus.PENDING.equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可支付");
        }
        order.setPayMethod(payMethod);
        order.setPayTime(new Date());
        applyStatus(order, OrderStatus.PAID, operator, OrderStatus.TRIGGER_USER, "用户完成模拟支付");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refundOrder(Long orderId, User operator) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(operator.getId())) {
            throw new RuntimeException("无权操作此订单");
        }
        if (isHotel(order)) {
            throw new RuntimeException("酒店订单不支持退款，如需取消请使用取消功能");
        }
        if (!OrderStatus.PAID.equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可退款");
        }
        applyStatus(order, OrderStatus.REFUNDED, operator, OrderStatus.TRIGGER_USER, "用户申请退款");
        return true;
    }

    // ==================== 酒店确认 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmHotelOrder(Long orderId, User operator) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!isHotel(order)) {
            throw new RuntimeException("仅酒店订单需要酒店确认");
        }
        if (!OrderStatus.PENDING_CONFIRM.equals(order.getStatus())) {
            throw new RuntimeException("仅待确认的酒店订单可以确认");
        }
        applyStatus(order, OrderStatus.CONFIRMED, operator, OrderStatus.TRIGGER_ADMIN,
                "酒店确认订单（操作人：" + displayName(operator) + "）");
        messageService.sendMessage(order.getUserId(), "酒店预订已确认",
                "您的订单 " + order.getOrderNo() + " 已被酒店确认，"
                        + order.getCheckInDate() + " 入住、" + order.getCheckOutDate() + " 离店，祝您旅途愉快！");
        return true;
    }

    // ==================== 改期 ====================

    @Override
    public Map<String, Object> getRescheduleInfo(Long orderId, User operator) {
        OrderInfo order = getById(orderId);
        Map<String, Object> result = new HashMap<>();
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(operator.getId())) {
            throw new RuntimeException("无权操作此订单");
        }
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus());
        result.put("checkInDate", order.getCheckInDate());
        result.put("checkOutDate", order.getCheckOutDate());
        result.put("quantity", order.getQuantity() == null ? 1 : order.getQuantity());
        result.put("currentAmount", order.getAmount());
        result.put("rescheduleCount", order.getRescheduleCount() == null ? 0 : order.getRescheduleCount());
        result.put("today", today());

        String reason = checkReschedulable(order, false);
        result.put("canReschedule", reason == null);
        if (reason != null) {
            result.put("reason", reason);
        }
        Hotel hotel = isHotel(order) ? hotelService.getById(order.getTargetId()) : null;
        BigDecimal unitPrice = hotel != null && hotel.getPrice() != null
                ? hotel.getPrice() : BigDecimal.ZERO;
        result.put("unitPrice", unitPrice);
        result.put("freeReschedule", OrderStatus.PENDING_CONFIRM.equals(order.getStatus()));
        return result;
    }

    /**
     * 服务端统一判定可改范围。
     * 返回 null 表示可改；否则返回不可改原因。
     *
     * @param forUpdate 是否处于行锁临界区（用于给并发判定的错误信息区分措辞）
     */
    private String checkReschedulable(OrderInfo order, boolean forUpdate) {
        if (!isHotel(order)) {
            return "仅酒店订单支持改期";
        }
        String st = order.getStatus();
        if (!OrderStatus.PENDING_CONFIRM.equals(st) && !OrderStatus.CONFIRMED.equals(st)) {
            return "当前状态不可改期（仅待确认/已确认且未入住的订单可改期）";
        }
        LocalDate t = today();
        if (order.getCheckInDate() == null) {
            return "订单缺少入住日期，无法改期";
        }
        // 跨过入住日之后不允许再改期
        if (order.getCheckInDate().isBefore(t)) {
            return "已跨过入住日，不能再改期";
        }
        // 入住日当天并发改期只接受最早的一次：
        // 当天最早一次改期成功时会把 checkinDayRescheduleDate 记为今天（即使入住日随后被改到未来），
        // 因此同日内其余并发请求/再次提交一律拒绝，由行锁串行化判定保证
        if (t.equals(order.getCheckinDayRescheduleDate())) {
            return forUpdate
                    ? "入住日当天已有更早的改期请求被接受，本次并发改期不予处理（当天仅接受最早一次）"
                    : "入住日当天已完成一次改期，不能再次改期";
        }
        return null;
    }

    /** 校验新日期范围（可改范围由服务端统一判定，前端只做展示）。 */
    private void validateNewDates(OrderInfo order, LocalDate newIn, LocalDate newOut) {
        if (newIn == null || newOut == null) {
            throw new IllegalArgumentException("请选择新的入住日期和离店日期");
        }
        if (!newOut.isAfter(newIn)) {
            throw new IllegalArgumentException("新的离店日期必须晚于新的入住日期");
        }
        if (newIn.isBefore(today())) {
            throw new IllegalArgumentException("新的入住日期不能早于今天");
        }
        // 改期只能向后续期，不允许把入住日改到更早的日期
        if (order.getCheckInDate() != null && newIn.isBefore(order.getCheckInDate())) {
            throw new IllegalArgumentException("改期后的入住日期不能早于原入住日期");
        }
    }

    private long nights(LocalDate in, LocalDate out) {
        return Math.max(1, ChronoUnit.DAYS.between(in, out));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderInfo rescheduleHotelOrder(Long orderId, LocalDate newCheckIn,
                                          LocalDate newCheckOut, User operator) {
        // 行锁串行化同一订单的并发改期，保证“入住日当天只接受最早一次”
        OrderInfo order = baseMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(operator.getId())) {
            throw new RuntimeException("无权操作此订单");
        }
        String blocked = checkReschedulable(order, true);
        if (blocked != null) {
            throw new RuntimeException(blocked);
        }
        validateNewDates(order, newCheckIn, newCheckOut);

        LocalDate oldIn = order.getCheckInDate();
        LocalDate oldOut = order.getCheckOutDate();
        if (newCheckIn.equals(oldIn) && newCheckOut.equals(oldOut)) {
            throw new RuntimeException("新日期与原日期一致，无需改期");
        }

        int qty = order.getQuantity() == null ? 1 : order.getQuantity();
        long oldNights = nights(oldIn, oldOut);
        long newNights = nights(newCheckIn, newCheckOut);
        BigDecimal oldAmount = order.getAmount() == null ? BigDecimal.ZERO : order.getAmount();
        BigDecimal newAmount = oldAmount;
        String priceRemark;

        if (OrderStatus.PENDING_CONFIRM.equals(order.getStatus())) {
            // 待确认阶段可以免费改期：不重算房价，沿用下单时的间夜均价
            BigDecimal nightlyRate = oldAmount.divide(BigDecimal.valueOf(oldNights * qty),
                    2, BigDecimal.ROUND_HALF_UP);
            newAmount = nightlyRate.multiply(BigDecimal.valueOf(newNights * qty))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            priceRemark = "待确认阶段免费改期，按原间夜单价 " + nightlyRate + " 元/间夜折算，金额 "
                    + oldAmount + " -> " + newAmount + " 元";
        } else {
            // 已确认阶段改期要按新的间夜重算房价（以酒店当前挂牌价为准）
            Hotel hotel = hotelService.getById(order.getTargetId());
            BigDecimal unitPrice = hotel != null && hotel.getPrice() != null
                    ? hotel.getPrice() : oldAmount.divide(BigDecimal.valueOf(oldNights * qty),
                    2, BigDecimal.ROUND_HALF_UP);
            newAmount = unitPrice.multiply(BigDecimal.valueOf(newNights * qty))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            priceRemark = "已确认阶段改期，按新的间夜重算房价：" + unitPrice + " 元/晚 × "
                    + newNights + " 间夜 × " + qty + " 间 = " + newAmount + " 元（原金额 " + oldAmount + " 元）";
        }

        order.setCheckInDate(newCheckIn);
        order.setCheckOutDate(newCheckOut);
        order.setAmount(newAmount);
        order.setRescheduleCount((order.getRescheduleCount() == null ? 0 : order.getRescheduleCount()) + 1);
        order.setLastRescheduleDate(today());
        // 若本次改期发生在原入住日当天，钉住该自然日：当天后续（含并发）改期全部拒绝
        if (oldIn.isEqual(today())) {
            order.setCheckinDayRescheduleDate(today());
        }
        updateById(order);

        String remark = "用户改期：" + oldIn + "~" + oldOut + " -> " + newCheckIn + "~" + newCheckOut
                + "（间夜 " + oldNights + " -> " + newNights + "）；" + priceRemark
                + "；操作人：" + displayName(operator);
        // 改期不改变状态，但作为一条状态变更记录留存（to_status 记当前状态）
        writeLog(order.getId(), order.getStatus(), order.getStatus(), operator,
                OrderStatus.TRIGGER_USER, remark);
        messageService.sendMessage(order.getUserId(), "酒店订单改期成功",
                "您的订单 " + order.getOrderNo() + " 已改期为 " + newCheckIn + " 入住、"
                        + newCheckOut + " 离店，最新金额 ¥" + newAmount + "。");
        return order;
    }

    // ==================== 自动状态推进（入住中 / 已结束） ====================

    /**
     * 按日期自动推进酒店订单：
     * CONFIRMED 且今天 >= 入住日 -> CHECKED_IN（入住日当天转入住中）；
     * CONFIRMED/CHECKED_IN 且今天 >= 离店日 -> FINISHED（离店后转为结束记录）。
     */
    private boolean transitionByDate(OrderInfo order, User operator, String triggerType) {
        if (!isHotel(order) || order.getCheckInDate() == null || order.getCheckOutDate() == null) {
            return false;
        }
        LocalDate t = today();
        String st = order.getStatus();
        if (!order.getCheckOutDate().isAfter(t)) {
            // 今天已到/过离店日：离店之后转为结束记录。
            // 若错过入住日当天的推进（如服务长期未运行），先补发“入住中”这一步，保证状态链完整。
            if (OrderStatus.CONFIRMED.equals(st)) {
                applyStatus(order, OrderStatus.CHECKED_IN, operator, triggerType,
                        "入住日 " + order.getCheckInDate() + " 已过，补记入住中状态");
            }
            applyStatus(order, OrderStatus.FINISHED, operator, triggerType,
                    "到达离店日 " + order.getCheckOutDate() + "，系统自动转为已结束");
            return true;
        }
        if (OrderStatus.CONFIRMED.equals(st) && !order.getCheckInDate().isAfter(t)) {
            // 入住日当天转为入住中
            applyStatus(order, OrderStatus.CHECKED_IN, operator, triggerType,
                    "入住日 " + order.getCheckInDate() + " 当天，系统自动转为入住中");
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoTransitionHotelOrders() {
        List<OrderInfo> candidates = baseMapper.selectAutoTransitionHotelOrders();
        int changed = 0;
        for (OrderInfo o : candidates) {
            // 对每条记录单独加行锁，避免与用户改期并发冲突
            OrderInfo locked = baseMapper.selectByIdForUpdate(o.getId());
            if (locked != null && transitionByDate(locked, null, OrderStatus.TRIGGER_SYSTEM)) {
                changed++;
            }
        }
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lazyTransition(Long orderId) {
        OrderInfo order = getById(orderId);
        if (order == null || !isHotel(order)) return;
        if (OrderStatus.CONFIRMED.equals(order.getStatus()) || OrderStatus.CHECKED_IN.equals(order.getStatus())) {
            transitionByDate(order, null, OrderStatus.TRIGGER_SYSTEM);
        }
    }

    // ==================== 查询 ====================

    @Override
    public IPage<OrderInfo> listUserOrders(int page, int size, Long userId, String orderType, String status) {
        // 查询前先统一推进到期的酒店订单，保证“我的订单列表”状态最新
        self.autoTransitionHotelOrders();
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, userId);
        if (StringUtils.hasText(orderType)) {
            wrapper.eq(OrderInfo::getOrderType, orderType);
        }
        applyStatusFilter(wrapper, status);
        wrapper.orderByDesc(OrderInfo::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public IPage<OrderInfo> listAllOrders(int page, int size, String orderType, String status) {
        self.autoTransitionHotelOrders();
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(orderType)) {
            wrapper.eq(OrderInfo::getOrderType, orderType);
        }
        applyStatusFilter(wrapper, status);
        wrapper.orderByDesc(OrderInfo::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    /** status 支持逗号分隔的多状态查询（前端“进行中”等聚合筛选用）。 */
    private void applyStatusFilter(LambdaQueryWrapper<OrderInfo> wrapper, String status) {
        if (!StringUtils.hasText(status)) return;
        Set<String> statuses = Arrays.stream(status.split(","))
                .map(String::trim).filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (statuses.size() == 1) {
            wrapper.eq(OrderInfo::getStatus, statuses.iterator().next());
        } else if (!statuses.isEmpty()) {
            wrapper.in(OrderInfo::getStatus, new ArrayList<>(statuses));
        }
    }

    @Override
    public List<OrderStatusLog> listStatusLogs(Long orderId, User operator, boolean isAdmin) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!isAdmin && !order.getUserId().equals(operator.getId())) {
            throw new RuntimeException("无权查看此订单的流转记录");
        }
        LambdaQueryWrapper<OrderStatusLog> w = new LambdaQueryWrapper<>();
        w.eq(OrderStatusLog::getOrderId, orderId).orderByAsc(OrderStatusLog::getId);
        return statusLogMapper.selectList(w);
    }

    // ==================== 供后台调用的管理动作 ====================

    /** 后台取消订单（规则与用户取消一致，触发人记录为后台操作人）。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminCancel(Long orderId, User operator) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        doCancel(order, operator, OrderStatus.TRIGGER_ADMIN, "后台取消订单（操作人：" + displayName(operator) + "）");
    }

    /** 后台退款（仅普通订单已支付）。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminRefund(Long orderId, User operator) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (isHotel(order)) {
            throw new RuntimeException("酒店订单不支持退款");
        }
        if (!OrderStatus.PAID.equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可退款");
        }
        applyStatus(order, OrderStatus.REFUNDED, operator, OrderStatus.TRIGGER_ADMIN,
                "后台操作退款（操作人：" + displayName(operator) + "）");
    }

    /** 后台完成（仅普通订单已支付）。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminComplete(Long orderId, User operator) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (isHotel(order)) {
            throw new RuntimeException("酒店订单由系统按入住/离店日自动推进，不能手动完成");
        }
        if (!OrderStatus.PAID.equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可完成");
        }
        applyStatus(order, OrderStatus.COMPLETED, operator, OrderStatus.TRIGGER_ADMIN,
                "后台标记完成（操作人：" + displayName(operator) + "）");
    }
}
