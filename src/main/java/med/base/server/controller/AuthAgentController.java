package med.base.server.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import med.base.server.annotation.PassToken;
import med.base.server.common.DefaultResponse;
import med.base.server.model.AuthAgent;
import med.base.server.model.AuthRole;
import med.base.server.model.ViewModel.LoginModel;
import med.base.server.service.AuthAgentService;
import med.base.server.service.TokenService;
import med.base.server.util.MD5Util;
import med.base.server.util.ValidateCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/agent")
public class AuthAgentController {

    @Autowired
    TokenService tokenService;

    @Autowired
    AuthAgentService authAgentService;



    @GetMapping("/getVerifyCode")
    @PassToken
    public void getVerifyCode(HttpServletRequest request, HttpServletResponse response){
        try {
            response.setContentType("image/jpeg");//设置相应类型,告诉浏览器输出的内容为图片
            response.setHeader("Pragma", "No-cache");//设置响应头信息，告诉浏览器不要缓存此内容
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expire", 10);
            ValidateCodeUtil randomValidateCode = new ValidateCodeUtil();
            randomValidateCode.getRandcode(request, response);//输出验证码图片方法
        } catch (Exception e) {
            System.out.println("获取验证码失败>>>> "+e.getMessage());
        }
    }

    @GetMapping("/checkVerifyCode")
    @PassToken
    public String checkVerifyCode(String verifyCode, HttpServletRequest request) throws RuntimeException{

        try{

            if(!StringUtils.hasLength(verifyCode)){
                return DefaultResponse.error("验证码不能为空");
            }

            String code = "";
            if(request.getCookies() != null){
                Cookie[] cookies = request.getCookies();
                for (Cookie cookie : cookies) {
                    if (ValidateCodeUtil.RandomCodeKey.equals(cookie.getName())) {
                        code = cookie.getValue();
                        break;
                    }
                }
            }

            if(!code.equals(MD5Util.getMD5(verifyCode))){
                return DefaultResponse.error("验证码错误");
            }

            return DefaultResponse.success(true);

        }catch (RuntimeException e){
//            throw e;
            return DefaultResponse.error(e.getMessage());
        }
    }

    @PostMapping("login")
    @PassToken
    public String login(@RequestBody LoginModel loginModel) {

        Map<String, Object> map = new HashMap<String,Object>();

        AuthAgent authAgent = authAgentService.getAuthAgentByUserName(loginModel.getUserName());

        if(authAgent == null){
            return DefaultResponse.error("用户不存在！");
        }

        if(!authAgent.getPassword().equals(MD5Util.getMD5(loginModel.getPassword()))){
            return DefaultResponse.error("密码错误！");
        }

        // 更新登录时间
        int updateSuccess = authAgentService.modifyUpdatedTime(authAgent);


        int roleId = authAgent.getRoleId();
        if(roleId<=0){
            roleId = 2; //默认值 渠道
        }
        AuthRole authRole = authAgentService.getAuthRoleById(roleId);


        String token = tokenService.getToken(authAgent);
        map.put("token", token);
        map.put("authAgent", authAgent);
        map.put("roleId",  authRole.getId());
        return DefaultResponse.success(map);
    }
}
