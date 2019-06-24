package com.lishang.checkin;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.lishang.checkin.adapter.CheckInAdapter;
import com.lishang.checkin.bean.CheckIn;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CheckInProgress checkIn;
    private List<CheckIn> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkIn = findViewById(R.id.checkIn);

        initDate();

        checkIn.setAdapter(new CheckInAdapter() {

            @Override
            public String getDateText(int position) {
                CheckIn in = list.get(position);
                return in.date;
            }

            @Override
            public String getScoreText(int position) {
                CheckIn in = list.get(position);
                return in.score;
            }

            @Override
            public boolean isCheckIn(int position) {
                CheckIn in = list.get(position);
                return position < 3;
            }

            @Override
            public int size() {
                return list.size();
            }
        });
    }


    private void initDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd");

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        // 判断要计算的日期是否是周日，如果是则减一天计算周六的，否则会出问题，计算到下一周去了
        int dayWeek = cal.get(Calendar.DAY_OF_WEEK);// 获得当前日期是一个星期的第几天
        if (1 == dayWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        // 设置一个星期的第一天，按中国的习惯一个星期的第一天是星期一
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        // 获得当前日期是一个星期的第几天
        int day = cal.get(Calendar.DAY_OF_WEEK);
        // 根据日历的规则，给当前日期减去星期几与一个星期第一天的差值
        cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - day);
        Calendar start = Calendar.getInstance();
        start.setTime(cal.getTime());
        int score = 1;
        if (isToday(start)) {

            CheckIn in = new CheckIn();
            in.date = "今天";
            in.score = "" + score;
            list.add(in);
        } else {
            CheckIn in = new CheckIn();
            in.date = sdf.format(start.getTime());
            in.score = "" + score;
            list.add(in);
        }

        cal.add(Calendar.DATE, 6);
        Calendar end = Calendar.getInstance();
        end.setTime(cal.getTime());
        while (end.after(start)) {
            // 根据日历的规则，为给定的日历字段添加或减去指定的时间量
            start.add(Calendar.DAY_OF_MONTH, 1);
            score++;

            if (isToday(start)) {
                CheckIn in = new CheckIn();
                in.date = "今天";
                in.score = "" + score;
                list.add(in);
            } else {
                CheckIn in = new CheckIn();
                in.date = sdf.format(start.getTime());
                in.score = "" + score;
                list.add(in);
            }
        }
    }

    private boolean isToday(Calendar other) {
        Calendar calendar = Calendar.getInstance();

        if (other.get(Calendar.YEAR) == (calendar.get(Calendar.YEAR))) {
            int diffDay = other.get(Calendar.DAY_OF_YEAR)
                    - calendar.get(Calendar.DAY_OF_YEAR);
            if (diffDay == 0) {
                return true;
            }
        }
        return false;
    }


}
