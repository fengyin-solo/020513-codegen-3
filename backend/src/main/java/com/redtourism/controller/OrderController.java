package com.redtourism.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.redtourism.common.Constants;
import com.redtourism.common.Result;
import com.redtourism.entity.OrderInfo;
import com.redtourism.entity.OrderStatusLog;
import com.redtourism.entity.User;
import com.redtourism.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private OrderService orderService;

    @GetMapping("/create")
    public Result<OrderInfo> create(@RequestParam String orderType,
                                     @RequestParam Long targetId,
                                     @RequestParam String targetName,
                                     @RequestParam BigDecimal amount,
                                     @RequestParam(defaultValue = "1") Integer quantity,
                                     @RequestParam(required = false) String checkInDate,
                                     @RequestParam(required = false) String checkOutDate,
                                     HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        OrderInfo order = new OrderInfo();
        order.setUserId(user.getId());
        order.setOrderType(orderType);
        order.setTargetId(targetId);
        order.setTargetName(targetName);
        order.setAmount(amount);
        order.setQuantity(quantity);
        if (checkInDate != null && !checkInDate.isEmpty()) {
            order.setCheckInDate(LocalDate.parse(checkInDate, DATE_FMT));
        }
        if (checkOutDate != null && !checkOutDate.isEmpty()) {
            order.setCheckOutDate(LocalDate.parse(checkOutDate, DATE_FMT));
        }
        return Result.success("下单成功", orderService.createOrder(order, user));
    }

    @GetMapping("/cancel")
    public Result<String> cancel(@RequestParam Long orderId, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        orderService.cancelOrder(orderId, user);
        return Result.success("取消成功", null);
    }

    @GetMapping("/pay")
    public Result<String> pay(@RequestParam Long orderId,
                               @RequestParam String payMethod,
                               HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        orderService.payOrder(orderId, payMethod, user);
        return Result.success("支付成功（模拟）", null);
    }

    @GetMapping("/refund")
    public Result<String> refund(@RequestParam Long orderId, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        orderService.refundOrder(orderId, user);
        return Result.success("退款成功（模拟）", null);
    }

    /**
     * 酒店订单改期。可改范围完全由服务端统一判定：
     * 待确认免费改期；已确认按新间夜重算房价；跨过入住日不可改；入住日当天并发只接受最早一次。
     */
    @GetMapping("/reschedule")
    public Result<OrderInfo> reschedule(@RequestParam Long orderId,
                                         @RequestParam String newCheckInDate,
                                         @RequestParam String newCheckOutDate,
                                         HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        LocalDate newIn = LocalDate.parse(newCheckInDate, DATE_FMT);
        LocalDate newOut = LocalDate.parse(newCheckOutDate, DATE_FMT);
        OrderInfo order = orderService.rescheduleHotelOrder(orderId, newIn, newOut, user);
        return Result.success("改期成功", order);
    }

    /** 改期资格与可改范围预判（返回可改标记、原因、间夜单价、预估金额等）。 */
    @GetMapping("/rescheduleInfo")
    public Result<Map<String, Object>> rescheduleInfo(@RequestParam Long orderId, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(orderService.getRescheduleInfo(orderId, user));
    }

    /** 查询订单的状态流转记录（每一步变更时间与触发人）。 */
    @GetMapping("/statusLogs")
    public Result<List<OrderStatusLog>> statusLogs(@RequestParam Long orderId, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(orderService.listStatusLogs(orderId, user, false));
    }

    @GetMapping("/myList")
    public Result<IPage<OrderInfo>> myList(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String orderType,
                                            @RequestParam(required = false) String status,
                                            HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(orderService.listUserOrders(page, size, user.getId(), orderType, status));
    }

    @GetMapping("/detail")
    public Result<OrderInfo> detail(@RequestParam Long id, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        // 详情打开时先按日期惰性推进，保证后台/用户看到的是最新状态
        orderService.lazyTransition(id);
        OrderInfo order = orderService.getById(id);
        if (order != null && !order.getUserId().equals(user.getId())) {
            return Result.error(403, "无权查看此订单");
        }
        return Result.success(order);
    }
}
