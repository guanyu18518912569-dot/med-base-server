package med.base.server.model;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PmsSpuAttr  implements Serializable {
    @Serial
    private static final long serialVersionUID = -4694520410084760645L;


    @TableId
    private Integer spuAttrId;

    private int spuId;
    private String attrName;
    private String attrValue;
    private String isSearch;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
