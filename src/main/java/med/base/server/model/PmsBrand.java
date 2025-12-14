package med.base.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PmsBrand implements Serializable {
    @Serial
    private static final long serialVersionUID = -1590413818747309840L;

    @TableId(type = IdType.AUTO)
    private Integer brandId;
    private String brandName;
    private String logoUrl;
    private String firstLetter;
    private int sort;
}
