package com.redtourism.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("order_info")
public class OrderInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String orderType;
    private Long targetId;
    private String orderNo;
    private BigDecimal amount;
    private String status;
    private String payMethod;
    private Date payTime;
    private String targetName;
    private Integer quantity;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    /** 酒店订单状态流转时间点（每一步状态变更都保留变更时间） */
    private Date confirmedTime;
    private Date checkedInTime;
    private Date finishedTime;
    private Date cancelledTime;
    /** 累计改期次数 */
    private Integer rescheduleCount;
    /** 最近一次改期操作发生的自然日，用于“入住日当天并发改期只接受最早一次”判定 */
    private LocalDate lastRescheduleDate;
    /** 在“原入住日当天”完成过改期的自然日；当天后续（并发）改期一律拒绝 */
    private LocalDate checkinDayRescheduleDate;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
