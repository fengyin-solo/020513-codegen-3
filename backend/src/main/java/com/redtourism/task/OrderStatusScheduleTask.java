package com.redtourism.task;

import com.redtourism.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 酒店订单状态自动推进定时任务。
 * 到入住日当天：已确认 -> 入住中；
 * 到离店日：入住中（含未及时推进的已确认）-> 已结束。
 * 同时列表/详情查询也有惰性推进兜底，定时任务保证无人访问时状态也能准时变更。
 */
@Slf4j
@Component
public class OrderStatusScheduleTask {

    @Autowired
    private OrderService orderService;

    /** 每 10 分钟扫描一次（变更记录的触发人记为“系统”）。 */
    @Scheduled(cron = "0 */10 * * * ?")
    public void autoTransition() {
        try {
            int changed = orderService.autoTransitionHotelOrders();
            if (changed > 0) {
                log.info("酒店订单状态自动推进完成，本次变更 {} 单", changed);
            }
        } catch (Exception e) {
            log.error("酒店订单状态自动推进失败", e);
        }
    }
}
