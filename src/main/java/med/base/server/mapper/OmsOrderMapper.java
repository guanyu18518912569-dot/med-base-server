package med.base.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import med.base.server.model.OmsOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface OmsOrderMapper extends BaseMapper<OmsOrder> {

    /**
     * 根据订单编号查询
     */
    @Select("SELECT * FROM oms_order WHERE order_no = #{orderNo} AND deleted = 0")
    OmsOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询用户订单列表
     */
    @Select("SELECT * FROM oms_order WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_time DESC")
    List<OmsOrder> selectByUserId(@Param("userId") String userId);

    /**
     * 根据状态查询用户订单
     */
    @Select("SELECT * FROM oms_order WHERE user_id = #{userId} AND status = #{status} AND deleted = 0 ORDER BY created_time DESC")
    List<OmsOrder> selectByUserIdAndStatus(@Param("userId") String userId, @Param("status") Integer status);

    /**
     * 更新订单状态
     */
    @Update("UPDATE oms_order SET status = #{status}, updated_time = NOW() WHERE order_id = #{orderId}")
    int updateStatus(@Param("orderId") String orderId, @Param("status") Integer status);

    /**
     * 更新支付状态
     */
    @Update("UPDATE oms_order SET pay_status = #{payStatus}, pay_time = NOW(), status = 1, updated_time = NOW() WHERE order_id = #{orderId}")
    int updatePayStatus(@Param("orderId") String orderId, @Param("payStatus") Integer payStatus);

    /**
     * 根据订单编号更新支付状态（微信支付回调使用）
     */
    @Update("UPDATE oms_order SET pay_status = 1, pay_time = NOW(), status = 1, transaction_id = #{transactionId}, updated_time = NOW() WHERE order_no = #{orderNo} AND status = 0")
    int updatePayStatusByOrderNo(@Param("orderNo") String orderNo, @Param("transactionId") String transactionId);

    /**
     * 取消订单
     */
    @Update("UPDATE oms_order SET status = 4, cancel_time = NOW(), updated_time = NOW() WHERE order_id = #{orderId} AND status = 0")
    int cancelOrder(@Param("orderId") String orderId);

    /**
     * 确认收货
     */
    @Update("UPDATE oms_order SET status = 3, receive_time = NOW(), finish_time = NOW(), updated_time = NOW() WHERE order_id = #{orderId} AND status = 2")
    int confirmReceive(@Param("orderId") String orderId);

    /**
     * 发货
     */
    @Update("UPDATE oms_order SET status = 2, delivery_time = NOW(), delivery_company = #{deliveryCompany}, delivery_sn = #{deliverySn}, updated_time = NOW() WHERE order_id = #{orderId} AND status = 1")
    int deliverOrder(@Param("orderId") String orderId, @Param("deliveryCompany") String deliveryCompany, @Param("deliverySn") String deliverySn);

    /**
     * 统计用户各状态订单数量
     */
    @Select("SELECT status, COUNT(*) as count FROM oms_order WHERE user_id = #{userId} AND deleted = 0 GROUP BY status")
    List<java.util.Map<String, Object>> countByStatus(@Param("userId") String userId);
}
