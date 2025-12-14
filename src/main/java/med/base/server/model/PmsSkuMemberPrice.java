package med.base.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PmsSkuMemberPrice implements Serializable {
    @Serial
    private static final long serialVersionUID = 6793985854876099749L;

    @TableId
    private Integer skuMemberPriceId;

    private int skuId;
    private int memberLevelId;
    private BigDecimal discount;
}
