package med.base.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收货地址表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ums_address")
public class UmsAddress implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String addressId;
    
    // 用户ID
    private String userId;
    
    // 收货人姓名
    private String receiverName;
    
    // 收货人电话
    private String receiverPhone;
    
    // 省份编码
    private String provinceCode;
    
    // 省份名称
    private String provinceName;
    
    // 城市编码
    private String cityCode;
    
    // 城市名称
    private String cityName;
    
    // 区县编码
    private String districtCode;
    
    // 区县名称
    private String districtName;
    
    // 详细地址
    private String detailAddress;
    
    // 完整地址
    private String fullAddress;
    
    // 地址标签
    private String tag;
    
    // 是否默认地址
    private Boolean isDefault;
    
    // 纬度
    private BigDecimal latitude;
    
    // 经度
    private BigDecimal longitude;
    
    // 创建时间
    private LocalDateTime createdTime;
    
    // 更新时间
    private LocalDateTime updatedTime;
}
