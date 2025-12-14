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
public class PmsSkuSpuRatio implements Serializable {
    @Serial
    private static final long serialVersionUID = -6268032385353526148L;

    @TableId
    private Integer ratioId;

    private int spuId;
    private int skuId;
    private int level;
    private BigDecimal levelRatio;
}
