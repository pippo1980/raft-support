package com.baidu.raft.protocol;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Created by pippo on 16/7/25.
 */
public class BaseMessage implements Cloneable {

    public BaseMessage() {
    }

    public BaseMessage(MessageType type, int source, int destination, long term) {
        this.type = type;
        this.source = source;
        this.destination = destination;
        this.term = term;
    }

    public MessageType type;
    public int source;
    public int destination;
    public long term;

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getDestination() {
        return destination;
    }

    public void setDestination(int destination) {
        this.destination = destination;
    }

    public long getTerm() {
        return term;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("type", type)
                .append("source", source)
                .append("destination", destination)
                .append("term", term)
                .toString();
    }
}
