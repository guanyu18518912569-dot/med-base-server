package med.base.server.service;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import med.base.server.mapper.PmsSpuMapper;
import med.base.server.mapper.PmsSkuMapper;
import med.base.server.mapper.PmsCategoryMapper;
import med.base.server.mapper.PmsBrandMapper;
import med.base.server.model.PmsSku;
import med.base.server.model.PmsSpu;
import med.base.server.model.PmsCategory;
import med.base.server.model.PmsBrand;
import med.base.server.model.ViewModel.SpuModel;
import med.base.server.model.ViewModel.SpuPageVo;
import med.base.server.util.SysUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PmsService {

    private final PmsSpuMapper pmsSpuMapper;
    private final PmsSkuMapper pmsSkuMapper;
    private final PmsCategoryMapper pmsCategoryMapper;
    private final PmsBrandMapper pmsBrandMapper;

    public PmsService(PmsSpuMapper pmsSpuMapper, PmsSkuMapper pmsSkuMapper, 
                      PmsCategoryMapper pmsCategoryMapper, PmsBrandMapper pmsBrandMapper) {
        this.pmsSpuMapper = pmsSpuMapper;
        this.pmsSkuMapper = pmsSkuMapper;
        this.pmsCategoryMapper = pmsCategoryMapper;
        this.pmsBrandMapper = pmsBrandMapper;
    }

    public IPage<SpuPageVo> spuPage(int pageNum, int pageSize, int categoryId, int brandId, String keyword, int status){

        Page<PmsSpu> page = new Page<>(pageNum, pageSize);

        QueryWrapper<PmsSpu> qw = new QueryWrapper<>();

        if (categoryId > 0) {
            qw.eq("category_id ", categoryId);
        }
        if (brandId > 0) {
            qw.eq("brand_id", brandId);
        }
        if (status > 0) {
            qw.eq("status", status);
        }
        if (StringUtils.hasLength(keyword)) {
            qw.like("spu_name", keyword);
        }
        qw.eq("spu_deleted", 0);
        qw.orderByDesc("created_time");

        IPage<PmsSpu> spuPage = pmsSpuMapper.selectPage(page, qw);
        
        // 转换为 SpuPageVo 并关联分类和品牌名称
        Page<SpuPageVo> voPage = new Page<>(spuPage.getCurrent(), spuPage.getSize(), spuPage.getTotal());
        
        List<SpuPageVo> voList = spuPage.getRecords().stream().map(spu -> {
            SpuPageVo vo = SpuPageVo.builder()
                    .spuId(spu.getSpuId())
                    .spuName(spu.getSpuName())
                    .categoryId(spu.getCategoryId())
                    .brandId(spu.getBrandId())
                    .status(spu.getStatus())
                    .createdTime(spu.getCreatedTime())
                    .allocationRatioProvince(spu.getAllocationRatioProvince())
                    .allocationRatioCity(spu.getAllocationRatioCity())
                    .allocationRatioDistrict(spu.getAllocationRatioDistrict())
                    .inviteIncomeRatio(spu.getInviteIncomeRatio())
                    .build();
            
            // 查询分类名称
            PmsCategory category = pmsCategoryMapper.selectById(spu.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }
            
            // 查询品牌名称
            PmsBrand brand = pmsBrandMapper.selectById(spu.getBrandId());
            if (brand != null) {
                vo.setBrandName(brand.getBrandName());
            }
            
            return vo;
        }).collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 保存 SPU 及其关联的 SKU 列表
     */
    @Transactional
    public void saveSpuWithSkus(SpuModel spuModel) {
        // 保存 SPU

        LocalDateTime now = LocalDateTime.now();

        PmsSpu spu = PmsSpu.builder()
                .spuId(SysUtil.createOrderId("SPU"))
                .spuName(spuModel.getSpuName())
                .categoryId(spuModel.getCategoryId())
                .brandId(spuModel.getBrandId())
                .sellPoint(spuModel.getSellPoint())
                .description(spuModel.getDescription())
                .picUrls(spuModel.getPicUrls())
                .videoUrl(spuModel.getVideoUrl())
                .sort(0)
                .spuDeleted(0)
                .status(1)
                .mainImage(spuModel.getMainImage())
                .createdTime(now)
                .updatedTime(now)
                .allocationRatioCity(spuModel.getAllocationRatioCity())
                .allocationRatioProvince(spuModel.getAllocationRatioProvince())
                .allocationRatioDistrict(spuModel.getAllocationRatioDistrict())
                .inviteIncomeRatio(spuModel.getInviteIncomeRatio())
                        .build();

        pmsSpuMapper.insert(spu);

        String spuId = spu.getSpuId();

        // 保存 SKU 列表
        List<PmsSku> skuList = spuModel.getSkuList();
        if (skuList != null && !skuList.isEmpty()) {
            for (PmsSku sku : skuList) {
                // 设置 SPU ID
                sku.setSpuId(spuId);
                sku.setSkuId(SysUtil.createOrderId("SKU"));
                
                // 设置价格和库存信息（如果未设置则使用默认值）
                if (sku.getPrice() == null) {
                    sku.setPrice(java.math.BigDecimal.ZERO);
                }
                if (sku.getCostPrice() == null) {
                    sku.setCostPrice(java.math.BigDecimal.ZERO);
                }
                if (sku.getMarketPrice() == null) {
                    sku.setMarketPrice(java.math.BigDecimal.ZERO);
                }
                if (sku.getStock() == 0) {
                    sku.setStock(0);
                }
                if (sku.getWarnStock() == 0) {
                    sku.setWarnStock(0);
                }
                
                // 设置删除状态和时间戳
                sku.setIsDeleted(0);
                sku.setCreatedTime(now);
                sku.setUpdatedTime(now);
                
                // 插入 SKU
                pmsSkuMapper.insert(sku);
            }
        }
    }

    /**
     * 获取 SPU 详情
     */
    public SpuModel getSpuDetail(String spuId) {
        PmsSpu spu = pmsSpuMapper.selectById(spuId);
        if (spu == null) {
            return null;
        }

        // 获取关联的 SKU 列表
        QueryWrapper<PmsSku> skuQw = new QueryWrapper<>();
        skuQw.eq("spu_id", spuId).eq("is_deleted", 0);
        List<PmsSku> skuList = pmsSkuMapper.selectList(skuQw);

        SpuModel spuModel = new SpuModel();
        spuModel.setSpuId(spu.getSpuId());
        spuModel.setSpuName(spu.getSpuName());
        spuModel.setCategoryId(spu.getCategoryId());
        spuModel.setBrandId(spu.getBrandId());
        spuModel.setSellPoint(spu.getSellPoint());
        spuModel.setDescription(spu.getDescription());
        spuModel.setPicUrls(spu.getPicUrls());
        spuModel.setVideoUrl(spu.getVideoUrl());
        spuModel.setMainImage(spu.getMainImage());
        spuModel.setStatus(String.valueOf(spu.getStatus()));
        spuModel.setAllocationRatioProvince(spu.getAllocationRatioProvince());
        spuModel.setAllocationRatioCity(spu.getAllocationRatioCity());
        spuModel.setAllocationRatioDistrict(spu.getAllocationRatioDistrict());
        spuModel.setInviteIncomeRatio(spu.getInviteIncomeRatio());
        spuModel.setSkuList(skuList);

        return spuModel;
    }

    /**
     * 更新 SPU 及其关联的 SKU 列表
     */
    @Transactional
    public void updateSpuWithSkus(SpuModel spuModel) {
        LocalDateTime now = LocalDateTime.now();

        PmsSpu spu = PmsSpu.builder()
                .spuId(spuModel.getSpuId())
                .spuName(spuModel.getSpuName())
                .categoryId(spuModel.getCategoryId())
                .brandId(spuModel.getBrandId())
                .sellPoint(spuModel.getSellPoint())
                .description(spuModel.getDescription())
                .picUrls(spuModel.getPicUrls())
                .videoUrl(spuModel.getVideoUrl())
                .mainImage(spuModel.getMainImage())
                .status(Integer.parseInt(spuModel.getStatus()))
                .allocationRatioProvince(spuModel.getAllocationRatioProvince())
                .allocationRatioCity(spuModel.getAllocationRatioCity())
                .allocationRatioDistrict(spuModel.getAllocationRatioDistrict())
                .inviteIncomeRatio(spuModel.getInviteIncomeRatio())
                .updatedTime(now)
                .build();

        pmsSpuMapper.updateById(spu);

        // 更新 SKU 列表
        List<PmsSku> skuList = spuModel.getSkuList();
        if (skuList != null && !skuList.isEmpty()) {
            for (PmsSku sku : skuList) {
                // 设置 SPU ID
                sku.setSpuId(spuModel.getSpuId());
                
                // 设置价格和库存信息（如果未设置则使用默认值）
                if (sku.getPrice() == null) {
                    sku.setPrice(java.math.BigDecimal.ZERO);
                }
                if (sku.getCostPrice() == null) {
                    sku.setCostPrice(java.math.BigDecimal.ZERO);
                }
                if (sku.getMarketPrice() == null) {
                    sku.setMarketPrice(java.math.BigDecimal.ZERO);
                }
                if (sku.getStock() == 0) {
                    sku.setStock(0);
                }
                if (sku.getWarnStock() == 0) {
                    sku.setWarnStock(0);
                }
                
                // 设置时间戳
                sku.setUpdatedTime(now);
                
                // 如果 SKU 已存在则更新，否则插入
                if (sku.getSkuId() != null && !sku.getSkuId().isEmpty()) {
                    pmsSkuMapper.updateById(sku);
                } else {
                    sku.setSkuId(SysUtil.createOrderId("SKU"));
                    sku.setIsDeleted(0);
                    sku.setCreatedTime(now);
                    pmsSkuMapper.insert(sku);
                }
            }
        }
    }

    @Transactional
    public void deleteSpu(String spuId) {
        // 逻辑删除 SPU - 使用 UpdateWrapper 确保更新生效
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<PmsSpu> updateWrapper = 
            new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        updateWrapper.eq("spu_id", spuId)
                     .set("spu_deleted", 1)
                     .set("updated_time", LocalDateTime.now());
        pmsSpuMapper.update(null, updateWrapper);
        
        // 同时逻辑删除相关的 SKU
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<PmsSku> skuUpdateWrapper = 
            new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        skuUpdateWrapper.eq("spu_id", spuId)
                        .eq("is_deleted", 0)
                        .set("is_deleted", 1)
                        .set("updated_time", LocalDateTime.now());
        pmsSkuMapper.update(null, skuUpdateWrapper);
    }
}
