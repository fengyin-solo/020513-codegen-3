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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/create")
    public Result<OrderInfo> create(@RequestParam String orderType,
                                     @RequestParam Long targetId,
                                     @RequestParam String targetName,
                                     @RequestParam(required = false) BigDecimal amount,
                                     @RequestParam(defaultValue = "1") Integer quantity,
                                     @RequestParam(required = false) String checkInDate,
                                     @RequestParam(required = false) String checkOutDate,
                                     HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        if (Constants.ORDER_HOTEL.equals(orderType)) {
            // 酒店预订：服务端校验日期并按间夜重算金额，订单进入待确认状态
            OrderInfo hotelOrder = orderService.createHotelOrder(user, targetId, quantity, checkInDate, checkOutDate);
            return Result.success("预订成功，等待酒店确认", hotelOrder);
        }
        OrderInfo order = new OrderInfo();
        order.setUserId(user.getId());
        order.setOrderType(orderType);
        order.setTargetId(targetId);
        order.setTargetName(targetName);
        order.setAmount(amount);
        order.setQuantity(quantity);
        return Result.success("下单成功", orderService.createOrder(order));
    }

    @GetMapping("/cancel")
    public Result<String> cancel(@RequestParam Long orderId, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        orderService.cancelOrder(orderId, user.getId());
        return Result.success("取消成功", null);
    }

    @GetMapping("/pay")
    public Result<String> pay(@RequestParam Long orderId,
                               @RequestParam String payMethod,
                               HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        orderService.payOrder(orderId, payMethod, user.getId());
        return Result.success("支付成功（模拟）", null);
    }

    @GetMapping("/refund")
    public Result<String> refund(@RequestParam Long orderId, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        orderService.refundOrder(orderId, user.getId());
        return Result.success("退款成功（模拟）", null);
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
    public Result<OrderInfo> detail(@RequestParam Long id) {
        return Result.success(orderService.getById(id));
    }

    /** 酒店订单改期信息：可改范围由服务端统一判定后返回给前端 */
    @GetMapping("/rescheduleInfo")
    public Result<Map<String, Object>> rescheduleInfo(@RequestParam Long orderId, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(orderService.getRescheduleInfo(orderId, user.getId()));
    }

    /** 酒店订单改期：待确认免费改期，已确认按新间夜重算房价 */
    @GetMapping("/reschedule")
    public Result<String> reschedule(@RequestParam Long orderId,
                                      @RequestParam String checkInDate,
                                      @RequestParam String checkOutDate,
                                      HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        orderService.rescheduleOrder(orderId, user.getId(), checkInDate, checkOutDate);
        return Result.success("改期成功", null);
    }

    /** 订单状态变更日志（每一步变更均含变更时间与触发人） */
    @GetMapping("/logs")
    public Result<List<OrderStatusLog>> logs(@RequestParam Long orderId, HttpSession session) {
        User user = (User) session.getAttribute(Constants.SESSION_USER);
        if (user == null) return Result.error(401, "请先登录");
        OrderInfo order = orderService.getById(orderId);
        if (order == null) return Result.error("订单不存在");
        if (!order.getUserId().equals(user.getId())) return Result.error("无权查看此订单");
        return Result.success(orderService.listOrderLogs(orderId));
    }
}
