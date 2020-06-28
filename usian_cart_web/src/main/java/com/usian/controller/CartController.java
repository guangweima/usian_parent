package com.usian.controller;

import com.usian.feign.CartServiceFeign;
import com.usian.feign.ItemServiceFeign;
import com.usian.pojo.TbItem;
import com.usian.utils.CookieUtils;
import com.usian.utils.JsonUtils;
import com.usian.utils.Result;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping("/frontend/cart")
public class CartController {

    @Value("${CART_COOKIE_KEY}")
    private String CART_COOKIE_KEY;

    @Value("${CART_COOKIE_EXPIRE}")
    private Integer CART_COOKIE_EXPIRE;

    @Autowired
    private ItemServiceFeign itemServiceFeign;

    @Autowired
    private CartServiceFeign cartServiceFeign;

    @RequestMapping("/addItem")
    public Result addItem(Long itemId, String userId, @RequestParam(defaultValue = "1") Integer num,
                          HttpServletRequest request, HttpServletResponse response){
        try {
            if(StringUtils.isBlank(userId)){
                /*********未登录********/
                //1、查询购物车列表:从cookie中获取map_json-->map
                Map<String, TbItem> cart = getCartFromCookie(request);

                //2、添加商品到购物车
                addItemToCart(cart,itemId,num);

                //3、把购物车写到cookie
                addClientCookie(cart,request,response);

            }else{
                /*********已登录*****/
                //1、查询购物车列表：从redis中获取map_json-->map
                Map<String, TbItem> cart = getCartFromRedis(userId);

                //2、添加商品到购物车：修改cart
                addItemToCart(cart,itemId,num);

                //3、把购物车写到redis
                addCartToRedis(cart,userId);
            }
            return Result.ok();
        }catch (Exception e){
            e.printStackTrace();
            return Result.error("添加失败");
        }
    }

    /**
     * 添加购物车到redis
     * @param cart
     * @param userId
     */
    private void addCartToRedis(Map<String, TbItem> cart, String userId) {
        cartServiceFeign.insertCart(cart,userId);
    }

    /**
     * 查询购物车
     * @param userId
     * @return
     */
    private Map<String, TbItem> getCartFromRedis(String userId) {
        Map<String, TbItem> cart = cartServiceFeign.selectCartByUserId(userId);
        if(cart!=null && cart.size()>0){
            return cart;
        }
        return new HashMap<String,TbItem>();
    }





    /**
     * 把购物车列表写到cookie
     * @param cart
     * @param request
     * @param response
     */
    private void addClientCookie(Map<String, TbItem> cart, HttpServletRequest request, HttpServletResponse response) {
        String cartJson = JsonUtils.objectToJson(cart);
        CookieUtils.setCookie(request,response,CART_COOKIE_KEY,cartJson,CART_COOKIE_EXPIRE,true);
    }

    /**
     *
     * @param itemId
     */
    private void addItemToCart(Map<String, TbItem> cart,Long itemId, Integer num) {

        TbItem tbItem = cart.get(itemId.toString());
        if(tbItem!=null){
            //购物已存在该商品：数量+num
            tbItem.setNum(tbItem.getNum()+num);
        }else{
            //购物车不存在该商品：根据itemId查询商品，再把商品信息添加到购物车
            tbItem = itemServiceFeign.selectItemInfo(itemId);
            tbItem.setNum(num);
        }
        cart.put(itemId.toString(),tbItem);
    }

    /**
     * 从cookie获取购物车列表
     * @param request
     * @return
     */
    private Map<String, TbItem> getCartFromCookie(HttpServletRequest request) {
        String cartJson = CookieUtils.getCookieValue(request, CART_COOKIE_KEY, true);
        //购物车已存在
        if(StringUtils.isNotBlank(cartJson)) {
            Map map = JsonUtils.jsonToMap(cartJson, TbItem.class);
            return map;
        }
        //购物车不存在
        return new HashMap<String, TbItem>();
    }

    @RequestMapping("/showCart")
    public Result showCart(String userId,HttpServletRequest request){
        try {
            List<TbItem> tbItemList = new ArrayList<TbItem>();
            if(StringUtils.isBlank(userId)){
                //未登录：Map<itemId,TbItem>----->List<TbItem>
                Map<String,TbItem> cart = getCartFromCookie(request);
                Set<String> keySet = cart.keySet();
                for (String itemId : keySet) {
                    TbItem tbItem = cart.get(itemId);
                    tbItemList.add(tbItem);
                }
            }else{
                //登录
                Map<String, TbItem> cart = getCartFromRedis(userId);
                Set<String> keySet = cart.keySet();
                for(String itemId : keySet){
                    TbItem tbItem = cart.get(itemId);
                    tbItemList.add(tbItem);
                }
            }
            return Result.ok(tbItemList);
        }catch (Exception e){
            e.printStackTrace();
            return Result.error("查询失败");
        }

    }

    @RequestMapping("/updateItemNum")
    public Result updateItemNum(String userId,Long itemId,Integer num,HttpServletRequest request,
                                HttpServletResponse response){
        try {
            if(StringUtils.isBlank(userId)){
                //未登录
                //1、获得cookie中的购物车
                Map<String, TbItem> cart = getCartFromCookie(request);

                //2、修改购物车中的商品
                TbItem tbItem = cart.get(itemId.toString());
                tbItem.setNum(num);
                cart.put(itemId.toString(),tbItem);

                //3、把购物车写到cookie
                addClientCookie(cart,request,response);
            }else{
                //登录
                //1、获得cookie的购物车
                Map<String, TbItem> cart = getCartFromRedis(userId);

                //2、修改购物车中的商品
                TbItem tbItem = cart.get(itemId.toString());
                tbItem.setNum(num);
                cart.put(itemId.toString(),tbItem);

                //3、把购物车写到redis
                addCartToRedis(cart,userId);
            }
            return Result.ok();
        }catch (Exception e){
            e.printStackTrace();
            return Result.error("修改失败");
        }
    }

    @RequestMapping("/deleteItemFromCart")
    public Result deleteItemFromCart(String userId,Long itemId,HttpServletRequest request,HttpServletResponse response){
        try {
            if(StringUtils.isBlank(userId)){
                //未登录
                //1、获得cookie的购物车
                Map<String, TbItem> cart = getCartFromCookie(request);
                //2、删除购物车中的商品
                cart.remove(itemId.toString());
                //3、把购车从写到cookie
                addClientCookie(cart,request,response);
            }else{
                //登录
                //1、获得redis中的购物车
                Map<String, TbItem> cart = getCartFromRedis(userId);

                //2、删除购物车中的商品
                cart.remove(itemId.toString());

                //3、把购物车写到redis中
                addCartToRedis(cart,userId);
            }
            return Result.ok();
        }catch (Exception e){
            e.printStackTrace();
            return Result.error("删除失败");
        }
    }
}
