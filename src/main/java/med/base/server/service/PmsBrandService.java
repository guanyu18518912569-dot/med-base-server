package med.base.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import med.base.server.mapper.PmsBrandMapper;
import med.base.server.model.PmsBrand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PmsBrandService {

    @Autowired
    private PmsBrandMapper pmsBrandMapper;

    public void save(PmsBrand pmsBrand) {
        pmsBrandMapper.insert(pmsBrand);
    }

    public void update(PmsBrand pmsBrand) {
        pmsBrandMapper.updateById(pmsBrand);
    }

    public void delete(int brandId) {
        pmsBrandMapper.deleteById(brandId);
    }

    public List<PmsBrand> getList() {
        QueryWrapper<PmsBrand> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().orderByAsc(PmsBrand::getSort).orderByAsc(PmsBrand::getFirstLetter);
        return pmsBrandMapper.selectList(queryWrapper);
    }

    public PmsBrand getById(int brandId) {
        return pmsBrandMapper.selectById(brandId);
    }
}

