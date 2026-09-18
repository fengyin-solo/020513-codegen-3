package com.redtourism;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.redtourism.common.OrderStatus;
import com.redtourism.entity.Hotel;
import com.redtourism.entity.OrderInfo;
import com.redtourism.entity.OrderStatusLog;
import com.redtourism.entity.User;
import com.redtourism.mapper.HotelMapper;
import com.redtourism.mapper.OrderInfoMapper;
import com.redtourism.mapper.OrderStatusLogMapper;
import com.redtourism.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HotelOrderFlowTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderInfoMapper orderMapper;
    @Autowired private OrderStatusLogMapper logMapper;
    @Autowired private HotelMapper hotelMapper;

    private User zhangsan;
    private User admin;
    private Long hotelId;
    private static final BigDecimal HOTEL_PRICE = new BigDecimal("200.00");

    @BeforeEach
    void setUp() {
        orderMapper.delete(null);
        logMapper.delete(null);
        Hotel hotel = new Hotel();
        hotel.setName("测试酒店");
        hotel.setPrice(HOTEL_PRICE);
        hotel.setStatus(1);
        hotelMapper.insert(hotel);
        hotelId = hotel.getId();

        zhangsan = new User();
        zhangsan.setId(1001L);
        zhangsan.setUsername("zhangsan");
        zhangsan.setNickname("张三");
        zhangsan.setRole("USER");

        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setNickname("管理员");
        admin.setRole("ADMIN");
    }

    private OrderInfo buildHotelOrder(LocalDate in, LocalDate out, int nights) {
        assertEquals(nights, java.time.temporal.ChronoUnit.DAYS.between(in, out));
        OrderInfo o = new OrderInfo();
        o.setUserId(zhangsan.getId());
        o.setOrderType("HOTEL");
        o.setTargetId(hotelId);
        o.setTargetName("测试酒店");
        o.setQuantity(1);
        o.setCheckInDate(in);
        o.setCheckOutDate(out);
        // 1 间 × nights 晚 × 200
        o.setAmount(HOTEL_PRICE.multiply(BigDecimal.valueOf(nights)));
        return o;
    }

    /**
     * 直接落库一条已确认的历史/当日酒店订单（绕过“入住日不能早于今天”的下单校验），
     * 用于模拟定时任务在真实时间线上推进。状态流转日志也补齐。
     */
    private OrderInfo insertConfirmedOrder(LocalDate in, LocalDate out, BigDecimal amount) {
        OrderInfo o = new OrderInfo();
        o.setUserId(zhangsan.getId());
        o.setOrderType("HOTEL");
        o.setTargetId(hotelId);
        o.setTargetName("测试酒店");
        o.setQuantity(1);
        o.setCheckInDate(in);
        o.setCheckOutDate(out);
        o.setAmount(amount);
        o.setStatus(OrderStatus.CONFIRMED);
        o.setRescheduleCount(0);
        o.setOrderNo("ORDFIX" + System.nanoTime());
        orderMapper.insert(o);
        OrderStatusLog submit = new OrderStatusLog();
        submit.setOrderId(o.getId());
        submit.setFromStatus(null);
        submit.setToStatus(OrderStatus.PENDING_CONFIRM);
        submit.setTriggerType(OrderStatus.TRIGGER_USER);
        submit.setTriggerName(zhangsan.getNickname());
        logMapper.insert(submit);
        OrderStatusLog confirm = new OrderStatusLog();
        confirm.setOrderId(o.getId());
        confirm.setFromStatus(OrderStatus.PENDING_CONFIRM);
        confirm.setToStatus(OrderStatus.CONFIRMED);
        confirm.setTriggerType(OrderStatus.TRIGGER_ADMIN);
        confirm.setTriggerName(admin.getNickname());
        logMapper.insert(confirm);
        return o;
    }

    @Test
    void fullFlow_pendingConfirm_checkedIn_finished_withLogs() {
        LocalDate today = LocalDate.now();
        // 1. 提交 -> 待确认
        OrderInfo order = orderService.createOrder(buildHotelOrder(today.plusDays(3), today.plusDays(5), 2), zhangsan);
        assertEquals(OrderStatus.PENDING_CONFIRM, order.getStatus());

        // 2. 后台确认 -> 已确认
        orderService.confirmHotelOrder(order.getId(), admin);
        order = orderService.getById(order.getId());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertNotNull(order.getConfirmedTime());

        // 未到入住日：自动推进不应改变状态
        assertEquals(0, orderService.autoTransitionHotelOrders());
        assertEquals(OrderStatus.CONFIRMED, orderService.getById(order.getId()).getStatus());

        // 3. 模拟到入住日：直接构造“入住日=今天”的已确认订单验证推进
        OrderInfo todayOrder = insertConfirmedOrder(today, today.plusDays(2), new BigDecimal("400.00"));
        int changed = orderService.autoTransitionHotelOrders();
        assertTrue(changed >= 1);
        assertEquals(OrderStatus.CHECKED_IN, orderService.getById(todayOrder.getId()).getStatus());
        assertNotNull(orderService.getById(todayOrder.getId()).getCheckedInTime());

        // 4. 离店日 -> 已结束：构造入住日为昨天、离店日为今天的入住中订单
        OrderInfo checkoutOrder = insertConfirmedOrder(today.minusDays(1), today, new BigDecimal("200.00"));
        orderService.autoTransitionHotelOrders();
        OrderInfo ended = orderService.getById(checkoutOrder.getId());
        assertEquals(OrderStatus.FINISHED, ended.getStatus());
        assertNotNull(ended.getFinishedTime());
        assertNotNull(ended.getCheckedInTime(), "错过入住日推进时应补记入住中步骤");

        // 流转记录完整且注明触发人
        List<OrderStatusLog> logs = orderService.listStatusLogs(checkoutOrder.getId(), admin, true);
        assertTrue(logs.size() >= 4, "提交/确认/补入住/结束至少4条");
        assertEquals(OrderStatus.TRIGGER_USER, logs.get(0).getTriggerType());
        assertEquals(zhangsan.getNickname(), logs.get(0).getTriggerName());
        assertEquals(OrderStatus.TRIGGER_ADMIN, logs.get(1).getTriggerType());
        assertTrue(logs.stream().anyMatch(l -> OrderStatus.TRIGGER_SYSTEM.equals(l.getTriggerType())
                && "系统".equals(l.getTriggerName())));
    }

    @Test
    void reschedule_pendingConfirm_isFree_usingOriginalRate() {
        LocalDate today = LocalDate.now();
        OrderInfo order = orderService.createOrder(buildHotelOrder(today.plusDays(5), today.plusDays(7), 2), zhangsan);
        // 待确认免费改期：原 2 晚 400 元 -> 3 晚，按原间夜单价 200 折算 = 600
        OrderInfo updated = orderService.rescheduleHotelOrder(order.getId(), today.plusDays(5), today.plusDays(8), zhangsan);
        assertEquals(new BigDecimal("600.00"), updated.getAmount());
        assertEquals(1, updated.getRescheduleCount());

        // 改期记录写入且状态保持
        List<OrderStatusLog> logs = logMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, order.getId()));
        assertTrue(logs.stream().anyMatch(l -> l.getRemark() != null && l.getRemark().contains("免费改期")));
    }

    @Test
    void reschedule_confirmed_recalculatesWithCurrentHotelPrice() {
        LocalDate today = LocalDate.now();
        OrderInfo order = orderService.createOrder(buildHotelOrder(today.plusDays(5), today.plusDays(6), 1), zhangsan);
        orderService.confirmHotelOrder(order.getId(), admin);
        // 酒店涨价到 300
        Hotel hotel = hotelMapper.selectById(hotelId);
        hotel.setPrice(new BigDecimal("300.00"));
        hotelMapper.updateById(hotel);
        // 改到 3 晚：300 × 3 = 900
        OrderInfo updated = orderService.rescheduleHotelOrder(order.getId(), today.plusDays(6), today.plusDays(9), zhangsan);
        assertEquals(new BigDecimal("900.00"), updated.getAmount());
        assertTrue(orderService.listStatusLogs(order.getId(), zhangsan, false).stream()
                .anyMatch(l -> l.getRemark() != null && l.getRemark().contains("按新的间夜重算房价")));
    }

    @Test
    void reschedule_afterCheckInDate_isRejected() {
        LocalDate today = LocalDate.now();
        OrderInfo order = insertConfirmedOrder(today.minusDays(2), today.minusDays(1), new BigDecimal("200.00"));
        orderService.autoTransitionHotelOrders(); // -> FINISHED
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.rescheduleHotelOrder(order.getId(), today.plusDays(1), today.plusDays(2), zhangsan));
        assertTrue(ex.getMessage().contains("不可改期"));
    }

    @Test
    void cancel_rules_checkedInCannotCancel() {
        LocalDate today = LocalDate.now();
        // 入住日当天的已确认订单：到达入住日后不可取消
        OrderInfo order = insertConfirmedOrder(today, today.plusDays(1), new BigDecimal("200.00"));
        orderService.autoTransitionHotelOrders(); // -> CHECKED_IN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.cancelOrder(order.getId(), zhangsan));
        assertTrue(ex.getMessage().contains("不可取消"));
        assertEquals(OrderStatus.CHECKED_IN, orderService.getById(order.getId()).getStatus());

        // 已确认但入住日在未来：可取消，且记录取消时间与后台触发人
        OrderInfo future = orderService.createOrder(buildHotelOrder(today.plusDays(2), today.plusDays(3), 1), zhangsan);
        orderService.confirmHotelOrder(future.getId(), admin);
        orderService.adminCancel(future.getId(), admin);
        OrderInfo cancelled = orderService.getById(future.getId());
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledTime());
        assertTrue(orderService.listStatusLogs(future.getId(), admin, true).stream()
                .anyMatch(l -> OrderStatus.TRIGGER_ADMIN.equals(l.getTriggerType())
                        && OrderStatus.CANCELLED.equals(l.getToStatus())));
    }

    @Test
    void concurrentReschedule_onCheckInDay_onlyEarliestAccepted() throws Exception {
        LocalDate today = LocalDate.now();
        // 入住日 = 今天，已确认状态
        OrderInfo order = insertConfirmedOrder(today, today.plusDays(2), new BigDecimal("400.00"));

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        java.util.List<String> otherErrors = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    orderService.rescheduleHotelOrder(order.getId(),
                            today.plusDays(idx + 1), today.plusDays(idx + 3), zhangsan);
                    success.incrementAndGet();
                } catch (RuntimeException e) {
                    if (e.getMessage() != null && (e.getMessage().contains("更早的改期请求")
                            || e.getMessage().contains("已完成一次改期"))) {
                        rejected.incrementAndGet();
                    } else {
                        otherErrors.add(e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        pool.shutdown();
        if (!otherErrors.isEmpty()) {
            fail("存在非预期的并发改期异常: " + otherErrors);
        }

        assertEquals(1, success.get(), "并发改期只有最早一次被接受");
        assertEquals(threads - 1, rejected.get(), "其余并发请求应全部被拒绝");
        OrderInfo finalOrder = orderService.getById(order.getId());
        assertEquals(1, finalOrder.getRescheduleCount());
        assertEquals(today, finalOrder.getLastRescheduleDate());
    }

    @Test
    void rescheduleInfo_reportsEligibility() {
        LocalDate today = LocalDate.now();
        OrderInfo order = orderService.createOrder(buildHotelOrder(today.plusDays(2), today.plusDays(3), 1), zhangsan);
        Map<String, Object> info = orderService.getRescheduleInfo(order.getId(), zhangsan);
        assertEquals(true, info.get("canReschedule"));
        assertEquals(true, info.get("freeReschedule"));

        // 别人无权查询
        User other = new User();
        other.setId(9999L);
        other.setUsername("lisi");
        assertThrows(RuntimeException.class, () -> orderService.getRescheduleInfo(order.getId(), other));
    }
}
