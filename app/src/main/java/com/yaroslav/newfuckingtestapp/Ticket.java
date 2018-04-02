package com.yaroslav.newfuckingtestapp;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Ticket implements Serializable{
    private String mDescription;
    private String mLatitude;
    private String mLongitude;
    private String mTime;
    private String mUid;

    public Ticket() {}

    public Ticket(String description, String latitude, String longitude, String time, String uuid) {
        this.mDescription = description;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mTime = time;
        this.mUid = uuid;
    }

    @Override
    public String toString() {
        return mDescription + "; " + converteTime(Long.parseLong(mTime));
    }


    public void setmDescription(String value) {
        mDescription = value;
    }

    public void setmLatitude(String value) {
        mLatitude = value;
    }

    public void setmLongitude(String value) {
        mLongitude = value;
    }

    public void setmTime(String value) {
        mTime = value;
    }

    public void setmUid(String value) {
        mUid = value;
    }

    public String getmDescription() {
        return mDescription;
    }

    public String getmLatitude() {
        return mLatitude;
    }

    public String getmLongitude() {
        return mLongitude;
    }

    public String getmTime() {
        return mTime;
    }

    public String getmUid() {
        return mUid;
    }

    private String converteTime(long value) {
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date(value);
        String formatted = format.format(date);
        return formatted;
    }
}
