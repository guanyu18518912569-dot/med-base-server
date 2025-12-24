package med.base.server.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员用户表
 * 支持多级推荐关系，用于直销电商业绩统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UmsUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 8617607551196313129L;

    @TableId
    private String umsUserId;

    private String nickName;
    private String openid;
    private String unionid;
    private String photoUrl;
    private String gender;
    private String birthday;
    private String country;
    private String province;
    private String city;
    
    // 账户余额（可提现金额）
    private BigDecimal account;
    
    // 微信 session_key
    private String sessionKey;
    
    // ========== 推荐关系 ==========
    
    // 直接推荐人ID（上级，一级）
    private String parentId;
    
    // 推荐路径（存储从根到当前用户的完整路径，用逗号分隔）
    // 例如: "U001,U002,U003" 表示 U001 推荐了 U002，U002 推荐了 U003（当前用户）
    // 用于快速查询某用户的所有下级团队
    private String parentPath;
    
    // 推荐层级（当前用户是第几级，顶级为0）
    private Integer level;
    
    // ========== 业绩统计（定期更新或实时计算）==========
    
    // 直推人数（一级下线数量）
    private Integer directCount;
    
    // 团队总人数（所有下级数量）
    private Integer teamCount;
    
    // 个人消费总额（自己的消费）
    private BigDecimal selfConsumption;
    
    // 直推业绩（一级下线的消费总额，用于计算收益）
    private BigDecimal directPerformance;
    
    // 团队业绩（所有下级的消费总额）
    private BigDecimal teamPerformance;
    
    // 累计收益（历史总收益）
    private BigDecimal totalIncome;
    
    // 积分（每消费1元=1积分）
    private Long points;
    
    // ========== 会员等级 ==========
    
    // 会员等级 (0-普通会员, 1-VIP, 2-高级VIP 等，可根据业务扩展)
    private Integer memberLevel;
    
    // 会员状态 (0-正常, 1-冻结, 2-注销)
    private Integer status;
    
    // 邀请码（用于分享推荐）
    private String inviteCode;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
