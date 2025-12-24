package med.base.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单商品明细表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("oms_order_item")
public class OmsOrderItem implements Serializable {

    @Serial
    private static final long serialVersionUID = -3491364576174962458L;
    
    @TableId
    private String orderItemId;
    
    // 订单ID
    private String orderId;
    
    // 订单编号
    private String orderNo;
    
    // ========== 商品信息 ==========
    
    // SPU ID
    private String spuId;
    
    // SKU ID
    private String skuId;
    
    // 商品名称
    private String goodsName;
    
    // 商品图片
    private String goodsImage;
    
    // 商品规格（JSON格式）
    private String specs;
    
    // 商品单价（分）
    private Long price;
    
    // 购买数量
    private Integer quantity;
    
    // 小计金额（分）= price * quantity
    private Long totalAmount;
    
    // 实付金额（分）
    private Long payAmount;
    
    // ========== 促销信息 ==========
    
    // 优惠券抵扣（分）
    private Long couponAmount;
    
    // 促销优惠（分）
    private Long promotionAmount;
    
    private LocalDateTime createdTime;
}
