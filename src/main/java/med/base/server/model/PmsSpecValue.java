package med.base.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PmsSpecValue implements Serializable {
    private static final long serialVersionUID = -6050833337470781957L;

    @TableId(type = IdType.AUTO)
    private Integer specValueId;

    private int specKeyId;
    private String specValueName;

    private int sort;
}
