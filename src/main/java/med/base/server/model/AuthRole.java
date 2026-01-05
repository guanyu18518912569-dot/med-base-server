package med.base.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("auth_role")
public class AuthRole implements Serializable {

    private static final long serialVersionUID = 2659934616449315526L;
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String roleName;
    private int state;
    private String roleMenu;
}
