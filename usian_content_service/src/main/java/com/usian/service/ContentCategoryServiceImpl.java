package com.usian.service;

import com.usian.mapper.TbContentCategoryMapper;
import com.usian.pojo.TbContentCategory;
import com.usian.pojo.TbContentCategoryExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ContentCategoryServiceImpl implements ContentCategoryService{

    @Autowired
    private TbContentCategoryMapper tbContentCategoryMapper;

    @Override
    public List<TbContentCategory> selectContentCategoryByParentId(Long id) {
        TbContentCategoryExample tbContentCategoryExample = new TbContentCategoryExample();
        TbContentCategoryExample.Criteria criteria = tbContentCategoryExample.createCriteria();
        criteria.andParentIdEqualTo(id);
        return tbContentCategoryMapper.selectByExample(tbContentCategoryExample);
    }

    @Override
    public Integer insertContentCategory(TbContentCategory tbContentCategory) {
        //1、添加内容分类
        Date date = new Date();
        tbContentCategory.setSortOrder(1);
        tbContentCategory.setStatus(1);
        tbContentCategory.setIsParent(false);
        tbContentCategory.setCreated(date);
        tbContentCategory.setUpdated(date);
        Integer insertSelective = tbContentCategoryMapper.insertSelective(tbContentCategory);

        //2、如果他爹不是爹，要把他爹改成爹
        //2.1、查询他爹
        TbContentCategory parentContentCategory = tbContentCategoryMapper.selectByPrimaryKey(tbContentCategory.getParentId());
        if(!parentContentCategory.getIsParent()){
            parentContentCategory.setIsParent(true);
            parentContentCategory.setUpdated(new Date());
            int updateByPrimaryKeySelective = tbContentCategoryMapper.updateByPrimaryKeySelective(parentContentCategory);
        }

        return insertSelective;
    }

    @Override
    public Integer deleteContentCategoryById(Long categoryId) {
        //1、如果有子节点，直接返回 0
        TbContentCategory tbContentCategory = tbContentCategoryMapper.selectByPrimaryKey(categoryId);
        if(tbContentCategory.getIsParent()){
            return 0;
        }
        //2、删除当前节点
        tbContentCategoryMapper.deleteByPrimaryKey(categoryId);

        //3、如果他爹不是爹，要把他爹改成不是爹
        TbContentCategoryExample tbContentCategoryExample = new TbContentCategoryExample();
        TbContentCategoryExample.Criteria criteria = tbContentCategoryExample.createCriteria();
        criteria.andParentIdEqualTo(tbContentCategory.getParentId());
        List<TbContentCategory> tbContentCategoryList = tbContentCategoryMapper.selectByExample(tbContentCategoryExample);
        if(tbContentCategoryList==null || tbContentCategoryList.size()==0){
            TbContentCategory parentTbContentCategory = new TbContentCategory();
            parentTbContentCategory.setId(tbContentCategory.getParentId());
            parentTbContentCategory.setIsParent(false);
            parentTbContentCategory.setUpdated(new Date());
            tbContentCategoryMapper.updateByPrimaryKeySelective(parentTbContentCategory);
        }
        return 200;
    }

    @Override
    public Integer updateContentCategory(TbContentCategory tbContentCategory) {
        tbContentCategory.setUpdated(new Date());
        return tbContentCategoryMapper.updateByPrimaryKeySelective(tbContentCategory);
    }
}
