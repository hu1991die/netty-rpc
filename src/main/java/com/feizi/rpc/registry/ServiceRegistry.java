package com.feizi.rpc.registry;

import com.feizi.rpc.constant.Constant;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by feizi on 2018/5/7.
 */
public class ServiceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistry.class);

    /*计数器*/
    private CountDownLatch latch = new CountDownLatch(1);

    /*注册地址*/
    private String registryAddress;

    public ServiceRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public void register(String data){
        if(null != data){
            /*连接zk服务*/
            ZooKeeper zk = connectServer();
            if(null != zk){
                /*创建zk节点*/
                createNode(zk, data);
            }
        }
    }

    /**
     * 连接服务
     * @return
     */
    private ZooKeeper connectServer(){
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    // 判断是否已连接ZK,连接后计数器递减.
                    if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                        latch.countDown();
                    }
                }
            });
            // 若计数器不为0,则等待.
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("连接zk服务报错: {}", e.getMessage());
        }
        return zk;
    }

    /**
     * 创建zk临时节点
     * @param zk
     * @param data
     */
    private void createNode(ZooKeeper zk, String data){
        try {
            byte[] bytes = data.getBytes();
            // 创建 registry 节点（临时）
            String path = zk.create(Constant.ZK_DATA_PATH, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            LOGGER.debug("create zookeeper node: ({} => {})", path, data);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("创建zk节点出错：{}", e.getMessage());
        }
    }
}
