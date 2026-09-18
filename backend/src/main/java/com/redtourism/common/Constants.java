package com.redtourism.common;

public class Constants {
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STAFF = "STAFF";
    public static final String SESSION_USER = "currentUser";

    public static final String TARGET_SPOT = "SPOT";
    public static final String TARGET_ROUTE = "ROUTE";
    public static final String TARGET_CULTURE = "CULTURE";
    public static final String TARGET_HOTEL = "HOTEL";
    public static final String TARGET_FOOD = "FOOD";
    public static final String TARGET_COMMENT = "COMMENT";

    public static final String ORDER_HOTEL = "HOTEL";
    public static final String ORDER_FOOD = "FOOD";

    /** 酒店订单状态流转：待确认 -> 已确认 -> 入住中 -> 结束记录 */
    public static final String HOTEL_ORDER_PENDING_CONFIRM = "PENDING_CONFIRM";
    public static final String HOTEL_ORDER_CONFIRMED = "CONFIRMED";
    public static final String HOTEL_ORDER_CHECKED_IN = "CHECKED_IN";
    public static final String HOTEL_ORDER_COMPLETED = "COMPLETED";
    public static final String HOTEL_ORDER_CANCELLED = "CANCELLED";

    /** 订单状态变更日志动作 */
    public static final String ORDER_LOG_CREATE = "CREATE";
    public static final String ORDER_LOG_CONFIRM = "CONFIRM";
    public static final String ORDER_LOG_RESCHEDULE = "RESCHEDULE";
    public static final String ORDER_LOG_CHECKIN = "CHECKIN";
    public static final String ORDER_LOG_FINISH = "FINISH";
    public static final String ORDER_LOG_CANCEL = "CANCEL";

    /** 订单状态变更触发人类型 */
    public static final String OPERATOR_USER = "USER";
    public static final String OPERATOR_ADMIN = "ADMIN";
    public static final String OPERATOR_STAFF = "STAFF";
    public static final String OPERATOR_SYSTEM = "SYSTEM";

    /** 酒店订单改期规则（服务端统一判定） */
    public static final int RESCHEDULE_MAX_DAYS_AHEAD = 90;
    public static final int RESCHEDULE_MAX_NIGHTS = 30;

    public static final String PAY_WECHAT = "WECHAT";
    public static final String PAY_BANK_ICBC = "BANK_ICBC";
    public static final String PAY_BANK_CCB = "BANK_CCB";
    public static final String PAY_BANK_ABC = "BANK_ABC";
    public static final String PAY_BANK_BOC = "BANK_BOC";
    public static final String PAY_BANK_BOCOM = "BANK_BOCOM";
    public static final String PAY_BANK_CMB = "BANK_CMB";
    public static final String PAY_BANK_PSBC = "BANK_PSBC";

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;
}
