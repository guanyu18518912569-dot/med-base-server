package med.base.server.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 省市县区域实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_region")
public class SysRegion {
    
    /**
     * 区域ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 区域名称
     */
    private String name;
    
    /**
     * 父级ID，0表示顶级
     */
    private Long parentId;
    
    /**
     * 层级：1-省份，2-城市，3-区县
     */
    private Integer level;
    
    /**
     * 排序
     */
    private Integer sort;
    
    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
