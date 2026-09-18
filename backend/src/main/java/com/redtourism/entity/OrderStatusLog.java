package com.redtourism.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 订单状态流转记录。
 * 每一次状态变更（提交/酒店确认/入住/离店/取消/改期）都保留一条记录，
 * 记录变更时间（create_time）与触发人（trigger_type/trigger_id/trigger_name）。
 */
@Data
@TableName("order_status_log")
public class OrderStatusLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    /** 变更前状态（下单时为 null） */
    private String fromStatus;
    /** 变更后状态 */
    private String toStatus;
    /** 触发类型：USER（用户）/ ADMIN（后台）/ SYSTEM（系统定时任务） */
    private String triggerType;
    private Long triggerId;
    private String triggerName;
    /** 备注：改期前后日期、房价重算说明、取消原因等 */
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
