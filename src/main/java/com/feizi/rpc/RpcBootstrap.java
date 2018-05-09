package com.feizi.rpc;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Hello world!
 *
 */
public class RpcBootstrap
{
    public static void main( String[] args )
    {
        new ClassPathXmlApplicationContext("provider/application-server.xml");
    }
}
