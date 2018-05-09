package com.feizi.rpc.client.proxy;

import com.feizi.rpc.bean.RpcRequest;
import com.feizi.rpc.bean.RpcResponse;
import com.feizi.rpc.client.RpcClient;
import com.feizi.rpc.registry.ServiceDiscovery;
import net.sf.cglib.proxy.InvocationHandler;
import net.sf.cglib.proxy.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * RPC代理
 * Created by feizi on 2018/5/7.
 */
public class RpcProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcProxy.class);

    private String serverAddress;

    private ServiceDiscovery serviceDiscovery;
    public RpcProxy(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    public <T> T create(Class<?> interfaceClass){
        //创建动态代理对象
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        //创建并且初始化RPC请求，并设置请求参数
                        RpcRequest request = new RpcRequest();
                        request.setRequestId(UUID.randomUUID().toString());
                        request.setClassName(method.getDeclaringClass().getName());
                        request.setMethodName(method.getName());
                        request.setParameterTypes(method.getParameterTypes());
                        request.setParameters(args);

                        if(null != serviceDiscovery){
                            //发现服务
                            serverAddress = serviceDiscovery.discovery();
                        }

                        if(serverAddress == null){
                            throw new RuntimeException("serverAddress is null...");
                        }

                        //解析主机名和端口
                        String[] array = serverAddress.split(":");
                        String host = array[0];
                        int port = Integer.parseInt(array[1]);

                        //初始化RPC客户端
                        RpcClient client = new RpcClient(host, port);

                        long startTime = System.currentTimeMillis();
                        //通过RPC客户端发送rpc请求并且获取rpc响应
                        RpcResponse response = client.send(request);
                        LOGGER.debug("send rpc request elapsed time: {}ms...", System.currentTimeMillis() - startTime);

                        if (response == null) {
                            throw new RuntimeException("response is null...");
                        }

                        //返回RPC响应结果
                        if(response.hasError()){
                            throw response.getError();
                        }else {
                            return response.getResult();
                        }
                    }
                }
        );
    }
}
