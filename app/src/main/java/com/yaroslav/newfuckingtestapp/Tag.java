package com.yaroslav.newfuckingtestapp;

public class Tag {
    private String mTitle, mDate;

    public Tag() {}

    public Tag(String mTitle, String mDate) {
        this.mTitle = mTitle;
        this.mDate = mDate;
    }

    public String getmDate() {
        return mDate;
    }

    public String getmTitle() {
        return mTitle;
    }

    public void setmDate(String mDate) {
        this.mDate = mDate;
    }

    public void setmTitle(String mTitle) {
        this.mTitle = mTitle;
    }
}
