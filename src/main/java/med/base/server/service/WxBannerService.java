package med.base.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import med.base.server.mapper.WxBannerMapper;
import med.base.server.model.WxBanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 轮播图服务
 */
@Service
public class WxBannerService {

    @Autowired
    private WxBannerMapper bannerMapper;

    /**
     * 获取所有轮播图（按排序和创建时间排序）
     */
    public List<WxBanner> getAllBanners() {
        LambdaQueryWrapper<WxBanner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(WxBanner::getSort);
        wrapper.orderByDesc(WxBanner::getCreatedTime);
        return bannerMapper.selectList(wrapper);
    }

    /**
     * 获取启用的轮播图（用于小程序展示）
     */
    public List<WxBanner> getEnabledBanners() {
        LambdaQueryWrapper<WxBanner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WxBanner::getStatus, 1);
        wrapper.orderByAsc(WxBanner::getSort);
        wrapper.orderByDesc(WxBanner::getCreatedTime);
        return bannerMapper.selectList(wrapper);
    }

    /**
     * 根据ID获取轮播图
     */
    public WxBanner getBannerById(Integer id) {
        return bannerMapper.selectById(id);
    }

    /**
     * 添加轮播图
     */
    public int addBanner(WxBanner banner) {
        if (banner.getSort() == null) {
            // 如果没有设置排序，自动设置为最大值+1
            LambdaQueryWrapper<WxBanner> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(WxBanner::getSort);
            wrapper.orderByDesc(WxBanner::getSort);
            wrapper.last("LIMIT 1");
            WxBanner lastBanner = bannerMapper.selectOne(wrapper);
            banner.setSort(lastBanner != null && lastBanner.getSort() != null ? lastBanner.getSort() + 1 : 1);
        }
        if (banner.getStatus() == null) {
            banner.setStatus(1); // 默认启用
        }
        banner.setCreatedTime(LocalDateTime.now());
        banner.setUpdatedTime(LocalDateTime.now());
        return bannerMapper.insert(banner);
    }

    /**
     * 更新轮播图
     */
    public int updateBanner(WxBanner banner) {
        banner.setUpdatedTime(LocalDateTime.now());
        return bannerMapper.updateById(banner);
    }

    /**
     * 删除轮播图
     */
    public int deleteBanner(Integer id) {
        return bannerMapper.deleteById(id);
    }

    /**
     * 更新轮播图状态
     */
    public int updateBannerStatus(Integer id, Integer status) {
        LambdaUpdateWrapper<WxBanner> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(WxBanner::getId, id);
        wrapper.set(WxBanner::getStatus, status);
        wrapper.set(WxBanner::getUpdatedTime, LocalDateTime.now());
        return bannerMapper.update(null, wrapper);
    }
}

