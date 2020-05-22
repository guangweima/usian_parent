package com.usian.service;

import com.usian.mapper.TbItemCatMapper;
import com.usian.pojo.TbItemCat;
import com.usian.pojo.TbItemCatExample;
import com.usian.utils.CatNode;
import com.usian.utils.CatResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
@Service
@Transactional
public class ItemCatServiceImpl implements ItemCatService{

    @Autowired
    private TbItemCatMapper tbItemCatMapper;

    /**
     * 根据id查询商品类目
     * @param id
     * @return
     */
    @Override
    public List<TbItemCat> selectItemCategoryByParentId(Long id) {
        TbItemCatExample tbItemCatExample = new TbItemCatExample();
        TbItemCatExample.Criteria criteria = tbItemCatExample.createCriteria();
        criteria.andStatusEqualTo(1);
        criteria.andParentIdEqualTo(id);
        return tbItemCatMapper.selectByExample(tbItemCatExample);
    }

    @Override
    public CatResult selectItemCategoryAll() {
        //因为一级菜单有子菜单，子菜单有子子菜单，所以要递归调用
        List catlist = getCatlist(0L);
        CatResult catResult = new CatResult();
        catResult.setData(catlist);

        return catResult;
    }

    private List getCatlist(Long parentId){
        //1、查询商品类目列表
        TbItemCatExample tbItemCatExample = new TbItemCatExample();
        TbItemCatExample.Criteria criteria = tbItemCatExample.createCriteria();
        criteria.andParentIdEqualTo(parentId);
        List<TbItemCat> tbItemCatList = tbItemCatMapper.selectByExample(tbItemCatExample);

        //2、拼接CatNode
        List catNodeList = new ArrayList();
        int count = 0;
        for (int i = 0; i < tbItemCatList.size(); i++) {
            TbItemCat tbItemCat =  tbItemCatList.get(i);
            //2.1、该类目是父节点：n:"",i:[]
            if(tbItemCat.getIsParent()){
                CatNode catNode = new CatNode();
                catNode.setName(tbItemCat.getName());
                catNode.setItem(getCatlist(tbItemCat.getId()));
                catNodeList.add(catNode);
                count = count + 1;
                if(count == 18){
                    break;
                }
            }else{
              //2.2、该类不是父节点：直接把类目名称添加到集合中
                catNodeList.add(tbItemCat.getName());
            }
        }
        return catNodeList;
    }
}
