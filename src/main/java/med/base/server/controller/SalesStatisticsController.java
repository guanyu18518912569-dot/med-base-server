package med.base.server.controller;

import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.service.SalesStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售统计控制器
 */
@RestController
@RequestMapping("/admin/statistics/sales")
public class SalesStatisticsController {

    @Autowired
    private SalesStatisticsService salesStatisticsService;

    /**
     * 获取销售统计概览
     */
    @GetMapping("/overview")
    @UserLoginToken
    public String getOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Map<String, Object> result = salesStatisticsService.getOverview(startDate, endDate);
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("获取统计概览失败：" + e.getMessage());
        }
    }

    /**
     * 按分类统计销售额
     */
    @GetMapping("/by-category")
    @UserLoginToken
    public String getStatisticsByCategory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<Map<String, Object>> result = salesStatisticsService.getStatisticsByCategory(startDate, endDate);
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("获取分类统计失败：" + e.getMessage());
        }
    }

    /**
     * 按商品统计销售额
     */
    @GetMapping("/by-product")
    @UserLoginToken
    public String getStatisticsByProduct(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer categoryId) {
        try {
            List<Map<String, Object>> result = salesStatisticsService.getStatisticsByProduct(startDate, endDate, categoryId);
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("获取商品统计失败：" + e.getMessage());
        }
    }

    /**
     * 获取销售趋势（按日期）
     */
    @GetMapping("/trend")
    @UserLoginToken
    public String getSalesTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<Map<String, Object>> result = salesStatisticsService.getSalesTrend(startDate, endDate);
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("获取销售趋势失败：" + e.getMessage());
        }
    }

    /**
     * 获取完整统计数据（概览+分类+趋势）
     */
    @GetMapping("/all")
    @UserLoginToken
    public String getAllStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("overview", salesStatisticsService.getOverview(startDate, endDate));
            result.put("categoryStats", salesStatisticsService.getStatisticsByCategory(startDate, endDate));
            result.put("trend", salesStatisticsService.getSalesTrend(startDate, endDate));
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("获取统计数据失败：" + e.getMessage());
        }
    }
}
