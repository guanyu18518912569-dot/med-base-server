package med.base.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import med.base.server.model.UmsAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 收货地址Mapper
 */
@Mapper
public interface UmsAddressMapper extends BaseMapper<UmsAddress> {
    
    /**
     * 根据用户ID查询地址列表
     */
    @Select("SELECT * FROM ums_address WHERE user_id = #{userId} ORDER BY is_default DESC, updated_time DESC")
    List<UmsAddress> selectByUserId(@Param("userId") String userId);
    
    /**
     * 根据用户ID查询默认地址
     */
    @Select("SELECT * FROM ums_address WHERE user_id = #{userId} AND is_default = 1 LIMIT 1")
    UmsAddress selectDefaultByUserId(@Param("userId") String userId);
    
    /**
     * 清除用户的所有默认地址
     */
    @Update("UPDATE ums_address SET is_default = 0 WHERE user_id = #{userId}")
    int clearDefaultByUserId(@Param("userId") String userId);
    
    /**
     * 设置默认地址
     */
    @Update("UPDATE ums_address SET is_default = 1 WHERE address_id = #{addressId}")
    int setDefault(@Param("addressId") String addressId);
}
