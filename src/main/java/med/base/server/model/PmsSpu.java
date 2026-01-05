package med.base.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PmsSpu implements Serializable {

    private static final long serialVersionUID = 4531418457058369308L;

    @TableId
    private String spuId;

    private int categoryId;
    private int brandId;
    private String spuName;
    private String sellPoint;
    private String description;

    private String picUrls;
    private String videoUrl;
    private String mainImage;

    private int freightTemplateId;
    private int status;
    private int sort;
    
    @TableField("spu_deleted")
    private int spuDeleted;  // 对应数据库 spu_deleted 列
    
    // 拨付比例
    private BigDecimal allocationRatioProvince;
    private BigDecimal allocationRatioCity;
    private BigDecimal allocationRatioDistrict;
    private BigDecimal inviteIncomeRatio;
    
    // 已售数量
    private Integer salesCount;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
