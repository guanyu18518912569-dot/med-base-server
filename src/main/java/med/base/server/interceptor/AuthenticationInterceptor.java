package med.base.server.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import med.base.server.annotation.PassToken;
import med.base.server.annotation.RequiresRolePermissions;
import med.base.server.annotation.UserLoginToken;
import med.base.server.model.AuthAgent;
import med.base.server.model.AuthMenu;
import med.base.server.model.AuthRole;
import med.base.server.service.AuthAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.util.List;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;


public class AuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    AuthAgentService authAgentService;


    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse response, Object handler) throws Exception {

        String token = httpServletRequest.getHeader("Token");// 从 http 请求头中取出 token
        AuthAgent authAgent = null;

        //如果不是映射到方法直接通过
        if(!(handler instanceof HandlerMethod)){
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod)handler;
        Method method = handlerMethod.getMethod();
        //检查是否有passtoken注释，有则跳过认证
        if(method.isAnnotationPresent(PassToken.class)){
            PassToken passToken = method.getAnnotation(PassToken.class);
            if(passToken.required()){
                return true;
            }
        }
        //检查有没有需要用户权限的注解
        if(method.isAnnotationPresent(UserLoginToken.class)){
            UserLoginToken userLoginToken = method.getAnnotation(UserLoginToken.class);
            if(userLoginToken.required()){
                if(token == null){
                    throw new RuntimeException("无token,请重新登录");
                }
                String userId;
                try{
                    userId = JWT.decode(token).getAudience().get(0);
                }catch (JWTDecodeException j){
                    throw new RuntimeException("401");
                }

                authAgent = authAgentService.getAuthAgentByUserId(Integer.parseInt(userId));
                if(authAgent == null){
                    throw new RuntimeException("用户不存在，请重新登录");
                }

                JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(authAgent.getPassword())).build();
                try {
                    jwtVerifier.verify(token);
                }catch (JWTVerificationException e){
                    throw new RuntimeException("401");
                }
                return true;
            }
        }

        if(method.isAnnotationPresent(RequiresRolePermissions.class)){
            RequiresRolePermissions requiresRolePermissions = method.getAnnotation(RequiresRolePermissions.class);
            if(requiresRolePermissions.required()){
                String route = httpServletRequest.getRequestURI();

                if(token == null){
                    throw new RuntimeException("无token,请重新登录");
                }
                String userId;
                try{
                    userId = JWT.decode(token).getAudience().get(0);
                }catch (JWTDecodeException j){
                    throw new RuntimeException("userid获取失败");
                }
                if(authAgent == null){
                    authAgent = authAgentService.getAuthAgentByUserId(Integer.parseInt(userId));
                }

                AuthRole authRole = authAgentService.getAuthRoleById(authAgent.getRoleId());
                String roleMenu = authRole.getRoleMenu();
                String a = roleMenu.substring(1);
                String[] b = a.substring(0,a.length()-1).split(",");

                List<AuthMenu> listMenu = authAgentService.getMenuByMenuName(route, b);

                if(listMenu.size() == 0){
                    throw new RuntimeException("没有权限！");
                }
                return true;
            }
        }
        return  false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }
}
