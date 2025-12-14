package med.base.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import med.base.server.mapper.PmsSpecKeyMapper;
import med.base.server.mapper.PmsSpecValueMapper;
import med.base.server.model.PmsSpecKey;
import med.base.server.model.PmsSpecValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PmsSpecKeyService {

    @Autowired
    private PmsSpecKeyMapper pmsSpecKeyMapper;

    @Autowired
    private PmsSpecValueMapper pmsSpecValueMapper;

    /**
     * 保存规格键及其规格值列表
     */
    @Transactional
    public void save(PmsSpecKey pmsSpecKey, List<PmsSpecValue> specValues) {
        // 保存规格键，MyBatis-Plus 会自动将生成的主键回填到对象中
        pmsSpecKeyMapper.insert(pmsSpecKey);
        
        // 确保主键已回填
        int specKeyId = pmsSpecKey.getSpecKeyId();
        if (specKeyId <= 0) {
            throw new RuntimeException("保存规格键失败，未能获取生成的主键ID");
        }
        
        // 保存规格值列表
        if (specValues != null && !specValues.isEmpty()) {
            for (PmsSpecValue specValue : specValues) {
                specValue.setSpecKeyId(specKeyId);
                pmsSpecValueMapper.insert(specValue);
            }
        }
    }

    /**
     * 更新规格键及其规格值列表
     */
    @Transactional
    public void update(PmsSpecKey pmsSpecKey, List<PmsSpecValue> specValues) {
        // 更新规格键
        pmsSpecKeyMapper.updateById(pmsSpecKey);
        
        // 删除旧的规格值
        QueryWrapper<PmsSpecValue> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.lambda().eq(PmsSpecValue::getSpecKeyId, pmsSpecKey.getSpecKeyId());
        pmsSpecValueMapper.delete(deleteWrapper);
        
        // 保存新的规格值列表
        if (specValues != null && !specValues.isEmpty()) {
            for (PmsSpecValue specValue : specValues) {
                specValue.setSpecKeyId(pmsSpecKey.getSpecKeyId());
                pmsSpecValueMapper.insert(specValue);
            }
        }
    }

    /**
     * 删除规格键及其关联的规格值
     */
    @Transactional
    public void delete(int specKeyId) {
        // 删除关联的规格值
        QueryWrapper<PmsSpecValue> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.lambda().eq(PmsSpecValue::getSpecKeyId, specKeyId);
        pmsSpecValueMapper.delete(deleteWrapper);
        
        // 删除规格键
        pmsSpecKeyMapper.deleteById(specKeyId);
    }

    /**
     * 获取规格键列表
     */
    public List<PmsSpecKey> getList() {
        QueryWrapper<PmsSpecKey> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByAsc(PmsSpecKey::getSort);
        return pmsSpecKeyMapper.selectList(queryWrapper);
    }

    /**
     * 根据ID获取规格键（包含规格值）
     */
    public PmsSpecKey getById(int specKeyId) {
        return pmsSpecKeyMapper.selectById(specKeyId);
    }

    /**
     * 根据规格键ID获取规格值列表
     */
    public List<PmsSpecValue> getSpecValuesBySpecKeyId(int specKeyId) {
        QueryWrapper<PmsSpecValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(PmsSpecValue::getSpecKeyId, specKeyId);
        return pmsSpecValueMapper.selectList(queryWrapper);
    }
}

