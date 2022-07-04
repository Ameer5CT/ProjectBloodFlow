package com.example.bloodflow.model;

public class SingleDb {
    public String dbBpm;
    public String dbSpo2;
    public String dbTime;
    public String dbDate;

    public SingleDb(String dbBpm, String dbSpo2, String dbTime, String dbDate) {
        this.dbBpm = dbBpm;
        this.dbSpo2 = dbSpo2;
        this.dbTime = dbTime;
        this.dbDate = dbDate;
    }
}
