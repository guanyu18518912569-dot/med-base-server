package med.base.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import med.base.server.common.DefaultResponse;
import med.base.server.mapper.PmsSkuMapper;
import med.base.server.mapper.PmsSpuMapper;
import med.base.server.model.PmsSku;
import med.base.server.model.PmsCategory;
import med.base.server.model.PmsSpu;
import med.base.server.model.UmsUser;
import med.base.server.service.PmsCategoryService;
import med.base.server.service.PmsService;
import med.base.server.service.UmsUserService;
import med.base.server.service.WxBannerService;
import med.base.server.model.WxBanner;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 微信小程序端接口
 * 无需登录验证
 */
@RestController
@RequestMapping("/wxapp")
public class WxAppController {

    private final PmsService pmsService;
    private final PmsCategoryService pmsCategoryService;
    private final PmsSpuMapper pmsSpuMapper;
    private final PmsSkuMapper pmsSkuMapper;
    private final UmsUserService umsUserService;
    private final WxBannerService wxBannerService;

    // 微信小程序配置（建议放到配置文件中）
    private static final String WX_APPID = "wxe697d25bc6019fc9";
    private static final String WX_SECRET = "8d775a5b0fa924b544b4d63f63c31cda";

    public WxAppController(PmsService pmsService, PmsCategoryService pmsCategoryService,
                           PmsSpuMapper pmsSpuMapper, PmsSkuMapper pmsSkuMapper,
                           UmsUserService umsUserService, WxBannerService wxBannerService) {
        this.pmsService = pmsService;
        this.pmsCategoryService = pmsCategoryService;
        this.pmsSpuMapper = pmsSpuMapper;
        this.pmsSkuMapper = pmsSkuMapper;
        this.umsUserService = umsUserService;
        this.wxBannerService = wxBannerService;
    }

