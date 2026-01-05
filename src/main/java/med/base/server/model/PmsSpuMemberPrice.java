package med.base.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PmsSpuMemberPrice implements Serializable {
    private static final long serialVersionUID = 5494628696007742941L;

    @TableId
    private Integer spuMemberPriceId;

    private int spuId;
    private int memberLevelId;
    private BigDecimal discount;
}
