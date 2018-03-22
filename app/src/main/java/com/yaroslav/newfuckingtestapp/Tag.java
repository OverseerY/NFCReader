package com.yaroslav.newfuckingtestapp;

import java.io.Serializable;

public class Tag implements Serializable{
    private String tName;
    private String tTime;

    public Tag() {}

    public Tag(String name, String time) {
        this.tName = name;
        this.tTime = time;
    }

    public String gettName() {
        return tName;
    }

    public String gettTime() {
        return tTime;
    }

    public void settName(String value) {
        tName = value;
    }

    public void settTime(String value) {
        tTime = value;
    }
}
