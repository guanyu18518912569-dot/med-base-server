package med.base.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 发票表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("oms_invoice")
public class OmsInvoice implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private String invoiceId;
    
    // 关联订单ID
    private String orderId;
    
    // 订单编号
    private String orderNo;
    
    // 用户ID
    private String userId;
    
    // ========== 发票基本信息 ==========
    
    // 发票类型：0-不开发票 5-电子普通发票
    private Integer invoiceType;
    
    // 抬头类型：1-个人 2-公司
    private Integer titleType;
    
    // 发票抬头（个人姓名或公司名称）
    private String invoiceTitle;
    
    // 纳税人识别号（公司开票必填）
    private String taxNo;
    
    // 发票内容类型：1-商品明细 2-商品类别
    private Integer contentType;
    
    // ========== 收票人信息 ==========
    
    // 发票接收邮箱
    private String email;
    
    // 发票接收手机号
    private String phone;
    
    // ========== 发票金额 ==========
    
    // 发票金额（分）
    private Long amount;
    
    // ========== 开票状态 ==========
    
    // 开票状态：0-待开票 1-已开票 2-开票失败
    private Integer invoiceStatus;
    
    // 发票号码
    private String invoiceNo;
    
    // 电子发票下载链接
    private String invoiceUrl;
    
    // 开票时间
    private LocalDateTime invoiceTime;
    
    // ========== 其他 ==========
    
    // 备注
    private String remark;
    
    // 删除标记：0-正常 1-已删除
    private Integer deleted;
    
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
