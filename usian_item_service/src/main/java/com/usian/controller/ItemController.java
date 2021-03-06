package com.usian.controller;

import com.usian.pojo.TbItem;
import com.usian.pojo.TbItemDesc;
import com.usian.service.ItemService;
import com.usian.utils.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/service/item")
public class ItemController {
    @Autowired
    private ItemService itemService;

    /**
     * 查询商品信息
     * @param itemId
     * @return
     */
    @RequestMapping("/selectItemInfo")
    public TbItem selectItemInfo(Long itemId){
        return itemService.selectItemInfo(itemId);
    }

    /**
     * 分页查询商品信息
     * @param page
     * @param rows
     * @return
     */
    @RequestMapping("/selectTbItemAllByPage")
    public PageResult selectTbItemAllByPage(Integer page,Long rows){
        return itemService.selectTbItemAllByPage(page,rows);
    }

    @RequestMapping("/insertTbItem")
    public Integer insertTbItem(@RequestBody TbItem tbItem, String desc, String itemParams){
        return itemService.insertTbItem(tbItem,desc,itemParams);
    }

    @RequestMapping("/selectItemDescByItemId")
    public TbItemDesc selectItemDescByItemId(Long itemId){
        return itemService.selectItemDescByItemId(itemId);
    }

}
