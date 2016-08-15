package com.baidu.raft.storage;

import com.baidu.raft.utils.JSONUtil;
import com.google.common.primitives.Longs;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.fusesource.leveldbjni.JniDBFactory.factory;

/**
 * Created by pippo on 16/8/5.
 */
public class LevelDBLogStorage implements LogStorage {

    public LevelDBLogStorage(String workDir, int nodeId) throws IOException {
        db = factory.open(new File(String.format("%s/%s/raft.db", workDir, nodeId)), new Options());
    }

    ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
    DB db;

    private AtomicLong commitIndex = new AtomicLong(0);
    private AtomicLong lastIndex = new AtomicLong(0);

    @Override
    public long getCommitIndex() {
        return commitIndex.get();
    }

    @Override
    public void setCommitIndex(long index) {
        db.put("commit_index".getBytes(), Longs.toByteArray(index));
        commitIndex.set(index);
    }

    @Override
    public long getLastIndex() {
        return lastIndex.get();
    }

    @Override
    public LogEntry getLast() {
        return get(lastIndex.get());
    }

    @Override
    public LogEntry get(long index) {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();
        try {
            return JSONUtil.toObject(db.get(Longs.toByteArray(index)), LogEntry.class);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void append(LogEntry... entry) {
        append(lastIndex.incrementAndGet(), entry);
    }

    @Override
    public void append(long index, LogEntry... entry) {
        append(index, true, entry);
    }

    @Override
    public void append(long index, boolean overwrite, LogEntry... entry) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();
        try {
            WriteBatch batch = db.createWriteBatch();
            for (int i = 0; i < entry.length; i++) {
                LogEntry logEntry = entry[i];

                if (!overwrite && get(index) != null) {
                    continue;
                }

                batch.put(Longs.toByteArray(index), JSONUtil.toBytes(logEntry));
                batch.put("last_index".getBytes(), Longs.toByteArray(index));
                db.write(batch);

                if (i < (entry.length - 1)) {
                    index = lastIndex.incrementAndGet();
                }
            }
        } catch (DBException e) {
            // do nothing
        } finally {
            writeLock.unlock();
        }
    }
}
