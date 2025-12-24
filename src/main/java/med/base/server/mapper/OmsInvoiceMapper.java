package med.base.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import med.base.server.model.OmsInvoice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 发票 Mapper
 */
@Mapper
public interface OmsInvoiceMapper extends BaseMapper<OmsInvoice> {
    
    /**
     * 根据订单ID查询发票
     */
    @Select("SELECT * FROM oms_invoice WHERE order_id = #{orderId} AND deleted = 0")
    OmsInvoice selectByOrderId(@Param("orderId") String orderId);
    
    /**
     * 根据订单编号查询发票
     */
    @Select("SELECT * FROM oms_invoice WHERE order_no = #{orderNo} AND deleted = 0")
    OmsInvoice selectByOrderNo(@Param("orderNo") String orderNo);
}
