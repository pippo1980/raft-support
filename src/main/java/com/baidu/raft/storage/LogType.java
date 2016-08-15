package com.baidu.raft.storage;

/**
 * Created by pippo on 16/7/25.
 */
public enum LogType {

    Application,
    Configuration,
    ClusterServer,
    LogPack,
    SnapshotSyncRequest

}
