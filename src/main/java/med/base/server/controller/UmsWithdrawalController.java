package med.base.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.mapper.UmsWithdrawalMapper;
import med.base.server.model.UmsWithdrawal;
import med.base.server.service.UmsWithdrawalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理后台 - 提现审核接口
 */
@RestController
@RequestMapping("/admin/withdrawal")
public class UmsWithdrawalController {

    @Autowired
    private UmsWithdrawalService withdrawalService;

    @Autowired
    private UmsWithdrawalMapper withdrawalMapper;

    /**
     * 分页查询提现申请列表
     */
    @GetMapping("/list")
    @UserLoginToken
    public String list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String nickName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Page<UmsWithdrawal> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<UmsWithdrawal> wrapper = new LambdaQueryWrapper<>();

            // 状态筛选
            if (status != null) {
                wrapper.eq(UmsWithdrawal::getStatus, status);
            }

            // 用户昵称模糊查询
            if (StringUtils.hasText(nickName)) {
                wrapper.like(UmsWithdrawal::getNickName, nickName);
            }

            // 时间范围筛选
            if (StringUtils.hasText(startDate)) {
                wrapper.ge(UmsWithdrawal::getCreatedTime, LocalDateTime.parse(startDate + "T00:00:00"));
            }
            if (StringUtils.hasText(endDate)) {
                wrapper.le(UmsWithdrawal::getCreatedTime, LocalDateTime.parse(endDate + "T23:59:59"));
            }

            // 按创建时间倒序
            wrapper.orderByDesc(UmsWithdrawal::getCreatedTime);

            IPage<UmsWithdrawal> result = withdrawalMapper.selectPage(page, wrapper);

