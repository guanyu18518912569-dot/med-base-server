package med.base.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单分成记录表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("oms_order_allocation")
public class OmsOrderAllocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String allocationId;

    // 订单ID
    private String orderId;

    // 订单编号
    private String orderNo;

    // 订单商品ID
    private String orderItemId;

    // 商品SPU ID
    private String spuId;

    // 商品名称
    private String goodsName;

    // 下单用户ID和昵称
    private String userId;
    private String nickName;

    // 获得收益的上级用户ID和昵称
    private String parentUserId;
    private String parentNickName;

    // 实付金额（分）
    private Long payAmount;

    // 购买数量
    private Integer quantity;

    // 收货地址
    private String receiverProvince;
    private String receiverCity;
    private String receiverDistrict;

    // 分成比例（%）
    private BigDecimal allocationRatioProvince;
    private BigDecimal allocationRatioCity;
    private BigDecimal allocationRatioDistrict;

    // 邀请收益比例（直推分享比例）
    private BigDecimal inviteIncomeRatio;

    // 分成金额（元）
    private BigDecimal allocationAmountProvince;
    private BigDecimal allocationAmountCity;
    private BigDecimal allocationAmountDistrict;

    // 结算状态：0-未结算 1-已结算
    private Integer settlementStatus;

    // 结算时间
    private LocalDateTime settlementTime;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
