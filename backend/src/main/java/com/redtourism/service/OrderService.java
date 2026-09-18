package com.redtourism.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.redtourism.entity.OrderInfo;
import com.redtourism.entity.OrderStatusLog;
import com.redtourism.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderInfo> {

    /** 创建订单。酒店订单提交后为“待确认”，普通订单为“待支付”，并写入第一条状态流转记录。 */
    OrderInfo createOrder(OrderInfo order, User operator);

    /** 用户取消订单。 */
    boolean cancelOrder(Long orderId, User operator);

    /** 模拟支付（仅普通订单的待支付状态可用；酒店订单不走支付流程）。 */
    boolean payOrder(Long orderId, String payMethod, User operator);

    /** 申请退款（普通订单）。 */
    boolean refundOrder(Long orderId, User operator);

    /** 后台确认酒店订单：待确认 -> 已确认。 */
    boolean confirmHotelOrder(Long orderId, User operator);

    /**
     * 改期（仅酒店订单，可改范围由服务端统一判定）。
     * 待确认阶段免费改期；已确认阶段按新的间夜重算房价。
     * 跨过入住日不允许改期；入住日当天并发改期只接受最早一次（事务 + 行锁保证）。
     */
    OrderInfo rescheduleHotelOrder(Long orderId, LocalDate newCheckIn, LocalDate newCheckOut, User operator);

    /**
     * 查询改期资格（服务端统一判定可改范围）。
     * 返回 canReschedule、reason、nights、unitPrice、estimatedAmount、rescheduleCount。
     */
    Map<String, Object> getRescheduleInfo(Long orderId, User operator);

    /** 按当前日期推进酒店订单状态（入住日转入住中、离店日转已结束）。返回实际变更的订单数。 */
    int autoTransitionHotelOrders();

    /** 对单个酒店订单做惰性状态推进（在列表/详情查询时兜底）。 */
    void lazyTransition(Long orderId);

    /** 我的订单分页（自动推进酒店订单状态后返回；status 支持逗号分隔多状态）。 */
    IPage<OrderInfo> listUserOrders(int page, int size, Long userId, String orderType, String status);

    /** 后台订单分页。 */
    IPage<OrderInfo> listAllOrders(int page, int size, String orderType, String status);

    /** 查询订单的状态流转记录（用户只能查自己的订单）。 */
    List<OrderStatusLog> listStatusLogs(Long orderId, User operator, boolean isAdmin);

    /** 后台取消订单（取消规则与用户侧一致，触发人记为后台操作人）。 */
    void adminCancel(Long orderId, User operator);

    /** 后台退款（仅普通订单的已支付状态）。 */
    void adminRefund(Long orderId, User operator);

    /** 后台标记完成（仅普通订单的已支付状态；酒店订单由系统按日期自动推进）。 */
    void adminComplete(Long orderId, User operator);
}
