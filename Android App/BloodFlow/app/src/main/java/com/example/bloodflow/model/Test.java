package com.example.bloodflow.model;

import java.util.Date;

public class Test {

    private int id;
    private int bpm;
    private int spo2;
    private Date date;

    public Test() {
    }

    public Test(int bpm, int spo2, Date date) {
        this.bpm = bpm;
        this.spo2 = spo2;
        this.date = date;
    }

    public Test(int id, int bpm, int spo2, Date date) {
        this.id = id;
        this.bpm = bpm;
        this.spo2 = spo2;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    public int getSpo2() {
        return spo2;
    }

    public void setSpo2(int spo2) {
        this.spo2 = spo2;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
