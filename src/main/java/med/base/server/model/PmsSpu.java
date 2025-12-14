package med.base.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PmsSpu implements Serializable {

    @Serial
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
    private int isDeleted;
    
    // 拨付比例
    private BigDecimal allocationRatioProvince;
    private BigDecimal allocationRatioCity;
    private BigDecimal allocationRatioDistrict;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
