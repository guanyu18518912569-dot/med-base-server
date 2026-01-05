package med.base.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
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
public class PmsSpecKey implements Serializable {
    private static final long serialVersionUID = 6356725234272704889L;

    @TableId(type = IdType.AUTO)
    private Integer specKeyId;

    private String specKeyName;
    private int sort;
}
