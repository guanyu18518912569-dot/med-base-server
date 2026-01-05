package med.base.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import med.base.server.mapper.OmsOrderItemMapper;
import med.base.server.mapper.OmsOrderMapper;
import med.base.server.mapper.PmsCategoryMapper;
import med.base.server.mapper.PmsSpuMapper;
import med.base.server.model.OmsOrder;
import med.base.server.model.OmsOrderItem;
import med.base.server.model.PmsCategory;
import med.base.server.model.PmsSpu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 销售统计服务
 */
@Service
public class SalesStatisticsService {

    @Autowired
    private OmsOrderMapper orderMapper;

    @Autowired
    private OmsOrderItemMapper orderItemMapper;

    @Autowired
    private PmsSpuMapper spuMapper;

    @Autowired
    private PmsCategoryMapper categoryMapper;

    /**
     * 获取销售统计概览
     */
    public Map<String, Object> getOverview(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();

        // 获取已完成订单（状态3）
        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getStatus, 3);
        wrapper.eq(OmsOrder::getDeleted, 0);
        
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(OmsOrder::getFinishTime, start);
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
            wrapper.lt(OmsOrder::getFinishTime, end);
        }

        List<OmsOrder> orders = orderMapper.selectList(wrapper);

        // 统计数据
        long totalOrders = orders.size();
        long totalAmount = orders.stream().mapToLong(o -> o.getPayAmount() != null ? o.getPayAmount() : 0).sum();
        long totalGoods = orders.stream().mapToLong(o -> o.getGoodsCount() != null ? o.getGoodsCount() : 0).sum();

        result.put("totalOrders", totalOrders);
        result.put("totalAmount", totalAmount);
        result.put("totalAmountYuan", formatAmount(totalAmount));
        result.put("totalGoods", totalGoods);
        result.put("avgOrderAmount", totalOrders > 0 ? formatAmount(totalAmount / totalOrders) : "0.00");

        return result;
    }

    /**
     * 按分类统计销售额
     */
    public List<Map<String, Object>> getStatisticsByCategory(String startDate, String endDate) {
        // 获取已完成订单
        LambdaQueryWrapper<OmsOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(OmsOrder::getStatus, 3);
        orderWrapper.eq(OmsOrder::getDeleted, 0);
        
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            orderWrapper.ge(OmsOrder::getFinishTime, start);
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
            orderWrapper.lt(OmsOrder::getFinishTime, end);
        }

        List<OmsOrder> orders = orderMapper.selectList(orderWrapper);
        
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取订单商品明细
        List<String> orderIds = orders.stream().map(OmsOrder::getOrderId).collect(Collectors.toList());
        LambdaQueryWrapper<OmsOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(OmsOrderItem::getOrderId, orderIds);
        List<OmsOrderItem> items = orderItemMapper.selectList(itemWrapper);

        // 获取所有SPU信息
        Set<String> spuIds = items.stream().map(OmsOrderItem::getSpuId).collect(Collectors.toSet());
        Map<String, PmsSpu> spuMap = new HashMap<>();
        if (!spuIds.isEmpty()) {
            List<PmsSpu> spuList = spuMapper.selectBatchIds(spuIds);
            spuMap = spuList.stream().collect(Collectors.toMap(PmsSpu::getSpuId, s -> s));
        }

        // 获取所有分类信息
        List<PmsCategory> categories = categoryMapper.selectList(null);
        Map<Integer, PmsCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(PmsCategory::getCategoryId, c -> c));

        // 按分类汇总销售额
        Map<Integer, Long> categoryAmountMap = new HashMap<>();
        Map<Integer, Long> categoryQuantityMap = new HashMap<>();
        Map<Integer, Long> categoryOrderCountMap = new HashMap<>();

        for (OmsOrderItem item : items) {
            PmsSpu spu = spuMap.get(item.getSpuId());
            if (spu == null) continue;

            int categoryId = spu.getCategoryId();
            Long payAmount = item.getPayAmount() != null ? item.getPayAmount() : item.getTotalAmount();
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;

            categoryAmountMap.merge(categoryId, payAmount, Long::sum);
            categoryQuantityMap.merge(categoryId, (long) quantity, Long::sum);
            categoryOrderCountMap.merge(categoryId, 1L, Long::sum);
        }

        // 构建返回结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : categoryAmountMap.entrySet()) {
            Integer categoryId = entry.getKey();
            Long amount = entry.getValue();

            Map<String, Object> stat = new HashMap<>();
            stat.put("categoryId", categoryId);
            
            PmsCategory category = categoryMap.get(categoryId);
            stat.put("categoryName", category != null ? category.getCategoryName() : "未知分类");
            
            stat.put("totalAmount", amount);
            stat.put("totalAmountYuan", formatAmount(amount));
            stat.put("quantity", categoryQuantityMap.getOrDefault(categoryId, 0L));
            stat.put("orderCount", categoryOrderCountMap.getOrDefault(categoryId, 0L));

            result.add(stat);
        }

        // 按销售额降序排序
        result.sort((a, b) -> Long.compare(
                (Long) b.get("totalAmount"), 
                (Long) a.get("totalAmount")));

        return result;
    }

    /**
     * 按商品统计销售额
     */
    public List<Map<String, Object>> getStatisticsByProduct(String startDate, String endDate, Integer categoryId) {
        // 获取已完成订单
        LambdaQueryWrapper<OmsOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(OmsOrder::getStatus, 3);
        orderWrapper.eq(OmsOrder::getDeleted, 0);
        
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            orderWrapper.ge(OmsOrder::getFinishTime, start);
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
            orderWrapper.lt(OmsOrder::getFinishTime, end);
        }

        List<OmsOrder> orders = orderMapper.selectList(orderWrapper);
        
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取订单商品明细
        List<String> orderIds = orders.stream().map(OmsOrder::getOrderId).collect(Collectors.toList());
        LambdaQueryWrapper<OmsOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(OmsOrderItem::getOrderId, orderIds);
        List<OmsOrderItem> items = orderItemMapper.selectList(itemWrapper);

        // 获取所有SPU信息
        Set<String> spuIds = items.stream().map(OmsOrderItem::getSpuId).collect(Collectors.toSet());
        Map<String, PmsSpu> spuMap = new HashMap<>();
        if (!spuIds.isEmpty()) {
            List<PmsSpu> spuList = spuMapper.selectBatchIds(spuIds);
            spuMap = spuList.stream().collect(Collectors.toMap(PmsSpu::getSpuId, s -> s));
        }

        // 如果指定了分类，过滤商品
        if (categoryId != null && categoryId > 0) {
            Set<String> filteredSpuIds = spuMap.values().stream()
                    .filter(s -> s.getCategoryId() == categoryId)
                    .map(PmsSpu::getSpuId)
                    .collect(Collectors.toSet());
            items = items.stream()
                    .filter(i -> filteredSpuIds.contains(i.getSpuId()))
                    .collect(Collectors.toList());
        }

        // 按商品汇总销售额
        Map<String, Long> productAmountMap = new HashMap<>();
        Map<String, Long> productQuantityMap = new HashMap<>();
        Map<String, Long> productOrderCountMap = new HashMap<>();
        Map<String, String> productNameMap = new HashMap<>();
        Map<String, String> productImageMap = new HashMap<>();

        for (OmsOrderItem item : items) {
            String spuId = item.getSpuId();
            Long payAmount = item.getPayAmount() != null ? item.getPayAmount() : item.getTotalAmount();
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;

            productAmountMap.merge(spuId, payAmount, Long::sum);
            productQuantityMap.merge(spuId, (long) quantity, Long::sum);
            productOrderCountMap.merge(spuId, 1L, Long::sum);
            productNameMap.put(spuId, item.getGoodsName());
            productImageMap.put(spuId, item.getGoodsImage());
        }

        // 构建返回结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : productAmountMap.entrySet()) {
            String spuId = entry.getKey();
            Long amount = entry.getValue();

            Map<String, Object> stat = new HashMap<>();
            stat.put("spuId", spuId);
            stat.put("goodsName", productNameMap.get(spuId));
            stat.put("goodsImage", productImageMap.get(spuId));
            stat.put("totalAmount", amount);
            stat.put("totalAmountYuan", formatAmount(amount));
            stat.put("quantity", productQuantityMap.getOrDefault(spuId, 0L));
            stat.put("orderCount", productOrderCountMap.getOrDefault(spuId, 0L));

            PmsSpu spu = spuMap.get(spuId);
            if (spu != null) {
                stat.put("categoryId", spu.getCategoryId());
            }

            result.add(stat);
        }

        // 按销售额降序排序
        result.sort((a, b) -> Long.compare(
                (Long) b.get("totalAmount"), 
                (Long) a.get("totalAmount")));

        return result;
    }

    /**
     * 按日期统计销售趋势
     */
    public List<Map<String, Object>> getSalesTrend(String startDate, String endDate) {
        // 获取已完成订单
        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getStatus, 3);
        wrapper.eq(OmsOrder::getDeleted, 0);
        
        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(OmsOrder::getFinishTime, start);
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
            wrapper.lt(OmsOrder::getFinishTime, end);
        }

        wrapper.orderByAsc(OmsOrder::getFinishTime);
        List<OmsOrder> orders = orderMapper.selectList(wrapper);

        // 按日期汇总
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Long> dateAmountMap = new LinkedHashMap<>();
        Map<String, Long> dateOrderCountMap = new LinkedHashMap<>();

        for (OmsOrder order : orders) {
            if (order.getFinishTime() == null) continue;
            String date = order.getFinishTime().format(formatter);
            Long amount = order.getPayAmount() != null ? order.getPayAmount() : 0L;

            dateAmountMap.merge(date, amount, Long::sum);
            dateOrderCountMap.merge(date, 1L, Long::sum);
        }

        // 构建返回结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : dateAmountMap.entrySet()) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("date", entry.getKey());
            stat.put("totalAmount", entry.getValue());
            stat.put("totalAmountYuan", formatAmount(entry.getValue()));
            stat.put("orderCount", dateOrderCountMap.getOrDefault(entry.getKey(), 0L));
            result.add(stat);
        }

        return result;
    }

    /**
     * 格式化金额（分转元）
     */
    private String formatAmount(Long amount) {
        if (amount == null) return "0.00";
        return new BigDecimal(amount)
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP)
                .toString();
    }
}
