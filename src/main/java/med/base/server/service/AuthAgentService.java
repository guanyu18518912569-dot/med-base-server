package med.base.server.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import med.base.server.mapper.AuthAgentMapper;
import med.base.server.mapper.AuthMenuMapper;
import med.base.server.mapper.AuthRoleMapper;
import med.base.server.model.AuthAgent;
import med.base.server.model.AuthMenu;
import med.base.server.model.AuthRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
@Service
public class AuthAgentService {


    @Autowired
    private AuthAgentMapper authAgentMapper;

    @Autowired
    private AuthRoleMapper authRoleMapper;

    @Autowired
    private AuthMenuMapper authMenuMapper;



    public AuthAgent getAuthAgentByUserId(int userId){

        return authAgentMapper.selectById(userId);
    }

    public AuthRole getAuthRoleById(int id){

        return authRoleMapper.selectById(id);
    }

    public AuthAgent getAuthAgentByUserName(String userName) {

        QueryWrapper<AuthAgent> qw = new QueryWrapper<>();

        if (StringUtils.hasLength(userName)) {
            qw.eq("phone", userName);
        }

        return authAgentMapper.selectOne(qw);
    }

    public List<AuthMenu> getMenuByMenuName(String menuName, String[] roleMenu){

        return authMenuMapper.getMenuByMenuName(menuName, roleMenu);
    }

    public int modifyUpdatedTime(AuthAgent authAgent){

        authAgent.setUpdatedTime(LocalDateTime.now());

        return authAgentMapper.updateById(authAgent);
    }
}
