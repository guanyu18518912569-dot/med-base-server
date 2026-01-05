package med.base.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.mapper.AuthAgentMapper;
import med.base.server.mapper.AuthRoleMapper;
import med.base.server.model.AuthAgent;
import med.base.server.model.AuthRole;
import med.base.server.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台用户管理控制器
 */
@RestController
@RequestMapping("/admin/auth-agent")
public class AuthAgentManageController {

    @Autowired
    private AuthAgentMapper authAgentMapper;

    @Autowired
    private AuthRoleMapper authRoleMapper;

    /**
     * 分页查询后台用户列表
     */
    @GetMapping("/list")
    @UserLoginToken
    public String list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String nickName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer regionType,
            @RequestParam(required = false) Integer state) {
        try {
            LambdaQueryWrapper<AuthAgent> wrapper = new LambdaQueryWrapper<>();
            
            if (StringUtils.hasLength(nickName)) {
                wrapper.like(AuthAgent::getNickName, nickName);
            }
            if (StringUtils.hasLength(phone)) {
                wrapper.like(AuthAgent::getPhone, phone);
            }
            if (regionType != null) {
                wrapper.eq(AuthAgent::getRegionType, regionType);
            }
            if (state != null) {
                wrapper.eq(AuthAgent::getState, state);
            }
            
            wrapper.orderByDesc(AuthAgent::getId);
            
            Page<AuthAgent> page = new Page<>(pageNum, pageSize);
            IPage<AuthAgent> result = authAgentMapper.selectPage(page, wrapper);
            
            // 获取角色列表用于显示角色名称
            List<AuthRole> roles = authRoleMapper.selectList(null);
            Map<Integer, String> roleMap = new HashMap<>();
            for (AuthRole role : roles) {
                roleMap.put(role.getId(), role.getRoleName());
            }
            
            // 构建返回结果
            Map<String, Object> data = new HashMap<>();
            data.put("list", result.getRecords());
            data.put("total", result.getTotal());
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);
            data.put("roleMap", roleMap);
            
            return DefaultResponse.success(data);
        } catch (Exception e) {
            return DefaultResponse.error("获取用户列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/detail/{id}")
    @UserLoginToken
    public String getDetail(@PathVariable Integer id) {
        try {
            AuthAgent agent = authAgentMapper.selectById(id);
            if (agent == null) {
                return DefaultResponse.error("用户不存在");
            }
            // 不返回密码
            agent.setPassword(null);
            return DefaultResponse.success(agent);
        } catch (Exception e) {
            return DefaultResponse.error("获取用户详情失败：" + e.getMessage());
        }
    }

    /**
     * 添加后台用户
     */
    @PostMapping("/add")
    @UserLoginToken
    public String add(@RequestBody AuthAgentDTO dto) {
        try {
            // 检查手机号是否已存在
            LambdaQueryWrapper<AuthAgent> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AuthAgent::getPhone, dto.getPhone());
            AuthAgent existing = authAgentMapper.selectOne(wrapper);
            if (existing != null) {
                return DefaultResponse.error("手机号已存在");
            }
            
            AuthAgent agent = AuthAgent.builder()
                    .nickName(dto.getNickName())
                    .phone(dto.getPhone())
                    .password(MD5Util.getMD5(dto.getPassword()))
                    .regionType(dto.getRegionType())
                    .province(dto.getProvince())
                    .city(dto.getCity())
                    .country(dto.getCountry())
                    .roleId(dto.getRoleId() != null ? dto.getRoleId() : 2)
                    .ratio(dto.getRatio() != null ? dto.getRatio() : 0)
                    .state(dto.getState() != null ? dto.getState() : 1)
                    .createdTime(LocalDateTime.now())
                    .updatedTime(LocalDateTime.now())
                    .build();
            
            authAgentMapper.insert(agent);
            return DefaultResponse.success("添加成功");
        } catch (Exception e) {
            return DefaultResponse.error("添加用户失败：" + e.getMessage());
        }
    }

    /**
     * 更新后台用户
     */
    @PostMapping("/update")
    @UserLoginToken
    public String update(@RequestBody AuthAgentDTO dto) {
        try {
            if (dto.getId() == null) {
                return DefaultResponse.error("用户ID不能为空");
            }
            
            AuthAgent agent = authAgentMapper.selectById(dto.getId());
            if (agent == null) {
                return DefaultResponse.error("用户不存在");
            }
            
            // 检查手机号是否被其他用户使用
            if (StringUtils.hasLength(dto.getPhone()) && !dto.getPhone().equals(agent.getPhone())) {
                LambdaQueryWrapper<AuthAgent> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(AuthAgent::getPhone, dto.getPhone());
                wrapper.ne(AuthAgent::getId, dto.getId());
                AuthAgent existing = authAgentMapper.selectOne(wrapper);
                if (existing != null) {
                    return DefaultResponse.error("手机号已被其他用户使用");
                }
            }
            
            // 更新字段
            if (StringUtils.hasLength(dto.getNickName())) {
                agent.setNickName(dto.getNickName());
            }
            if (StringUtils.hasLength(dto.getPhone())) {
                agent.setPhone(dto.getPhone());
            }
            if (StringUtils.hasLength(dto.getPassword())) {
                agent.setPassword(MD5Util.getMD5(dto.getPassword()));
            }
            if (dto.getRegionType() != null) {
                agent.setRegionType(dto.getRegionType());
            }
            if (dto.getProvince() != null) {
                agent.setProvince(dto.getProvince());
            }
            if (dto.getCity() != null) {
                agent.setCity(dto.getCity());
            }
            if (dto.getCountry() != null) {
                agent.setCountry(dto.getCountry());
            }
            if (dto.getRoleId() != null) {
                agent.setRoleId(dto.getRoleId());
            }
            if (dto.getRatio() != null) {
                agent.setRatio(dto.getRatio());
            }
            if (dto.getState() != null) {
                agent.setState(dto.getState());
            }
            agent.setUpdatedTime(LocalDateTime.now());
            
            authAgentMapper.updateById(agent);
            return DefaultResponse.success("更新成功");
        } catch (Exception e) {
            return DefaultResponse.error("更新用户失败：" + e.getMessage());
        }
    }

    /**
     * 删除后台用户
     */
    @PostMapping("/delete/{id}")
    @UserLoginToken
    public String delete(@PathVariable Integer id) {
        try {
            AuthAgent agent = authAgentMapper.selectById(id);
            if (agent == null) {
                return DefaultResponse.error("用户不存在");
            }
            
            authAgentMapper.deleteById(id);
            return DefaultResponse.success("删除成功");
        } catch (Exception e) {
            return DefaultResponse.error("删除用户失败：" + e.getMessage());
        }
    }

    /**
     * 修改用户状态
     */
    @PostMapping("/change-state/{id}")
    @UserLoginToken
    public String changeState(@PathVariable Integer id, @RequestParam Integer state) {
        try {
            AuthAgent agent = authAgentMapper.selectById(id);
            if (agent == null) {
                return DefaultResponse.error("用户不存在");
            }
            
            agent.setState(state);
            agent.setUpdatedTime(LocalDateTime.now());
            authAgentMapper.updateById(agent);
            
            return DefaultResponse.success(state == 1 ? "启用成功" : "禁用成功");
        } catch (Exception e) {
            return DefaultResponse.error("修改状态失败：" + e.getMessage());
        }
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password/{id}")
    @UserLoginToken
    public String resetPassword(@PathVariable Integer id, @RequestParam String newPassword) {
        try {
            AuthAgent agent = authAgentMapper.selectById(id);
            if (agent == null) {
                return DefaultResponse.error("用户不存在");
            }
            
            agent.setPassword(MD5Util.getMD5(newPassword));
            agent.setUpdatedTime(LocalDateTime.now());
            authAgentMapper.updateById(agent);
            
            return DefaultResponse.success("密码重置成功");
        } catch (Exception e) {
            return DefaultResponse.error("重置密码失败：" + e.getMessage());
        }
    }

    /**
     * 获取角色列表
     */
    @GetMapping("/roles")
    @UserLoginToken
    public String getRoles() {
        try {
            List<AuthRole> roles = authRoleMapper.selectList(null);
            return DefaultResponse.success(roles);
        } catch (Exception e) {
            return DefaultResponse.error("获取角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 用户数据传输对象
     */
    @Data
    public static class AuthAgentDTO {
        private Integer id;
        private String nickName;
        private String phone;
        private String password;
        private Integer regionType;
        private String province;
        private String city;
        private String country;
        private Integer roleId;
        private Integer ratio;
        private Integer state;
    }
}
