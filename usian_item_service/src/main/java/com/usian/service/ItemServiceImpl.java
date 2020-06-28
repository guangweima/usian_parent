package com.usian.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.usian.mapper.*;
import com.usian.pojo.*;
import com.usian.redis.RedisClient;
import com.usian.utils.IDUtils;
import com.usian.utils.PageResult;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class ItemServiceImpl implements ItemService {

    @Autowired
    private TbItemMapper tbItemMapper;

    @Autowired
    private TbItemDescMapper tbItemDescMapper;

    @Autowired
    private TbItemParamItemMapper tbItemParamItemMapper;

    @Autowired
    private TbItemCatMapper tbItemCatMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private RedisClient redisClient;

    @Value("${ITEM_INFO}")
    private String ITEM_INFO;

    @Value("${BASE}")
    private String BASE;

    @Value("${DESC}")
    private String DESC;

    @Value("${ITEM_INFO_EXPIRE}")
    private Long ITEM_INFO_EXPIRE;

    @Value("${SETNX_BASC_LOCK_KEY}")
    private String SETNX_BASC_LOCK_KEY;

    @Value("${SETNX_DESC_LOCK_KEY}")
    private String SETNX_DESC_LOCK_KEY;

    @Autowired
    private TbOrderItemMapper tbOrderItemMapper;

    @Override
    public TbItem selectItemInfo(Long itemId){
        //1、先查询redis,如果有直接返回
        TbItem tbItem = (TbItem) redisClient.get(ITEM_INFO+":"+itemId+":"+BASE);
        if(tbItem!=null){
            return tbItem;
        }
        /*****************解决缓存击穿***************/
        if(redisClient.setnx(SETNX_BASC_LOCK_KEY+":"+itemId,itemId,30L)){
            //2、再查询mysql,并把查询结果缓存到redis,并设置失效时间
            tbItem = tbItemMapper.selectByPrimaryKey(itemId);

            /*****************解决缓存穿透*****************/
            if(tbItem!=null){
                redisClient.set(ITEM_INFO+":"+itemId+":"+BASE,tbItem);
                redisClient.expire(ITEM_INFO+":"+itemId+":"+BASE,ITEM_INFO_EXPIRE);
            }else{
                redisClient.set(ITEM_INFO+":"+itemId+":"+BASE,null);
                redisClient.expire(ITEM_INFO+":"+itemId+":"+BASE,30L);
            }
            redisClient.del(SETNX_BASC_LOCK_KEY+":"+itemId);
            return tbItem;
        }else{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
           return selectItemInfo(itemId);
        }
    }

    @Override
    public PageResult selectTbItemAllByPage(Integer page, Long rows) {
        PageHelper.startPage(page, rows.intValue());

        TbItemExample tbItemExample = new TbItemExample();
        tbItemExample.setOrderByClause("updated DESC");
        TbItemExample.Criteria criteria = tbItemExample.createCriteria();
        criteria.andStatusEqualTo((byte) 1);

        List<TbItem> tbItemList = tbItemMapper.selectByExample(tbItemExample);
        for (int i = 0; i < tbItemList.size(); i++) {
            TbItem tbItem = tbItemList.get(i);
            tbItem.setPrice(tbItem.getPrice() / 100);

        }
        PageInfo<TbItem> pageInfo = new PageInfo<TbItem>(tbItemList);

        PageResult pageResult = new PageResult();
        pageResult.setPageIndex(pageInfo.getPageNum());
        pageResult.setTotalPage(Long.valueOf(pageInfo.getPages()));
        pageResult.setResult(pageInfo.getList());
        return pageResult;
    }

    @Override
    public Integer insertTbItem(TbItem tbItem, String desc, String itemParams) {
        //1、保存商品信息
        long itemId = IDUtils.genItemId();
        Date date = new Date();
        tbItem.setId(itemId);
        tbItem.setStatus((byte) 1);
        tbItem.setCreated(date);
        tbItem.setUpdated(date);
        tbItem.setPrice(tbItem.getPrice() * 100);
        int tbItemNum = tbItemMapper.insertSelective(tbItem);

        //2、保存商品描述信息
        TbItemDesc tbItemDesc = new TbItemDesc();
        tbItemDesc.setItemId(itemId);
        tbItemDesc.setItemDesc(desc);
        tbItemDesc.setCreated(date);
        tbItemDesc.setUpdated(date);
        int tbItemDescNum = tbItemDescMapper.insertSelective(tbItemDesc);

        //3、保存商品规格信息
        TbItemParamItem tbItemParamItem = new TbItemParamItem();
        tbItemParamItem.setItemId(itemId);
        tbItemParamItem.setParamData(itemParams);
        tbItemParamItem.setCreated(date);
        tbItemParamItem.setUpdated(date);
        int tbItemParamItemNum = tbItemParamItemMapper.insertSelective(tbItemParamItem);

        //发送mq，完成索引库同步
        amqpTemplate.convertAndSend("item_exchange","item.add",itemId);
        return tbItemNum + tbItemDescNum + tbItemParamItemNum;
    }

    @Override
    public TbItemDesc selectItemDescByItemId(Long itemId) {
        //1、先查询redis,如果有直接返回
        TbItemDesc tbItemDesc = (TbItemDesc) redisClient.get(ITEM_INFO + ":" + itemId + ":" + DESC);
        if(tbItemDesc!=null){
            return tbItemDesc;
        }
        if(redisClient.setnx(SETNX_DESC_LOCK_KEY+":"+itemId,itemId,30L)){
            //2、再查询mysql,并把查询结果缓存到redis,并设置失效时间
            tbItemDesc = tbItemDescMapper.selectByPrimaryKey(itemId);

            if(tbItemDesc!=null){
                redisClient.set(ITEM_INFO + ":" + itemId + ":" + DESC,tbItemDesc);
                redisClient.expire(ITEM_INFO + ":" + itemId + ":" + DESC,ITEM_INFO_EXPIRE);

            }else{
                redisClient.set(ITEM_INFO + ":" + itemId + ":" + DESC,null);
                redisClient.expire(ITEM_INFO + ":" + itemId + ":" + DESC,30L);
            }
            redisClient.del(SETNX_DESC_LOCK_KEY+":"+itemId);
            return tbItemDesc;
        }else{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return selectItemDescByItemId(itemId);
        }
    }

    @Override
    public Integer updateTbItemByOrderId(String orderId) {
        //1、根据orderId查询List<TbOrderItem> tbOrderItemList
        TbOrderItemExample tbOrderItemExample = new TbOrderItemExample();
        TbOrderItemExample.Criteria criteria = tbOrderItemExample.createCriteria();
        criteria.andOrderIdEqualTo(orderId);
        List<TbOrderItem> tbOrderItemList = tbOrderItemMapper.selectByExample(tbOrderItemExample);

        //2、遍历tbOrderItemList---->TbOrderItem(itemId,num)，并且itemId修改库存
        int result = 0;
        for (int i = 0; i < tbOrderItemList.size(); i++) {
            TbOrderItem tbOrderItem =  tbOrderItemList.get(i);
            TbItem tbItem = tbItemMapper.selectByPrimaryKey(Long.valueOf(tbOrderItem.getItemId()));
            tbItem.setNum(tbItem.getNum()-tbOrderItem.getNum());
            result += tbItemMapper.updateByPrimaryKeySelective(tbItem);
        }
        return result;
    }

    //修改商品-->删除redis中的商品信息
    //删除商品-->删除redis中的商品信息
}
