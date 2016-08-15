package com.baidu.raft.protocol;

/**
 * Created by pippo on 16/7/25.
 */
public enum MessageType {

    Heartbeat((byte) 0),

    VoteRQ((byte) 1),

    VoteRS((byte) 2),

    AppendRQ((byte) 3),

    AppendRS((byte) 4),

    JoinClusterRQ((byte) 5),

    JoinClusterRS((byte) 6),

    LeaveClusterRQ((byte) 7),

    LeaveClusterRS((byte) 8),

    SyncLogRQ((byte) 9),

    SyncLogRS((byte) 10),

    InstallSnapshotRQ((byte) 11),

    InstallSnapshotRS((byte) 12),;

    MessageType(byte code) {
        this.code = code;
    }

    public final byte code;

    public byte getCode() {
        return code;
    }

    public static MessageType from(byte code) {

        for (MessageType type : MessageType.values()) {
            if (type.code == code) {
                return type;
            }
        }

        throw new IllegalArgumentException(String.format("unknown type code:[%s]", code));
    }

}
