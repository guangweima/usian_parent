package com.usian.feign;

import com.usian.pojo.*;
import com.usian.utils.CatResult;
import com.usian.utils.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("usian-item-service")
public interface ItemServiceFeign {
    /**
     * 查询商品信息
     * @param itemId
     * @return TbItem
     */
    @RequestMapping("/service/item/selectItemInfo")
    public TbItem selectItemInfo(@RequestParam Long itemId);

    /**
     * 分页查询商品列表
     * @param page
     * @param rows
     * @return PageResult
     */
    @RequestMapping("/service/item/selectTbItemAllByPage")
    public PageResult selectTbItemAllByPage(@RequestParam Integer page, @RequestParam Long rows);

    /**
     * 根据id查询商品类目
     * @param id
     * @return
     */
    @RequestMapping("/service/itemCat/selectItemCategoryByParentId")
    List<TbItemCat> selectItemCategoryByParentId(@RequestParam Long id);

    /**
     * 查询商品规格参数模板
     * @param itemCatId
     * @return
     */
    @RequestMapping("/service/itemParam/selectItemParamByItemCatId/{itemCatId}")
    TbItemParam selectItemParamByItemCatId(@PathVariable Long itemCatId);

    @RequestMapping("/service/item/insertTbItem")
    Integer insertTbItem(TbItem tbItem, @RequestParam String desc, @RequestParam String itemParams);

    @RequestMapping("/service/itemParam/selectItemParamAll")
    PageResult selectItemParamAll(@RequestParam Integer page, @RequestParam Integer rows);

    @RequestMapping("/service/itemParam/insertItemParam")
    Integer insertItemParam(@RequestParam Long itemCatId, @RequestParam String paramData);

    @RequestMapping("/service/itemCat/selectItemCategoryAll")
    CatResult selectItemCategoryAll();

    @RequestMapping("/service/item/selectItemDescByItemId")
    TbItemDesc selectItemDescByItemId(@RequestParam Long itemId);

    @RequestMapping("/service/itemParam/selectTbItemParamItemByItemId")
    TbItemParamItem selectTbItemParamItemByItemId(@RequestParam Long itemId);
}
