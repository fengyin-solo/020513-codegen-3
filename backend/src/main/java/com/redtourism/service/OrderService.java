package com.redtourism.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.redtourism.entity.OrderInfo;
import com.redtourism.entity.OrderStatusLog;
import com.redtourism.entity.User;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderInfo> {
    OrderInfo createOrder(OrderInfo order);
    boolean cancelOrder(Long orderId, Long userId);
    boolean payOrder(Long orderId, String payMethod, Long userId);
    boolean refundOrder(Long orderId, Long userId);
    IPage<OrderInfo> listUserOrders(int page, int size, Long userId, String orderType, String status);
    IPage<OrderInfo> listAllOrders(int page, int size, String orderType, String status);

    /** 创建酒店预订订单：服务端校验日期并按间夜重算金额，初始状态为待确认 */
    OrderInfo createHotelOrder(User user, Long hotelId, Integer quantity, String checkInDate, String checkOutDate);

    /** 查询酒店订单的可改期信息（可改范围由服务端统一判定） */
    Map<String, Object> getRescheduleInfo(Long orderId, Long userId);

    /** 用户改期：待确认免费改期，已确认按新间夜重算房价；乐观锁保证并发改期只接受最早一次 */
    void rescheduleOrder(Long orderId, Long userId, String checkInDate, String checkOutDate);

    /** 酒店（管理员）确认订单：待确认 -> 已确认 */
    void confirmOrder(Long orderId, User operator);

    /** 管理员取消订单（酒店订单走状态流转校验并记录日志） */
    void adminCancelOrder(Long orderId, User operator);

    /** 订单状态变更日志（每条含变更时间与触发人） */
    List<OrderStatusLog> listOrderLogs(Long orderId);

    /** 定时任务：入住日当天转为入住中，离店之后转为结束记录 */
    void autoTransitionHotelOrders();
}
