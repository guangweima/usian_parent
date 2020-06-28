package com.usian.controller;

import com.usian.feign.CartServiceFeign;
import com.usian.feign.OrderServiceFeign;
import com.usian.pojo.OrderInfo;
import com.usian.pojo.TbItem;
import com.usian.pojo.TbOrder;
import com.usian.pojo.TbOrderShipping;
import com.usian.utils.Result;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/frontend/order")
public class OrderController {

    @Autowired
    private CartServiceFeign cartServiceFeign;

    @Autowired
    private OrderServiceFeign orderServiceFeign;

    @RequestMapping("/goSettlement")
    public Result goSettlement(String[] ids, String userId) {
        List<TbItem> tbItemList = new ArrayList<TbItem>();
        Map<String, TbItem> cart = cartServiceFeign.selectCartByUserId(userId);
        for (int i = 0; i < ids.length; i++) {
            String itemId = ids[i];
            tbItemList.add(cart.get(itemId));
        }
        if(tbItemList.size()>0){
            return Result.ok(tbItemList);
        }
        return Result.error("查询失败");
    }

    @RequestMapping("/insertOrder")
    public Result insertOrder(TbOrder tbOrder, String orderItem, TbOrderShipping tbOrderShipping){

        /**
         * @RequestBody：获取请求体中的json串--->pojo
         * 下游服务controller：
         *      public Result insertOrder(@RequestBody TbOrder tbOrder, @RequestBody TbOrderShipping tbOrderShipping, String orderItem)
         *
         * 一个request只包含一个request body，所以feign不支持多个@RequestBody
         *
         */
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setTbOrder(tbOrder);
        orderInfo.setTbOrderShipping(tbOrderShipping);
        orderInfo.setOrderItem(orderItem);
        String orderId = orderServiceFeign.insertOrder(orderInfo);
        if(StringUtils.isNotBlank(orderId)){
            return Result.ok(orderId);
        }
        return Result.error("订单保存失败");
    }
}
