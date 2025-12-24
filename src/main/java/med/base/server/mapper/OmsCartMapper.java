package med.base.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import med.base.server.model.OmsCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 购物车 Mapper
 */
@Mapper
public interface OmsCartMapper extends BaseMapper<OmsCart> {

    /**
     * 查询用户购物车商品
     */
    @Select("SELECT * FROM oms_cart WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_time DESC")
    List<OmsCart> selectByUserId(@Param("userId") String userId);

    /**
     * 查询用户购物车中指定 SKU
     */
    @Select("SELECT * FROM oms_cart WHERE user_id = #{userId} AND sku_id = #{skuId} AND deleted = 0 LIMIT 1")
    OmsCart selectByUserIdAndSkuId(@Param("userId") String userId, @Param("skuId") String skuId);

    /**
     * 更新商品数量
     */
    @Update("UPDATE oms_cart SET quantity = #{quantity}, updated_time = NOW() WHERE cart_id = #{cartId}")
    int updateQuantity(@Param("cartId") Long cartId, @Param("quantity") Integer quantity);

    /**
     * 更新选中状态
     */
    @Update("UPDATE oms_cart SET is_selected = #{isSelected}, updated_time = NOW() WHERE cart_id = #{cartId}")
    int updateSelected(@Param("cartId") Long cartId, @Param("isSelected") Integer isSelected);

    /**
     * 批量更新选中状态
     */
    @Update("UPDATE oms_cart SET is_selected = #{isSelected}, updated_time = NOW() WHERE user_id = #{userId} AND deleted = 0")
    int updateAllSelected(@Param("userId") String userId, @Param("isSelected") Integer isSelected);

    /**
     * 删除购物车商品（软删除）
     */
    @Update("UPDATE oms_cart SET deleted = 1, updated_time = NOW() WHERE cart_id = #{cartId}")
    int softDelete(@Param("cartId") Long cartId);

    /**
     * 批量删除购物车商品（软删除）
     */
    @Update("<script>" +
            "UPDATE oms_cart SET deleted = 1, updated_time = NOW() " +
            "WHERE cart_id IN " +
            "<foreach collection='cartIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int softDeleteBatch(@Param("cartIds") List<Long> cartIds);

    /**
     * 清空用户购物车
     */
    @Update("UPDATE oms_cart SET deleted = 1, updated_time = NOW() WHERE user_id = #{userId} AND deleted = 0")
    int clearByUserId(@Param("userId") String userId);

    /**
     * 统计用户购物车商品数量
     */
    @Select("SELECT COALESCE(SUM(quantity), 0) FROM oms_cart WHERE user_id = #{userId} AND deleted = 0")
    int countByUserId(@Param("userId") String userId);

    /**
     * 查询用户已选中的购物车商品
     */
    @Select("SELECT * FROM oms_cart WHERE user_id = #{userId} AND is_selected = 1 AND deleted = 0")
    List<OmsCart> selectSelectedByUserId(@Param("userId") String userId);
}
