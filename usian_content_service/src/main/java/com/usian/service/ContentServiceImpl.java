package com.usian.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.usian.mapper.TbContentMapper;
import com.usian.pojo.TbContent;
import com.usian.pojo.TbContentExample;
import com.usian.redis.RedisClient;
import com.usian.utils.AdNode;
import com.usian.utils.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ContentServiceImpl implements ContentService{

    @Value("${AD_CATEGORY_ID}")
    private Long AD_CATEGORY_ID;

    @Value("${AD_HEIGHT}")
    private Integer AD_HEIGHT;

    @Value("${AD_WIDTH}")
    private Integer AD_WIDTH;

    @Value("${AD_HEIGHTB}")
    private Integer AD_HEIGHTB;

    @Value("${AD_WIDTHB}")
    private Integer AD_WIDTHB;

    @Autowired
    private TbContentMapper tbContentMapper;

    @Autowired
    private RedisClient redisClient;

    @Value("${PORTAL_AD_KEY}")
    private String PORTAL_AD_KEY;

    @Override
    public PageResult selectTbContentAllByCategoryId(Integer page, Integer rows, Long categoryId) {
        PageHelper.startPage(page,rows);
        TbContentExample tbContentExample = new TbContentExample();
        tbContentExample.setOrderByClause("updated DESC");
        TbContentExample.Criteria criteria = tbContentExample.createCriteria();
        criteria.andCategoryIdEqualTo(categoryId);
        List<TbContent> tbContentList = tbContentMapper.selectByExampleWithBLOBs(tbContentExample);

        PageInfo<TbContent> PageInfo = new PageInfo<>(tbContentList);
        PageResult pageResult = new PageResult();
        pageResult.setResult(PageInfo.getList());
        pageResult.setPageIndex(PageInfo.getPageNum());
        pageResult.setTotalPage(Long.valueOf(PageInfo.getPages()));
        return pageResult;
    }

    @Override
    public Integer insertTbContent(TbContent tbContent) {
        Date date = new Date();
        tbContent.setCreated(date);
        tbContent.setUpdated(date);
        int insertSelective = tbContentMapper.insertSelective(tbContent);
        //缓存同步
        redisClient.hdel(PORTAL_AD_KEY,AD_CATEGORY_ID);
        return insertSelective;
    }

    @Override
    public Integer deleteContentByIds(Long ids) {
        int deleteByPrimaryKey = tbContentMapper.deleteByPrimaryKey(ids);
        //缓存同步
        redisClient.hdel(PORTAL_AD_KEY,AD_CATEGORY_ID);
        return deleteByPrimaryKey;
    }

    @Override
    public List<AdNode> selectFrontendContentByAD() {
        //1、查询redis
        List<AdNode> adNodeListRedis = (List<AdNode>) redisClient.hget(PORTAL_AD_KEY, AD_CATEGORY_ID.toString());
        if(adNodeListRedis!=null){
            //2、如果redis查询到，直接返回
            return adNodeListRedis;
        }

        //3、如果redis查询不到，则查询数据库并把结果添加到redis
        TbContentExample tbContentExample = new TbContentExample();
        TbContentExample.Criteria criteria = tbContentExample.createCriteria();
        criteria.andCategoryIdEqualTo(AD_CATEGORY_ID);
        List<TbContent> tbContentList = tbContentMapper.selectByExample(tbContentExample);
        List<AdNode> adNodeList = new ArrayList<AdNode>();
        for(TbContent tbContent : tbContentList){
            AdNode adNode = new AdNode();
            adNode.setSrc(tbContent.getPic());
            adNode.setSrcB(tbContent.getPic2());
            adNode.setHref(tbContent.getUrl());
            adNode.setHeight(AD_HEIGHT);
            adNode.setWidth(AD_WIDTH);
            adNode.setHeightB(AD_HEIGHTB);
            adNode.setWidthB(AD_WIDTHB);
            adNodeList.add(adNode);
        }
        redisClient.hset(PORTAL_AD_KEY,AD_CATEGORY_ID.toString(),adNodeList);
        return adNodeList;
    }
}
