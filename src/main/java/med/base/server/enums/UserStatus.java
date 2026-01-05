package med.base.server.enums;

/**
 * 用户状态枚举
 * 使用枚举避免汉字编码问题
 */
public enum UserStatus {

    /** 正常 */
    NORMAL(0, "\u6b63\u5e38"),

    /** 冻结 */
    FROZEN(1, "\u51bb\u7ed3"),

    /** 注销 */
    DELETED(2, "\u6ce8\u9500"),

    /** 未知 */
    UNKNOWN(-1, "\u672a\u77e5");

    private final int code;
    private final String desc;

    UserStatus(int code, String desc) {
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
     */
    public static String getDescByCode(Integer status) {
        if (status == null) {
            return UNKNOWN.getDesc();
        }
        for (UserStatus userStatus : UserStatus.values()) {
            if (userStatus.getCode() == status) {
                return userStatus.getDesc();
            }
        }
        return UNKNOWN.getDesc();
    }
}
