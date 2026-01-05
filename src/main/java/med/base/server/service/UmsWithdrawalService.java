package med.base.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import med.base.server.mapper.OmsOrderAllocationMapper;
import med.base.server.mapper.UmsUserMapper;
import med.base.server.mapper.UmsWithdrawalMapper;
import med.base.server.model.UmsUser;
import med.base.server.model.UmsWithdrawal;
import med.base.server.util.SysUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户提现服务
 */
@Service
public class UmsWithdrawalService {

    @Autowired
    private UmsWithdrawalMapper withdrawalMapper;

    @Autowired
    private UmsUserMapper userMapper;

    @Autowired
    private OmsOrderAllocationMapper allocationMapper;

    /**
     * 获取用户的收益统计
     */
    public Map<String, Object> getUserIncomeStats(String userId) {
        Map<String, Object> stats = new HashMap<>();

        // 已结算业绩
        BigDecimal settledIncome = allocationMapper.sumSettledIncome(userId);
        stats.put("settledIncome", settledIncome);

        // 未结算业绩
        BigDecimal unsettledIncome = allocationMapper.sumUnsettledIncome(userId);
        stats.put("unsettledIncome", unsettledIncome);

        // 获取用户账户余额（可提现金额）
        UmsUser user = userMapper.selectById(userId);
        BigDecimal account = user != null && user.getAccount() != null ? user.getAccount() : BigDecimal.ZERO;
        stats.put("availableBalance", account);

        // 总收益 = 已结算 + 未结算
        stats.put("totalIncome", settledIncome.add(unsettledIncome));

        return stats;
    }

    /**
     * 申请提现（通过openid）
     */
    @Transactional
    public UmsWithdrawal applyWithdrawalByOpenid(String openid, BigDecimal amount, Integer paymentMethod, String paymentAccount) {
        // 1. 通过openid查询用户
        UmsUser user = userMapper.selectByOpenid(openid);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 调用原有的申请提现方法
        return applyWithdrawal(user.getUmsUserId(), amount, paymentMethod, paymentAccount);
    }

    /**
     * 申请提现
     */
    @Transactional
    public UmsWithdrawal applyWithdrawal(String userId, BigDecimal amount, Integer paymentMethod, String paymentAccount) {
        // 1. 验证用户
        UmsUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 验证余额
        BigDecimal account = user.getAccount() != null ? user.getAccount() : BigDecimal.ZERO;
        if (account.compareTo(amount) < 0) {
            throw new RuntimeException("账户余额不足");
        }

        // 3. 验证提现金额
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("提现金额必须大于0");
        }

        // 4. 创建提现申请
        UmsWithdrawal withdrawal = UmsWithdrawal.builder()
                .withdrawalId(SysUtil.createOrderId("WD"))
                .userId(userId)
                .nickName(user.getNickName())
                .amount(amount)
                .beforeBalance(account)
                .afterBalance(account.subtract(amount))
                .status(0) // 待审核
                .paymentMethod(paymentMethod)
                .paymentAccount(paymentAccount)
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .build();

        withdrawalMapper.insert(withdrawal);

        // 5. 冻结账户余额（扣减账户余额）
        user.setAccount(account.subtract(amount));
        userMapper.updateById(user);

        return withdrawal;
    }

    /**
     * 审核提现申请
     */
    @Transactional
    public void reviewWithdrawal(String withdrawalId, Integer status, String remark, String reviewerId, String reviewerName) {
        UmsWithdrawal withdrawal = withdrawalMapper.selectById(withdrawalId);
        if (withdrawal == null) {
            throw new RuntimeException("提现申请不存在");
        }

        if (withdrawal.getStatus() != 0) {
            throw new RuntimeException("该申请已被处理");
        }

        withdrawal.setStatus(status);
        withdrawal.setRemark(remark);
        withdrawal.setReviewerId(reviewerId);
        withdrawal.setReviewerName(reviewerName);
        withdrawal.setReviewTime(LocalDateTime.now());
        withdrawal.setUpdatedTime(LocalDateTime.now());

        // 如果拒绝，需要退回金额到账户
        if (status == 2) {
            UmsUser user = userMapper.selectById(withdrawal.getUserId());
            if (user != null) {
                BigDecimal currentBalance = user.getAccount() != null ? user.getAccount() : BigDecimal.ZERO;
                user.setAccount(currentBalance.add(withdrawal.getAmount()));
                userMapper.updateById(user);
            }
        }

        withdrawalMapper.updateById(withdrawal);
    }

    /**
     * 确认打款
     */
    @Transactional
    public void confirmPayment(String withdrawalId) {
        UmsWithdrawal withdrawal = withdrawalMapper.selectById(withdrawalId);
        if (withdrawal == null) {
            throw new RuntimeException("提现申请不存在");
        }

        if (withdrawal.getStatus() != 1) {
            throw new RuntimeException("只有已通过的申请才能确认打款");
        }

        withdrawal.setStatus(3); // 已完成
        withdrawal.setPaymentTime(LocalDateTime.now());
        withdrawal.setUpdatedTime(LocalDateTime.now());

        withdrawalMapper.updateById(withdrawal);
    }

    /**
     * 获取用户提现记录
     */
    public java.util.List<UmsWithdrawal> getUserWithdrawals(String userId) {
        LambdaQueryWrapper<UmsWithdrawal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UmsWithdrawal::getUserId, userId);
        wrapper.orderByDesc(UmsWithdrawal::getCreatedTime);
        return withdrawalMapper.selectList(wrapper);
    }
}
