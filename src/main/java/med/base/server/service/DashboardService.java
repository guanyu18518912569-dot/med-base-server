package med.base.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import med.base.server.mapper.OmsOrderItemMapper;
import med.base.server.mapper.OmsOrderMapper;
import med.base.server.mapper.PmsCategoryMapper;
import med.base.server.mapper.PmsSpuMapper;
import med.base.server.mapper.UmsUserMapper;
import med.base.server.model.OmsOrder;
import med.base.server.model.OmsOrderItem;
import med.base.server.model.PmsCategory;
import med.base.server.model.PmsSpu;
import med.base.server.model.UmsUser;
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
 * 首页统计服务
 */
@Service
public class DashboardService {

    @Autowired
    private OmsOrderMapper orderMapper;

    @Autowired
    private OmsOrderItemMapper orderItemMapper;

    @Autowired
    private UmsUserMapper userMapper;

    @Autowired
    private PmsSpuMapper spuMapper;

    @Autowired
    private PmsCategoryMapper categoryMapper;

    /**
     * 获取首页统计概览
     */
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> result = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // 今日统计
        Map<String, Object> today = getOrderStatsByPeriod(todayStart, now);
        result.put("today", today);

        // 本周统计
        Map<String, Object> week = getOrderStatsByPeriod(weekStart, now);
        result.put("week", week);

        // 本月统计
        Map<String, Object> month = getOrderStatsByPeriod(monthStart, now);
        result.put("month", month);

        // 总体统计
        Map<String, Object> total = getOrderStatsByPeriod(null, null);
        result.put("total", total);

        // 用户总数（status不为2表示未注销）
        LambdaQueryWrapper<UmsUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.ne(UmsUser::getStatus, 2);
        long totalUsers = userMapper.selectCount(userWrapper);
        result.put("totalUsers", totalUsers);

        // 今日新增用户
        LambdaQueryWrapper<UmsUser> todayUserWrapper = new LambdaQueryWrapper<>();
        todayUserWrapper.ne(UmsUser::getStatus, 2);
        todayUserWrapper.ge(UmsUser::getCreatedTime, todayStart);
        long todayNewUsers = userMapper.selectCount(todayUserWrapper);
        result.put("todayNewUsers", todayNewUsers);

