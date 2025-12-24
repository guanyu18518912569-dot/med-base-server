package med.base.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import med.base.server.model.OmsOrderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface OmsOrderItemMapper extends BaseMapper<OmsOrderItem> {

    /**
     * 根据订单ID查询商品列表
     */
    @Select("SELECT * FROM oms_order_item WHERE order_id = #{orderId}")
    List<OmsOrderItem> selectByOrderId(@Param("orderId") String orderId);

    /**
     * 根据订单编号查询商品列表
     */
    @Select("SELECT * FROM oms_order_item WHERE order_no = #{orderNo}")
    List<OmsOrderItem> selectByOrderNo(@Param("orderNo") String orderNo);
}
