package com.redtourism.task;

import com.redtourism.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 酒店订单状态自动流转：
 * 入住日当天 已确认 -> 入住中；离店之后 入住中 -> 结束记录。
 */
@Slf4j
@Component
public class HotelOrderTask {

    @Autowired
    private OrderService orderService;

    /** 每分钟扫描一次，保证入住日/离店日的状态及时推进 */
    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void autoTransitionHotelOrders() {
        try {
            orderService.autoTransitionHotelOrders();
        } catch (Exception e) {
            log.error("酒店订单状态自动流转失败: ", e);
        }
    }
}
