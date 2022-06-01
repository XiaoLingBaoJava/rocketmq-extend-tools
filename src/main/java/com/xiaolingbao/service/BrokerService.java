package com.xiaolingbao.service;

import java.util.Map;

/**
 * @author: xiaolingbao
 * @date: 2022/5/31 9:34
 * @description: 
 */
public interface BrokerService {

    Map<String, Long> getProduceAndConsumeTpsByBrokerAddr(String brokerAddr);

    String getBrokerAddrByBrokerName(String brokerName);

}
