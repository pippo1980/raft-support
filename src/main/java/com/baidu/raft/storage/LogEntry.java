package com.baidu.raft.storage;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Created by pippo on 16/7/25.
 */
public class LogEntry {

    public LogEntry() {

    }

    public LogEntry(LogType type, long term, byte[] payload) {
        this.type = type;
        this.term = term;
        this.payload = payload;
    }

    public LogType type;
    public long term;
    public byte[] payload;

    public LogType getType() {
        return type;
    }

    public void setType(LogType type) {
        this.type = type;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("type", type)
                .append("term", term)
                .append("payload", payload)
                .toString();
    }
}
