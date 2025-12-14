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
public class PmsSpuImage implements Serializable {
    @Serial
    private static final long serialVersionUID = 161367749857588332L;

    @TableId
    private Integer imageId;

    private int spuId;
    private String url;
    private int type;
    private int sort;
}
