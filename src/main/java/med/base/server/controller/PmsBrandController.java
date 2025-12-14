package med.base.server.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.model.PmsBrand;
import med.base.server.service.PmsBrandService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/pms/brand")
public class PmsBrandController {

    private final PmsBrandService pmsBrandService;

    public PmsBrandController(PmsBrandService pmsBrandService) {
        this.pmsBrandService = pmsBrandService;
    }

    /**
     * 获取品牌列表
     */
    @GetMapping(value = "/list")
    @UserLoginToken
    public String getBrandList() {
        List<PmsBrand> list = pmsBrandService.getList();
        return DefaultResponse.success(list);
    }

    /**
     * 根据ID获取品牌
     */
    @GetMapping(value = "/get")
    @UserLoginToken
    public String getBrandById(int brandId) {
        if (brandId <= 0) {
            return DefaultResponse.error("品牌ID不能为空");
        }

        PmsBrand brand = pmsBrandService.getById(brandId);
        if (brand == null) {
            return DefaultResponse.error("品牌不存在");
        }
        return DefaultResponse.success(brand);
    }

    /**
     * 添加品牌
     */
    @PostMapping(value = "/add")
    @UserLoginToken
    public String addBrand(@RequestBody BrandViewModel viewModel) {
        // 参数验证
        if (!StringUtils.hasText(viewModel.getBrandName())) {
            return DefaultResponse.error("品牌名称不能为空");
        }

        PmsBrand pmsBrand = new PmsBrand();
        pmsBrand.setBrandName(viewModel.getBrandName());
        pmsBrand.setLogoUrl(viewModel.getLogoUrl());
        pmsBrand.setFirstLetter(viewModel.getFirstLetter());
        pmsBrand.setSort(Optional.ofNullable(viewModel.getSort()).orElse(0));

        pmsBrandService.save(pmsBrand);

        return DefaultResponse.success();
    }

    /**
     * 更新品牌
     */
    @PostMapping(value = "/update")
    @UserLoginToken
    public String updateBrand(@RequestBody BrandViewModel viewModel) {
        // 参数验证
        if (viewModel.getBrandId() == null || viewModel.getBrandId() <= 0) {
            return DefaultResponse.error("品牌ID不能为空");
        }
        if (!StringUtils.hasText(viewModel.getBrandName())) {
            return DefaultResponse.error("品牌名称不能为空");
        }

        PmsBrand pmsBrand = pmsBrandService.getById(viewModel.getBrandId());
        if (pmsBrand == null) {
            return DefaultResponse.error("品牌不存在");
        }

        pmsBrand.setBrandName(viewModel.getBrandName());
        if (viewModel.getLogoUrl() != null) {
            pmsBrand.setLogoUrl(viewModel.getLogoUrl());
        }
        if (viewModel.getFirstLetter() != null) {
            pmsBrand.setFirstLetter(viewModel.getFirstLetter());
        }
        if (viewModel.getSort() != null) {
            pmsBrand.setSort(viewModel.getSort());
        }

        pmsBrandService.update(pmsBrand);

        return DefaultResponse.success();
    }

    /**
     * 删除品牌
     */
    @PostMapping(value = "/delete")
    @UserLoginToken
    public String deleteBrand(int brandId) {
        if (brandId <= 0) {
            return DefaultResponse.error("品牌ID不能为空");
        }

        PmsBrand brand = pmsBrandService.getById(brandId);
        if (brand == null) {
            return DefaultResponse.error("品牌不存在");
        }

        pmsBrandService.delete(brandId);

        return DefaultResponse.success();
    }
}

// ==================== ViewModel 类 ====================

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class BrandViewModel {
    private Integer brandId;
    private String brandName;
    private String logoUrl;
    private String firstLetter;
    private Integer sort;
}

