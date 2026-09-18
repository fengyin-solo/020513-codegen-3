package com.redtourism.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 订单状态变更日志：记录每一步状态变更的变更时间与触发人
 */
@Data
@TableName("order_status_log")
public class OrderStatusLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    /** 变更动作：CREATE/CONFIRM/RESCHEDULE/CHECKIN/FINISH/CANCEL */
    private String action;
    private String fromStatus;
    private String toStatus;
    /** 触发人类型：USER/ADMIN/STAFF/SYSTEM */
    private String operatorType;
    private Long operatorId;
    private String operatorName;
    /** 变更说明（如改期前后日期与金额） */
    private String detail;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
