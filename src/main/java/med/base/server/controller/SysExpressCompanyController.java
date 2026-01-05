package med.base.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.mapper.SysExpressCompanyMapper;
import med.base.server.model.SysExpressCompany;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 快递公司模板管理控制器
 */
@RestController
@RequestMapping("/admin/express-company")
public class SysExpressCompanyController {

    @Autowired
    private SysExpressCompanyMapper expressCompanyMapper;

    /**
     * 分页查询快递公司列表
     */
    @GetMapping("/list")
    @UserLoginToken
    public String list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String companyCode,
            @RequestParam(required = false) Integer status) {
        try {
            LambdaQueryWrapper<SysExpressCompany> wrapper = new LambdaQueryWrapper<>();
            
            if (StringUtils.hasLength(companyName)) {
                wrapper.like(SysExpressCompany::getCompanyName, companyName);
            }
            if (StringUtils.hasLength(companyCode)) {
                wrapper.like(SysExpressCompany::getCompanyCode, companyCode);
            }
            if (status != null) {
                wrapper.eq(SysExpressCompany::getStatus, status);
            }
            
            wrapper.orderByAsc(SysExpressCompany::getSort);
            
            Page<SysExpressCompany> page = new Page<>(pageNum, pageSize);
            IPage<SysExpressCompany> result = expressCompanyMapper.selectPage(page, wrapper);
            
            Map<String, Object> data = new HashMap<>();
            data.put("list", result.getRecords());
            data.put("total", result.getTotal());
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);
            
            return DefaultResponse.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取快递公司列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有启用的快递公司（用于下拉选择）
     */
    @GetMapping("/options")
    @UserLoginToken
    public String options() {
        try {
            LambdaQueryWrapper<SysExpressCompany> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysExpressCompany::getStatus, 1);
            wrapper.orderByAsc(SysExpressCompany::getSort);
            
            List<SysExpressCompany> list = expressCompanyMapper.selectList(wrapper);
            return DefaultResponse.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取快递公司选项失败：" + e.getMessage());
        }
    }

    /**
     * 获取快递公司详情
     */
    @GetMapping("/detail/{id}")
    @UserLoginToken
    public String getDetail(@PathVariable Integer id) {
        try {
            SysExpressCompany company = expressCompanyMapper.selectById(id);
            if (company == null) {
                return DefaultResponse.error("快递公司不存在");
            }
            return DefaultResponse.success(company);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取快递公司详情失败：" + e.getMessage());
        }
    }

    /**
     * 新增快递公司
     */
    @PostMapping("/add")
    @UserLoginToken
    public String add(@RequestBody ExpressCompanyDTO dto) {
        try {
            // 检查编码是否已存在
            LambdaQueryWrapper<SysExpressCompany> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysExpressCompany::getCompanyCode, dto.getCompanyCode());
            if (expressCompanyMapper.selectCount(wrapper) > 0) {
                return DefaultResponse.error("快递公司编码已存在");
            }

            SysExpressCompany company = SysExpressCompany.builder()
                    .companyCode(dto.getCompanyCode())
                    .companyName(dto.getCompanyName())
                    .logo(dto.getLogo())
                    .phone(dto.getPhone())
                    .website(dto.getWebsite())
                    .sort(dto.getSort() != null ? dto.getSort() : 0)
                    .status(dto.getStatus() != null ? dto.getStatus() : 1)
                    .remark(dto.getRemark())
                    .createdTime(LocalDateTime.now())
                    .updatedTime(LocalDateTime.now())
                    .build();

            expressCompanyMapper.insert(company);
            return DefaultResponse.success();
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("添加快递公司失败：" + e.getMessage());
        }
    }

    /**
     * 更新快递公司
     */
    @PostMapping("/update")
    @UserLoginToken
    public String update(@RequestBody ExpressCompanyDTO dto) {
        try {
            if (dto.getId() == null) {
                return DefaultResponse.error("ID不能为空");
            }

            SysExpressCompany company = expressCompanyMapper.selectById(dto.getId());
            if (company == null) {
                return DefaultResponse.error("快递公司不存在");
            }

            // 检查编码是否被其他记录使用
            if (dto.getCompanyCode() != null && !dto.getCompanyCode().equals(company.getCompanyCode())) {
                LambdaQueryWrapper<SysExpressCompany> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SysExpressCompany::getCompanyCode, dto.getCompanyCode());
                wrapper.ne(SysExpressCompany::getId, dto.getId());
                if (expressCompanyMapper.selectCount(wrapper) > 0) {
                    return DefaultResponse.error("快递公司编码已存在");
                }
                company.setCompanyCode(dto.getCompanyCode());
            }

            if (dto.getCompanyName() != null) {
                company.setCompanyName(dto.getCompanyName());
            }
            if (dto.getLogo() != null) {
                company.setLogo(dto.getLogo());
            }
            if (dto.getPhone() != null) {
                company.setPhone(dto.getPhone());
            }
            if (dto.getWebsite() != null) {
                company.setWebsite(dto.getWebsite());
            }
            if (dto.getSort() != null) {
                company.setSort(dto.getSort());
            }
            if (dto.getStatus() != null) {
                company.setStatus(dto.getStatus());
            }
            if (dto.getRemark() != null) {
                company.setRemark(dto.getRemark());
            }
            company.setUpdatedTime(LocalDateTime.now());

            expressCompanyMapper.updateById(company);
            return DefaultResponse.success();
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("更新快递公司失败：" + e.getMessage());
        }
    }

    /**
     * 删除快递公司
     */
    @DeleteMapping("/delete/{id}")
    @UserLoginToken
    public String delete(@PathVariable Integer id) {
        try {
            SysExpressCompany company = expressCompanyMapper.selectById(id);
            if (company == null) {
                return DefaultResponse.error("快递公司不存在");
            }

            expressCompanyMapper.deleteById(id);
            return DefaultResponse.success();
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("删除快递公司失败：" + e.getMessage());
        }
    }

    /**
     * 修改快递公司状态
     */
    @PostMapping("/changeStatus")
    @UserLoginToken
    public String changeStatus(@RequestParam Integer id, @RequestParam Integer status) {
        try {
            SysExpressCompany company = expressCompanyMapper.selectById(id);
            if (company == null) {
                return DefaultResponse.error("快递公司不存在");
            }

            company.setStatus(status);
            company.setUpdatedTime(LocalDateTime.now());
            expressCompanyMapper.updateById(company);

            return DefaultResponse.success();
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("修改状态失败：" + e.getMessage());
        }
    }
}

@Data
class ExpressCompanyDTO {
    private Integer id;
    private String companyCode;
    private String companyName;
    private String logo;
    private String phone;
    private String website;
    private Integer sort;
    private Integer status;
    private String remark;
}
