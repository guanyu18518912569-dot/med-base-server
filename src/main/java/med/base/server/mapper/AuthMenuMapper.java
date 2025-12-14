package med.base.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import med.base.server.model.AuthMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AuthMenuMapper extends BaseMapper<AuthMenu> {

    @Select("select * from users a inner join order_deposit b on a.openid=b.openid ${ew.customSqlSegment}")
    List<AuthMenu> getMenuByMenuName(String menuName, String[] roleMenu);//判断是否有菜单权限
}