    /**
     * 获取首页数据 - 包含轮播图和Tab分类列表
     * 隐藏"酒类"和"母婴用品"分类
     */
    @GetMapping("/home")
    public String getHomeData() {
        try {
            // 获取一级分类作为Tab列表
            List<PmsCategory> categories = pmsCategoryService.getPmsCategoryParentList(0);

            List<WxTabItem> tabList = new ArrayList<>();


            // 添加一个"全部"选项
            tabList.add(new WxTabItem("全部", 0));

            int index = 1;
            for (PmsCategory category : categories) {
                // 跳过"酒类"和"母婴用品"分类（去除首尾空格后比较）
//                String categoryName = category.getCategoryName();
//                if (categoryName != null) {
//                    categoryName = categoryName.trim();
//                    if ("酒类".equals(categoryName)) {
//                        continue;
//                    }
//                    if("母婴用品".equals(categoryName)){
//                        continue;
//                    }
//                }
                tabList.add(new WxTabItem(category.getCategoryName(), category.getCategoryId()));
                index++;
                if (index >= 8) break; // 最多显示8个Tab
            }

            // 从数据库获取启用的轮播图
            List<WxBanner> banners = wxBannerService.getEnabledBanners();
            List<WxSwiperItem> swiperList = new ArrayList<>();
            for (WxBanner banner : banners) {
                swiperList.add(new WxSwiperItem(banner.getImageUrl()));
            }
            // 如果没有配置轮播图，返回空列表（由前端处理空列表情况）

            WxHomeData homeData = new WxHomeData();
            homeData.setSwiper(swiperList);
            homeData.setTabList(tabList);

            return DefaultResponse.success(homeData);
        } catch (Exception e) {
            return DefaultResponse.error("获取首页数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取商品列表
     * @param pageIndex 页码（从0开始）
     * @param pageSize 每页数量
     * @param categoryId 分类ID（0表示全部）
     * @param keyword 搜索关键词
     */
    @GetMapping("/goods/list")
    public String getGoodsList(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "0") int categoryId,
            @RequestParam(required = false) String keyword) {
        try {
            Page<PmsSpu> page = new Page<>(pageIndex + 1, pageSize);

            QueryWrapper<PmsSpu> qw = new QueryWrapper<>();
            qw.eq("status", 1); // 只查询上架商品
            qw.eq("spu_deleted", 0);

            if (categoryId > 0) {
                // 获取该分类及其所有子分类ID（支持一级分类查询三级分类商品）
                List<Integer> categoryIds = getAllChildCategoryIds(categoryId);
                categoryIds.add(categoryId); // 包含自身
                qw.in("category_id", categoryIds);
            }
            if (StringUtils.hasLength(keyword)) {
                qw.like("spu_name", keyword);
            }
            qw.orderByDesc("sort");

            IPage<PmsSpu> spuPage = pmsSpuMapper.selectPage(page, qw);

            // 转换为小程序需要的格式
            List<WxGoodsItem> goodsList = spuPage.getRecords().stream().map(spu -> {
                WxGoodsItem item = new WxGoodsItem();
                item.setSpuId(spu.getSpuId());
                item.setTitle(spu.getSpuName());
                item.setThumb(spu.getMainImage());

                // 获取该SPU下的SKU价格信息
                QueryWrapper<PmsSku> skuQw = new QueryWrapper<>();
                skuQw.eq("spu_id", spu.getSpuId());
                List<PmsSku> skuList = pmsSkuMapper.selectList(skuQw);

                if (!skuList.isEmpty()) {
                    // 获取最低价格
                    BigDecimal minPrice = skuList.stream()
                            .map(PmsSku::getPrice)
                            .filter(p -> p != null)
                            .min(BigDecimal::compareTo)
                            .orElse(BigDecimal.ZERO);

                    // 获取最高市场价（用于显示划线价）
                    BigDecimal maxMarketPrice = skuList.stream()
                            .map(PmsSku::getMarketPrice)
                            .filter(p -> p != null)
                            .max(BigDecimal::compareTo)
                            .orElse(null);

                    // 价格转换为分（微信小程序通常使用分作为单位）
                    item.setPrice(minPrice.multiply(new BigDecimal("100")).longValue());
                    if (maxMarketPrice != null) {
                        item.setOriginPrice(maxMarketPrice.multiply(new BigDecimal("100")).longValue());
                    }
                }

                // 标签统一显示为"直营"
                List<String> tags = new ArrayList<>();
                tags.add("直营");
                item.setTags(tags);

                // 设置已售数量
                item.setSoldNum(spu.getSalesCount() != null ? spu.getSalesCount() : 0);

                return item;
            }).collect(Collectors.toList());

            return DefaultResponse.success(goodsList);
        } catch (Exception e) {
            return DefaultResponse.error("获取商品列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取商品详情
     */
    @GetMapping("/goods/detail/{spuId}")
    public String getGoodsDetail(@PathVariable String spuId) {
        try {
            if (spuId == null || spuId.trim().isEmpty()) {
                return DefaultResponse.error("商品ID不能为空");
            }

            // 获取SPU信息
            PmsSpu spu = pmsSpuMapper.selectById(spuId);
            if (spu == null || spu.getSpuDeleted() == 1 || spu.getStatus() != 1) {
                return DefaultResponse.error("商品不存在或已下架");
            }

            // 获取SKU列表
            QueryWrapper<PmsSku> skuQw = new QueryWrapper<>();
            skuQw.eq("spu_id", spuId);
            List<PmsSku> skuList = pmsSkuMapper.selectList(skuQw);

            // 构建详情数据
            WxGoodsDetail detail = new WxGoodsDetail();
            detail.setSpuId(spu.getSpuId());
            detail.setTitle(spu.getSpuName());
            detail.setPrimaryImage(spu.getMainImage());
            detail.setVideo(spu.getVideoUrl());
            detail.setDesc(spu.getDescription());

            // 解析图片列表
            List<String> images = new ArrayList<>();
            if (spu.getMainImage() != null) {
                images.add(spu.getMainImage());
            }
            if (spu.getPicUrls() != null && !spu.getPicUrls().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<String> picUrls = mapper.readValue(spu.getPicUrls(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){});
                    images.addAll(picUrls);
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
            detail.setImages(images);

            // 价格信息
            if (!skuList.isEmpty()) {
                BigDecimal minPrice = skuList.stream()
                        .map(PmsSku::getPrice)
                        .filter(p -> p != null)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                BigDecimal maxPrice = skuList.stream()
                        .map(PmsSku::getPrice)
                        .filter(p -> p != null)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);

                BigDecimal maxMarketPrice = skuList.stream()
                        .map(PmsSku::getMarketPrice)
                        .filter(p -> p != null)
                        .max(BigDecimal::compareTo)
                        .orElse(null);

                int totalStock = skuList.stream()
                        .mapToInt(PmsSku::getStock)
                        .sum();

                detail.setMinSalePrice(minPrice.multiply(new BigDecimal("100")).longValue());
                detail.setMaxSalePrice(maxPrice.multiply(new BigDecimal("100")).longValue());
                if (maxMarketPrice != null) {
                    detail.setMaxLinePrice(maxMarketPrice.multiply(new BigDecimal("100")).longValue());
                }
                detail.setSpuStockQuantity(totalStock);
            } else {
                // 没有SKU时设置默认值
                detail.setMinSalePrice(0L);
                detail.setMaxSalePrice(0L);
                detail.setSpuStockQuantity(0);
            }

            // 设置基本信息
            detail.setSoldNum(spu.getSalesCount() != null ? spu.getSalesCount() : 0);
            detail.setSellPoint(spu.getSellPoint());
            detail.setIsPutOnSale(1);
            detail.setAvailable(1);

            // 构建SKU列表和规格列表
            List<WxSkuItem> wxSkuList = new ArrayList<>();
            java.util.Map<String, WxSpecItem> specMap = new java.util.LinkedHashMap<>();

            // 如果没有SKU，创建一个默认SKU
            if (skuList.isEmpty()) {
                WxSkuItem defaultSku = new WxSkuItem();
                defaultSku.setSkuId("default_" + spuId);
                defaultSku.setSkuImage(spu.getMainImage());
                defaultSku.setPrice(0L);
                defaultSku.setStockQuantity(0);

                List<WxSpecInfo> defaultSpecInfoList = new ArrayList<>();
                WxSpecInfo defaultSpecInfo = new WxSpecInfo();
                defaultSpecInfo.setSpecId("default_spec");
                defaultSpecInfo.setSpecValueId("default_value");
                defaultSpecInfoList.add(defaultSpecInfo);
                defaultSku.setSpecInfo(defaultSpecInfoList);

                wxSkuList.add(defaultSku);

                // 创建默认规格
                WxSpecItem defaultSpecItem = new WxSpecItem();
                defaultSpecItem.setSpecId("default_spec");
                defaultSpecItem.setTitle("规格");
                List<WxSpecValue> defaultValueList = new ArrayList<>();
                WxSpecValue defaultValue = new WxSpecValue();
                defaultValue.setSpecValueId("default_value");
                defaultValue.setSpecValue("默认");
                defaultValueList.add(defaultValue);
                defaultSpecItem.setSpecValueList(defaultValueList);
                specMap.put("default_spec", defaultSpecItem);
            }

            // 标记是否有任何SKU包含规格信息
            boolean hasSpecs = skuList.stream().anyMatch(sku -> sku.getSpecs() != null && !sku.getSpecs().isEmpty());

            for (PmsSku sku : skuList) {
                WxSkuItem wxSku = new WxSkuItem();
                wxSku.setSkuId(sku.getSkuId());
                wxSku.setSkuImage(sku.getPicUrl() != null ? sku.getPicUrl() : spu.getMainImage());
                wxSku.setPrice(sku.getPrice() != null ? sku.getPrice().multiply(new BigDecimal("100")).longValue() : 0L);
                wxSku.setStockQuantity(sku.getStock());

                // 解析规格信息
                List<WxSpecInfo> specInfoList = new ArrayList<>();
                if (sku.getSpecs() != null && !sku.getSpecs().isEmpty()) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        List<java.util.Map<String, String>> specs = mapper.readValue(sku.getSpecs(),
                                new com.fasterxml.jackson.core.type.TypeReference<List<java.util.Map<String, String>>>(){});

                        for (java.util.Map<String, String> spec : specs) {
                            String specKeyId = spec.get("specKeyId");
                            String specKeyName = spec.get("specKeyName");
                            String specValueId = spec.get("specValueId");
                            String specValueName = spec.get("specValueName");

                            // 添加到SKU的规格信息
                            WxSpecInfo specInfo = new WxSpecInfo();
                            specInfo.setSpecId(specKeyId);
                            specInfo.setSpecValueId(specValueId);
                            specInfoList.add(specInfo);

                            // 构建规格列表
                            if (!specMap.containsKey(specKeyId)) {
                                WxSpecItem specItem = new WxSpecItem();
                                specItem.setSpecId(specKeyId);
                                specItem.setTitle(specKeyName);
                                specItem.setSpecValueList(new ArrayList<>());
                                specMap.put(specKeyId, specItem);
                            }

                            // 添加规格值（避免重复）
                            WxSpecItem specItem = specMap.get(specKeyId);
                            boolean valueExists = specItem.getSpecValueList().stream()
                                    .anyMatch(v -> v.getSpecValueId().equals(specValueId));
                            if (!valueExists) {
                                WxSpecValue specValue = new WxSpecValue();
                                specValue.setSpecValueId(specValueId);
                                specValue.setSpecValue(specValueName);
                                specItem.getSpecValueList().add(specValue);
                            }
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }

                // 如果没有规格信息，创建默认规格
                if (specInfoList.isEmpty() && !hasSpecs) {
                    String defaultSpecId = "default_spec";
                    String defaultValueId = "default_value_" + sku.getSkuId();

                    WxSpecInfo specInfo = new WxSpecInfo();
                    specInfo.setSpecId(defaultSpecId);
                    specInfo.setSpecValueId(defaultValueId);
                    specInfoList.add(specInfo);

                    // 构建默认规格列表
                    if (!specMap.containsKey(defaultSpecId)) {
                        WxSpecItem specItem = new WxSpecItem();
                        specItem.setSpecId(defaultSpecId);
                        specItem.setTitle("规格");
                        specItem.setSpecValueList(new ArrayList<>());
                        specMap.put(defaultSpecId, specItem);
                    }

                    WxSpecItem specItem = specMap.get(defaultSpecId);
                    WxSpecValue specValue = new WxSpecValue();
                    specValue.setSpecValueId(defaultValueId);
                    specValue.setSpecValue("默认");
                    specItem.getSpecValueList().add(specValue);
                }

                wxSku.setSpecInfo(specInfoList);
                wxSkuList.add(wxSku);
            }

            detail.setSkuList(wxSkuList);
            detail.setSpecList(new ArrayList<>(specMap.values()));

            return DefaultResponse.success(detail);
        } catch (Exception e) {
            return DefaultResponse.error("获取商品详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类列表（树形结构）
     * 隐藏"酒类"和"母婴用品"分类及其所有子分类
     */
    @GetMapping("/category/list")
    public String getCategoryList() {
        try {
            List<PmsCategory> list = pmsCategoryService.getPmsCategoryList();

            // 找到需要隐藏的分类ID（包括所有子分类）
//            java.util.Set<Integer> hiddenCategoryIds = new java.util.HashSet<>();
//            for (PmsCategory category : list) {
//                String categoryName = category.getCategoryName();
//                if (categoryName != null) {
//                    categoryName = categoryName.trim();
//                    if ("酒类".equals(categoryName) || "母婴用品".equals(categoryName)) {
//                        // 添加该分类及其所有子分类ID
//                        hiddenCategoryIds.add(category.getCategoryId());
//                        hiddenCategoryIds.addAll(getAllChildCategoryIds(category.getCategoryId()));
//                    }
//                }
//            }

            // 过滤掉需要隐藏的分类
//            List<PmsCategory> filteredList = list.stream().collect(Collectors.toList());
                    //.filter(category -> !hiddenCategoryIds.contains(category.getCategoryId()))
                    //.collect(Collectors.toList());

            List<WxCategoryItem> treeList = buildCategoryTree(list);
            return DefaultResponse.success(treeList);
        } catch (Exception e) {
            return DefaultResponse.error("获取分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 调试接口：查看SKU原始数据
     */
    @GetMapping("/debug/sku/{spuId}")
    public String debugSku(@PathVariable String spuId) {
        try {
            QueryWrapper<PmsSku> skuQw = new QueryWrapper<>();
            skuQw.eq("spu_id", spuId);
            List<PmsSku> skuList = pmsSkuMapper.selectList(skuQw);

            List<java.util.Map<String, Object>> result = new ArrayList<>();
            for (PmsSku sku : skuList) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("skuId", sku.getSkuId());
                map.put("spuId", sku.getSpuId());
                map.put("specs", sku.getSpecs());
                map.put("price", sku.getPrice());
                map.put("stock", sku.getStock());
                result.add(map);
            }
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 调试接口：查看SPU的spu_deleted字段值
     */
    @GetMapping("/debug/spu-deleted")
    public String debugSpuDeleted() {
        try {
            // 查询所有SPU（不带条件）
            List<PmsSpu> allSpuList = pmsSpuMapper.selectList(null);

            List<java.util.Map<String, Object>> result = new ArrayList<>();
            for (PmsSpu spu : allSpuList) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("spuId", spu.getSpuId());
                map.put("spuName", spu.getSpuName());
                map.put("status", spu.getStatus());
                map.put("spuDeleted", spu.getSpuDeleted());  // 查看 spuDeleted 字段值
                result.add(map);
            }
            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 调试接口：测试带条件查询
     */
    @GetMapping("/debug/spu-filter")
    public String debugSpuFilter() {
        try {
            QueryWrapper<PmsSpu> qw = new QueryWrapper<>();
            qw.eq("status", 1);
            qw.eq("spu_deleted", 0);

            List<PmsSpu> filteredList = pmsSpuMapper.selectList(qw);

            // 同时查询不带过滤条件的总数
            List<PmsSpu> allList = pmsSpuMapper.selectList(null);

            // 统计 spuDeleted=1 的数量
            long deletedCount = allList.stream().filter(s -> s.getSpuDeleted() == 1).count();

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("filteredCount", filteredList.size());
            result.put("totalCount", allList.size());
            result.put("deletedCount", deletedCount);
            result.put("condition", "status=1 AND spu_deleted=0");
            result.put("sqlSegment", qw.getSqlSegment());  // 查看实际SQL条件

            // 列出所有商品的删除状态
            List<java.util.Map<String, Object>> details = new ArrayList<>();
            for (PmsSpu spu : allList) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("spuId", spu.getSpuId());
                map.put("spuName", spu.getSpuName());
                map.put("status", spu.getStatus());
                map.put("spuDeleted", spu.getSpuDeleted());
                details.add(map);
            }
            result.put("allSpuDetails", details);

            return DefaultResponse.success(result);
        } catch (Exception e) {
            return DefaultResponse.error("查询失败: " + e.getMessage() + " - 可能是数据库字段名不匹配");
        }
    }

    /**
     * 构建分类树
     */
    private List<WxCategoryItem> buildCategoryTree(List<PmsCategory> nodes) {
        if (nodes == null || nodes.isEmpty()) return new ArrayList<>();

        java.util.Map<Integer, WxCategoryItem> nodeMap = nodes.stream()
                .collect(Collectors.toMap(
                        PmsCategory::getCategoryId,
                        c -> {
                            WxCategoryItem vo = new WxCategoryItem();
                            vo.setGroupId(String.valueOf(c.getCategoryId()));
                            vo.setName(c.getCategoryName());
                            vo.setThumbnail(StringUtils.hasLength(c.getIcon()) ? c.getIcon() : "https://tdesign.gtimg.com/miniprogram/template/retail/category/category-default.png");
                            vo.setChildren(new ArrayList<>());
                            return vo;
                        },
                        (a, b) -> a
                ));

        List<WxCategoryItem> roots = new ArrayList<>();

        nodes.forEach(node -> {
            int pid = Optional.of(node.getParentId()).orElse(0);
            WxCategoryItem current = nodeMap.get(node.getCategoryId());

            if (pid == 0) {
                roots.add(current);
            } else {
                WxCategoryItem parent = nodeMap.get(pid);
                if (parent != null) {
                    parent.getChildren().add(current);
                }
            }
        });

        return roots;
    }

    /**
     * 递归获取指定分类的所有子分类ID（包括二级、三级等）
     */
    private List<Integer> getAllChildCategoryIds(int parentId) {
        List<Integer> result = new ArrayList<>();
        List<PmsCategory> children = pmsCategoryService.getPmsCategoryParentList(parentId);
        for (PmsCategory child : children) {
            result.add(child.getCategoryId());
            // 递归获取子分类的子分类
            result.addAll(getAllChildCategoryIds(child.getCategoryId()));
        }
        return result;
    }

    // ==================== 用户登录相关接口 ====================

    /**
     * 微信小程序登录
     * @param loginRequest 包含 code、userInfo、inviteCode
     * @return 用户信息和token
     */
    @PostMapping("/user/login")
    public String wxLogin(@RequestBody WxLoginRequest loginRequest) {
        try {
            // 验证 code 参数
            if (loginRequest.getCode() == null || loginRequest.getCode().trim().isEmpty()) {
                return DefaultResponse.error("code 参数不能为空");
            }

            // 1. 通过code换取openid
            String openid = getOpenidByCode(loginRequest.getCode());
            if (openid == null || openid.isEmpty()) {
                return DefaultResponse.error("获取用户信息失败，请重试");
            }

            // 2. 登录或注册
            UmsUser user = umsUserService.loginOrRegister(
                    openid,
                    null,  // unionid 暂时传null
                    loginRequest.getNickName(),
                    loginRequest.getAvatarUrl(),
                    loginRequest.getInviteCode()
            );

            // 3. 构建返回结果
            WxLoginResponse response = new WxLoginResponse();
            response.setUserId(user.getUmsUserId());
            response.setOpenId(openid);  // 返回openId用于支付
            response.setNickName(user.getNickName());
            response.setAvatarUrl(user.getPhotoUrl());
            response.setInviteCode(user.getInviteCode());
            response.setLevel(user.getLevel());
            response.setDirectCount(user.getDirectCount());
            response.setTeamCount(user.getTeamCount());
            // 简单token，实际项目建议使用JWT
            response.setToken(java.util.UUID.randomUUID().toString().replace("-", ""));

            return DefaultResponse.success(response);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("登录失败：" + e.getMessage());
        }
    }

    /**
     * 静默注册 - 仅根据openid创建用户记录
     * 小程序启动时调用，不需要用户交互
     * @param request 包含 code 和可选的 inviteCode
     * @return 用户基本信息（包含openid和userId）
     */
    @PostMapping("/user/silent-register")
    public String silentRegister(@RequestBody WxSilentRegisterRequest request) {
        try {
            // 验证 code 参数
            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                return DefaultResponse.error("code 参数不能为空");
            }

            // 1. 通过code换取openid
            String openid = getOpenidByCode(request.getCode());
            if (openid == null || openid.isEmpty()) {
                return DefaultResponse.error("获取用户信息失败，请重试");
            }

            // 2. 静默注册（不带昵称和头像）
            UmsUser user = umsUserService.loginOrRegister(
                    openid,
                    null,
                    "",  // 昵称为空
                    "",  // 头像为空
                    request.getInviteCode()
            );

            // 3. 构建返回结果
            WxLoginResponse response = new WxLoginResponse();
            response.setUserId(user.getUmsUserId());
            response.setOpenId(openid);
            response.setNickName(user.getNickName());
            response.setAvatarUrl(user.getPhotoUrl());
            response.setInviteCode(user.getInviteCode());
            response.setLevel(user.getLevel());
            response.setDirectCount(user.getDirectCount());
            response.setTeamCount(user.getTeamCount());
            response.setToken(java.util.UUID.randomUUID().toString().replace("-", ""));

            return DefaultResponse.success(response);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("静默注册失败：" + e.getMessage());
        }
    }

    /**
     * 根据openid更新用户信息
     * 用户在个人中心登录时调用
     * @param request 包含 openId、nickName、avatarUrl
     * @return 更新后的用户信息
     */
    @PostMapping("/user/update-info")
    public String updateUserInfo(@RequestBody WxUpdateUserInfoRequest request) {
        try {
            if (request.getOpenId() == null || request.getOpenId().trim().isEmpty()) {
                return DefaultResponse.error("openId不能为空");
            }

            // 验证 openId 格式（微信 openid 为 28 位字符）
            if (request.getOpenId().length() > 100 || !request.getOpenId().matches("^[a-zA-Z0-9_-]+$")) {
                return DefaultResponse.error("openId 格式不正确");
            }

            // 根据openid更新用户信息
            UmsUser user = umsUserService.updateUserInfoByOpenid(
                    request.getOpenId(),
                    request.getNickName(),
                    request.getAvatarUrl()
            );

            if (user == null) {
                return DefaultResponse.error("用户不存在，请重新进入小程序");
            }

            // 构建返回结果
            WxLoginResponse response = new WxLoginResponse();
            response.setUserId(user.getUmsUserId());
            response.setOpenId(request.getOpenId());
            response.setNickName(user.getNickName());
            response.setAvatarUrl(user.getPhotoUrl());
            response.setInviteCode(user.getInviteCode());
            response.setLevel(user.getLevel());
            response.setDirectCount(user.getDirectCount());
            response.setTeamCount(user.getTeamCount());
            response.setPoints(user.getPoints() != null ? user.getPoints() : 0L);
            response.setToken(java.util.UUID.randomUUID().toString().replace("-", ""));

            return DefaultResponse.success(response);
        } catch (Exception e) {
            e.printStackTrace();
            return DefaultResponse.error("更新用户信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/user/info")
    public String getUserInfo(@RequestParam String userId) {
        try {
            UmsUser user = umsUserService.getById(userId);
            if (user == null) {
                return DefaultResponse.error("用户不存在");
            }

            WxUserInfo info = new WxUserInfo();
            info.setUserId(user.getUmsUserId());
            info.setNickName(user.getNickName());
            info.setAvatarUrl(user.getPhotoUrl());
            info.setInviteCode(user.getInviteCode());
            info.setLevel(user.getLevel());
            info.setDirectCount(user.getDirectCount());
            info.setTeamCount(user.getTeamCount());
            info.setDirectPerformance(user.getDirectPerformance());
            info.setTeamPerformance(user.getTeamPerformance());
            info.setTotalIncome(user.getTotalIncome());
            info.setAccount(user.getAccount());
            info.setPoints(user.getPoints() != null ? user.getPoints() : 0L);

            return DefaultResponse.success(info);
        } catch (Exception e) {
            return DefaultResponse.error("获取用户信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取我的直推用户列表
     */
    @GetMapping("/user/direct-children")
    public String getDirectChildren(@RequestParam String userId) {
        try {
            List<UmsUser> children = umsUserService.getDirectChildren(userId);
            List<WxTeamMember> members = new ArrayList<>();
            for (UmsUser u : children) {
                WxTeamMember m = new WxTeamMember();
                m.setUserId(u.getUmsUserId());
                m.setNickName(u.getNickName());
                m.setAvatarUrl(u.getPhotoUrl());
                m.setLevel(u.getLevel());
                m.setCreatedTime(u.getCreatedTime());
                m.setSelfConsumption(u.getSelfConsumption());
                m.setDirectPerformance(u.getDirectPerformance());
                members.add(m);
            }

            return DefaultResponse.success(members);
        } catch (Exception e) {
            return DefaultResponse.error("获取直推列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取我的团队（所有下级）
     */
    @GetMapping("/user/team")
    public String getTeamMembers(@RequestParam String userId) {
        try {
            UmsUser user = umsUserService.getById(userId);
            if (user == null) {
                return DefaultResponse.error("用户不存在");
            }

            List<UmsUser> allMembers = umsUserService.getAllTeamMembers(userId);

            WxTeamStats stats = new WxTeamStats();
            stats.setDirectCount(user.getDirectCount());
            stats.setTeamCount(user.getTeamCount());
            stats.setDirectPerformance(user.getDirectPerformance());
            stats.setTeamPerformance(user.getTeamPerformance());

            List<WxTeamMember> members = new ArrayList<>();
            for (UmsUser u : allMembers) {
                WxTeamMember m = new WxTeamMember();
                m.setUserId(u.getUmsUserId());
                m.setNickName(u.getNickName());
                m.setAvatarUrl(u.getPhotoUrl());
                m.setLevel(u.getLevel());
                m.setCreatedTime(u.getCreatedTime());
                m.setSelfConsumption(u.getSelfConsumption());
                m.setDirectPerformance(u.getDirectPerformance());
                members.add(m);
            }
            stats.setMembers(members);

            return DefaultResponse.success(stats);
        } catch (Exception e) {
            return DefaultResponse.error("获取团队信息失败：" + e.getMessage());
        }
    }

    /**
     * 生成分享信息（用于小程序分享）
     */
    @GetMapping("/user/share-info")
    public String getShareInfo(@RequestParam String userId) {
        try {
            UmsUser user = umsUserService.getById(userId);
            if (user == null) {
                return DefaultResponse.error("用户不存在");
            }

            WxShareInfo shareInfo = new WxShareInfo();
            shareInfo.setTitle("邀请您加入，享专属优惠");
            shareInfo.setPath("/pages/home/home?inviteCode=" + user.getInviteCode());
            shareInfo.setImageUrl(""); // 分享图片，可设置默认值
            shareInfo.setInviteCode(user.getInviteCode());

            return DefaultResponse.success(shareInfo);
        } catch (Exception e) {
            return DefaultResponse.error("获取分享信息失败：" + e.getMessage());
        }
    }

    /**
     * 通过微信code换取openid
     */
    private String getOpenidByCode(String code) {
        try {
            String url = String.format(
                    "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    WX_APPID, WX_SECRET, code
            );

            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(response);

            if (jsonNode.has("openid")) {
                return jsonNode.get("openid").asText();
            }

            // 开发环境下，如果获取失败，可以用code作为模拟openid（仅限测试）
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                System.err.println("获取openid失败: " + response);
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

// DTO 类定义
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxHomeData {
    private List<WxSwiperItem> swiper;
    private List<WxTabItem> tabList;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxSwiperItem {
    private String src;

    public WxSwiperItem(String src) {
        this.src = src;
    }

    public WxSwiperItem() {}
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxTabItem {
    private String text;
    private int key;

    public WxTabItem(String text, int key) {
        this.text = text;
        this.key = key;
    }

    public WxTabItem() {}
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxGoodsItem {
    private String spuId;
    private String thumb;
    private String title;
    private Long price;
    private Long originPrice;
    private List<String> tags;
    private Integer soldNum;  // 已售数量
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxGoodsDetail {
    private String spuId;
    private String title;
    private String primaryImage;
    private List<String> images;
    private String video;
    private String desc;
    private String sellPoint;      // 卖点
    private Long minSalePrice;
    private Long maxSalePrice;
    private Long maxLinePrice;
    private Integer spuStockQuantity;
    private Integer soldNum;
    private Integer isPutOnSale;
    private Integer available;
    private List<WxSkuItem> skuList;
    private List<WxSpecItem> specList;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxSkuItem {
    private String skuId;
    private String skuImage;
    private Long price;
    private Integer stockQuantity;
    private List<WxSpecInfo> specInfo;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxSpecItem {
    private String specId;
    private String title;
    private List<WxSpecValue> specValueList;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxSpecValue {
    private String specValueId;
    private String specValue;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxSpecInfo {
    private String specId;
    private String specValueId;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class WxCategoryItem {
    private String groupId;
    private String name;
    private String thumbnail;
    private List<WxCategoryItem> children;
}

// ==================== 用户登录相关 DTO ====================

@Data
class WxLoginRequest {
    private String code;         // 微信登录code
    private String nickName;     // 昵称
    private String avatarUrl;    // 头像
    private String inviteCode;   // 邀请码（可选）
}

@Data
class WxSilentRegisterRequest {
    private String code;         // 微信登录code
    private String inviteCode;   // 邀请码（可选）
}

@Data
class WxUpdateUserInfoRequest {
    private String openId;       // 用户OpenID
    private String nickName;     // 昵称
    private String avatarUrl;    // 头像
}

@Data
class WxLoginResponse {
    private String userId;
    private String openId;      // 用户微信OpenID，用于支付
    private String nickName;
    private String avatarUrl;
    private String inviteCode;
    private Integer level;
    private Integer directCount;
    private Integer teamCount;
    private Long points;        // 积分
    private String token;
}

@Data
class WxUserInfo {
    private String userId;
    private String nickName;
    private String avatarUrl;
    private String inviteCode;
    private Integer level;
    private Integer directCount;
    private Integer teamCount;
    private java.math.BigDecimal directPerformance;
    private java.math.BigDecimal teamPerformance;
    private java.math.BigDecimal totalIncome;     // 累计收益
    private java.math.BigDecimal account;          // 账户余额（可提现）
    private Long points;                            // 积分
}

@Data
class WxTeamMember {
    private String userId;
    private String nickName;
    private String avatarUrl;
    private Integer level;
    private java.time.LocalDateTime createdTime;   // 注册时间
    private java.math.BigDecimal selfConsumption;   // 个人消费金额
    private java.math.BigDecimal directPerformance; // 直推业绩（下级的消费总额）
}

@Data
class WxTeamStats {
    private Integer directCount;
    private Integer teamCount;
    private java.math.BigDecimal directPerformance;
    private java.math.BigDecimal teamPerformance;
    private List<WxTeamMember> members;
}

@Data
class WxShareInfo {
    private String title;
    private String path;
    private String imageUrl;
    private String inviteCode;
}
