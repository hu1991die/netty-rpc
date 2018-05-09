package com.feizi.rpc.server;

import com.feizi.rpc.handler.RpcHandler;
import com.feizi.rpc.bean.RpcRequest;
import com.feizi.rpc.bean.RpcResponse;
import com.feizi.rpc.registry.ServiceRegistry;
import com.feizi.rpc.codec.RpcDecoder;
import com.feizi.rpc.codec.RpcEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC服务器
 * Created by feizi on 2018/5/7.
 */
public class RpcServer implements ApplicationContextAware, InitializingBean{
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    /* 服务地址 */
    private String serverAddress;

    /* 服务注册中心 */
    private ServiceRegistry serviceRegistry;

    /*存放接口名与服务对象之间的映射关系*/
    private Map<String, Object> handlerMap = new HashMap<>();

    public RpcServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        /*获取所有带@RpcService注解的Spring Bean*/
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if(null != serviceBeanMap && serviceBeanMap.size() > 0){
            for (Object serviceBean : serviceBeanMap.values()){
                String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
                handlerMap.put(interfaceName, serviceBean);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        EventLoopGroup masterGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // 创建并初始化 Netty 服务端 Bootstrap 对象
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(masterGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    //将RPC请求进行解码（为了处理请求）
                                    .addLast(new RpcDecoder(RpcRequest.class))
                                    //将RPC请求进行编码（为了返回响应）
                                    .addLast(new RpcEncoder(RpcResponse.class))
                                    //处理RPC请求
                                    .addLast(new RpcHandler(handlerMap));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            //解析IP地址和端口信息
            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.parseInt(array[1]);

            //启动RPC服务端
            ChannelFuture channelFuture = bootstrap.bind(host, port).sync();
            LOGGER.debug("server started on port: {}", port);

            if(null != serviceRegistry){
                //注册服务地址
                serviceRegistry.register(serverAddress);
                LOGGER.debug("register service:{}", serverAddress);
            }

            //关闭RPC服务器
            channelFuture.channel().closeFuture().sync();
        }finally {
            workerGroup.shutdownGracefully();
            masterGroup.shutdownGracefully();
        }
    }
}
