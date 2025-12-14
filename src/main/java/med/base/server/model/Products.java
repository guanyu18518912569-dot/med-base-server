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
public class Products implements Serializable {
    @Serial
    private static final long serialVersionUID = -5563913894246426452L;

    @TableId
    private Integer productId;

    private String productName;

    private String productType;
    private String productImageUrl;
    private String productPrice;
    private String productScore;
    private String jdPrice;
    private int productState;
    private int storage;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
