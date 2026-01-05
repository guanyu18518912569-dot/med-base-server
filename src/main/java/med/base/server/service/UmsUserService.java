package med.base.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import med.base.server.mapper.UmsUserMapper;
import med.base.server.model.UmsUser;
import med.base.server.util.SysUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员用户服务
 * 包含推荐关系、业绩统计等核心功能
 */
@Service
public class UmsUserService {

    private final UmsUserMapper umsUserMapper;

    // 邀请码起始值
    private static final long INVITE_CODE_START = 100000;

    public UmsUserService(UmsUserMapper umsUserMapper) {
        this.umsUserMapper = umsUserMapper;
    }

    // ==================== 用户查询 ====================

    /**
     * 根据ID查询用户
     */
    public UmsUser getById(String userId) {
        return umsUserMapper.selectById(userId);
    }

    /**
     * 根据openid查询用户
     */
    public UmsUser getByOpenid(String openid) {
        return umsUserMapper.selectByOpenid(openid);
    }

    /**
     * 根据邀请码查询用户
     */
    public UmsUser getByInviteCode(String inviteCode) {
        return umsUserMapper.selectByInviteCode(inviteCode);
    }

    // ==================== 推荐关系查询 ====================

    /**
     * 获取直接下级列表（一级下线）
     */
    public List<UmsUser> getDirectChildren(String userId) {
        return umsUserMapper.selectDirectChildren(userId);
    }

    /**
     * 获取所有团队成员（所有下级）
     */
    public List<UmsUser> getAllTeamMembers(String userId) {
        return umsUserMapper.selectAllTeamMembers(userId);
    }

    /**
     * 统计直接下级数量
     */
    public int countDirectChildren(String userId) {
        return umsUserMapper.countDirectChildren(userId);
    }

    /**
     * 统计团队总人数
     */
    public int countTeamMembers(String userId) {
        return umsUserMapper.countTeamMembers(userId);
    }

    /**
     * 获取上级用户（推荐人）
     */
    public UmsUser getParent(String userId) {
        UmsUser user = umsUserMapper.selectById(userId);
        if (user != null && StringUtils.hasLength(user.getParentId())) {
            return umsUserMapper.selectById(user.getParentId());
        }
        return null;
    }

    /**
     * 获取推荐路径上的所有上级用户ID列表
     */
    public String[] getParentPathArray(String userId) {
        UmsUser user = umsUserMapper.selectById(userId);
        if (user != null && StringUtils.hasLength(user.getParentPath())) {
            return user.getParentPath().split(",");
        }
        return new String[0];
    }

    // ==================== 用户注册 ====================

    /**
     * 生成下一个邀请码（有规律的数字，从100000开始递增）
     */
    public String generateInviteCode() {
        Long maxCode = umsUserMapper.selectMaxInviteCode();
        if (maxCode == null || maxCode < INVITE_CODE_START) {
            return String.valueOf(INVITE_CODE_START);
        }
        return String.valueOf(maxCode + 1);
    }

    /**
     * 小程序登录/注册
     * @param openid 微信openid
     * @param unionid 微信unionid（可选）
     * @param nickName 昵称
     * @param photoUrl 头像
     * @param inviteCode 邀请码（可选，用于建立推荐关系）
     * @return 用户信息
     */
    @Transactional
    public UmsUser loginOrRegister(String openid, String unionid, String nickName, String photoUrl, String inviteCode) {
        // 1. 查询是否已存在
        UmsUser existUser = umsUserMapper.selectByOpenid(openid);
        if (existUser != null) {
            // 已存在，更新信息并返回
            existUser.setNickName(nickName);
            existUser.setPhotoUrl(photoUrl);
            existUser.setUpdatedTime(LocalDateTime.now());
            if (StringUtils.hasLength(unionid)) {
                existUser.setUnionid(unionid);
            }
            umsUserMapper.updateById(existUser);
            return existUser;
        }

        // 2. 新用户注册
        LocalDateTime now = LocalDateTime.now();
        String userId = SysUtil.createOrderId("U");

        UmsUser newUser = UmsUser.builder()
                .umsUserId(userId)
                .openid(openid)
                .unionid(unionid)
                .nickName(nickName)
                .photoUrl(photoUrl)
                .account(BigDecimal.ZERO)
                .level(0)
                .directCount(0)
                .teamCount(0)
                .selfConsumption(BigDecimal.ZERO)
                .directPerformance(BigDecimal.ZERO)
                .teamPerformance(BigDecimal.ZERO)
                .totalIncome(BigDecimal.ZERO)
                .memberLevel(0)
                .status(0)
                .inviteCode(generateInviteCode())
                .createdTime(now)
                .updatedTime(now)
                .build();

        // 3. 处理推荐关系
        if (StringUtils.hasLength(inviteCode)) {
            UmsUser parent = umsUserMapper.selectByInviteCode(inviteCode);
            if (parent != null) {
                newUser.setParentId(parent.getUmsUserId());

                // 构建推荐路径
                if (StringUtils.hasLength(parent.getParentPath())) {
                    newUser.setParentPath(parent.getParentPath() + "," + parent.getUmsUserId());
                } else {
                    newUser.setParentPath(parent.getUmsUserId());
                }

                // 设置层级
                newUser.setLevel(parent.getLevel() + 1);
            }
        }

        // 4. 插入新用户
        umsUserMapper.insert(newUser);

        // 5. 更新上级的统计数据
        if (StringUtils.hasLength(newUser.getParentId())) {
            // 直接上级的直推人数 +1
            umsUserMapper.updateDirectCount(newUser.getParentId(), 1);

            // 路径上所有上级的团队人数 +1
            String[] parentIds = getParentPathArray(userId);
            for (String parentId : parentIds) {
                umsUserMapper.updateTeamCount(parentId, 1);
            }
            // 直接上级也要更新团队人数
            umsUserMapper.updateTeamCount(newUser.getParentId(), 1);
        }

        return newUser;
    }

