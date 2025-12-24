package med.base.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import med.base.server.mapper.PmsCategoryMapper;
import med.base.server.mapper.PmsSpuMapper;
import med.base.server.model.PmsCategory;
import med.base.server.model.PmsSpu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PmsCategoryService {

    @Autowired
    private PmsCategoryMapper pmsCategoryMapper;

    public void save(PmsCategory pmsCategory){

        pmsCategoryMapper.insert(pmsCategory);
    }

    public List<PmsCategory> getPmsCategoryList(){

        return pmsCategoryMapper.selectList(null);
    }

    public List<PmsCategory> getPmsCategoryParentList(int parentId){

        QueryWrapper<PmsCategory> queryWrapper = new QueryWrapper<>();

        queryWrapper.lambda().eq(PmsCategory::getParentId, parentId);

        return pmsCategoryMapper.selectList(queryWrapper);
    }

    public PmsCategory getById(int categoryId){
        return pmsCategoryMapper.selectById(categoryId);
    }

    public void updateById(PmsCategory pmsCategory){
        pmsCategoryMapper.updateById(pmsCategory);
    }
}
