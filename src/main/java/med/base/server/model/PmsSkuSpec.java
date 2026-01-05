package med.base.server.model;

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
public class PmsSkuSpec implements Serializable {
    private static final long serialVersionUID = -1233732318771663252L;


    @TableId
    private Integer skuSpecId;

    private int skuId;
    private int specKeyId;

    private String skuName;
    private String specKeyName;


}
