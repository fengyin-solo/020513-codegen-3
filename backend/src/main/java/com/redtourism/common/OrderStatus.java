package com.redtourism.common;

/**
 * 订单状态与状态流转触发人常量。
 *
 * 普通订单（景点/线路/美食）沿用原有流程：PENDING 待支付 -> PAID 已支付 -> COMPLETED 已完成，
 * 可被取消为 CANCELLED 或退款为 REFUNDED。
 *
 * 酒店订单采用完整的预订状态流转：
 * PENDING_CONFIRM 待确认 -> CONFIRMED 已确认 -> CHECKED_IN 入住中 -> FINISHED 已结束（离店记录），
 * 入住日之前可取消为 CANCELLED。
 */
public class OrderStatus {

    /* ========== 通用/普通订单状态 ========== */
    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String REFUNDED = "REFUNDED";

    /* ========== 酒店订单状态 ========== */
    /** 待确认（订单提交后，等待酒店确认） */
    public static final String PENDING_CONFIRM = "PENDING_CONFIRM";
    /** 已确认（酒店确认后） */
    public static final String CONFIRMED = "CONFIRMED";
    /** 入住中（入住日当天自动转入） */
    public static final String CHECKED_IN = "CHECKED_IN";
    /** 已结束（离店后转入，作为结束记录留存） */
    public static final String FINISHED = "FINISHED";

    /* ========== 状态变更触发人类型 ========== */
    /** 用户本人触发 */
    public static final String TRIGGER_USER = "USER";
    /** 后台管理员/工作人员触发 */
    public static final String TRIGGER_ADMIN = "ADMIN";
    /** 系统定时任务自动触发 */
    public static final String TRIGGER_SYSTEM = "SYSTEM";
    public static final String SYSTEM_NAME = "系统";
}
