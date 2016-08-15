package com.baidu.raft.election;

import com.baidu.raft.PeerRole;

/**
 * Created by pippo on 16/8/1.
 */
public interface StatChangeListener {

    void onChange(PeerRole from, PeerRole to);

}
