package med.base.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户提现申请表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ums_withdrawal")
public class UmsWithdrawal implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String withdrawalId;

    // 用户ID
    private String userId;

    // 用户昵称
    private String nickName;

    // 提现金额（元）
    private BigDecimal amount;

    // 提现前账户余额（元）
    private BigDecimal beforeBalance;

    // 提现后账户余额（元）
    private BigDecimal afterBalance;

    // 申请状态：0-待审核 1-已通过 2-已拒绝 3-已完成（已打款）
    private Integer status;

    // 审核备注
    private String remark;

    // 审核人ID
    private String reviewerId;

    // 审核人姓名
    private String reviewerName;

    // 审核时间
    private LocalDateTime reviewTime;

    // 打款时间
    private LocalDateTime paymentTime;

    // 收款方式：1-微信 2-支付宝 3-银行卡
    private Integer paymentMethod;

    // 收款账号信息（JSON）
    private String paymentAccount;

    // 申请时间
    private LocalDateTime createdTime;

    // 更新时间
    private LocalDateTime updatedTime;
}
