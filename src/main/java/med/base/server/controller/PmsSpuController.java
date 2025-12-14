package med.base.server.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import med.base.server.model.PmsSku;
import med.base.server.model.PmsSkuSpec;
import med.base.server.model.PmsSpecValue;
import med.base.server.model.PmsSpu;
import med.base.server.model.ViewModel.SpuPageVo;
import org.springframework.web.bind.WebDataBinder;
import med.base.server.annotation.UserLoginToken;
import med.base.server.common.DefaultResponse;
import med.base.server.model.ViewModel.SpuModel;
import med.base.server.service.PmsService;
import med.base.server.util.QiniuUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/pms/sku")
public class PmsSpuController {

    private final PmsService pmsService;
    private final QiniuUtil qiniuUtil;

    public PmsSpuController(PmsService pmsService, QiniuUtil qiniuUtil) {
        this.pmsService = pmsService;
        this.qiniuUtil = qiniuUtil;
    }

    @org.springframework.web.bind.annotation.InitBinder
    public void initBinder(WebDataBinder binder) {
        // 禁止 Spring 自动将请求中的 skuList、mainImage、galleryImages 字段绑定到 SpuModel，避免 multipart/form-data 下的类型转换错误
        binder.setDisallowedFields("skuList", "mainImage", "galleryImages");
    }

    @GetMapping(value = "spuPage")
    @UserLoginToken
    public String spuPage(int pageNum, int pageSize, int categoryId, int brandId, String keyword, int status){
        try {
            com.baomidou.mybatisplus.core.metadata.IPage<SpuPageVo> page = pmsService.spuPage(pageNum, pageSize, categoryId, brandId, keyword, status);
            return DefaultResponse.success(page);
        } catch (Exception e) {
            return DefaultResponse.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 保存商品 SPU 及其 SKU 列表（接收文件并上传至七牛）
     *
     * 说明：
     * - 前端以 multipart/form-data 提交
     * - 文本字段按 SpuModel 字段名提交（例如 spuName、categoryId、brandId 等）
     * - 图片文件参数：
     *   - mainImage（首图，单个文件）
     *   - galleryImages（轮播图，多个文件）
     * - 视频文件参数名：videoFile（可选）
     */
    @PostMapping(value = "spuSave")
    @UserLoginToken
    public String spuSave(SpuModelVo spuModel,
                          @RequestParam(value = "skuList", required = false) String skuListJson,
                          @RequestParam(value = "mainImage", required = false) MultipartFile mainImage,
                          @RequestParam(value = "galleryImages", required = false) MultipartFile[] galleryImages,
                          MultipartFile videoFile) throws Exception{

        // 如果前端以字符串形式提交 skuList（JSON），手动反序列化并设置到 spuModel
        if (skuListJson != null && !skuListJson.trim().isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            List<SkuListVo> skuList = mapper.readValue(skuListJson, new TypeReference<List<SkuListVo>>(){});
            spuModel.setSkuList(skuList);
        }

        // 处理首图
        String mainImageUrl = null;
        if (mainImage != null && !mainImage.isEmpty()) {
            mainImageUrl = qiniuUtil.upload(mainImage, "image/spu/main");
        }

        // 处理轮播图
        List<String> galleryUrlList = new ArrayList<>();
        if (galleryImages != null && galleryImages.length > 0) {
            for (MultipartFile gallery : galleryImages) {
                if (gallery != null && !gallery.isEmpty()) {
                    String url = qiniuUtil.upload(gallery, "image/spu/gallery");
                    galleryUrlList.add(url);
                }
            }
        }

        // 简单参数校验
        if (spuModel == null) {
            return DefaultResponse.error("参数不能为空");
        }
        if (spuModel.getCategoryId() <= 0) {
            return DefaultResponse.error("分类ID不能为空");
        }
        if (spuModel.getBrandId() <= 0) {
            return DefaultResponse.error("品牌ID不能为空");
        }
        if (spuModel.getSpuName() == null || spuModel.getSpuName().trim().isEmpty()) {
            return DefaultResponse.error("商品名称不能为空");
        }

        // 1. 构建图片 URL 列表（首图 + 轮播图）为 JSON 数组格式
        List<String> picUrlList = new ArrayList<>();
        
        // 添加首图
        if (mainImageUrl != null) {
//            picUrlList.add(mainImageUrl);
            spuModel.setMainImageUrl(mainImageUrl);
        }
        
        // 添加轮播图
        if (!galleryUrlList.isEmpty()) {
            picUrlList.addAll(galleryUrlList);
        }
        
        // 将图片 URL 列表转换为 JSON 数组字符串
        if (!picUrlList.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String picUrlsJson = mapper.writeValueAsString(picUrlList);
                spuModel.setPicUrls(picUrlsJson);
            } catch (Exception e) {
                return DefaultResponse.error("图片 URL 处理失败");
            }
        }

        // 3. 上传商品视频到七牛
        if (videoFile != null && !videoFile.isEmpty()) {
            String videoUrl = qiniuUtil.upload(videoFile, "video/spu");
            spuModel.setVideoUrl(videoUrl);
        }

        SpuModel spu = new SpuModel();
        spu.setCategoryId(spuModel.getCategoryId());
        spu.setBrandId(spuModel.getBrandId());
        spu.setSpuName(spuModel.getSpuName());
        spu.setSellPoint(spuModel.getSellPoint());
        spu.setDescription(spuModel.getDescription());
        spu.setStatus(spuModel.getStatus());
        spu.setAllocationRatioCity(spuModel.getAllocationRatioCity());
        spu.setAllocationRatioProvince(spuModel.getAllocationRatioProvince());
        spu.setAllocationRatioDistrict(spuModel.getAllocationRatioDistrict());
        spu.setPicUrls(spuModel.getPicUrls());
        spu.setVideoUrl(spuModel.getVideoUrl());
        spu.setMainImage(spuModel.getMainImageUrl());

        // 设置 SKU 列表数据
        if (spuModel.getSkuList() != null && !spuModel.getSkuList().isEmpty()) {
            List<PmsSku> skuList = new ArrayList<>();
            int skuIndex = 1;
            for (SkuListVo skuVo : spuModel.getSkuList()) {
                PmsSku sku = PmsSku.builder()
                        .build();
                
                // 生成 SKU 编码：格式为 CAT{categoryId}-SPEC{specHash}-{index}
                // 例如：CAT123-SPEC45a7-001
                String specKey = skuVo.getKey(); // key 格式为 "specKeyId_specValueId-specKeyId_specValueId"
                String skuCode = "SKU-" + spuModel.getCategoryId() + "-" + specKey + "-" + String.format("%03d", skuIndex);
                sku.setSkuCode(skuCode);
                skuIndex++;
                
                // 转换 BigDecimal 为对应的类型
                if (skuVo.getPrice() != null) {
                    sku.setPrice(skuVo.getPrice());
                }
                if (skuVo.getStock() != null) {
                    sku.setStock(skuVo.getStock().intValue());
                }
                if (skuVo.getCostPrice() != null) {
                    sku.setCostPrice(skuVo.getCostPrice());
                }
                if (skuVo.getMarketPrice() != null) {
                    sku.setMarketPrice(skuVo.getMarketPrice());
                }
                if (skuVo.getWarnStock() != null) {
                    sku.setWarnStock(skuVo.getWarnStock().intValue());
                }
                
                // 保存规格信息为 JSON 字符串
                if (skuVo.getSpecs() != null && !skuVo.getSpecs().isEmpty()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        String specsJson = mapper.writeValueAsString(skuVo.getSpecs());
                        sku.setSpecs(specsJson);
                    } catch (Exception e) {
                        // 规格转换失败，继续处理其他 SKU
                    }
                }
                
                skuList.add(sku);
            }
            spu.setSkuList(skuList);
        }

        // 3. 保存 SPU 和 SKU 数据
        pmsService.saveSpuWithSkus(spu);

        return DefaultResponse.success();
    }

