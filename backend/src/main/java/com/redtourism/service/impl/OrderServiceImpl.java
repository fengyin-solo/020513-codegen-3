package com.redtourism.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.redtourism.common.Constants;
import com.redtourism.entity.Hotel;
import com.redtourism.entity.OrderInfo;
import com.redtourism.entity.OrderStatusLog;
import com.redtourism.entity.User;
import com.redtourism.mapper.HotelMapper;
import com.redtourism.mapper.OrderInfoMapper;
import com.redtourism.mapper.OrderStatusLogMapper;
import com.redtourism.mapper.UserMapper;
import com.redtourism.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    /** 业务日期统一使用东八区，避免服务器时区影响“入住日当天”的判定 */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private HotelMapper hotelMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Override
    public OrderInfo createOrder(OrderInfo order) {
        order.setOrderNo("ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        order.setStatus("PENDING");
        order.setRescheduleCount(0);
        order.setVersion(0);
        save(order);
        return order;
    }

    @Override
    public boolean cancelOrder(Long orderId, Long userId) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (Constants.ORDER_HOTEL.equals(order.getOrderType())) {
            // 酒店订单：待确认/已确认阶段可取消，取消后同步记录状态变更日志
            String status = order.getStatus();
            if (!Constants.HOTEL_ORDER_PENDING_CONFIRM.equals(status) && !Constants.HOTEL_ORDER_CONFIRMED.equals(status)) {
                throw new RuntimeException("当前订单状态不可取消");
            }
            order.setStatus(Constants.HOTEL_ORDER_CANCELLED);
            if (!updateById(order)) {
                throw new RuntimeException("取消失败：订单状态刚发生变更，请刷新后重试");
            }
            User user = userMapper.selectById(userId);
            saveLog(orderId, Constants.ORDER_LOG_CANCEL, status, Constants.HOTEL_ORDER_CANCELLED,
                    Constants.OPERATOR_USER, userId, displayName(user), "用户取消订单");
            return true;
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可取消");
        }
        order.setStatus("CANCELLED");
        return updateById(order);
    }

    @Override
    public boolean payOrder(Long orderId, String payMethod, Long userId) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可支付");
        }
        order.setStatus("PAID");
        order.setPayMethod(payMethod);
        order.setPayTime(new Date());
        return updateById(order);
    }

    @Override
    public boolean refundOrder(Long orderId, Long userId) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可退款");
        }
        order.setStatus("REFUNDED");
        return updateById(order);
    }

    @Override
    public IPage<OrderInfo> listUserOrders(int page, int size, Long userId, String orderType, String status) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, userId);
        if (StringUtils.hasText(orderType)) {
            wrapper.eq(OrderInfo::getOrderType, orderType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(OrderInfo::getStatus, status);
        }
        wrapper.orderByDesc(OrderInfo::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public IPage<OrderInfo> listAllOrders(int page, int size, String orderType, String status) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(orderType)) {
            wrapper.eq(OrderInfo::getOrderType, orderType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(OrderInfo::getStatus, status);
        }
        wrapper.orderByDesc(OrderInfo::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    // ==================== 酒店订单状态流转 ====================

    @Override
    @Transactional
    public OrderInfo createHotelOrder(User user, Long hotelId, Integer quantity, String checkInDate, String checkOutDate) {
        Hotel hotel = hotelMapper.selectById(hotelId);
        if (hotel == null || hotel.getStatus() == null || hotel.getStatus() != Constants.STATUS_ENABLED) {
            throw new RuntimeException("酒店不存在或已下架");
        }
        LocalDate checkIn = parseDate(checkInDate);
        LocalDate checkOut = parseDate(checkOutDate);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        validateStayDates(today, checkIn, checkOut);
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        int qty = quantity == null || quantity < 1 ? 1 : quantity;

        OrderInfo order = new OrderInfo();
        order.setUserId(user.getId());
        order.setOrderType(Constants.ORDER_HOTEL);
        order.setTargetId(hotelId);
        order.setTargetName(hotel.getName());
        order.setQuantity(qty);
        order.setCheckInDate(toDate(checkIn));
        order.setCheckOutDate(toDate(checkOut));
        // 金额由服务端按间夜重算，避免前端篡改
        order.setAmount(hotel.getPrice().multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(qty)));
        order.setOrderNo("ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        order.setStatus(Constants.HOTEL_ORDER_PENDING_CONFIRM);
        order.setRescheduleCount(0);
        order.setVersion(0);
        save(order);
        saveLog(order.getId(), Constants.ORDER_LOG_CREATE, null, Constants.HOTEL_ORDER_PENDING_CONFIRM,
                Constants.OPERATOR_USER, user.getId(), displayName(user),
                "提交酒店预订：" + checkIn + " 至 " + checkOut + "，共 " + nights + " 间夜，待酒店确认");
        return order;
    }

    @Override
    public Map<String, Object> getRescheduleInfo(Long orderId, Long userId) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!Constants.ORDER_HOTEL.equals(order.getOrderType())) {
            throw new RuntimeException("仅酒店订单支持改期");
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate checkIn = toLocalDate(order.getCheckInDate());
        LocalDate checkOut = toLocalDate(order.getCheckOutDate());
        String status = order.getStatus();
        boolean statusOk = Constants.HOTEL_ORDER_PENDING_CONFIRM.equals(status) || Constants.HOTEL_ORDER_CONFIRMED.equals(status);
        boolean notPassed = checkIn != null && !today.isAfter(checkIn);
        String reason = null;
        if (!statusOk) {
            reason = "当前订单状态不可改期";
        } else if (!notPassed) {
            reason = "已跨过入住日，不可再改期";
        }
        Hotel hotel = hotelMapper.selectById(order.getTargetId());
        Map<String, Object> info = new HashMap<>();
        info.put("reschedulable", statusOk && notPassed);
        info.put("reason", reason);
        info.put("free", Constants.HOTEL_ORDER_PENDING_CONFIRM.equals(status));
        info.put("status", status);
        info.put("checkInDate", checkIn == null ? null : checkIn.toString());
        info.put("checkOutDate", checkOut == null ? null : checkOut.toString());
        // 可改范围由服务端统一判定：今天起 90 天内，单次最多 30 间夜
        info.put("minCheckInDate", today.toString());
        info.put("maxCheckInDate", today.plusDays(Constants.RESCHEDULE_MAX_DAYS_AHEAD).toString());
        info.put("maxNights", Constants.RESCHEDULE_MAX_NIGHTS);
        info.put("pricePerNight", hotel == null ? null : hotel.getPrice());
        info.put("quantity", order.getQuantity());
        info.put("amount", order.getAmount());
        info.put("rescheduleCount", order.getRescheduleCount());
        return info;
    }

    @Override
    @Transactional
    public void rescheduleOrder(Long orderId, Long userId, String checkInDate, String checkOutDate) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!Constants.ORDER_HOTEL.equals(order.getOrderType())) {
            throw new RuntimeException("仅酒店订单支持改期");
        }
        String status = order.getStatus();
        boolean free = Constants.HOTEL_ORDER_PENDING_CONFIRM.equals(status);
        if (!free && !Constants.HOTEL_ORDER_CONFIRMED.equals(status)) {
            throw new RuntimeException("当前订单状态不可改期");
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate oldCheckIn = toLocalDate(order.getCheckInDate());
        LocalDate oldCheckOut = toLocalDate(order.getCheckOutDate());
        // 跨过入住日之后不允许再改期
        if (oldCheckIn != null && today.isAfter(oldCheckIn)) {
            throw new RuntimeException("已跨过入住日，不可再改期");
        }
        LocalDate newCheckIn = parseDate(checkInDate);
        LocalDate newCheckOut = parseDate(checkOutDate);
        validateStayDates(today, newCheckIn, newCheckOut);
        long newNights = ChronoUnit.DAYS.between(newCheckIn, newCheckOut);

        BigDecimal oldAmount = order.getAmount();
        BigDecimal newAmount = oldAmount;
        String detail;
        if (free) {
            // 待确认阶段：免费改期，仅调整日期，金额不变
            detail = "待确认阶段免费改期：" + oldCheckIn + " ~ " + oldCheckOut + " 改为 " + newCheckIn + " ~ " + newCheckOut;
        } else {
            // 已确认阶段：按新的间夜重算房价
            Hotel hotel = hotelMapper.selectById(order.getTargetId());
            if (hotel == null || hotel.getPrice() == null) {
                throw new RuntimeException("酒店信息不存在，无法重算房价");
            }
            int qty = order.getQuantity() == null ? 1 : order.getQuantity();
            newAmount = hotel.getPrice().multiply(BigDecimal.valueOf(newNights)).multiply(BigDecimal.valueOf(qty));
            detail = "已确认阶段改期，按新间夜重算房价：" + oldCheckIn + " ~ " + oldCheckOut + " 改为 " + newCheckIn + " ~ " + newCheckOut
                    + "（" + newNights + " 间夜 × ¥" + hotel.getPrice() + " × " + qty + " 间），金额 ¥" + oldAmount + " → ¥" + newAmount;
        }
        order.setCheckInDate(toDate(newCheckIn));
        order.setCheckOutDate(toDate(newCheckOut));
        order.setAmount(newAmount);
        order.setRescheduleCount(order.getRescheduleCount() == null ? 1 : order.getRescheduleCount() + 1);
        // 乐观锁：入住日当天并发改期时，仅最早提交的一次更新成功，其余失败
        if (!updateById(order)) {
            throw new RuntimeException("改期失败：订单刚被其他操作变更，请刷新后重试");
        }
        User user = userMapper.selectById(userId);
        saveLog(orderId, Constants.ORDER_LOG_RESCHEDULE, status, status,
                Constants.OPERATOR_USER, userId, displayName(user), detail);
    }

    @Override
    @Transactional
    public void confirmOrder(Long orderId, User operator) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!Constants.ORDER_HOTEL.equals(order.getOrderType())) {
            throw new RuntimeException("仅酒店订单需要确认");
        }
        if (!Constants.HOTEL_ORDER_PENDING_CONFIRM.equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不可确认");
        }
        order.setStatus(Constants.HOTEL_ORDER_CONFIRMED);
        if (!updateById(order)) {
            throw new RuntimeException("确认失败：订单状态刚发生变更，请刷新后重试");
        }
        saveLog(orderId, Constants.ORDER_LOG_CONFIRM, Constants.HOTEL_ORDER_PENDING_CONFIRM, Constants.HOTEL_ORDER_CONFIRMED,
                operatorTypeOf(operator), operator == null ? null : operator.getId(), operatorNameOf(operator), "酒店确认订单");
    }

    @Override
    @Transactional
    public void adminCancelOrder(Long orderId, User operator) {
        OrderInfo order = getById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (Constants.ORDER_HOTEL.equals(order.getOrderType())) {
            String status = order.getStatus();
            if (!Constants.HOTEL_ORDER_PENDING_CONFIRM.equals(status) && !Constants.HOTEL_ORDER_CONFIRMED.equals(status)) {
                throw new RuntimeException("当前订单状态不可取消");
            }
            order.setStatus(Constants.HOTEL_ORDER_CANCELLED);
            if (!updateById(order)) {
                throw new RuntimeException("取消失败：订单状态刚发生变更，请刷新后重试");
            }
            saveLog(orderId, Constants.ORDER_LOG_CANCEL, status, Constants.HOTEL_ORDER_CANCELLED,
                    operatorTypeOf(operator), operator == null ? null : operator.getId(), operatorNameOf(operator), "酒店方取消订单");
            return;
        }
        order.setStatus("CANCELLED");
        updateById(order);
    }

    @Override
    public List<OrderStatusLog> listOrderLogs(Long orderId) {
        return orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, orderId)
                .orderByAsc(OrderStatusLog::getCreateTime)
                .orderByAsc(OrderStatusLog::getId));
    }

    @Override
    public void autoTransitionHotelOrders() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        // 入住日当天：已确认 -> 入住中
        List<OrderInfo> toCheckIn = lambdaQuery()
                .eq(OrderInfo::getOrderType, Constants.ORDER_HOTEL)
                .eq(OrderInfo::getStatus, Constants.HOTEL_ORDER_CONFIRMED)
                .isNotNull(OrderInfo::getCheckInDate)
                .le(OrderInfo::getCheckInDate, java.sql.Date.valueOf(today))
                .list();
        for (OrderInfo o : toCheckIn) {
            boolean ok = lambdaUpdate()
                    .eq(OrderInfo::getId, o.getId())
                    .eq(OrderInfo::getStatus, Constants.HOTEL_ORDER_CONFIRMED)
                    .set(OrderInfo::getStatus, Constants.HOTEL_ORDER_CHECKED_IN)
                    .update();
            if (ok) {
                saveLog(o.getId(), Constants.ORDER_LOG_CHECKIN, Constants.HOTEL_ORDER_CONFIRMED, Constants.HOTEL_ORDER_CHECKED_IN,
                        Constants.OPERATOR_SYSTEM, null, "系统", "到达入住日，自动转为入住中");
            }
        }
        // 离店之后：入住中 -> 结束记录
        List<OrderInfo> toFinish = lambdaQuery()
                .eq(OrderInfo::getOrderType, Constants.ORDER_HOTEL)
                .eq(OrderInfo::getStatus, Constants.HOTEL_ORDER_CHECKED_IN)
                .isNotNull(OrderInfo::getCheckOutDate)
                .lt(OrderInfo::getCheckOutDate, java.sql.Date.valueOf(today))
                .list();
        for (OrderInfo o : toFinish) {
            boolean ok = lambdaUpdate()
                    .eq(OrderInfo::getId, o.getId())
                    .eq(OrderInfo::getStatus, Constants.HOTEL_ORDER_CHECKED_IN)
                    .set(OrderInfo::getStatus, Constants.HOTEL_ORDER_COMPLETED)
                    .update();
            if (ok) {
                saveLog(o.getId(), Constants.ORDER_LOG_FINISH, Constants.HOTEL_ORDER_CHECKED_IN, Constants.HOTEL_ORDER_COMPLETED,
                        Constants.OPERATOR_SYSTEM, null, "系统", "离店完成，订单转为结束记录");
            }
        }
    }

    // ==================== 内部工具方法 ====================

    private void saveLog(Long orderId, String action, String fromStatus, String toStatus,
                         String operatorType, Long operatorId, String operatorName, String detail) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setDetail(detail);
        orderStatusLogMapper.insert(log);
    }

    /** 入住/退房日期校验：可改范围由服务端统一判定 */
    private void validateStayDates(LocalDate today, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new RuntimeException("请选择入住与退房日期");
        }
        if (checkIn.isBefore(today)) {
            throw new RuntimeException("入住日期不能早于今天");
        }
        if (checkIn.isAfter(today.plusDays(Constants.RESCHEDULE_MAX_DAYS_AHEAD))) {
            throw new RuntimeException("入住日期最晚可选 " + today.plusDays(Constants.RESCHEDULE_MAX_DAYS_AHEAD));
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new RuntimeException("退房日期必须晚于入住日期");
        }
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > Constants.RESCHEDULE_MAX_NIGHTS) {
            throw new RuntimeException("单次最多可订 " + Constants.RESCHEDULE_MAX_NIGHTS + " 间夜");
        }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim().substring(0, 10));
        } catch (Exception e) {
            throw new RuntimeException("日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    private LocalDate toLocalDate(Date d) {
        if (d == null) {
            return null;
        }
        if (d instanceof java.sql.Date) {
            return ((java.sql.Date) d).toLocalDate();
        }
        return d.toInstant().atZone(BUSINESS_ZONE).toLocalDate();
    }

    private Date toDate(LocalDate ld) {
        return ld == null ? null : java.sql.Date.valueOf(ld);
    }

    private String displayName(User user) {
        if (user == null) {
            return "用户";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String operatorTypeOf(User operator) {
        if (operator == null || operator.getRole() == null) {
            return Constants.OPERATOR_ADMIN;
        }
        return Constants.ROLE_STAFF.equals(operator.getRole()) ? Constants.OPERATOR_STAFF : Constants.OPERATOR_ADMIN;
    }

    private String operatorNameOf(User operator) {
        if (operator == null) {
            return "管理员";
        }
        String name = StringUtils.hasText(operator.getNickname()) ? operator.getNickname() : operator.getUsername();
        return name == null ? "管理员" : name;
    }
}
