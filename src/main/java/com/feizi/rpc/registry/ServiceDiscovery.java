package com.feizi.rpc.registry;

import com.feizi.rpc.constant.Constant;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 服务发现
 * Created by feizi on 2018/5/7.
 */
public class ServiceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDiscovery.class);

    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<String> dataList = new ArrayList<>();

    /*注册地址*/
    private String registryAddress;

    public ServiceDiscovery(String registryAddress) {
        this.registryAddress = registryAddress;
        /*连接zk服务*/
        ZooKeeper zk = connectServer();
        if(null != zk){
            /*监视zk节点*/
            watchNode(zk);
        }
    }

    /**
     * 服务发现
     * @return
     */
    public String discovery(){
        String data = null;
        int size = dataList.size();
        if(size > 0){
            if(size == 1){
                // 若只有一个地址，则获取该地址
                data = dataList.get(0);
                LOGGER.debug("using only data: {}", data);
            }else {
                // 若存在多个地址，则随机获取一个地址
                data = dataList.get(ThreadLocalRandom.current().nextInt(size));
                LOGGER.debug("using random data: {}", data);
            }
        }
        return data;
    }

    /**
     * 连接zk服务
     * @return
     */
    private ZooKeeper connectServer(){
        ZooKeeper zk = null;
        try {
            /*创建zk客户端*/
            zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                        latch.countDown();
                    }
                }
            });
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("连接zk服务报错...", e);
        }
        return zk;
    }

    /**
     * 监视zk节点
     * @param zk
     */
    private void watchNode(final ZooKeeper zk){
        try {
            List<String> nodeList = zk.getChildren(Constant.ZK_REGISTRY_ROOT_PATH, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if(watchedEvent.getType() == Event.EventType.NodeChildrenChanged){
                        watchNode(zk);
                    }
                }
            });

            List<String> dataList = new ArrayList<>();
            byte[] bytes;
            for (String node : nodeList){
                bytes = zk.getData(Constant.ZK_REGISTRY_ROOT_PATH + "/" + node, false, null);
                dataList.add(new String(bytes));
            }
            LOGGER.debug("node data: {}", dataList);
            this.dataList = dataList;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("监视zk节点异常...", e);
        }
    }
}
