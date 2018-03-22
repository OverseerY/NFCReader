package com.yaroslav.newfuckingtestapp;

public class Ticket {
    private String mDescription;
    private String mLatitude;
    private String mLongitude;
    private String mTime;
    private String mImei;

    public Ticket() {}
    public Ticket(String description, String latitude, String longitude, String time, String imei) {
        this.mDescription = description;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mTime = time;
        this.mImei = imei;
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

    public void setmImei(String value) {
        mImei = value;
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

    public String getmImei() {
        return mImei;
    }
}
