package com.baidu.raft.endpoint;

import com.baidu.raft.protocol.BaseMessage;

/**
 * Created by pippo on 16/7/25.
 */
public interface Transport {

    void brodcast(BaseMessage message);

    void send(BaseMessage message);

}
