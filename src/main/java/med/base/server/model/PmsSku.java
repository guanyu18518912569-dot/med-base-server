package med.base.server.model;

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
public class PmsSku  implements Serializable {
    @Serial
    private static final long serialVersionUID = 7121218300015146842L;



    @TableId
    private String skuId;
    private String spuId;
    private String skuCode;

    private BigDecimal price;
    private BigDecimal marketPrice;
    private BigDecimal costPrice;
    private int stock;
    private int warnStock;
    private String picUrl;


    private int isDeleted;

    private String specs;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