    // ==================== 业绩更新 ====================

    /**
     * 订单完成后更新业绩
     * @param userId 消费用户ID
     * @param orderAmount 订单金额
     */
    @Transactional
    public void updatePerformanceOnOrderComplete(String userId, BigDecimal orderAmount) {
        // 1. 更新个人消费
        umsUserMapper.addSelfConsumption(userId, orderAmount);

        // 2. 获取用户信息
        UmsUser user = umsUserMapper.selectById(userId);
        if (user == null) return;

        // 3. 更新直接上级的直推业绩（用于计算一级收益）
        if (StringUtils.hasLength(user.getParentId())) {
            umsUserMapper.addDirectPerformance(user.getParentId(), orderAmount);
        }

        // 4. 更新路径上所有上级的团队业绩
        String[] parentIds = getParentPathArray(userId);
        for (String parentId : parentIds) {
            umsUserMapper.addTeamPerformance(parentId, orderAmount);
        }
        // 直接上级也要更新团队业绩
        if (StringUtils.hasLength(user.getParentId())) {
            umsUserMapper.addTeamPerformance(user.getParentId(), orderAmount);
        }
    }

    /**
     * 计算并发放推荐收益（一级收益）
     * 注意：此方法只增加 total_income（累计收益），不增加 account（可提现余额）
     * 可提现余额需要通过结算流程（将 oms_order_allocation 的 settlement_status 改为1）后才会增加
     * @param userId 消费用户ID
     * @param orderAmount 订单金额
     * @param commissionRate 佣金比例（例如 0.1 表示 10%）
     * @return 发放的佣金金额
     */
    @Transactional
    public BigDecimal distributeCommission(String userId, BigDecimal orderAmount, BigDecimal commissionRate) {
        UmsUser user = umsUserMapper.selectById(userId);
        if (user == null || !StringUtils.hasLength(user.getParentId())) {
            return BigDecimal.ZERO;
        }

        // 计算佣金
        BigDecimal commission = orderAmount.multiply(commissionRate);

        // 只增加累计收益，不增加可提现余额
        // 可提现余额通过结算功能手动处理
        umsUserMapper.addTotalIncome(user.getParentId(), commission);

        return commission;
    }

    // ==================== 其他 ====================

    /**
     * 更新用户信息
     */
    public void updateUser(UmsUser user) {
        user.setUpdatedTime(LocalDateTime.now());
        umsUserMapper.updateById(user);
    }

    /**
     * 根据openid更新用户信息（昵称和头像）
     * 用于小程序个人中心登录时更新用户信息
     * @param openid 用户的微信openid
     * @param nickName 昵称
     * @param avatarUrl 头像URL
     * @return 更新后的用户信息，如果用户不存在返回null
     */
    public UmsUser updateUserInfoByOpenid(String openid, String nickName, String avatarUrl) {
        // 查询用户
        UmsUser user = umsUserMapper.selectByOpenid(openid);
        if (user == null) {
            return null;
        }

        // 更新昵称和头像
        if (StringUtils.hasLength(nickName)) {
            user.setNickName(nickName);
        }
        if (StringUtils.hasLength(avatarUrl)) {
            user.setPhotoUrl(avatarUrl);
        }
        user.setUpdatedTime(LocalDateTime.now());

        umsUserMapper.updateById(user);
        return user;
    }
}
