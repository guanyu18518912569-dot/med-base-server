package med.base.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.mapper.UmsUserMapper;
import med.base.server.model.UmsUser;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理后台用户管理接口
 */
@RestController
@RequestMapping("/admin/ums/user")
@RequiredArgsConstructor
public class UmsUserController {

    private final UmsUserMapper userMapper;

    /**
     * 分页查询用户列表
     */
    @GetMapping("/list")
    @UserLoginToken
    public String list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String nickName,
            @RequestParam(required = false) String inviteCode,
            @RequestParam(required = false) Integer memberLevel,
            @RequestParam(required = false) Integer status) {

        try {
            LambdaQueryWrapper<UmsUser> wrapper = new LambdaQueryWrapper<>();

            if (StringUtils.hasText(nickName)) {
                wrapper.like(UmsUser::getNickName, nickName);
            }
            if (StringUtils.hasText(inviteCode)) {
                wrapper.eq(UmsUser::getInviteCode, inviteCode);
            }
            if (memberLevel != null && memberLevel >= 0) {
                wrapper.eq(UmsUser::getMemberLevel, memberLevel);
            }
            if (status != null) {
                wrapper.eq(UmsUser::getStatus, status);
            } else {
                // 默认不显示已注销用户
                wrapper.ne(UmsUser::getStatus, 2);
            }

            wrapper.orderByDesc(UmsUser::getCreatedTime);

            Page<UmsUser> page = new Page<>(pageNum, pageSize);
            IPage<UmsUser> result = userMapper.selectPage(page, wrapper);

            // 转换为 VO
            List<UserListVO> voList = result.getRecords().stream().map(user -> {
                UserListVO vo = new UserListVO();
                vo.setUserId(user.getUmsUserId());
                vo.setNickName(user.getNickName());
                vo.setPhotoUrl(user.getPhotoUrl());
                vo.setGender(user.getGender());
                vo.setProvince(user.getProvince());
                vo.setCity(user.getCity());
                vo.setMemberLevel(user.getMemberLevel());
                vo.setMemberLevelDesc(getMemberLevelDesc(user.getMemberLevel()));
                vo.setStatus(user.getStatus());
                vo.setStatusDesc(getStatusDesc(user.getStatus()));
                vo.setInviteCode(user.getInviteCode());
                vo.setDirectCount(user.getDirectCount());
                vo.setTeamCount(user.getTeamCount());
                vo.setSelfConsumption(user.getSelfConsumption());
                vo.setDirectPerformance(user.getDirectPerformance());
                vo.setTeamPerformance(user.getTeamPerformance());
                vo.setTotalIncome(user.getTotalIncome());
                vo.setAccount(user.getAccount());
                vo.setPoints(user.getPoints());
                vo.setCreatedTime(user.getCreatedTime());
                return vo;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("list", voList);
            data.put("total", result.getTotal());
            data.put("pageNum", result.getCurrent());
            data.put("pageSize", result.getSize());
            data.put("pages", result.getPages());

            return DefaultResponse.success(data);
        } catch (Exception e) {
            return DefaultResponse.error(e.getMessage());
        }
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/detail/{userId}")
    @UserLoginToken
    public String detail(@PathVariable String userId) {
        try {
            UmsUser user = userMapper.selectById(userId);
            if (user == null) {
                return DefaultResponse.error("用户不存在");
            }

            UserDetailVO vo = new UserDetailVO();
            vo.setUserId(user.getUmsUserId());
            vo.setNickName(user.getNickName());
            vo.setPhotoUrl(user.getPhotoUrl());
            vo.setGender(user.getGender());
            vo.setBirthday(user.getBirthday());
            vo.setCountry(user.getCountry());
            vo.setProvince(user.getProvince());
            vo.setCity(user.getCity());
            vo.setMemberLevel(user.getMemberLevel());
            vo.setMemberLevelDesc(getMemberLevelDesc(user.getMemberLevel()));
            vo.setStatus(user.getStatus());
            vo.setStatusDesc(getStatusDesc(user.getStatus()));
            vo.setInviteCode(user.getInviteCode());
            vo.setParentId(user.getParentId());
            vo.setLevel(user.getLevel());
            vo.setDirectCount(user.getDirectCount());
            vo.setTeamCount(user.getTeamCount());
            vo.setSelfConsumption(user.getSelfConsumption());
            vo.setDirectPerformance(user.getDirectPerformance());
            vo.setTeamPerformance(user.getTeamPerformance());
            vo.setTotalIncome(user.getTotalIncome());
            vo.setAccount(user.getAccount());
            vo.setPoints(user.getPoints());
            vo.setCreatedTime(user.getCreatedTime());
            vo.setUpdatedTime(user.getUpdatedTime());

            // 获取上级用户信息
            if (StringUtils.hasText(user.getParentId())) {
                UmsUser parent = userMapper.selectById(user.getParentId());
                if (parent != null) {
                    vo.setParentNickName(parent.getNickName());
                    vo.setParentInviteCode(parent.getInviteCode());
                }
            }

            return DefaultResponse.success(vo);
        } catch (Exception e) {
            return DefaultResponse.error(e.getMessage());
        }
    }

    /**
     * 更新用户状态
     */
    @PostMapping("/updateStatus")
    @UserLoginToken
    public String updateStatus(@RequestBody UpdateStatusRequest request) {
        try {
            UmsUser user = userMapper.selectById(request.getUserId());
            if (user == null) {
                return DefaultResponse.error("用户不存在");
            }

            user.setStatus(request.getStatus());
            user.setUpdatedTime(LocalDateTime.now());
            userMapper.updateById(user);

            return DefaultResponse.success("更新成功");
        } catch (Exception e) {
            return DefaultResponse.error(e.getMessage());
        }
    }

    /**
     * 更新会员等级
     */
    @PostMapping("/updateLevel")
    @UserLoginToken
    public String updateLevel(@RequestBody UpdateLevelRequest request) {
        try {
            UmsUser user = userMapper.selectById(request.getUserId());
            if (user == null) {
                return DefaultResponse.error("用户不存在");
            }

            user.setMemberLevel(request.getMemberLevel());
            user.setUpdatedTime(LocalDateTime.now());
            userMapper.updateById(user);

            return DefaultResponse.success("更新成功");
        } catch (Exception e) {
            return DefaultResponse.error(e.getMessage());
        }
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/statistics")
    @UserLoginToken
    public String statistics() {
        try {
            Map<String, Object> data = new HashMap<>();

            // 总用户数
            LambdaQueryWrapper<UmsUser> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.ne(UmsUser::getStatus, 2);
            data.put("totalCount", userMapper.selectCount(totalWrapper));

            // 正常用户数
            LambdaQueryWrapper<UmsUser> normalWrapper = new LambdaQueryWrapper<>();
            normalWrapper.eq(UmsUser::getStatus, 0);
            data.put("normalCount", userMapper.selectCount(normalWrapper));

            // 冻结用户数
            LambdaQueryWrapper<UmsUser> frozenWrapper = new LambdaQueryWrapper<>();
            frozenWrapper.eq(UmsUser::getStatus, 1);
            data.put("frozenCount", userMapper.selectCount(frozenWrapper));

            // 各等级用户数
            Map<String, Long> levelCount = new HashMap<>();
            for (int i = 0; i <= 2; i++) {
                LambdaQueryWrapper<UmsUser> levelWrapper = new LambdaQueryWrapper<>();
                levelWrapper.eq(UmsUser::getMemberLevel, i);
                levelWrapper.ne(UmsUser::getStatus, 2);
                levelCount.put(getMemberLevelDesc(i), userMapper.selectCount(levelWrapper));
            }
            data.put("levelCount", levelCount);

            return DefaultResponse.success(data);
        } catch (Exception e) {
            return DefaultResponse.error(e.getMessage());
        }
    }

    /**
     * 获取用户下级列表
     */
    @GetMapping("/team/{userId}")
    @UserLoginToken
    public String team(@PathVariable String userId,
                       @RequestParam(defaultValue = "1") Integer type) {
        try {
            List<UmsUser> users;
            if (type == 1) {
                // 直接下级
                users = userMapper.selectDirectChildren(userId);
            } else {
                // 所有下级
                users = userMapper.selectAllTeamMembers(userId);
            }

            List<UserListVO> voList = users.stream().map(user -> {
                UserListVO vo = new UserListVO();
                vo.setUserId(user.getUmsUserId());
                vo.setNickName(user.getNickName());
                vo.setPhotoUrl(user.getPhotoUrl());
                vo.setMemberLevel(user.getMemberLevel());
                vo.setMemberLevelDesc(getMemberLevelDesc(user.getMemberLevel()));
                vo.setInviteCode(user.getInviteCode());
                vo.setDirectCount(user.getDirectCount());
                vo.setTeamCount(user.getTeamCount());
                vo.setSelfConsumption(user.getSelfConsumption());
                vo.setCreatedTime(user.getCreatedTime());
                return vo;
            }).collect(Collectors.toList());

            return DefaultResponse.success(voList);
        } catch (Exception e) {
            return DefaultResponse.error(e.getMessage());
        }
    }

    private String getStatusDesc(Integer status) {
        return med.base.server.enums.UserStatus.getDescByCode(status);
    }

    private String getMemberLevelDesc(Integer level) {
        return med.base.server.enums.MemberLevel.getDescByCode(level);
    }

    // ==================== VO 定义 ====================

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserListVO {
        private String userId;
        private String nickName;
        private String photoUrl;
        private String gender;
        private String province;
        private String city;
        private Integer memberLevel;
        private String memberLevelDesc;
        private Integer status;
        private String statusDesc;
        private String inviteCode;
        private Integer directCount;
        private Integer teamCount;
        private BigDecimal selfConsumption;
        private BigDecimal directPerformance;
        private BigDecimal teamPerformance;
        private BigDecimal totalIncome;
        private BigDecimal account;
        private Long points;
        private LocalDateTime createdTime;
    }

    @Data
    @lombok.EqualsAndHashCode(callSuper = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserDetailVO extends UserListVO {
        private String birthday;
        private String country;
        private String parentId;
        private String parentNickName;
        private String parentInviteCode;
        private Integer level;
        private LocalDateTime updatedTime;
    }

    @Data
    public static class UpdateStatusRequest {
        private String userId;
        private Integer status;
    }

    @Data
    public static class UpdateLevelRequest {
        private String userId;
        private Integer memberLevel;
    }
}
