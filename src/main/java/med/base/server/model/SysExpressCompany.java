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

/**
 * 快递公司模板表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_express_company")
public class SysExpressCompany implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 快递公司编码
     */
    @TableField("company_code")
    private String companyCode;

    /**
     * 快递公司名称
     */
    @TableField("company_name")
    private String companyName;

    /**
     * 公司Logo
     */
    private String logo;

    /**
     * 客服电话
     */
    private String phone;

    /**
     * 官网地址
     */
    private String website;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    @TableField("created_time")
    private LocalDateTime createdTime;

    @TableField("updated_time")
    private LocalDateTime updatedTime;
}