            // 转换为VO
            List<WithdrawalListVO> voList = result.getRecords().stream().map(w -> {
                WithdrawalListVO vo = new WithdrawalListVO();
                vo.setWithdrawalId(w.getWithdrawalId());
                vo.setUserId(w.getUserId());
                vo.setNickName(w.getNickName());
                vo.setAmount(w.getAmount());
                vo.setStatus(w.getStatus());
                vo.setStatusDesc(getStatusDesc(w.getStatus()));
                vo.setPaymentMethod(w.getPaymentMethod());
                vo.setPaymentMethodDesc(getPaymentMethodDesc(w.getPaymentMethod()));
                vo.setCreatedTime(w.getCreatedTime());
                vo.setReviewTime(w.getReviewTime());
                vo.setPaymentTime(w.getPaymentTime());
                vo.setReviewerName(w.getReviewerName());
                return vo;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("list", voList);
            data.put("total", result.getTotal());
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);

            return DefaultResponse.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("查询提现列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取提现申请详情
     */
    @GetMapping("/detail/{withdrawalId}")
    @UserLoginToken
    public String detail(@PathVariable String withdrawalId) {
        try {
            UmsWithdrawal withdrawal = withdrawalMapper.selectById(withdrawalId);
            if (withdrawal == null) {
                return DefaultResponse.error("提现申请不存在");
            }

            WithdrawalDetailVO vo = new WithdrawalDetailVO();
            vo.setWithdrawalId(withdrawal.getWithdrawalId());
            vo.setUserId(withdrawal.getUserId());
            vo.setNickName(withdrawal.getNickName());
            vo.setAmount(withdrawal.getAmount());
            vo.setBeforeBalance(withdrawal.getBeforeBalance());
            vo.setAfterBalance(withdrawal.getAfterBalance());
            vo.setStatus(withdrawal.getStatus());
            vo.setStatusDesc(getStatusDesc(withdrawal.getStatus()));
            vo.setPaymentMethod(withdrawal.getPaymentMethod());
            vo.setPaymentMethodDesc(getPaymentMethodDesc(withdrawal.getPaymentMethod()));
            vo.setPaymentAccount(withdrawal.getPaymentAccount());
            vo.setRemark(withdrawal.getRemark());
            vo.setReviewerId(withdrawal.getReviewerId());
            vo.setReviewerName(withdrawal.getReviewerName());
            vo.setCreatedTime(withdrawal.getCreatedTime());
            vo.setReviewTime(withdrawal.getReviewTime());
            vo.setPaymentTime(withdrawal.getPaymentTime());
            vo.setUpdatedTime(withdrawal.getUpdatedTime());

            return DefaultResponse.success(vo);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取提现详情失败：" + e.getMessage());
        }
    }

    /**
     * 审核提现申请
     */
    @PostMapping("/review")
    @UserLoginToken
    public String review(@RequestBody ReviewRequest request) {
        try {
            // 验证参数
            if (!StringUtils.hasText(request.getWithdrawalId())) {
                return DefaultResponse.error("提现ID不能为空");
            }
            if (request.getStatus() == null || (request.getStatus() != 1 && request.getStatus() != 2)) {
                return DefaultResponse.error("状态参数错误，1-通过 2-拒绝");
            }
            if (!StringUtils.hasText(request.getReviewerId())) {
                return DefaultResponse.error("审核人ID不能为空");
            }
            if (!StringUtils.hasText(request.getReviewerName())) {
                return DefaultResponse.error("审核人姓名不能为空");
            }

            withdrawalService.reviewWithdrawal(
                    request.getWithdrawalId(),
                    request.getStatus(),
                    request.getRemark(),
                    request.getReviewerId(),
                    request.getReviewerName()
            );

            return DefaultResponse.success("审核成功");
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("审核失败：" + e.getMessage());
        }
    }

    /**
     * 确认打款
     */
    @PostMapping("/confirm-payment/{withdrawalId}")
    @UserLoginToken
    public String confirmPayment(@PathVariable String withdrawalId) {
        try {
            withdrawalService.confirmPayment(withdrawalId);
            return DefaultResponse.success("打款确认成功");
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("确认打款失败：" + e.getMessage());
        }
    }

    /**
     * 获取提现统计数据
     */
    @GetMapping("/statistics")
    @UserLoginToken
    public String statistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            LambdaQueryWrapper<UmsWithdrawal> wrapper = new LambdaQueryWrapper<>();

            // 时间范围筛选
            if (StringUtils.hasText(startDate)) {
                wrapper.ge(UmsWithdrawal::getCreatedTime, LocalDateTime.parse(startDate + "T00:00:00"));
            }
            if (StringUtils.hasText(endDate)) {
                wrapper.le(UmsWithdrawal::getCreatedTime, LocalDateTime.parse(endDate + "T23:59:59"));
            }

            List<UmsWithdrawal> allWithdrawals = withdrawalMapper.selectList(wrapper);

            // 统计各状态数量
            long pendingCount = allWithdrawals.stream().filter(w -> w.getStatus() == 0).count();
            long approvedCount = allWithdrawals.stream().filter(w -> w.getStatus() == 1).count();
            long rejectedCount = allWithdrawals.stream().filter(w -> w.getStatus() == 2).count();
            long completedCount = allWithdrawals.stream().filter(w -> w.getStatus() == 3).count();

            // 统计各状态金额
            BigDecimal pendingAmount = allWithdrawals.stream()
                    .filter(w -> w.getStatus() == 0)
                    .map(UmsWithdrawal::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal approvedAmount = allWithdrawals.stream()
                    .filter(w -> w.getStatus() == 1)
                    .map(UmsWithdrawal::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal rejectedAmount = allWithdrawals.stream()
                    .filter(w -> w.getStatus() == 2)
                    .map(UmsWithdrawal::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal completedAmount = allWithdrawals.stream()
                    .filter(w -> w.getStatus() == 3)
                    .map(UmsWithdrawal::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalAmount = allWithdrawals.stream()
                    .map(UmsWithdrawal::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCount", allWithdrawals.size());
            stats.put("pendingCount", pendingCount);
            stats.put("approvedCount", approvedCount);
            stats.put("rejectedCount", rejectedCount);
            stats.put("completedCount", completedCount);

            stats.put("totalAmount", totalAmount);
            stats.put("pendingAmount", pendingAmount);
            stats.put("approvedAmount", approvedAmount);
            stats.put("rejectedAmount", rejectedAmount);
            stats.put("completedAmount", completedAmount);

            return DefaultResponse.success(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 批量审核提现申请
     */
    @PostMapping("/batch-review")
    @UserLoginToken
    public String batchReview(@RequestBody BatchReviewRequest request) {
        try {
            if (request.getWithdrawalIds() == null || request.getWithdrawalIds().isEmpty()) {
                return DefaultResponse.error("请选择要审核的申请");
            }
            if (request.getStatus() == null || (request.getStatus() != 1 && request.getStatus() != 2)) {
                return DefaultResponse.error("状态参数错误，1-通过 2-拒绝");
            }

            int successCount = 0;
            int failCount = 0;
            StringBuilder errorMsg = new StringBuilder();

            for (String withdrawalId : request.getWithdrawalIds()) {
                try {
                    withdrawalService.reviewWithdrawal(
                            withdrawalId,
                            request.getStatus(),
                            request.getRemark(),
                            request.getReviewerId(),
                            request.getReviewerName()
                    );
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errorMsg.append(withdrawalId).append(": ").append(e.getMessage()).append("; ");
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            if (failCount > 0) {
                result.put("errorMsg", errorMsg.toString());
            }

            return DefaultResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("批量审核失败：" + e.getMessage());
        }
    }

    private String getStatusDesc(Integer status) {
        if (status == null) return "\u672a\u77e5";
        switch (status) {
            case 0: return "\u5f85\u5ba1\u6838";
            case 1: return "\u5df2\u901a\u8fc7";
            case 2: return "\u5df2\u62d2\u7edd";
            case 3: return "\u5df2\u5b8c\u6210";
            default: return "\u672a\u77e5";
        }
    }

    private String getPaymentMethodDesc(Integer method) {
        if (method == null) return "\u672a\u77e5";
        switch (method) {
            case 1: return "\u5fae\u4fe1";
            case 2: return "\u652f\u4ed8\u5b9d";
            case 3: return "\u94f6\u884c\u5361";
            default: return "\u672a\u77e5";
        }
    }
}

// ==================== VO 定义 ====================

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class WithdrawalListVO {
    private String withdrawalId;
    private String userId;
    private String nickName;
    private BigDecimal amount;
    private Integer status;
    private String statusDesc;
    private Integer paymentMethod;
    private String paymentMethodDesc;
    private LocalDateTime createdTime;
    private LocalDateTime reviewTime;
    private LocalDateTime paymentTime;
    private String reviewerName;
}

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class WithdrawalDetailVO {
    private String withdrawalId;
    private String userId;
    private String nickName;
    private BigDecimal amount;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private Integer status;
    private String statusDesc;
    private Integer paymentMethod;
    private String paymentMethodDesc;
    private String paymentAccount;
    private String remark;
    private String reviewerId;
    private String reviewerName;
    private LocalDateTime createdTime;
    private LocalDateTime reviewTime;
    private LocalDateTime paymentTime;
    private LocalDateTime updatedTime;
}

@Data
class ReviewRequest {
    private String withdrawalId;
    private Integer status;      // 1-通过 2-拒绝
    private String remark;       // 审核备注
    private String reviewerId;   // 审核人ID
    private String reviewerName; // 审核人姓名
}

@Data
class BatchReviewRequest {
    private List<String> withdrawalIds;
    private Integer status;      // 1-通过 2-拒绝
    private String remark;       // 审核备注
    private String reviewerId;   // 审核人ID
    private String reviewerName; // 审核人姓名
}
