package med.base.server.enums;

/**
 * 订单状态枚举
 * 使用枚举避免汉字编码问题
 */
public enum OrderStatus {
    
    /** 待付款 */
    TO_PAY(0, "\u5f85\u4ed8\u6b3e"),
    
    /** 待发货 */
    TO_DELIVER(1, "\u5f85\u53d1\u8d27"),
    
    /** 待收货 */
    TO_RECEIVE(2, "\u5f85\u6536\u8d27"),
    
    /** 已完成 */
    COMPLETED(3, "\u5df2\u5b8c\u6210"),
    
    /** 已取消 */
    CANCELLED(4, "\u5df2\u53d6\u6d88"),
    
    /** 售后中 */
    AFTER_SALE(5, "\u552e\u540e\u4e2d"),
    
    /** 未知 */
    UNKNOWN(-1, "\u672a\u77e5");
    
    private final int code;
    private final String desc;
    
    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDesc() {
        return desc;
    }
    
    /**
     * 根据状态码获取描述
     * @param status 状态码
     * @return 状态描述
     */
    public static String getDescByCode(Integer status) {
        if (status == null) {
            return UNKNOWN.getDesc();
        }
        for (OrderStatus orderStatus : OrderStatus.values()) {
            if (orderStatus.getCode() == status) {
                return orderStatus.getDesc();
            }
        }
        return UNKNOWN.getDesc();
    }
    
    /**
     * 根据状态码获取枚举
     * @param status 状态码
     * @return 枚举对象
     */
    public static OrderStatus getByCode(Integer status) {
        if (status == null) {
            return UNKNOWN;
        }
        for (OrderStatus orderStatus : OrderStatus.values()) {
            if (orderStatus.getCode() == status) {
                return orderStatus;
            }
        }
        return UNKNOWN;
    }
}
