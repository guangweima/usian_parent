package com.usian.mq;

import com.usian.mapper.LocalMessageMapper;
import com.usian.pojo.LocalMessage;
import com.usian.utils.JsonUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ReturnCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MQSender implements ConfirmCallback, ReturnCallback {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private LocalMessageMapper localMessageMapper;
    /**
     * 发送消息，完成扣减库存业务
     * @param localMessage
     */
    public void sendMsg(LocalMessage localMessage) {
        RabbitTemplate rabbitTemplate = (RabbitTemplate) this.amqpTemplate;
        rabbitTemplate.setReturnCallback(this);
        rabbitTemplate.setConfirmCallback(this);
        //Correlation:[ˌkɔːrəˈleɪʃn] 关联数据，关联上游下游服务
        CorrelationData correlationData = new CorrelationData(localMessage.getTxNo());
        rabbitTemplate.convertAndSend("order_exchange","order.add", JsonUtils.objectToJson(localMessage),correlationData);
    }

    /**
     * 消息投递成功时调用
     * @param correlationData
     * @param ack
     * @param cause
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        //判断消息确认是否为true
        System.out.println("----------------------------------------ack: "+ack);
        if(ack){
            //修改本地消息表的状态：1
            String txNo = correlationData.getId();
            LocalMessage localMessage = new LocalMessage();
            localMessage.setTxNo(txNo);
            localMessage.setState(1);
            localMessageMapper.updateByPrimaryKeySelective(localMessage);
        }
    }

    /**
     * 消息发送失败时调用
     * @param message
     * @param replyCode
     * @param replyText
     * @param exchange
     * @param routingKey
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        System.out.println("return--message:" + new String(message.getBody())
                + ",exchange:" + exchange + ",routingKey:" + routingKey);
    }
}
