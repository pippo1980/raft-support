package com.baidu.raft.election;

/**
 * Created by pippo on 16/8/2.
 */
public interface ElectionStatProcessor<T> {

    T process(ElectionStat stat);

}