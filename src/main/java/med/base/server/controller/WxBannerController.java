package med.base.server.controller;

import lombok.Data;
import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.model.WxBanner;
import med.base.server.service.WxBannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序轮播图管理控制器
 */
@RestController
@RequestMapping("/admin/banner")
public class WxBannerController {

    @Autowired
    private WxBannerService bannerService;

    /**
     * 获取所有轮播图列表（管理后台用）
     */
    @GetMapping("/list")
    @UserLoginToken
    public String list() {
        try {
            List<WxBanner> list = bannerService.getAllBanners();
            return DefaultResponse.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取轮播图列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取启用的轮播图列表（小程序用）
     */
    @GetMapping("/enabled")
    public String getEnabledBanners() {
        try {
            List<WxBanner> list = bannerService.getEnabledBanners();
            return DefaultResponse.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取轮播图列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取轮播图详情
     */
    @GetMapping("/detail/{id}")
    @UserLoginToken
    public String getDetail(@PathVariable Integer id) {
        try {
            WxBanner banner = bannerService.getBannerById(id);
            if (banner == null) {
                return DefaultResponse.error("轮播图不存在");
            }
            return DefaultResponse.success(banner);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("获取轮播图详情失败：" + e.getMessage());
        }
    }

    /**
     * 新增轮播图
     */
    @PostMapping("/add")
    @UserLoginToken
    public String add(@RequestBody BannerDTO dto) {
        try {
            if (!StringUtils.hasText(dto.getImageUrl())) {
                return DefaultResponse.error("图片地址不能为空");
            }

            WxBanner banner = WxBanner.builder()
                    .imageUrl(dto.getImageUrl())
                    .sort(dto.getSort())
                    .status(dto.getStatus() != null ? dto.getStatus() : 1)
                    .build();

            bannerService.addBanner(banner);
            return DefaultResponse.success("添加成功");
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("添加轮播图失败：" + e.getMessage());
        }
    }

    /**
     * 更新轮播图
     */
    @PostMapping("/update")
    @UserLoginToken
    public String update(@RequestBody BannerDTO dto) {
        try {
            if (dto.getId() == null) {
                return DefaultResponse.error("ID不能为空");
            }

            WxBanner banner = bannerService.getBannerById(dto.getId());
            if (banner == null) {
                return DefaultResponse.error("轮播图不存在");
            }

            if (StringUtils.hasText(dto.getImageUrl())) {
                banner.setImageUrl(dto.getImageUrl());
            }
            if (dto.getSort() != null) {
                banner.setSort(dto.getSort());
            }
            if (dto.getStatus() != null) {
                banner.setStatus(dto.getStatus());
            }

            bannerService.updateBanner(banner);
            return DefaultResponse.success("更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("更新轮播图失败：" + e.getMessage());
        }
    }

    /**
     * 删除轮播图
     */
    @DeleteMapping("/delete/{id}")
    @UserLoginToken
    public String delete(@PathVariable Integer id) {
        try {
            WxBanner banner = bannerService.getBannerById(id);
            if (banner == null) {
                return DefaultResponse.error("轮播图不存在");
            }

            bannerService.deleteBanner(id);
            return DefaultResponse.success("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("删除轮播图失败：" + e.getMessage());
        }
    }

    /**
     * 修改轮播图状态
     */
    @PostMapping("/changeStatus")
    @UserLoginToken
    public String changeStatus(@RequestParam Integer id, @RequestParam Integer status) {
        try {
            WxBanner banner = bannerService.getBannerById(id);
            if (banner == null) {
                return DefaultResponse.error("轮播图不存在");
            }

            bannerService.updateBannerStatus(id, status);
            return DefaultResponse.success("修改状态成功");
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("修改状态失败：" + e.getMessage());
        }
    }
}

@Data
class BannerDTO {
    private Integer id;
    private String imageUrl;
    private Integer sort;
    private Integer status;
}

