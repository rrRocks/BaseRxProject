package com.rock.basemodel.http.basebean;

/**
 * time: 17/11/9 10:19
 * 解析错误实体基类 -- 具体情况看
 */
public class ErrResponse {
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String msg;
}
