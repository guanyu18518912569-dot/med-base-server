package med.base.server.enums;

/**
 * 会员等级枚举
 * 使用枚举避免汉字编码问题
 */
public enum MemberLevel {

    /** 普通会员 */
    NORMAL(0, "\u666e\u901a\u4f1a\u5458"),

    /** VIP会员 */
    VIP(1, "VIP\u4f1a\u5458"),

    /** 高级VIP */
    SENIOR_VIP(2, "\u9ad8\u7ea7VIP");

    private final int code;
    private final String desc;

    MemberLevel(int code, String desc) {
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
     * 根据等级码获取描述
     */
    public static String getDescByCode(Integer level) {
        if (level == null) {
            return NORMAL.getDesc();
        }
        for (MemberLevel memberLevel : MemberLevel.values()) {
            if (memberLevel.getCode() == level) {
                return memberLevel.getDesc();
            }
        }
        return NORMAL.getDesc();
    }
}
