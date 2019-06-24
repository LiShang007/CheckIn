package com.lishang.checkin.adapter;

public abstract class CheckInAdapter {

    public abstract String getDateText(int position);

    public abstract String getScoreText(int position);

    public abstract boolean isCheckIn(int position);

    public abstract int size();

}
