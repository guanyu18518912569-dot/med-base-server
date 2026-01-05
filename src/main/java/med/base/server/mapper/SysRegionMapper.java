package med.base.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import med.base.server.model.SysRegion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 省市县区域 Mapper
 */
@Mapper
public interface SysRegionMapper extends BaseMapper<SysRegion> {
    
    /**
     * 根据父级ID获取子区域列表
     */
    @Select("SELECT * FROM sys_region WHERE parent_id = #{parentId} AND status = 1 ORDER BY sort, id")
    List<SysRegion> selectByParentId(@Param("parentId") Long parentId);
    
    /**
     * 获取所有省份
     */
    @Select("SELECT * FROM sys_region WHERE level = 1 AND status = 1 ORDER BY sort, id")
    List<SysRegion> selectProvinces();
    
    /**
     * 获取省份下的城市
     */
    @Select("SELECT * FROM sys_region WHERE parent_id = #{provinceId} AND level = 2 AND status = 1 ORDER BY sort, id")
    List<SysRegion> selectCities(@Param("provinceId") Long provinceId);
    
    /**
     * 获取城市下的区县
     */
    @Select("SELECT * FROM sys_region WHERE parent_id = #{cityId} AND level = 3 AND status = 1 ORDER BY sort, id")
    List<SysRegion> selectDistricts(@Param("cityId") Long cityId);
    
    /**
     * 根据名称模糊查询
     */
    @Select("SELECT * FROM sys_region WHERE name LIKE CONCAT('%', #{name}, '%') AND status = 1 ORDER BY level, sort, id")
    List<SysRegion> selectByName(@Param("name") String name);
    
    /**
     * 根据ID获取区域（包含所有父级信息的完整路径）
     */
    @Select("SELECT * FROM sys_region WHERE id = #{id}")
    SysRegion selectById(@Param("id") Long id);
}
