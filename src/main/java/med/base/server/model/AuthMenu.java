package med.base.server.model;

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
public class AuthMenu implements Serializable {

    @Serial
    private static final long serialVersionUID = -7332144446634820464L;

    private Integer id;
    private String title;
    private String href;
    private String icon;
    private String menuType;
    private int parentId;
    private int weight;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
