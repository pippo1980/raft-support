package com.baidu.raft;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

/**
 * Created by pippo on 16/8/4.
 */
public class Config {

    public static Config load() {
        InputStream is = Config.class.getResourceAsStream("/raft.yml");
        if (is == null) {
            throw new IllegalArgumentException("can not find raft.yml settings");
        }

        return new Yaml().loadAs(is, Config.class);
    }

    public String workDir;
    public Seed[] seeds;

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public Seed[] getSeeds() {
        return seeds;
    }

    public void setSeeds(Seed[] seeds) {
        this.seeds = seeds;
    }

    public static class Seed {

        public int id;
        public String host;
        public int port;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}
