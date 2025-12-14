package med.base.server.model;

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
public class AuthRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 2659934616449315526L;
    private Integer id;
    private String roleName;
    private int state;
    private String roleMenu;
}
