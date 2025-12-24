package med.base.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import med.base.server.model.UmsUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

public interface UmsUserMapper extends BaseMapper<UmsUser> {

    /**
     * 根据邀请码查询用户
     */
    @Select("SELECT * FROM ums_user WHERE invite_code = #{inviteCode} AND status = 0 LIMIT 1")
    UmsUser selectByInviteCode(@Param("inviteCode") String inviteCode);

    /**
     * 根据openid查询用户
     */
    @Select("SELECT * FROM ums_user WHERE openid = #{openid} LIMIT 1")
    UmsUser selectByOpenid(@Param("openid") String openid);

    /**
     * 查询直接下级（一级下线）
     */
    @Select("SELECT * FROM ums_user WHERE parent_id = #{userId} AND status = 0")
    List<UmsUser> selectDirectChildren(@Param("userId") String userId);

    /**
     * 查询所有下级团队（通过 parent_path 模糊匹配）
     */
    @Select("SELECT * FROM ums_user WHERE (parent_path LIKE CONCAT(#{userId}, ',%') OR parent_path LIKE CONCAT('%,', #{userId}, ',%') OR parent_path LIKE CONCAT('%,', #{userId}) OR parent_path = #{userId}) AND status = 0")
    List<UmsUser> selectAllTeamMembers(@Param("userId") String userId);

    /**
     * 统计直接下级数量
     */
    @Select("SELECT COUNT(*) FROM ums_user WHERE parent_id = #{userId} AND status = 0")
    int countDirectChildren(@Param("userId") String userId);

    /**
     * 统计团队总人数
     */
    @Select("SELECT COUNT(*) FROM ums_user WHERE (parent_path LIKE CONCAT(#{userId}, ',%') OR parent_path LIKE CONCAT('%,', #{userId}, ',%') OR parent_path LIKE CONCAT('%,', #{userId}) OR parent_path = #{userId}) AND status = 0")
    int countTeamMembers(@Param("userId") String userId);

    /**
     * 更新直推人数
     */
    @Update("UPDATE ums_user SET direct_count = direct_count + #{delta} WHERE ums_user_id = #{userId}")
    int updateDirectCount(@Param("userId") String userId, @Param("delta") int delta);

    /**
     * 更新团队人数（路径上所有上级）
     */
    @Update("UPDATE ums_user SET team_count = team_count + #{delta} WHERE ums_user_id = #{userId}")
    int updateTeamCount(@Param("userId") String userId, @Param("delta") int delta);

    /**
     * 增加直推业绩
     */
    @Update("UPDATE ums_user SET direct_performance = direct_performance + #{amount} WHERE ums_user_id = #{userId}")
    int addDirectPerformance(@Param("userId") String userId, @Param("amount") BigDecimal amount);

    /**
     * 增加团队业绩
     */
    @Update("UPDATE ums_user SET team_performance = team_performance + #{amount} WHERE ums_user_id = #{userId}")
    int addTeamPerformance(@Param("userId") String userId, @Param("amount") BigDecimal amount);

    /**
     * 增加个人消费
     */
    @Update("UPDATE ums_user SET self_consumption = self_consumption + #{amount} WHERE ums_user_id = #{userId}")
    int addSelfConsumption(@Param("userId") String userId, @Param("amount") BigDecimal amount);

    /**
     * 增加累计收益
     */
    @Update("UPDATE ums_user SET total_income = total_income + #{amount}, account = account + #{amount} WHERE ums_user_id = #{userId}")
    int addIncome(@Param("userId") String userId, @Param("amount") BigDecimal amount);

    /**
     * 增加积分（每消费1元=1积分）
     */
    @Update("UPDATE ums_user SET points = COALESCE(points, 0) + #{points} WHERE ums_user_id = #{userId}")
    int addPoints(@Param("userId") String userId, @Param("points") Long points);

    /**
     * 获取用户积分
     */
    @Select("SELECT COALESCE(points, 0) FROM ums_user WHERE ums_user_id = #{userId}")
    Long getPoints(@Param("userId") String userId);

    /**
     * 获取最大邀请码数字
     */
    @Select("SELECT MAX(CAST(invite_code AS UNSIGNED)) FROM ums_user WHERE invite_code REGEXP '^[0-9]+$'")
    Long selectMaxInviteCode();
}
