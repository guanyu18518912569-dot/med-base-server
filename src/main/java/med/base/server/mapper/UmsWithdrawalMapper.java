package med.base.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import med.base.server.model.UmsWithdrawal;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户提现申请Mapper
 */
@Mapper
public interface UmsWithdrawalMapper extends BaseMapper<UmsWithdrawal> {
}
