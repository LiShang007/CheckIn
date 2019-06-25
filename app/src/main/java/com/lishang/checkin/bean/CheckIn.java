package com.lishang.checkin.bean;

public class CheckIn {
    public String date;
    public String score;
    public boolean isCheckIn;
    public boolean isLeakChekIn; //漏签


    @Override
    public String toString() {
        return "CheckIn{" +
                "date='" + date + '\'' +
                ", score='" + score + '\'' +
                ", isCheckIn=" + isCheckIn +
                ", isLeakChekIn=" + isLeakChekIn +
                '}';
    }
}
