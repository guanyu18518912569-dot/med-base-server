package med.base.server.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.model.PmsSpecKey;
import med.base.server.model.PmsSpecValue;
import med.base.server.service.PmsSpecKeyService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/pms/spec")
public class PmsSpecController {

    private final PmsSpecKeyService pmsSpecKeyService;

    public PmsSpecController(PmsSpecKeyService pmsSpecKeyService) {
        this.pmsSpecKeyService = pmsSpecKeyService;
    }

    // ==================== 规格键（SpecKey）接口 ====================

    /**
     * 获取规格键列表（包含规格值）
     */
    @GetMapping(value = "/list")
    @UserLoginToken
    public String getSpecKeyList() {
        List<PmsSpecKey> specKeys = pmsSpecKeyService.getList();
        
        // 构建包含规格值的VO列表
        List<SpecKeyVO> voList = specKeys.stream().map(specKey -> {
            SpecKeyVO vo = new SpecKeyVO();
            vo.setSpecKeyId(specKey.getSpecKeyId());
            vo.setSpecKeyName(specKey.getSpecKeyName());
            vo.setSort(specKey.getSort());
            
            // 加载规格值列表
            List<PmsSpecValue> specValues = pmsSpecKeyService.getSpecValuesBySpecKeyId(specKey.getSpecKeyId());
            vo.setSpecValues(specValues);
            
            return vo;
        }).collect(Collectors.toList());
        
        return DefaultResponse.success(voList);
    }

    /**
     * 根据ID获取规格键（包含规格值）
     */
    @GetMapping(value = "/get")
    @UserLoginToken
    public String getSpecKeyById(int specKeyId) {
        PmsSpecKey specKey = pmsSpecKeyService.getById(specKeyId);
        if (specKey == null) {
            return DefaultResponse.error("规格键不存在");
        }
        
        // 构建包含规格值的VO
        SpecKeyVO vo = new SpecKeyVO();
        vo.setSpecKeyId(specKey.getSpecKeyId());
        vo.setSpecKeyName(specKey.getSpecKeyName());
        vo.setSort(specKey.getSort());
        
        // 加载规格值列表
        List<PmsSpecValue> specValues = pmsSpecKeyService.getSpecValuesBySpecKeyId(specKeyId);
        vo.setSpecValues(specValues);
        
        return DefaultResponse.success(vo);
    }

    /**
     * 添加规格键（包含规格值）
     */
    @PostMapping(value = "/add")
    @UserLoginToken
    public String addSpecKey(@RequestBody SpecKeyViewModel viewModel) {
        // 参数验证
        if (!StringUtils.hasText(viewModel.getSpecKeyName())) {
            return DefaultResponse.error("规格键名称不能为空");
        }

        PmsSpecKey pmsSpecKey = new PmsSpecKey();
        pmsSpecKey.setSpecKeyName(viewModel.getSpecKeyName());
        pmsSpecKey.setSort(Optional.ofNullable(viewModel.getSort()).orElse(0));

        // 转换规格值列表
        List<PmsSpecValue> specValues = new ArrayList<>();
        if (viewModel.getSpecValues() != null && !viewModel.getSpecValues().isEmpty()) {
            for (SpecValueViewModel sv : viewModel.getSpecValues()) {
                if (StringUtils.hasText(sv.getSpecValueName())) {
                    PmsSpecValue specValue = new PmsSpecValue();
                    //specValue.setSpecKeyId(viewModel.getSpecKeyId());
                    specValue.setSpecValueName(sv.getSpecValueName());
                    specValue.setSort(sv.getSort());
                    specValues.add(specValue);
                }
            }
        }

        pmsSpecKeyService.save(pmsSpecKey, specValues);

        return DefaultResponse.success();
    }

    /**
     * 更新规格键（包含规格值）
     */
    @PostMapping(value = "/update")
    @UserLoginToken
    public String updateSpecKey(@RequestBody SpecKeyViewModel viewModel) {
        // 参数验证
        if (viewModel.getSpecKeyId() == null || viewModel.getSpecKeyId() <= 0) {
            return DefaultResponse.error("规格键ID不能为空");
        }
        if (!StringUtils.hasText(viewModel.getSpecKeyName())) {
            return DefaultResponse.error("规格键名称不能为空");
        }

        PmsSpecKey pmsSpecKey = pmsSpecKeyService.getById(viewModel.getSpecKeyId());
        if (pmsSpecKey == null) {
            return DefaultResponse.error("规格键不存在");
        }

        pmsSpecKey.setSpecKeyName(viewModel.getSpecKeyName());
        if (viewModel.getSort() != null) {
            pmsSpecKey.setSort(viewModel.getSort());
        }

        // 转换规格值列表
        List<PmsSpecValue> specValues = new ArrayList<>();
        if (viewModel.getSpecValues() != null && !viewModel.getSpecValues().isEmpty()) {
            for (SpecValueViewModel sv : viewModel.getSpecValues()) {
                if (StringUtils.hasText(sv.getSpecValueName())) {
                    PmsSpecValue specValue = new PmsSpecValue();
                    specValue.setSpecValueName(sv.getSpecValueName());
                    specValue.setSort(sv.getSort());
                    specValues.add(specValue);
                }
            }
        }

        pmsSpecKeyService.update(pmsSpecKey, specValues);

        return DefaultResponse.success();
    }

    /**
     * 删除规格键（同时删除关联的规格值）
     */
    @PostMapping(value = "/delete")
    @UserLoginToken
    public String deleteSpecKey(int specKeyId) {
        if (specKeyId <= 0) {
            return DefaultResponse.error("规格键ID不能为空");
        }

        PmsSpecKey specKey = pmsSpecKeyService.getById(specKeyId);
        if (specKey == null) {
            return DefaultResponse.error("规格键不存在");
        }

        // 删除规格键及其关联的规格值
        pmsSpecKeyService.delete(specKeyId);

        return DefaultResponse.success();
    }
}

// ==================== ViewModel 和 VO 类 ====================

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class SpecKeyViewModel {
    private Integer specKeyId;
    private String specKeyName;
    private Integer sort;
    private List<SpecValueViewModel> specValues;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class SpecValueViewModel {
    private Integer specValueId;
    private String specValueName;

    private Integer sort;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class SpecKeyVO {
    private Integer specKeyId;
    private String specKeyName;
    private Integer sort;
    private List<PmsSpecValue> specValues;
}

