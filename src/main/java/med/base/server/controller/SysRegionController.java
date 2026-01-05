package med.base.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.mapper.SysRegionMapper;
import med.base.server.model.SysRegion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 省市县区域接口
 */
@RestController
@RequestMapping("/admin/region")
public class SysRegionController {
    
    @Autowired
    private SysRegionMapper sysRegionMapper;
    
    /**
     * 获取所有省份列表
     */
    @GetMapping("/provinces")
    @UserLoginToken
    public String getProvinces(HttpServletRequest request) {
        try {
            List<SysRegion> provinces = sysRegionMapper.selectProvinces();
            List<RegionVO> result = provinces.stream()
                    .map(r -> RegionVO.builder()
                            .value(r.getId())
                            .label(r.getName())
                            .build())
                    .collect(Collectors.toList());
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("获取省份列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取城市列表
     */
    @GetMapping("/cities")
    @UserLoginToken
    public String getCities(@RequestParam Long provinceId, HttpServletRequest request) {
        try {
            List<SysRegion> cities = sysRegionMapper.selectCities(provinceId);
            List<RegionVO> result = cities.stream()
                    .map(r -> RegionVO.builder()
                            .value(r.getId())
                            .label(r.getName())
                            .build())
                    .collect(Collectors.toList());
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("获取城市列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取区县列表
     */
    @GetMapping("/districts")
    @UserLoginToken
    public String getDistricts(@RequestParam Long cityId, HttpServletRequest request) {
        try {
            List<SysRegion> districts = sysRegionMapper.selectDistricts(cityId);
            List<RegionVO> result = districts.stream()
                    .map(r -> RegionVO.builder()
                            .value(r.getId())
                            .label(r.getName())
                            .build())
                    .collect(Collectors.toList());
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("获取区县列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据父级ID获取子区域
     */
    @GetMapping("/children")
    @UserLoginToken
    public String getChildren(@RequestParam Long parentId, HttpServletRequest request) {
        try {
            List<SysRegion> children = sysRegionMapper.selectByParentId(parentId);
            List<RegionVO> result = children.stream()
                    .map(r -> RegionVO.builder()
                            .value(r.getId())
                            .label(r.getName())
                            .level(r.getLevel())
                            .build())
                    .collect(Collectors.toList());
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("获取子区域失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取级联选择器数据（一次性返回所有省市县，支持懒加载）
     * level: 要加载到的层级，1-只加载省，2-加载省市，3-加载省市县
     */
    @GetMapping("/cascade")
    @UserLoginToken
    public String getCascadeData(@RequestParam(required = false, defaultValue = "3") Integer level, 
                                  HttpServletRequest request) {
        try {
            List<CascadeRegionVO> result = new ArrayList<>();
            
            // 获取所有省份
            List<SysRegion> provinces = sysRegionMapper.selectProvinces();
            for (SysRegion province : provinces) {
                CascadeRegionVO provinceVO = CascadeRegionVO.builder()
                        .value(province.getId())
                        .label(province.getName())
                        .children(new ArrayList<>())
                        .build();
                
                if (level >= 2) {
                    // 获取城市
                    List<SysRegion> cities = sysRegionMapper.selectCities(province.getId());
                    for (SysRegion city : cities) {
                        CascadeRegionVO cityVO = CascadeRegionVO.builder()
                                .value(city.getId())
                                .label(city.getName())
                                .children(new ArrayList<>())
                                .build();
                        
                        if (level >= 3) {
                            // 获取区县
                            List<SysRegion> districts = sysRegionMapper.selectDistricts(city.getId());
                            for (SysRegion district : districts) {
                                CascadeRegionVO districtVO = CascadeRegionVO.builder()
                                        .value(district.getId())
                                        .label(district.getName())
                                        .build();
                                cityVO.getChildren().add(districtVO);
                            }
                        }
                        
                        provinceVO.getChildren().add(cityVO);
                    }
                }
                
                result.add(provinceVO);
            }
            
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("获取级联数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据区域ID获取完整路径（省/市/区名称）
     */
    @GetMapping("/path")
    @UserLoginToken
    public String getRegionPath(@RequestParam Long regionId, HttpServletRequest request) {
        try {
            List<String> path = new ArrayList<>();
            Long currentId = regionId;
            
            while (currentId != null && currentId > 0) {
                SysRegion region = sysRegionMapper.selectById(currentId);
                if (region != null) {
                    path.add(0, region.getName());
                    currentId = region.getParentId();
                } else {
                    break;
                }
            }
            
            return DefaultResponse.success(path);
        } catch (Exception e) {
            return DefaultResponse.error("获取区域路径失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据名称搜索区域
     */
    @GetMapping("/search")
    @UserLoginToken
    public String searchRegion(@RequestParam String name, HttpServletRequest request) {
        try {
            List<SysRegion> regions = sysRegionMapper.selectByName(name);
            List<RegionVO> result = regions.stream()
                    .map(r -> RegionVO.builder()
                            .value(r.getId())
                            .label(r.getName())
                            .level(r.getLevel())
                            .build())
                    .collect(Collectors.toList());
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("搜索区域失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取区域详情
     */
    @GetMapping("/detail/{id}")
    @UserLoginToken
    public String getRegionDetail(@PathVariable Long id, HttpServletRequest request) {
        try {
            SysRegion region = sysRegionMapper.selectById(id);
            if (region == null) {
                return DefaultResponse.error("区域不存在");
            }
            
            RegionDetailVO detail = RegionDetailVO.builder()
                    .id(region.getId())
                    .name(region.getName())
                    .parentId(region.getParentId())
                    .level(region.getLevel())
                    .build();
            
            // 获取父级信息
            if (region.getParentId() != null && region.getParentId() > 0) {
                SysRegion parent = sysRegionMapper.selectById(region.getParentId());
                if (parent != null) {
                    detail.setParentName(parent.getName());
                    
                    // 如果是区县，获取省份信息
                    if (region.getLevel() == 3 && parent.getParentId() != null && parent.getParentId() > 0) {
                        SysRegion province = sysRegionMapper.selectById(parent.getParentId());
                        if (province != null) {
                            detail.setProvinceName(province.getName());
                            detail.setProvinceId(province.getId());
                        }
                    }
                }
            }
            
            return DefaultResponse.success(detail);
        } catch (Exception e) {
            return DefaultResponse.error("获取区域详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 区域VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class RegionVO {
        private Long value;
        private String label;
        private Integer level;
    }
    
    /**
     * 级联区域VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class CascadeRegionVO {
        private Long value;
        private String label;
        private List<CascadeRegionVO> children;
    }
    
    /**
     * 区域详情VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class RegionDetailVO {
        private Long id;
        private String name;
        private Long parentId;
        private String parentName;
        private Integer level;
        private Long provinceId;
        private String provinceName;
    }
}
