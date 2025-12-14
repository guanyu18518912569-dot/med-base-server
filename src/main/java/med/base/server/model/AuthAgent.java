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
public class AuthAgent implements Serializable {
    @Serial
    private static final long serialVersionUID = -2044843579953078622L;

    private Integer id;
    private String nickName;

    private String password;
    private String phone;
    private String country;
    private String province;
    private String city;
    private int state;
    private int roleId;
    private int ratio;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
