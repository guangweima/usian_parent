package com.usian.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.usian.mapper.TbItemParamItemMapper;
import com.usian.mapper.TbItemParamMapper;
import com.usian.pojo.TbItemParam;
import com.usian.pojo.TbItemParamExample;
import com.usian.pojo.TbItemParamItem;
import com.usian.pojo.TbItemParamItemExample;
import com.usian.redis.RedisClient;
import com.usian.utils.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
@Service
@Transactional
public class ItemParamServiceImpl implements ItemParamService{

    @Value("${ITEM_INFO}")
    private String ITEM_INFO;

    @Value("${PARAM}")
    private String PARAM;

    @Value("${ITEM_INFO_EXPIRE}")
    private Long ITEM_INFO_EXPIRE;

    @Autowired
    private TbItemParamMapper tbItemParamMapper;

    @Autowired
    private TbItemParamItemMapper tbItemParamItemMapper;

    @Autowired
    private RedisClient redisClient;

    @Value("${SETNX_PARAM_LOCK_KEY}")
    private String SETNX_PARAM_LOCK_KEY;

    @Override
    public TbItemParamItem selectTbItemParamItemByItemId(Long itemId) {
        //1、先查询redis,如果有直接返回
        TbItemParamItem tbItemParamItem = (TbItemParamItem) redisClient.get(ITEM_INFO + ":" + itemId + ":" + PARAM);
        if(tbItemParamItem!=null){
            return tbItemParamItem;
        }
        if(redisClient.setnx(SETNX_PARAM_LOCK_KEY+":"+itemId,itemId,30L)){
            //2、再查询mysql,并把查询结果缓存到redis,并设置失效时间
            TbItemParamItemExample tbItemParamItemExample = new TbItemParamItemExample();
            TbItemParamItemExample.Criteria criteria = tbItemParamItemExample.createCriteria();
            criteria.andItemIdEqualTo(itemId);
            List<TbItemParamItem> tbItemParamItems = tbItemParamItemMapper.selectByExampleWithBLOBs(tbItemParamItemExample);
            if(tbItemParamItems!=null && tbItemParamItems.size()>0){
                tbItemParamItem = tbItemParamItems.get(0);
                redisClient.set(ITEM_INFO + ":" + itemId + ":" + PARAM,tbItemParamItem);
                redisClient.expire(ITEM_INFO + ":" + itemId + ":" + PARAM,ITEM_INFO_EXPIRE);

            }else{
                redisClient.set(ITEM_INFO + ":" + itemId + ":" + PARAM,null);
                redisClient.expire(ITEM_INFO + ":" + itemId + ":" + PARAM,30L);
            }
            redisClient.del(SETNX_PARAM_LOCK_KEY+":"+itemId);
            return  tbItemParamItem;
        }else{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return selectTbItemParamItemByItemId(itemId);
        }
    }

    @Override
    public TbItemParam selectItemParamByItemCatId(Long itemCatId) {

        TbItemParamExample tbItemParamExample = new TbItemParamExample();
        TbItemParamExample.Criteria criteria = tbItemParamExample.createCriteria();
        criteria.andItemCatIdEqualTo(itemCatId);
        List<TbItemParam> itemParamList = tbItemParamMapper.selectByExampleWithBLOBs(tbItemParamExample);
        if(itemParamList!=null && itemParamList.size()>0){
            return itemParamList.get(0);
        }
        return null;
    }

    @Override
    public PageResult selectItemParamAll(Integer page, Integer rows) {
        PageHelper.startPage(page,rows);
        TbItemParamExample tbItemParamExample = new TbItemParamExample();
        tbItemParamExample.setOrderByClause("updated DESC");
        List<TbItemParam> itemParamList = tbItemParamMapper.selectByExampleWithBLOBs(tbItemParamExample);

        PageInfo<TbItemParam> pageInfo = new PageInfo<TbItemParam>(itemParamList);

        PageResult pageResult = new PageResult();
        pageResult.setTotalPage(Long.valueOf(pageInfo.getPages()));
        pageResult.setPageIndex(pageInfo.getPageNum());
        pageResult.setResult(pageInfo.getList());
        return pageResult;
    }

    @Override
    public Integer insertItemParam(Long itemCatId, String paramData) {
        //1、判断该类别的商品是否有规格模板
        TbItemParamExample tbItemParamExample = new TbItemParamExample();
        TbItemParamExample.Criteria criteria = tbItemParamExample.createCriteria();
        criteria.andItemCatIdEqualTo(itemCatId);
        List<TbItemParam> itemParamList = tbItemParamMapper.selectByExample(tbItemParamExample);
        if(itemParamList.size()>0){
            return 0;
        }

        //2、保存规格模板
        Date date = new Date();
        TbItemParam tbItemParam = new TbItemParam();
        tbItemParam.setItemCatId(itemCatId);
        tbItemParam.setParamData(paramData);
        tbItemParam.setUpdated(date);
        tbItemParam.setCreated(date);
        return tbItemParamMapper.insertSelective(tbItemParam);
    }
}
