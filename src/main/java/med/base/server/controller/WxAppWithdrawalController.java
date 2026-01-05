package med.base.server.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import med.base.server.common.DefaultResponse;
import med.base.server.model.UmsWithdrawal;
import med.base.server.service.UmsWithdrawalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 微信小程序端 - 提现管理接口
 */
@RestController
@RequestMapping("/wxapp/withdrawal")
public class WxAppWithdrawalController {

    @Autowired
    private UmsWithdrawalService withdrawalService;

    /**
     * 获取用户收益统计
     */
    @GetMapping("/income-stats")
    public String getIncomeStats(@RequestParam String userId) {
        try {
            Map<String, Object> stats = withdrawalService.getUserIncomeStats(userId);
            return DefaultResponse.success(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取收益统计失败：" + e.getMessage());
        }
    }

    /**
     * 申请提现
     */
    @PostMapping("/apply")
    public String applyWithdrawal(@RequestBody WxWithdrawalRequest request) {
        try {
            // 验证参数
            if (request.getOpenid() == null || request.getOpenid().isEmpty()) {
                return DefaultResponse.error("openid不能为空");
            }
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return DefaultResponse.error("提现金额必须大于0");
            }
            if (request.getPaymentMethod() == null) {
                return DefaultResponse.error("请选择提现方式");
            }
            if (request.getPaymentAccount() == null || request.getPaymentAccount().isEmpty()) {
                return DefaultResponse.error("收款账号不能为空");
            }

            UmsWithdrawal withdrawal = withdrawalService.applyWithdrawalByOpenid(
                    request.getOpenid(),
                    request.getAmount(),
                    request.getPaymentMethod(),
                    request.getPaymentAccount()
            );

            WxWithdrawalResponse response = new WxWithdrawalResponse();
            response.setWithdrawalId(withdrawal.getWithdrawalId());
            response.setAmount(withdrawal.getAmount());
            response.setStatus(withdrawal.getStatus());
            response.setStatusDesc(getStatusDesc(withdrawal.getStatus()));
            response.setCreatedTime(withdrawal.getCreatedTime() != null ? withdrawal.getCreatedTime().toString() : null);

            return DefaultResponse.success(response);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("申请提现失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户提现记录
     */
    @GetMapping("/list")
    public String getUserWithdrawals(@RequestParam String userId) {
        try {
            List<UmsWithdrawal> withdrawals = withdrawalService.getUserWithdrawals(userId);

            List<WxWithdrawalVO> voList = withdrawals.stream().map(w -> {
                WxWithdrawalVO vo = new WxWithdrawalVO();
                vo.setWithdrawalId(w.getWithdrawalId());
                vo.setAmount(w.getAmount());
                vo.setStatus(w.getStatus());
                vo.setStatusDesc(getStatusDesc(w.getStatus()));
                vo.setPaymentMethod(w.getPaymentMethod());
                vo.setPaymentMethodDesc(getPaymentMethodDesc(w.getPaymentMethod()));
                vo.setRemark(w.getRemark());
                vo.setCreatedTime(w.getCreatedTime() != null ? w.getCreatedTime().toString() : null);
                vo.setReviewTime(w.getReviewTime() != null ? w.getReviewTime().toString() : null);
                vo.setPaymentTime(w.getPaymentTime() != null ? w.getPaymentTime().toString() : null);
                return vo;
            }).collect(java.util.stream.Collectors.toList());

            return DefaultResponse.success(voList);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取提现记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取提现申请详情
     */
    @GetMapping("/detail/{withdrawalId}")
    public String getWithdrawalDetail(@PathVariable String withdrawalId) {
        try {
            UmsWithdrawal withdrawal = withdrawalService.getUserWithdrawals(null).stream()
                    .filter(w -> w.getWithdrawalId().equals(withdrawalId))
                    .findFirst()
                    .orElse(null);

            if (withdrawal == null) {
                return DefaultResponse.error("提现申请不存在");
            }

            WxWithdrawalVO vo = new WxWithdrawalVO();
            vo.setWithdrawalId(withdrawal.getWithdrawalId());
            vo.setAmount(withdrawal.getAmount());
            vo.setBeforeBalance(withdrawal.getBeforeBalance());
            vo.setAfterBalance(withdrawal.getAfterBalance());
            vo.setStatus(withdrawal.getStatus());
            vo.setStatusDesc(getStatusDesc(withdrawal.getStatus()));
            vo.setPaymentMethod(withdrawal.getPaymentMethod());
            vo.setPaymentMethodDesc(getPaymentMethodDesc(withdrawal.getPaymentMethod()));
            vo.setPaymentAccount(withdrawal.getPaymentAccount());
            vo.setRemark(withdrawal.getRemark());
            vo.setCreatedTime(withdrawal.getCreatedTime() != null ? withdrawal.getCreatedTime().toString() : null);
            vo.setReviewTime(withdrawal.getReviewTime() != null ? withdrawal.getReviewTime().toString() : null);
            vo.setPaymentTime(withdrawal.getPaymentTime() != null ? withdrawal.getPaymentTime().toString() : null);

            return DefaultResponse.success(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取提现详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待审核";
            case 1: return "已通过";
            case 2: return "已拒绝";
            case 3: return "已完成";
            default: return "未知";
        }
    }

    /**
     * 获取提现方式描述
     */
    private String getPaymentMethodDesc(Integer method) {
        if (method == null) return "未知";
        switch (method) {
            case 1: return "微信";
            case 2: return "支付宝";
            case 3: return "银行卡";
            default: return "未知";
        }
    }
}

// ==================== DTO 类 ====================

@Data
class WxWithdrawalRequest {
    private String openid;
    private BigDecimal amount;
    private Integer paymentMethod;   // 1-微信 2-支付宝 3-银行卡
    private String paymentAccount;   // 收款账号信息JSON
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxWithdrawalResponse {
    private String withdrawalId;
    private BigDecimal amount;
    private Integer status;
    private String statusDesc;
    private String createdTime;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxWithdrawalVO {
    private String withdrawalId;
    private BigDecimal amount;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private Integer status;
    private String statusDesc;
    private Integer paymentMethod;
    private String paymentMethodDesc;
    private String paymentAccount;
    private String remark;
    private String createdTime;
    private String reviewTime;
    private String paymentTime;
}
