package com.usian.proxy.dynamicProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 动态代理类，提供InvocationHandler接口：通过invoke方法调用真实角色
 * 好处：
 *      1、只能代理Star类型的对象（能代理Object类型的对象）
 *      2、confer、signContract等代码不重复（没有代码重复）
 */
public class ProxyClass implements InvocationHandler{
    //真实角色
    private Object realStar;

    public ProxyClass(Object object){
        this.realStar=object;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("面谈，签合同，预付款，订机票");
        Object result = method.invoke(realStar, args);//反射调用真实角色的方法
        System.out.println("收尾款");
        return result;
    }

}
