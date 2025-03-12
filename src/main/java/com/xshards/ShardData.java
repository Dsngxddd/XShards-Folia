package com.xshards;

public class ShardData {
    private int shards;

    public ShardData() {
        this.shards = 0; // Default shards
    }

    public ShardData(int shards) {
        this.shards = shards;
    }

    public int getShards() {
        return shards;
    }

    public void addShards(int amount) {
        this.shards += amount;
    }
}