package med.base.server.service;

import med.base.server.mapper.OmsOrderAllocationMapper;
import med.base.server.mapper.OmsOrderItemMapper;
import med.base.server.mapper.PmsSpuMapper;
import med.base.server.mapper.UmsUserMapper;
import med.base.server.model.OmsOrder;
import med.base.server.model.OmsOrderAllocation;
import med.base.server.model.OmsOrderItem;
import med.base.server.model.PmsSpu;
import med.base.server.model.UmsUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 订单分成服务
 */
@Service
public class OmsOrderAllocationService {

    @Autowired
    private OmsOrderAllocationMapper allocationMapper;

    @Autowired
    private OmsOrderItemMapper orderItemMapper;

    @Autowired
    private PmsSpuMapper spuMapper;

    @Autowired
    private UmsUserMapper userMapper;

    /**
     * 创建订单分成记录（订单完成时调用）
     */
    @Transactional
    public void createAllocation(OmsOrder order) {
        // 获取订单商品列表
        List<OmsOrderItem> orderItems = orderItemMapper.selectByOrderId(order.getOrderId());

        for (OmsOrderItem item : orderItems) {
            // 获取商品的分成比例
            PmsSpu spu = spuMapper.selectById(item.getSpuId());
            if (spu == null) {
                continue;
            }

            // 计算分成金额（基于实付金额）
            Long payAmount = item.getPayAmount() != null ? item.getPayAmount() : item.getTotalAmount();

            BigDecimal ratioProvince = spu.getAllocationRatioProvince() != null
                    ? spu.getAllocationRatioProvince() : BigDecimal.ZERO;
            BigDecimal ratioCity = spu.getAllocationRatioCity() != null
                    ? spu.getAllocationRatioCity() : BigDecimal.ZERO;
            BigDecimal ratioDistrict = spu.getAllocationRatioDistrict() != null
                    ? spu.getAllocationRatioDistrict() : BigDecimal.ZERO;

            // 计算各级分成金额（元）= 实付金额（分） / 100 * 比例
            BigDecimal amountProvince = BigDecimal.valueOf(payAmount)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .multiply(ratioProvince);
            BigDecimal amountCity = BigDecimal.valueOf(payAmount)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .multiply(ratioCity);
            BigDecimal amountDistrict = BigDecimal.valueOf(payAmount)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .multiply(ratioDistrict);

            // 获取邀请收益比例
            BigDecimal inviteIncomeRatio = spu.getInviteIncomeRatio() != null
                    ? spu.getInviteIncomeRatio() : BigDecimal.ZERO;

            // 获取下单用户信息和上级用户信息
            String orderUserId = order.getUserId();
            String orderUserNickName = null;
            String parentUserId = null;
            String parentUserNickName = null;

            if (StringUtils.hasLength(orderUserId)) {
                UmsUser orderUser = userMapper.selectById(orderUserId);
                if (orderUser != null) {
                    orderUserNickName = orderUser.getNickName();

                    // 获取上级用户信息（收益人）
                    if (StringUtils.hasLength(orderUser.getParentId())) {
                        UmsUser parentUser = userMapper.selectById(orderUser.getParentId());
                        if (parentUser != null) {
                            parentUserId = parentUser.getUmsUserId();
                            parentUserNickName = parentUser.getNickName();
                        }
                    }
                }
            }

            // 创建分成记录
            OmsOrderAllocation allocation = OmsOrderAllocation.builder()
                    .allocationId(UUID.randomUUID().toString().replace("-", ""))
                    .orderId(order.getOrderId())
                    .orderNo(order.getOrderNo())
                    .orderItemId(item.getOrderItemId())
                    .spuId(item.getSpuId())
                    .goodsName(item.getGoodsName())
                    .userId(orderUserId)
                    .nickName(orderUserNickName)
                    .parentUserId(parentUserId)
                    .parentNickName(parentUserNickName)
                    .payAmount(payAmount)
                    .quantity(item.getQuantity())
                    .receiverProvince(order.getReceiverProvince())
                    .receiverCity(order.getReceiverCity())
                    .receiverDistrict(order.getReceiverDistrict())
                    .allocationRatioProvince(ratioProvince)
                    .allocationRatioCity(ratioCity)
                    .allocationRatioDistrict(ratioDistrict)
                    .inviteIncomeRatio(inviteIncomeRatio)
                    .allocationAmountProvince(amountProvince)
                    .allocationAmountCity(amountCity)
                    .allocationAmountDistrict(amountDistrict)
                    .settlementStatus(0)
                    .createdTime(LocalDateTime.now())
                    .build();

            allocationMapper.insert(allocation);
        }
    }

    /**
     * 获取分成统计（管理员：所有数据）
     */
    public Map<String, Object> getStatisticsForAdmin() {
        Map<String, Object> result = new HashMap<>();

        // 按省份汇总
        List<Map<String, Object>> provinceStats = allocationMapper.sumAllProvince();
        result.put("provinceStats", provinceStats);

        // 按市汇总
        List<Map<String, Object>> cityStats = allocationMapper.sumAllCity();
        result.put("cityStats", cityStats);

        // 按区汇总
        List<Map<String, Object>> districtStats = allocationMapper.sumAllDistrict();
        result.put("districtStats", districtStats);

        // 计算总金额
        long totalProvince = provinceStats.stream()
                .mapToLong(m -> ((Number) m.getOrDefault("totalAmount", 0)).longValue())
                .sum();
        long totalCity = cityStats.stream()
                .mapToLong(m -> ((Number) m.getOrDefault("totalAmount", 0)).longValue())
                .sum();
        long totalDistrict = districtStats.stream()
                .mapToLong(m -> ((Number) m.getOrDefault("totalAmount", 0)).longValue())
                .sum();

        result.put("totalProvinceAmount", totalProvince);
        result.put("totalCityAmount", totalCity);
        result.put("totalDistrictAmount", totalDistrict);
        result.put("grandTotal", totalProvince + totalCity + totalDistrict);

        return result;
    }

    /**
     * 获取省级账号的分成统计
     */
    public Map<String, Object> getStatisticsForProvince(String province) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> stats = allocationMapper.sumByProvince(province);
        result.put("stats", stats);
        result.put("province", province);

        return result;
    }

    /**
     * 获取市级账号的分成统计
     */
    public Map<String, Object> getStatisticsForCity(String province, String city) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> stats = allocationMapper.sumByCity(province, city);
        result.put("stats", stats);
        result.put("province", province);
        result.put("city", city);

        return result;
    }

    /**
     * 获取区级账号的分成统计
     */
    public Map<String, Object> getStatisticsForDistrict(String province, String city, String district) {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> stats = allocationMapper.sumByDistrict(province, city, district);
        result.put("stats", stats);
        result.put("province", province);
        result.put("city", city);
        result.put("district", district);

        return result;
    }

    /**
     * 获取分成明细列表（分页）
     */
    public Map<String, Object> getAllocationList(String province, String city, String district,
                                                  int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();

        int offset = (page - 1) * pageSize;
        List<OmsOrderAllocation> list = allocationMapper.selectByRegion(province, city, district, offset, pageSize);
        int total = allocationMapper.countByRegion(province, city, district);

        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (total + pageSize - 1) / pageSize);

        return result;
    }

    /**
     * 根据订单ID查询分成记录
     */
    public List<OmsOrderAllocation> getByOrderId(String orderId) {
        return allocationMapper.selectByOrderId(orderId);
    }
}
