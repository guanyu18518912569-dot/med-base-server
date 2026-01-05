package med.base.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import med.base.server.model.OmsOrderAllocation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface OmsOrderAllocationMapper extends BaseMapper<OmsOrderAllocation> {

    /**
     * 按省份统计分成金额
     */
    @Select("SELECT receiver_province as region, " +
            "SUM(allocation_amount_province) as totalAmount, " +
            "COUNT(DISTINCT order_id) as orderCount " +
            "FROM oms_order_allocation " +
            "WHERE receiver_province = #{province} " +
            "GROUP BY receiver_province")
    Map<String, Object> sumByProvince(@Param("province") String province);

    /**
     * 按市统计分成金额
     */
    @Select("SELECT receiver_city as region, " +
            "SUM(allocation_amount_city) as totalAmount, " +
            "COUNT(DISTINCT order_id) as orderCount " +
            "FROM oms_order_allocation " +
            "WHERE receiver_province = #{province} AND receiver_city = #{city} " +
            "GROUP BY receiver_city")
    Map<String, Object> sumByCity(@Param("province") String province, @Param("city") String city);

    /**
     * 按区统计分成金额
     */
    @Select("SELECT receiver_district as region, " +
            "SUM(allocation_amount_district) as totalAmount, " +
            "COUNT(DISTINCT order_id) as orderCount " +
            "FROM oms_order_allocation " +
            "WHERE receiver_province = #{province} AND receiver_city = #{city} AND receiver_district = #{district} " +
            "GROUP BY receiver_district")
    Map<String, Object> sumByDistrict(@Param("province") String province, @Param("city") String city, @Param("district") String district);

    /**
     * 查询所有省份的分成统计
     */
    @Select("SELECT receiver_province as region, " +
            "SUM(allocation_amount_province) as totalAmount, " +
            "COUNT(DISTINCT order_id) as orderCount " +
            "FROM oms_order_allocation " +
            "GROUP BY receiver_province " +
            "ORDER BY totalAmount DESC")
    List<Map<String, Object>> sumAllProvince();

    /**
     * 查询所有市的分成统计
     */
    @Select("SELECT receiver_province, receiver_city as region, " +
            "SUM(allocation_amount_city) as totalAmount, " +
            "COUNT(DISTINCT order_id) as orderCount " +
            "FROM oms_order_allocation " +
            "GROUP BY receiver_province, receiver_city " +
            "ORDER BY totalAmount DESC")
    List<Map<String, Object>> sumAllCity();

    /**
     * 查询所有区的分成统计
     */
    @Select("SELECT receiver_province, receiver_city, receiver_district as region, " +
            "SUM(allocation_amount_district) as totalAmount, " +
            "COUNT(DISTINCT order_id) as orderCount " +
            "FROM oms_order_allocation " +
            "GROUP BY receiver_province, receiver_city, receiver_district " +
            "ORDER BY totalAmount DESC")
    List<Map<String, Object>> sumAllDistrict();

    /**
     * 查询分成明细列表
     */
    @Select("<script>" +
            "SELECT * FROM oms_order_allocation " +
            "WHERE 1=1 " +
            "<if test='province != null and province != \"\"'> AND receiver_province = #{province}</if> " +
            "<if test='city != null and city != \"\"'> AND receiver_city = #{city}</if> " +
            "<if test='district != null and district != \"\"'> AND receiver_district = #{district}</if> " +
            "ORDER BY created_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<OmsOrderAllocation> selectByRegion(@Param("province") String province,
                                             @Param("city") String city,
                                             @Param("district") String district,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    /**
     * 统计分成明细总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM oms_order_allocation " +
            "WHERE 1=1 " +
            "<if test='province != null and province != \"\"'> AND receiver_province = #{province}</if> " +
            "<if test='city != null and city != \"\"'> AND receiver_city = #{city}</if> " +
            "<if test='district != null and district != \"\"'> AND receiver_district = #{district}</if> " +
            "</script>")
    int countByRegion(@Param("province") String province,
                      @Param("city") String city,
                      @Param("district") String district);

    /**
     * 根据订单ID查询分成记录
     */
    @Select("SELECT * FROM oms_order_allocation WHERE order_id = #{orderId}")
    List<OmsOrderAllocation> selectByOrderId(@Param("orderId") String orderId);

    /**
     * 计算用户的已结算收益（根据 parentUserId）
     */
    @Select("SELECT COALESCE(SUM(pay_amount / 100.0 * invite_income_ratio), 0) " +
            "FROM oms_order_allocation " +
            "WHERE parent_user_id = #{userId} " +
            "AND settlement_status = 1")
    BigDecimal sumSettledIncome(@Param("userId") String userId);

    /**
     * 计算用户的未结算收益（根据 parentUserId）
     */
    @Select("SELECT COALESCE(SUM(pay_amount / 100.0 * invite_income_ratio), 0) " +
            "FROM oms_order_allocation " +
            "WHERE parent_user_id = #{userId} " +
            "AND settlement_status = 0")
    BigDecimal sumUnsettledIncome(@Param("userId") String userId);
}