        return result;
    }

    /**
     * 获取指定时间段的订单统计
     */
    private Map<String, Object> getOrderStatsByPeriod(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getDeleted, 0);

        if (startTime != null) {
            wrapper.ge(OmsOrder::getCreatedTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OmsOrder::getCreatedTime, endTime);
        }

        List<OmsOrder> orders = orderMapper.selectList(wrapper);

        // 总订单数（包含所有状态）
        long totalOrders = orders.size();
        stats.put("totalOrders", totalOrders);

        // 待付款订单数（状态=0）
        long toPayOrders = orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 0).count();
        stats.put("toPayOrders", toPayOrders);

        // 已付款订单数（状态>=1）
        long paidOrders = orders.stream().filter(o -> o.getStatus() != null && o.getStatus() >= 1).count();
        stats.put("paidOrders", paidOrders);

        // 已完成订单数（状态=3）
        long completedOrders = orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 3).count();
        stats.put("completedOrders", completedOrders);

        // 总销售额（已完成订单，status=3）
        long totalAmount = orders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 3)
                .mapToLong(o -> o.getPayAmount() != null ? o.getPayAmount() : 0)
                .sum();
        stats.put("totalAmount", totalAmount);
        stats.put("totalAmountYuan", formatAmount(totalAmount));

        // 已付款金额（status>=1，包括待发货、待收货、已完成）
        long paidAmount = orders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() >= 1)
                .mapToLong(o -> o.getPayAmount() != null ? o.getPayAmount() : 0)
                .sum();
        stats.put("paidAmount", paidAmount);
        stats.put("paidAmountYuan", formatAmount(paidAmount));

        // 待付款订单总金额
        long toPayAmount = orders.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() == 0)
                .mapToLong(o -> o.getPayAmount() != null ? o.getPayAmount() : 0)
                .sum();
        stats.put("toPayAmount", toPayAmount);
        stats.put("toPayAmountYuan", formatAmount(toPayAmount));

        // 平均订单金额（基于已完成订单）
        if (completedOrders > 0) {
            stats.put("avgOrderAmount", formatAmount(totalAmount / completedOrders));
        } else {
            stats.put("avgOrderAmount", "0.00");
        }

        return stats;
    }

    /**
     * 获取订单状态统计
     */
    public Map<String, Object> getOrderStatusStatistics() {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getDeleted, 0);
        List<OmsOrder> orders = orderMapper.selectList(wrapper);

        // 按状态分组统计
        Map<Integer, Long> statusCountMap = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus() != null ? o.getStatus() : 0,
                        Collectors.counting()
                ));

        result.put("toPay", statusCountMap.getOrDefault(0, 0L));        // 待付款
        result.put("toDeliver", statusCountMap.getOrDefault(1, 0L));    // 待发货
        result.put("toReceive", statusCountMap.getOrDefault(2, 0L));    // 待收货
        result.put("completed", statusCountMap.getOrDefault(3, 0L));    // 已完成
        result.put("cancelled", statusCountMap.getOrDefault(4, 0L));    // 已取消
        result.put("afterSale", statusCountMap.getOrDefault(5, 0L));    // 售后中

        return result;
    }

    /**
     * 获取近期销售趋势
     */
    public Map<String, Object> getSalesTrendByDays(Integer days) {
        Map<String, Object> result = new HashMap<>();

        if (days == null || days <= 0) {
            days = 7;
        }

        LocalDateTime startTime = LocalDate.now().minusDays(days - 1).atStartOfDay();
        LocalDateTime endTime = LocalDateTime.now();

        // 查询已付款订单
        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getDeleted, 0);
        wrapper.ge(OmsOrder::getStatus, 1); // 已付款
        wrapper.ge(OmsOrder::getPayTime, startTime);
        wrapper.le(OmsOrder::getPayTime, endTime);
        wrapper.orderByAsc(OmsOrder::getPayTime);

        List<OmsOrder> orders = orderMapper.selectList(wrapper);

        // 按日期汇总
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Long> dateAmountMap = new LinkedHashMap<>();
        Map<String, Long> dateOrderCountMap = new LinkedHashMap<>();

        // 初始化所有日期（确保即使没有订单的日期也有数据）
        for (int i = days - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(formatter);
            dateAmountMap.put(date, 0L);
            dateOrderCountMap.put(date, 0L);
        }

        // 填充实际数据
        for (OmsOrder order : orders) {
            if (order.getPayTime() == null) continue;
            String date = order.getPayTime().format(formatter);
            Long amount = order.getPayAmount() != null ? order.getPayAmount() : 0L;

            if (dateAmountMap.containsKey(date)) {
                dateAmountMap.put(date, dateAmountMap.get(date) + amount);
                dateOrderCountMap.put(date, dateOrderCountMap.get(date) + 1);
            }
        }

        // 构建返回数据
        List<Map<String, Object>> trendList = new ArrayList<>();
        for (Map.Entry<String, Long> entry : dateAmountMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", entry.getKey());
            item.put("amount", entry.getValue());
            item.put("amountYuan", formatAmount(entry.getValue()));
            item.put("orderCount", dateOrderCountMap.get(entry.getKey()));
            trendList.add(item);
        }

        result.put("days", days);
        result.put("trend", trendList);

        return result;
    }

    /**
     * 获取热销商品Top N
     */
    public Map<String, Object> getTopSellingProducts(Integer limit) {
        Map<String, Object> result = new HashMap<>();

        if (limit == null || limit <= 0) {
            limit = 10;
        }

        // 获取最近30天已完成订单
        LocalDateTime startTime = LocalDate.now().minusDays(30).atStartOfDay();
        LambdaQueryWrapper<OmsOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(OmsOrder::getDeleted, 0);
        orderWrapper.eq(OmsOrder::getStatus, 3); // 已完成
        orderWrapper.ge(OmsOrder::getFinishTime, startTime);

        List<OmsOrder> orders = orderMapper.selectList(orderWrapper);

        if (orders.isEmpty()) {
            result.put("products", new ArrayList<>());
            return result;
        }

        // 获取订单商品明细
        List<String> orderIds = orders.stream().map(OmsOrder::getOrderId).collect(Collectors.toList());
        LambdaQueryWrapper<OmsOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(OmsOrderItem::getOrderId, orderIds);
        List<OmsOrderItem> items = orderItemMapper.selectList(itemWrapper);

        // 按商品汇总销量和销售额
        Map<String, Long> productSalesMap = new HashMap<>();
        Map<String, Long> productAmountMap = new HashMap<>();
        Map<String, String> productNameMap = new HashMap<>();
        Map<String, String> productImageMap = new HashMap<>();

        for (OmsOrderItem item : items) {
            String spuId = item.getSpuId();
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
            Long amount = item.getPayAmount() != null ? item.getPayAmount() : item.getTotalAmount();

            productSalesMap.merge(spuId, (long) quantity, Long::sum);
            productAmountMap.merge(spuId, amount, Long::sum);
            productNameMap.put(spuId, item.getGoodsName());
            productImageMap.put(spuId, item.getGoodsImage());
        }

        // 按销量排序取Top N
        List<Map<String, Object>> topProducts = productSalesMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(entry -> {
                    String spuId = entry.getKey();
                    Map<String, Object> product = new HashMap<>();
                    product.put("spuId", spuId);
                    product.put("goodsName", productNameMap.get(spuId));
                    product.put("goodsImage", productImageMap.get(spuId));
                    product.put("salesVolume", entry.getValue());
                    product.put("totalAmount", productAmountMap.get(spuId));
                    product.put("totalAmountYuan", formatAmount(productAmountMap.get(spuId)));
                    return product;
                })
                .collect(Collectors.toList());

        result.put("products", topProducts);
        result.put("period", "最近30天");

        return result;
    }

    /**
     * 获取分类销售分布
     */
    public Map<String, Object> getCategoryDistribution() {
        Map<String, Object> result = new HashMap<>();

        // 获取最近30天已完成订单
        LocalDateTime startTime = LocalDate.now().minusDays(30).atStartOfDay();
        LambdaQueryWrapper<OmsOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(OmsOrder::getDeleted, 0);
        orderWrapper.eq(OmsOrder::getStatus, 3); // 已完成
        orderWrapper.ge(OmsOrder::getFinishTime, startTime);

        List<OmsOrder> orders = orderMapper.selectList(orderWrapper);

        if (orders.isEmpty()) {
            result.put("distribution", new ArrayList<>());
            return result;
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
        for (OmsOrderItem item : items) {
            PmsSpu spu = spuMap.get(item.getSpuId());
            if (spu == null) continue;

            int categoryId = spu.getCategoryId();
            Long amount = item.getPayAmount() != null ? item.getPayAmount() : item.getTotalAmount();
            categoryAmountMap.merge(categoryId, amount, Long::sum);
        }

        // 计算总销售额
        long totalAmount = categoryAmountMap.values().stream().mapToLong(Long::longValue).sum();

        // 构建返回数据
        List<Map<String, Object>> distribution = categoryAmountMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(entry -> {
                    Integer categoryId = entry.getKey();
                    Long amount = entry.getValue();
                    PmsCategory category = categoryMap.get(categoryId);

                    Map<String, Object> item = new HashMap<>();
                    item.put("categoryId", categoryId);
                    item.put("categoryName", category != null ? category.getCategoryName() : "未知分类");
                    item.put("amount", amount);
                    item.put("amountYuan", formatAmount(amount));

                    // 计算占比
                    if (totalAmount > 0) {
                        BigDecimal percentage = new BigDecimal(amount * 100)
                                .divide(new BigDecimal(totalAmount), 2, RoundingMode.HALF_UP);
                        item.put("percentage", percentage.doubleValue());
                    } else {
                        item.put("percentage", 0.0);
                    }

                    return item;
                })
                .collect(Collectors.toList());

        result.put("distribution", distribution);
        result.put("totalAmount", totalAmount);
        result.put("totalAmountYuan", formatAmount(totalAmount));
        result.put("period", "最近30天");

        return result;
    }

    /**
     * 获取所有首页数据
     */
    public Map<String, Object> getAllDashboardData(Integer trendDays) {
        Map<String, Object> result = new HashMap<>();

        result.put("overview", getDashboardOverview());
        result.put("orderStatus", getOrderStatusStatistics());
        result.put("salesTrend", getSalesTrendByDays(trendDays));
        result.put("topProducts", getTopSellingProducts(10));
        result.put("categoryDistribution", getCategoryDistribution());

        return result;
    }

    /**
     * 获取实时统计（用于首页实时刷新）
     */
    public Map<String, Object> getRealtimeStatistics() {
        Map<String, Object> result = new HashMap<>();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // 今日订单数
        LambdaQueryWrapper<OmsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getDeleted, 0);
        wrapper.ge(OmsOrder::getCreatedTime, todayStart);
        wrapper.le(OmsOrder::getCreatedTime, now);
        long todayOrders = orderMapper.selectCount(wrapper);

        // 今日销售额（已付款）
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getDeleted, 0);
        wrapper.ge(OmsOrder::getStatus, 1);
        wrapper.ge(OmsOrder::getPayTime, todayStart);
        wrapper.le(OmsOrder::getPayTime, now);
        List<OmsOrder> paidOrders = orderMapper.selectList(wrapper);
        long todaySales = paidOrders.stream()
                .mapToLong(o -> o.getPayAmount() != null ? o.getPayAmount() : 0)
                .sum();

        // 待处理订单（待发货）
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OmsOrder::getDeleted, 0);
        wrapper.eq(OmsOrder::getStatus, 1);
        long pendingOrders = orderMapper.selectCount(wrapper);

        result.put("todayOrders", todayOrders);
        result.put("todaySales", todaySales);
        result.put("todaySalesYuan", formatAmount(todaySales));
        result.put("pendingOrders", pendingOrders);
        result.put("updateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

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
