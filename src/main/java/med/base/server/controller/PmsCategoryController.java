package med.base.server.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.model.PmsCategory;
import med.base.server.service.PmsCategoryService;
import med.base.server.service.PmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/pms/category")
public class PmsCategoryController {

//    @Autowired
    private PmsCategoryService pmsCategoryService;

    public PmsCategoryController(PmsCategoryService pmsCategoryService) {
        this.pmsCategoryService = pmsCategoryService;
    }

    @GetMapping(value = "/list")
    @UserLoginToken
    public String getPmsCategoryList(){

        List<PmsCategory> list = pmsCategoryService.getPmsCategoryList();

        return DefaultResponse.success(buildTree(list));
    }

    @GetMapping(value = "/parentList")
    @UserLoginToken
    public String getPmsCategoryParentList(int parentId){

        List<PmsCategory> list = pmsCategoryService.getPmsCategoryParentList(parentId);

        return DefaultResponse.success(list);
    }

    @PostMapping(value = "/add")
    @UserLoginToken
    public String addCategory(@RequestBody CategoryViewModel categoryViewModel){

        // 参数验证
        if (!StringUtils.hasText(categoryViewModel.getCategoryName())) {
            return DefaultResponse.error("分类名称不能为空");
        }

        // 处理 parentId，如果为 null 则默认为 0（顶级分类）
        int parentId = Optional.ofNullable(categoryViewModel.getParentId()).orElse(0);
        
        // 计算 level
        int level = 1; // 默认级别为 1
        if (parentId > 0) {
            // 查询父分类，如果父分类存在，则 level = 父分类的 level + 1
            PmsCategory parentCategory = pmsCategoryService.getById(parentId);
            if (parentCategory != null) {
                level = parentCategory.getLevel() + 1;
            } else {
                return DefaultResponse.error("父分类不存在");
            }
        }

        // 处理 sort，如果为 null 则默认为 0
        int sort = Optional.ofNullable(categoryViewModel.getSort()).orElse(0);

        // 构建实体对象
        PmsCategory pmsCategory = new PmsCategory();
        pmsCategory.setCategoryName(categoryViewModel.getCategoryName());
        pmsCategory.setParentId(parentId);
        pmsCategory.setLevel(level);
        pmsCategory.setSort(sort);

        // 保存
        pmsCategoryService.save(pmsCategory);

        return DefaultResponse.success();
    }

    @PostMapping(value = "/update")
    @UserLoginToken
    public String updateCategory(@RequestBody CategoryViewModel categoryViewModel){

        // 参数验证
        if (categoryViewModel.getCategoryId() == null) {
            return DefaultResponse.error("分类ID不能为空");
        }

        // 查询原分类
        PmsCategory existing = pmsCategoryService.getById(categoryViewModel.getCategoryId());
        if (existing == null) {
            return DefaultResponse.error("分类不存在");
        }

        // 更新字段（只更新非空字段）
        if (StringUtils.hasText(categoryViewModel.getCategoryName())) {
            existing.setCategoryName(categoryViewModel.getCategoryName());
        }
        if (categoryViewModel.getSort() != null) {
            existing.setSort(categoryViewModel.getSort());
        }
        if (categoryViewModel.getIcon() != null) {
            existing.setIcon(categoryViewModel.getIcon());
        }

        // 保存更新
        pmsCategoryService.updateById(existing);

        return DefaultResponse.success();
    }

    // 核心递归方法（只需这几行！）
    private List<CategoryTreeVO> buildTree(List<PmsCategory> nodes)
    {
        if (nodes == null || nodes.isEmpty()) return List.of();

        Map<Integer, CategoryTreeVO> nodeMap = nodes.stream()
                .collect(Collectors.toMap(
                        PmsCategory::getCategoryId,
                        c -> {
                            CategoryTreeVO vo = new CategoryTreeVO();
                            vo.setCategoryId(c.getCategoryId());
                            vo.setCategoryName(c.getCategoryName());
                            vo.setLevel(c.getLevel());
                            vo.setSort(Optional.of(c.getSort()).orElse(0));
                            vo.setIcon(c.getIcon());
                            vo.setChildren(new ArrayList<>());
                            return vo;
                        },
                        (a, b) -> a
                ));

        List<CategoryTreeVO> roots = new ArrayList<>();

        nodes.forEach(node -> {
            int pid = Optional.of(node.getParentId()).orElse(0);
            CategoryTreeVO current = nodeMap.get(node.getCategoryId());

            if (pid == 0) {
                roots.add(current);
            } else {
                CategoryTreeVO parent = nodeMap.get(pid);
                if (parent != null) {
                    parent.getChildren().add(current);
                }
            }
        });

        // 排序
        Comparator<CategoryTreeVO> sorter = Comparator
                .comparing(CategoryTreeVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(CategoryTreeVO::getCategoryId);

        nodeMap.values().forEach(vo -> vo.getChildren().sort(sorter));
        roots.sort(sorter);

        return roots;
    }

}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class CategoryTreeVO {
    private int CategoryId;
    private String CategoryName;
    private Integer level;
    private Integer sort;
    private String icon;
    private List<CategoryTreeVO> children;
}


@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class CategoryViewModel {
    private Integer categoryId; // 编辑时需要
    private String categoryName;
    private Integer parentId;
    private Integer sort;
    private String icon; // 分类图标/图片URL
}
