package med.base.server.model.ViewModel;

import lombok.Data;
import med.base.server.model.PmsSku;
import med.base.server.model.PmsSkuSpec;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SpuModel implements Serializable {

    private static final long serialVersionUID = -5300711729378468582L;

    String spuId;
    int categoryId;
    int brandId;
    String spuName;
    String sellPoint;

    String picUrls;
    String mainImage;
    String videoUrl;

    String description;
    String status;


    BigDecimal allocationRatioCity;
    BigDecimal allocationRatioDistrict;
    BigDecimal allocationRatioProvince ;

    List<PmsSkuSpec> pmsSkuSpecList;

    List<PmsSku> skuList;

}