    /**
     * 获取商品详情
     */
    @GetMapping(value = "detail/{spuId}")
    @UserLoginToken
    public String detail(@PathVariable String spuId) {
        try {
            if (spuId == null || spuId.trim().isEmpty()) {
                return DefaultResponse.error("商品ID不能为空");
            }
            SpuModel spuModel = pmsService.getSpuDetail(spuId);
            if (spuModel == null) {
                return DefaultResponse.error("商品不存在");
            }
            return DefaultResponse.success(spuModel);
        } catch (Exception e) {
            return DefaultResponse.error("获取商品详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新商品 SPU 及其 SKU 列表
     */
    @PostMapping(value = "spuUpdate")
    @UserLoginToken
    public String spuUpdate(SpuModelVo spuModel,
                            @RequestParam(value = "spuId", required = false) String spuId,
                            @RequestParam(value = "skuList", required = false) String skuListJson,
                            @RequestParam(value = "mainImage", required = false) MultipartFile mainImage,
                            @RequestParam(value = "galleryImages", required = false) MultipartFile[] galleryImages,
                            MultipartFile videoFile) throws Exception {

        // 如果前端以字符串形式提交 skuList（JSON），手动反序列化并设置到 spuModel
        if (skuListJson != null && !skuListJson.trim().isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            List<SkuListVo> skuList = mapper.readValue(skuListJson, new TypeReference<List<SkuListVo>>(){});
            spuModel.setSkuList(skuList);
        }

        // 处理首图
        String mainImageUrl = null;
        if (mainImage != null && !mainImage.isEmpty()) {
            mainImageUrl = qiniuUtil.upload(mainImage, "image/spu/main");
        }

        // 处理轮播图
        List<String> galleryUrlList = new ArrayList<>();
        if (galleryImages != null && galleryImages.length > 0) {
            for (MultipartFile gallery : galleryImages) {
                if (gallery != null && !gallery.isEmpty()) {
                    String url = qiniuUtil.upload(gallery, "image/spu/gallery");
                    galleryUrlList.add(url);
                }
            }
        }

        // 简单参数校验
        if (spuModel == null) {
            return DefaultResponse.error("参数不能为空");
        }
        if (spuId == null || spuId.trim().isEmpty()) {
            return DefaultResponse.error("商品ID不能为空");
        }
        if (spuModel.getCategoryId() <= 0) {
            return DefaultResponse.error("分类ID不能为空");
        }
        if (spuModel.getBrandId() <= 0) {
            return DefaultResponse.error("品牌ID不能为空");
        }
        if (spuModel.getSpuName() == null || spuModel.getSpuName().trim().isEmpty()) {
            return DefaultResponse.error("商品名称不能为空");
        }

        // 1. 构建图片 URL 列表（首图 + 轮播图）为 JSON 数组格式
        List<String> picUrlList = new ArrayList<>();
        
        // 添加首图
        if (mainImageUrl != null) {
            spuModel.setMainImageUrl(mainImageUrl);
        }
        
        // 添加轮播图
        if (!galleryUrlList.isEmpty()) {
            picUrlList.addAll(galleryUrlList);
        }
        
        // 将图片 URL 列表转换为 JSON 数组字符串
        if (!picUrlList.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String picUrlsJson = mapper.writeValueAsString(picUrlList);
                spuModel.setPicUrls(picUrlsJson);
            } catch (Exception e) {
                return DefaultResponse.error("图片 URL 处理失败");
            }
        }

        // 上传商品视频到七牛
        if (videoFile != null && !videoFile.isEmpty()) {
            String videoUrl = qiniuUtil.upload(videoFile, "video/spu");
            spuModel.setVideoUrl(videoUrl);
        }

        SpuModel spu = new SpuModel();
        spu.setSpuId(spuId);
        spu.setCategoryId(spuModel.getCategoryId());
        spu.setBrandId(spuModel.getBrandId());
        spu.setSpuName(spuModel.getSpuName());
        spu.setSellPoint(spuModel.getSellPoint());
        spu.setDescription(spuModel.getDescription());
        spu.setStatus(spuModel.getStatus());
        spu.setAllocationRatioCity(spuModel.getAllocationRatioCity());
        spu.setAllocationRatioProvince(spuModel.getAllocationRatioProvince());
        spu.setAllocationRatioDistrict(spuModel.getAllocationRatioDistrict());
        spu.setPicUrls(spuModel.getPicUrls());
        spu.setVideoUrl(spuModel.getVideoUrl());
        spu.setMainImage(spuModel.getMainImageUrl());

        // 设置 SKU 列表数据
        if (spuModel.getSkuList() != null && !spuModel.getSkuList().isEmpty()) {
            List<PmsSku> skuList = new ArrayList<>();
            int skuIndex = 1;
            for (SkuListVo skuVo : spuModel.getSkuList()) {
                PmsSku sku = PmsSku.builder()
                        .build();
                
                // 如果有 skuId，则为更新；否则为新增
                if (skuVo.getSkuId() != null && !skuVo.getSkuId().isEmpty()) {
                    sku.setSkuId(skuVo.getSkuId());
                }
                
                // 生成或保持 SKU 编码
                String specKey = skuVo.getKey();
                String skuCode = "SKU-" + spuModel.getCategoryId() + "-" + specKey + "-" + String.format("%03d", skuIndex);
                sku.setSkuCode(skuCode);
                skuIndex++;
                
                // 转换 BigDecimal 为对应的类型
                if (skuVo.getPrice() != null) {
                    sku.setPrice(skuVo.getPrice());
                }
                if (skuVo.getStock() != null) {
                    sku.setStock(skuVo.getStock().intValue());
                }
                if (skuVo.getCostPrice() != null) {
                    sku.setCostPrice(skuVo.getCostPrice());
                }
                if (skuVo.getMarketPrice() != null) {
                    sku.setMarketPrice(skuVo.getMarketPrice());
                }
                if (skuVo.getWarnStock() != null) {
                    sku.setWarnStock(skuVo.getWarnStock().intValue());
                }
                
                // 保存规格信息为 JSON 字符串
                if (skuVo.getSpecs() != null && !skuVo.getSpecs().isEmpty()) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        String specsJson = mapper.writeValueAsString(skuVo.getSpecs());
                        sku.setSpecs(specsJson);
                    } catch (Exception e) {
                        // 规格转换失败，继续处理其他 SKU
                    }
                }
                
                skuList.add(sku);
            }
            spu.setSkuList(skuList);
        }

        // 更新 SPU 和 SKU 数据
        pmsService.updateSpuWithSkus(spu);

        return DefaultResponse.success();
    }
}


@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class SpuModelVo {

    int categoryId;
    int brandId;
    String spuName;
    String sellPoint;

    String picUrls;
    String videoUrl;
    String mainImageUrl;

    String description;
    String status;

    BigDecimal allocationRatioCity;
    BigDecimal allocationRatioDistrict;
    BigDecimal allocationRatioProvince ;

    List<SkuListVo> skuList;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class SkuListVo {

    private String skuId;
    private String key;
    private List<SpecVo> specs;
    private BigDecimal price;
    private BigDecimal stock;
    private BigDecimal costPrice;
    private BigDecimal marketPrice;
    private BigDecimal warnStock;
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
class SpecVo {
    private String specKeyId;
    private String specKeyName;
    private String specValueId;
    private String specValueName;
}