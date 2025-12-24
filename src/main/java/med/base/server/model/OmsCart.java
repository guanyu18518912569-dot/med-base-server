package med.base.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * 购物车表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("oms_cart")
public class OmsCart implements Serializable {

    @Serial
    private static final long serialVersionUID = -5120968924389429993L;

    @TableId(type = IdType.AUTO)
    private Long cartId;
    
    // 用户ID
    private String userId;
    
    // 门店ID
    private String storeId;
    
    // SPU ID
    private String spuId;
    
    // SKU ID
    private String skuId;
    
    // 商品名称
    private String goodsName;
    
    // 商品图片
    private String goodsImage;
    
    // 规格信息（JSON格式）
    private String specs;
    
    // 单价（分）
    private Long price;
    
    // 数量
    private Integer quantity;
    
    // 是否选中：0-未选中 1-已选中
    private Integer isSelected;
    
    // 是否删除：0-未删除 1-已删除
    private Integer deleted;
    
    // 创建时间
    private LocalDateTime createdTime;
    
    // 更新时间
    private LocalDateTime updatedTime;
}
