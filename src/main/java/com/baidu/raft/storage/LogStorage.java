package com.baidu.raft.storage;

/**
 * Created by pippo on 16/7/25.
 */
public interface LogStorage {

    long getCommitIndex();

    void setCommitIndex(long index);

    long getLastIndex();

    LogEntry getLast();

    LogEntry get(long index);

    void append(LogEntry... entry);

    void append(long index, LogEntry... entry);

    void append(long index, boolean overwrite, LogEntry... entry);
}
