package com.jdclone.mall.common;

public final class Constants {
    private Constants() {
    }

    public static final String ROLE_USER = "USER";
    public static final String ROLE_MERCHANT = "MERCHANT";
    public static final String ROLE_SERVICE_ADMIN = "SERVICE_ADMIN";
    public static final String ROLE_PRODUCT_ADMIN = "PRODUCT_ADMIN";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    public static final String PRODUCT_PENDING = "PENDING";
    public static final String PRODUCT_APPROVED = "APPROVED";
    public static final String PRODUCT_REJECTED = "REJECTED";
    public static final String SHELF_ON = "ON";
    public static final String SHELF_OFF = "OFF";

    public static final String ORDER_WAIT_PAY = "WAIT_PAY";
    public static final String ORDER_WAIT_SHIP = "WAIT_SHIP";
    public static final String ORDER_WAIT_RECEIVE = "WAIT_RECEIVE";
    public static final String ORDER_COMPLETED = "COMPLETED";
    public static final String ORDER_CANCELED = "CANCELED";
    public static final String ORDER_REFUNDING = "REFUNDING";
    public static final String ORDER_REFUNDED = "REFUNDED";

    public static final String REFUND_REVIEWING = "MERCHANT_REVIEWING";
    public static final String REFUND_APPROVED = "MERCHANT_APPROVED";
    public static final String REFUND_REJECTED = "MERCHANT_REJECTED";
    public static final String REFUND_WAIT_RETURN = "WAIT_USER_RETURN";
    public static final String REFUND_WAIT_RECEIVE = "WAIT_MERCHANT_RECEIVE";
    public static final String REFUND_PLATFORM = "PLATFORM_INTERVENING";
    public static final String REFUND_SUCCESS = "REFUND_SUCCESS";
    public static final String REFUND_FAILED = "REFUND_FAILED";
    public static final String REFUND_CLOSED = "CLOSED";
}
