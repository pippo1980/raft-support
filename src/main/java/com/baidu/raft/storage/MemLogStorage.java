package com.baidu.raft.storage;

import java.util.Vector;

/**
 * Created by pippo on 16/7/27.
 */
public class MemLogStorage implements LogStorage {

    private volatile long commitIndex;
    private Vector<LogEntry> entries = new Vector<LogEntry>();

    @Override
    public long getCommitIndex() {
        return commitIndex;
    }

    @Override
    public void setCommitIndex(long index) {
        this.commitIndex = index;
    }

    @Override
    public long getLastIndex() {
        return entries.size() - 1;
    }

    @Override
    public LogEntry getLast() {
        return entries.lastElement();
    }

    @Override
    public LogEntry get(long index) {
        return entries.get((int) index);
    }

    @Override
    public void append(LogEntry... entry) {
        append(getLastIndex() + 1, entry);
    }

    @Override
    public void append(long index, LogEntry... entry) {
        append(index, true, entry);
    }

    @Override
    public synchronized void append(long index, boolean overwrite, LogEntry... entry) {
        for (LogEntry logEntry : entry) {
            entries.set((int) index, logEntry);
            index++;
        }
    }
}
