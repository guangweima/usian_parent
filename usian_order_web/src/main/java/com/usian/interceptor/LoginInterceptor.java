package com.usian.interceptor;

import com.usian.feign.SSOSeriviceFeign;
import com.usian.pojo.TbUser;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private SSOSeriviceFeign ssoSeriviceFeign;

    /**
     * 方法执行前调用
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getParameter("token");
        if(StringUtils.isBlank(token)){
            return false;//不放行
        }
        TbUser user = ssoSeriviceFeign.getUserByToken(token);
        if(user==null){
            return false;
        }
        return true;//放行
    }
}
