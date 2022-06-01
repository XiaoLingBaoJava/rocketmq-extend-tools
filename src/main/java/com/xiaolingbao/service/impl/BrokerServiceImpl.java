package com.xiaolingbao.service.impl;

import com.xiaolingbao.logging.ClientLogger;
import com.xiaolingbao.logging.Log;
import com.xiaolingbao.service.BrokerService;
import com.xiaolingbao.service.CommonService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.KVTable;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: xiaolingbao
 * @date: 2022/5/31 9:41
 * @description: 
 */
public class BrokerServiceImpl extends CommonService implements BrokerService {

    private static final Log log = ClientLogger.getLog();

    @Override
    public Map<String, Long> getProduceAndConsumeTpsByBrokerAddr(String brokerAddr) {
        try {
            mqAdminExt.init(0);
        } catch (MQClientException e) {
            log.error("初始化MQAdmin实例失败", e);
            return null;
        }
        KVTable kvTable;
        try {
            kvTable = mqAdminExt.fetchBrokerRuntimeStats(brokerAddr);
        } catch (Exception e) {
            log.error("通过brokerAddr获取broker的相关运行数据失败, brokerAddr: {}", brokerAddr, e);
            return null;
        }
        Map<String, Long> returnMap = new HashMap<>();
        returnMap.put("produceTps", (long)(Float.parseFloat(kvTable.getTable().get("putTps").split(" ")[0])));
        returnMap.put("consumeTps", (long)(Float.parseFloat(kvTable.getTable().get("getTransferedTps").split(" ")[0])));
        mqAdminExt.shutdown();
        return returnMap;
    }

    @Override
    public String getBrokerAddrByBrokerName(String brokerName) {
        try {
            mqAdminExt.init(0);
        } catch (MQClientException e) {
            log.error("初始化MQAdmin实例失败", e);
            return null;
        }
        ClusterInfo clusterInfo = null;
        try {
            clusterInfo = mqAdminExt.examineBrokerClusterInfo();
        } catch (Exception e) {
            log.error("通过brokerName获取brokerAddr失败, brokerName: {}", brokerName, e);
            return null;
        }
        mqAdminExt.shutdown();
        return clusterInfo.getBrokerAddrTable().get(brokerName).getBrokerAddrs().get(0L);
    }
}
