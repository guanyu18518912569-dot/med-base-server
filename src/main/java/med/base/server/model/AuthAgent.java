package med.base.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("auth_agent")
public class AuthAgent implements Serializable {
    private static final long serialVersionUID = -2044843579953078622L;

    @TableId(type = IdType.AUTO)
    private Integer id;
    
    @TableField("nick_name")
    private String nickName;

    private String password;
    private String phone;
    private String country;  // 区/县
    
    /**
     * 区域类型：
     * 0-管理员（可查看所有区域）
     * 1-省级（只能查看本省数据）
     * 2-市级（只能查看本市数据）
     * 3-区级（只能查看本区数据）
     */
    @TableField("region_type")
    private Integer regionType;
    
    private String province;
    private String city;
    private Integer state;
    
    @TableField("role_id")
    private Integer roleId;
    
    private Integer ratio;
    
    @TableField("created_time")
    private LocalDateTime createdTime;
    
    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
