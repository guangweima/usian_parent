package com.usian.controller;

import com.usian.feign.SSOSeriviceFeign;
import com.usian.pojo.TbUser;
import com.usian.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/frontend/sso")
public class SSOController {

    @Autowired
    private SSOSeriviceFeign ssoSeriviceFeign;

    /**
     * 用户信息校验
     * @param checkValue
     * @param checkFlag
     * @return
     */
    @RequestMapping("checkUserInfo/{checkValue}/{checkFlag}")
    public Result checkUserInfo(@PathVariable String checkValue, @PathVariable Integer checkFlag){
        Boolean checkUserInfo= ssoSeriviceFeign.checkUserInfo(checkValue,checkFlag);
        if(checkUserInfo){
            return Result.ok();
        }
        return Result.error("校验失败");
    }

    /**
     * 注册
     * @param tbUser
     * @return
     */
    @RequestMapping("/userRegister")
    public Result userRegister(TbUser tbUser){
        Integer userRegister = ssoSeriviceFeign.userRegister(tbUser);
        if(userRegister==1){
            return Result.ok();
        }
        return Result.error("注册失败");
    }

    /**
     * 登录
     * @param username
     * @param password
     * @return
     */
    @RequestMapping("/userLogin")
    public Result userLogin(String username, String password){
        Map map = ssoSeriviceFeign.userLogin(username,password);
        if(map!=null){
            return Result.ok(map);
        }
        return Result.error("登录失败");
    }

    /**
     * 根据token用户信息
     *  1、后台：redis用户信息过期
     *  2、前台：删除cookie中user信息
     * @param token
     * @return
     */
    @RequestMapping("/getUserByToken/{token}")
    public Result getUserByToken(@PathVariable String token){
        TbUser tbUser = ssoSeriviceFeign.getUserByToken(token);
        if(tbUser!=null){
            return Result.ok();
        }
        return Result.error("登录过期");
    }

    /**
     * 退出
     *  1、后台：删除redis中的user信息
     *  2、前台：删除cookie中user信息
     * @param token
     * @return
     */
    @RequestMapping("/logOut")
    public Result logOut(String token){
        Boolean logOut = ssoSeriviceFeign.logOut(token);
        if(logOut){
            return Result.ok();
        }
        return Result.error("退出失败");
    }
}
