package med.base.server.controller;

import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台首页统计控制器
 */
@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 获取首页统计数据概览
     * 包含：今日/本周/本月/总体订单数、销售额、用户数等核心指标
     */
    @GetMapping("/overview")
    @UserLoginToken
    public String getOverview() {
        try {
            Map<String, Object> result = dashboardService.getDashboardOverview();
            return DefaultResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取首页统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取订单状态统计
     * 统计各状态订单数量：待付款、待发货、待收货、已完成、已取消
     */
    @GetMapping("/order-status")
    @UserLoginToken
    public String getOrderStatusStats() {
        try {
            Map<String, Object> result = dashboardService.getOrderStatusStatistics();
            return DefaultResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取订单状态统计失败：" + e.getMessage());
        }
    }

    /**
     * 获取近期销售趋势
     * 最近7天或30天的销售趋势数据
     * @param days 天数，默认7天
     */
    @GetMapping("/sales-trend")
    @UserLoginToken
    public String getSalesTrend(@RequestParam(defaultValue = "7") Integer days) {
        try {
            Map<String, Object> result = dashboardService.getSalesTrendByDays(days);
            return DefaultResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取销售趋势失败：" + e.getMessage());
        }
    }

    /**
     * 获取热销商品Top10
     */
    @GetMapping("/top-products")
    @UserLoginToken
    public String getTopProducts(@RequestParam(defaultValue = "10") Integer limit) {
        try {
            Map<String, Object> result = dashboardService.getTopSellingProducts(limit);
            return DefaultResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取热销商品失败：" + e.getMessage());
        }
    }

    /**
     * 获取分类销售占比
     * 用于饼图展示各分类的销售额占比
     */
    @GetMapping("/category-distribution")
    @UserLoginToken
    public String getCategoryDistribution() {
        try {
            Map<String, Object> result = dashboardService.getCategoryDistribution();
            return DefaultResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取分类销售分布失败：" + e.getMessage());
        }
    }

    /**
     * 获取完整的首页统计数据
     * 一次性返回所有首页需要的数据，减少请求次数
     */
    @GetMapping("/all")
    @UserLoginToken
    public String getAllDashboardData(@RequestParam(defaultValue = "7") Integer trendDays) {
        try {
            Map<String, Object> result = dashboardService.getAllDashboardData(trendDays);
            return DefaultResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取首页数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取实时订单统计（用于首页动态刷新）
     */
    @GetMapping("/realtime")
    @UserLoginToken
    public String getRealtimeStats() {
        try {
            Map<String, Object> result = dashboardService.getRealtimeStatistics();
            return DefaultResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取实时统计失败：" + e.getMessage());
        }
    }
}
