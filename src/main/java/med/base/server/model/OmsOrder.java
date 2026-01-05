package med.base.server.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单主表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("oms_order")
public class OmsOrder implements Serializable {

    private static final long serialVersionUID = -5283072429574961307L;

    @TableId
    private String orderId;
    
    // 订单编号（展示用）
    private String orderNo;
    
    // 用户ID
    private String userId;
    
    // 门店ID
    private String storeId;
    
    // ========== 订单金额 ==========
    
    // 商品总金额（分）
    private Long totalAmount;
    
    // 运费（分）
    private Long freightAmount;
    
    // 优惠券抵扣金额（分）
    private Long couponAmount;
    
    // 促销优惠金额（分）
    private Long promotionAmount;
    
    // 实付金额（分）
    private Long payAmount;
    
    // ========== 收货信息 ==========
    
    // 收货人姓名
    private String receiverName;
    
    // 收货人电话
    private String receiverPhone;
    
    // 省
    private String receiverProvince;
    
    // 市
    private String receiverCity;
    
    // 区
    private String receiverDistrict;
    
    // 详细地址
    private String receiverAddress;
    
    // 完整地址
    private String receiverFullAddress;
    
    // ========== 订单状态 ==========
    
    /**
     * 订单状态：
     * 0-待付款
     * 1-待发货
     * 2-待收货
     * 3-已完成
     * 4-已取消
     * 5-售后中
     */
    private Integer status;
    
    // 支付状态：0-未支付 1-已支付
    private Integer payStatus;
    
    // 支付方式：1-微信支付
    private Integer payType;
    
    // 微信支付交易号
    private String transactionId;
    
    // 支付时间
    private LocalDateTime payTime;
    
    // 发货时间
    private LocalDateTime deliveryTime;
    
    // 收货时间
    private LocalDateTime receiveTime;
    
    // 完成时间
    private LocalDateTime finishTime;
    
    // 取消时间
    private LocalDateTime cancelTime;
    
    // ========== 物流信息 ==========
    
    // 物流公司
    private String deliveryCompany;
    
    // 物流单号
    private String deliverySn;
    
    // ========== 其他 ==========
    
    // 订单备注
    private String remark;
    
    // 商品数量
    private Integer goodsCount;
    
    // 删除标记：0-正常 1-已删除
    private Integer deleted;
    
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    
    // ========== 非数据库字段 ==========
    
    // 订单商品列表
    @TableField(exist = false)
    private List<OmsOrderItem> orderItems;
}
