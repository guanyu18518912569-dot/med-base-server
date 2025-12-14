package med.base.server.model.ViewModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SPU 列表展示 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpuPageVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String spuId;
    private String spuName;
    
    // 分类信息
    private Integer categoryId;
    private String categoryName;
    
    // 品牌信息
    private Integer brandId;
    private String brandName;
    
    // 拨付比例
    private java.math.BigDecimal allocationRatioProvince;
    private java.math.BigDecimal allocationRatioCity;
    private java.math.BigDecimal allocationRatioDistrict;
    
    private Integer status;
    private LocalDateTime createdTime;
}
