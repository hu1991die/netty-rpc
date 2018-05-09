package com.feizi.rpc.service.impl;


import com.feizi.rpc.server.RpcService;
import com.feizi.rpc.service.HelloService;

/**
 * 实现服务接口
 * Created by feizi on 2018/5/7.
 */
@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }
}
