package org.apache.tuweni.ethstats;

import com.fasterxml.jackson.annotation.JsonGetter;

public final class NodeStats {
    private final boolean active;
    private final boolean syncing;
    private final boolean mining;
    private final int hashrate;
    private final int peerCount;
    private final int gasPrice;
    private final int uptime;


    public NodeStats(boolean active, boolean syncing, boolean mining, int hashrate, int peerCount, int gasPrice, int uptime) {
        this.active = active;
        this.syncing = syncing;
        this.mining = mining;
        this.hashrate = hashrate;
        this.peerCount = peerCount;
        this.gasPrice = gasPrice;
        this.uptime = uptime;
    }

    @JsonGetter("active")
    public boolean isActive() {
        return active;
    }

    @JsonGetter("syncing")
    public boolean isSyncing() {
        return syncing;
    }

    @JsonGetter("mining")
    public boolean isMining() {
        return mining;
    }

    @JsonGetter("hashrate")
    public int getHashrate() {
        return hashrate;
    }

    @JsonGetter("peers")
    public int getPeerCount() {
        return peerCount;
    }

    @JsonGetter("gasPrice")
    public int getGasPrice() {
        return gasPrice;
    }

    @JsonGetter("uptime")
    public int getUptime() {
        return uptime;
    }
}
