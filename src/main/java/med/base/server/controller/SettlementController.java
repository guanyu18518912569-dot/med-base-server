package med.base.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.mapper.OmsOrderAllocationMapper;
import med.base.server.mapper.UmsUserMapper;
import med.base.server.model.OmsOrderAllocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收益结算管理接口
 */
@RestController
@RequestMapping("/admin/settlement")
public class SettlementController {

    @Autowired
    private OmsOrderAllocationMapper allocationMapper;

    @Autowired
    private UmsUserMapper userMapper;

    /**
     * 获取待结算收益列表
     */
    @UserLoginToken
    @GetMapping("/list")
    public String getSettlementList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String nickName,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        try {
            Page<OmsOrderAllocation> page = new Page<>(pageNum, pageSize);
            QueryWrapper<OmsOrderAllocation> wrapper = new QueryWrapper<>();

            // 只查询未结算的记录
            wrapper.eq("settlement_status", 0);

            // 筛选条件
            if (StringUtils.hasLength(nickName)) {
                wrapper.like("nick_name", nickName);
            }
            if (StringUtils.hasLength(orderId)) {
                wrapper.eq("order_id", orderId);
            }
            if (StringUtils.hasLength(startDate)) {
                wrapper.ge("created_time", startDate + " 00:00:00");
            }
            if (StringUtils.hasLength(endDate)) {
                wrapper.le("created_time", endDate + " 23:59:59");
            }

            wrapper.orderByDesc("created_time");

            IPage<OmsOrderAllocation> result = allocationMapper.selectPage(page, wrapper);

            // 转换为VO
            List<SettlementVO> voList = result.getRecords().stream().map(record -> {
                SettlementVO vo = new SettlementVO();
                vo.setAllocationId(record.getAllocationId());
                vo.setOrderId(record.getOrderId());
                vo.setUserId(record.getUserId());  // 下单用户
                vo.setNickName(record.getNickName());  // 下单用户昵称
                vo.setParentUserId(record.getParentUserId());  // 收益人
                vo.setParentNickName(record.getParentNickName());  // 收益人昵称
                // payAmount是Long类型（分），转为BigDecimal（元）
                vo.setPayAmount(new BigDecimal(record.getPayAmount()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
                // inviteIncomeRatio是BigDecimal，转为Integer（百分比）
                vo.setInviteIncomeRatio(record.getInviteIncomeRatio().multiply(new BigDecimal("100")).intValue());
                // 计算佣金：订单金额（分） / 100 * 佣金比例
                vo.setCommission(new BigDecimal(record.getPayAmount())
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                        .multiply(record.getInviteIncomeRatio())
                        .setScale(2, RoundingMode.HALF_UP));
                vo.setSettlementStatus(record.getSettlementStatus());
                vo.setCreatedTime(record.getCreatedTime() != null ? record.getCreatedTime().toString() : null);
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
            return DefaultResponse.error("\u83b7\u53d6\u5217\u8868\u5931\u8d25\uff1a" + e.getMessage());
        }
    }

    /**
     * 获取统计数据
     */
    @UserLoginToken
    @GetMapping("/statistics")
    public String getStatistics() {
        try {
            QueryWrapper<OmsOrderAllocation> wrapper = new QueryWrapper<>();
            wrapper.eq("settlement_status", 0);

            List<OmsOrderAllocation> unsettledList = allocationMapper.selectList(wrapper);

            long unsettledCount = unsettledList.size();
            BigDecimal unsettledAmount = unsettledList.stream()
                    .map(record -> new BigDecimal(record.getPayAmount())
                            .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                            .multiply(record.getInviteIncomeRatio()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> stats = new HashMap<>();
            stats.put("unsettledCount", unsettledCount);
            stats.put("unsettledAmount", unsettledAmount);

            return DefaultResponse.success(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("\u83b7\u53d6\u7edf\u8ba1\u6570\u636e\u5931\u8d25\uff1a" + e.getMessage());
        }
    }

    /**
     * 批量结算
     */
    @UserLoginToken
    @PostMapping("/batch-settle")
    @Transactional
    public String batchSettle(@RequestBody BatchSettleRequest request) {
        try {
            if (request.getAllocationIds() == null || request.getAllocationIds().isEmpty()) {
                return DefaultResponse.error("\u8bf7\u9009\u62e9\u8981\u7ed3\u7b97\u7684\u8bb0\u5f55");
            }

            // 按用户分组统计金额
            Map<String, BigDecimal> userAmounts = new HashMap<>();

            for (String allocationId : request.getAllocationIds()) {
                OmsOrderAllocation allocation = allocationMapper.selectById(allocationId);
                if (allocation == null) {
                    continue;
                }

                if (allocation.getSettlementStatus() == 1) {
                    continue; // 已结算的跳过
                }

                // 计算佣金：订单金额（分） / 100 * 佣金比例
                BigDecimal commission = new BigDecimal(allocation.getPayAmount())
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                        .multiply(allocation.getInviteIncomeRatio())
                        .setScale(2, RoundingMode.HALF_UP);

                // 累加到上级用户（parentUserId 已经记录了收益人）
                if (StringUtils.hasLength(allocation.getParentUserId())) {
                    userAmounts.merge(allocation.getParentUserId(), commission, BigDecimal::add);
                }

                // 更新结算状态
                allocation.setSettlementStatus(1);
                allocation.setSettlementTime(LocalDateTime.now());
                allocationMapper.updateById(allocation);
            }

            // 批量更新用户账户余额
            for (Map.Entry<String, BigDecimal> entry : userAmounts.entrySet()) {
                String userId = entry.getKey();
                BigDecimal amount = entry.getValue();

                // 增加可提现余额
                userMapper.addIncome(userId, amount);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("settledCount", request.getAllocationIds().size());
            result.put("settledAmount", userAmounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            result.put("userCount", userAmounts.size());

            return DefaultResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("\u7ed3\u7b97\u5931\u8d25\uff1a" + e.getMessage());
        }
    }

    /**
     * 全部结算（慎用）
     */
    @UserLoginToken
    @PostMapping("/settle-all")
    @Transactional
    public String settleAll() {
        try {
            QueryWrapper<OmsOrderAllocation> wrapper = new QueryWrapper<>();
            wrapper.eq("settlement_status", 0);

            List<OmsOrderAllocation> unsettledList = allocationMapper.selectList(wrapper);

            if (unsettledList.isEmpty()) {
                return DefaultResponse.error("\u6ca1\u6709\u5f85\u7ed3\u7b97\u8bb0\u5f55");
            }

            // 按用户分组统计金额
            Map<String, BigDecimal> userAmounts = new HashMap<>();

            for (OmsOrderAllocation allocation : unsettledList) {
                // 计算佣金：订单金额（分） / 100 * 佣金比例
                BigDecimal commission = new BigDecimal(allocation.getPayAmount())
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                        .multiply(allocation.getInviteIncomeRatio())
                        .setScale(2, RoundingMode.HALF_UP);

                // 累加到上级用户（parentUserId 已经记录了收益人）
                if (StringUtils.hasLength(allocation.getParentUserId())) {
                    userAmounts.merge(allocation.getParentUserId(), commission, BigDecimal::add);
                }

                // 更新结算状态
                allocation.setSettlementStatus(1);
                allocation.setSettlementTime(LocalDateTime.now());
                allocationMapper.updateById(allocation);
            }

            // 批量更新用户账户余额
            for (Map.Entry<String, BigDecimal> entry : userAmounts.entrySet()) {
                String userId = entry.getKey();
                BigDecimal amount = entry.getValue();

                // 增加可提现余额
                userMapper.addIncome(userId, amount);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("settledCount", unsettledList.size());
            result.put("settledAmount", userAmounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            result.put("userCount", userAmounts.size());

            return DefaultResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("\u7ed3\u7b97\u5931\u8d25\uff1a" + e.getMessage());
        }
    }
}

/**
 * 结算记录VO
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class SettlementVO {
    private String allocationId;
    private String orderId;
    private String userId;  // 下单用户ID
    private String nickName;  // 下单用户昵称
    private String parentUserId;  // 收益人ID
    private String parentNickName;  // 收益人昵称
    private BigDecimal payAmount;  // 订单金额（元）
    private Integer inviteIncomeRatio;  // 佣金比例（例如1000表示10%）
    private BigDecimal commission;  // 佣金金额（元）
    private Integer settlementStatus;  // 0-未结算 1-已结算
    private String createdTime;
}

/**
 * 批量结算请求
 */
@Data
class BatchSettleRequest {
    private List<String> allocationIds;
}
