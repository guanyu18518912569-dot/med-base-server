package med.base.server.model;

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
public class UmsMemberLevel implements Serializable {
    private static final long serialVersionUID = 5498689220044360445L;


    @TableId
    private Integer levelId;

    private String levelName;
    private int level;
    private BigDecimal discount;
    private int growthNeeded;

    private int isDefault;
    private int isDeleted;
}
