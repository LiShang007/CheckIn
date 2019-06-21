package com.lishang.checkin;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
        getTimeInterval(new Date());
    }

    public void getTimeInterval(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd");

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
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
//        System.out.println(sdf.format(start.getTime()));

        cal.add(Calendar.DATE, 6);
        Calendar end = Calendar.getInstance();
        end.setTime(cal.getTime());
        while (end.after(start)) {
            // 根据日历的规则，为给定的日历字段添加或减去指定的时间量
            start.add(Calendar.DAY_OF_MONTH, 1);
            if (isToday(start)) {
                System.out.println(sdf.format(start.getTime()) + " 今天");
            } else {
                System.out.println(sdf.format(start.getTime()));
            }


        }

    }

    public boolean isToday(Calendar other) {
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