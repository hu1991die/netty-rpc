package com.feizi.rpc.bean;

import java.io.Serializable;

/**
 * RPC响应封装Bean
 * Created by feizi on 2018/5/7.
 */
public class RpcResponse implements Serializable {
    private static final long serialVersionUID = 3374714188555167852L;
    /*响应ID*/
    private String reponseId;
    /*异常对象*/
    private Throwable error;
    /*响应结果*/
    private Object result;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getReponseId() {
        return reponseId;
    }

    public void setReponseId(String reponseId) {
        this.reponseId = reponseId;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public boolean hasError(){
        return error != null;
    }
}
