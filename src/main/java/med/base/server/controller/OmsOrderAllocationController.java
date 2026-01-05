package med.base.server.controller;

import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.model.AuthAgent;
import med.base.server.model.OmsOrderAllocation;
import med.base.server.service.AuthAgentService;
import med.base.server.service.OmsOrderAllocationService;
import med.base.server.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 订单分成统计控制器
 */
@RestController
@RequestMapping("/admin/allocation")
public class OmsOrderAllocationController {

    @Autowired
    private OmsOrderAllocationService allocationService;

    @Autowired
    private AuthAgentService authAgentService;

    @Autowired
    private TokenService tokenService;

    /**
     * 获取分成统计
     * 根据当前登录用户的区域类型返回对应的统计数据
     */
    @GetMapping("/statistics")
    @UserLoginToken
    public String getStatistics(HttpServletRequest request) {
        try {
            // 获取当前登录用户
            AuthAgent currentUser = getCurrentUser(request);
            if (currentUser == null) {
                return DefaultResponse.error("请先登录");
            }

            Integer regionType = currentUser.getRegionType();
            if (regionType == null) {
                regionType = 0; // 默认管理员
            }

            Map<String, Object> stats;
            switch (regionType) {
                case 1: // 省级账号
                    stats = allocationService.getStatisticsForProvince(currentUser.getProvince());
                    break;
                case 2: // 市级账号
                    stats = allocationService.getStatisticsForCity(
                            currentUser.getProvince(), 
                            currentUser.getCity());
                    break;
                case 3: // 区级账号
                    stats = allocationService.getStatisticsForDistrict(
                            currentUser.getProvince(), 
                            currentUser.getCity(), 
                            currentUser.getCountry());
                    break;
                default: // 管理员
                    stats = allocationService.getStatisticsForAdmin();
                    break;
            }

            stats.put("regionType", regionType);
            stats.put("userName", currentUser.getNickName());

            return DefaultResponse.success(stats);
        } catch (Exception e) {
            return DefaultResponse.error("获取统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取分成明细列表（分页）
     */
    @GetMapping("/list")
    @UserLoginToken
    public String getAllocationList(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        try {
            // 获取当前登录用户
            AuthAgent currentUser = getCurrentUser(request);
            if (currentUser == null) {
                return DefaultResponse.error("请先登录");
            }

            Integer regionType = currentUser.getRegionType();
            if (regionType == null) {
                regionType = 0;
            }

            // 根据用户区域类型限制查询范围
            String queryProvince = province;
            String queryCity = city;
            String queryDistrict = district;

            switch (regionType) {
                case 1: // 省级账号只能查本省
                    queryProvince = currentUser.getProvince();
                    break;
                case 2: // 市级账号只能查本市
                    queryProvince = currentUser.getProvince();
                    queryCity = currentUser.getCity();
                    break;
                case 3: // 区级账号只能查本区
                    queryProvince = currentUser.getProvince();
                    queryCity = currentUser.getCity();
                    queryDistrict = currentUser.getCountry();
                    break;
                default: // 管理员可以查所有
                    break;
            }

            Map<String, Object> result = allocationService.getAllocationList(
                    queryProvince, queryCity, queryDistrict, page, pageSize);
            
            result.put("regionType", regionType);
            
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("获取分成明细失败：" + e.getMessage());
        }
    }

    /**
     * 根据订单ID查询分成记录
     */
    @GetMapping("/order/{orderId}")
    @UserLoginToken
    public String getByOrderId(@PathVariable String orderId) {
        try {
            List<OmsOrderAllocation> list = allocationService.getByOrderId(orderId);
            return DefaultResponse.success(list);
        } catch (Exception e) {
            return DefaultResponse.error("获取订单分成记录失败：" + e.getMessage());
        }
    }

    /**
     * 从请求中获取当前登录用户
     */
    private AuthAgent getCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            int userId = tokenService.getUserIdFromToken(token);
            return authAgentService.getAuthAgentByUserId(userId);
        } catch (Exception e) {
            return null;
        }
    }
}
