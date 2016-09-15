package com.mhealth.chat.demo.util;

import com.twilio.ipmessaging.Message;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by luhonghai on 9/5/16.
 */

public class DatetimeUtils {

    public static Date getMessageTimestamp(Message message) throws ParseException {
        return getMessageTimestamp(message.getTimeStamp());
    }

    public static Date getMessageTimestamp(String timeStamp) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = sdf.parse(timeStamp);
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getDefault());
        return cal.getTime();
    }

    public static String getTimestamp(String timeStamp) {
        try {
            return getTimestamp(getMessageTimestamp(timeStamp));
        } catch (ParseException e) {
            e.printStackTrace();
            return timeStamp;
        }
    }

    public static String getTimestamp(Date date) {
        try {
            Calendar mCal = Calendar.getInstance();
            mCal.setTime(date);
            Calendar today = Calendar.getInstance();
            today.setTimeInMillis(System.currentTimeMillis());
            SimpleDateFormat sdfOut;
            if (mCal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
                    && mCal.get(Calendar.DATE) == today.get(Calendar.DATE)
                    && mCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                sdfOut  = new SimpleDateFormat("HH:mm", Locale.US);
            } else if (mCal.get(Calendar.YEAR) != today.get(Calendar.YEAR)) {
                sdfOut =  new SimpleDateFormat("HH:mm, dd MMM yyyy", Locale.US);
            } else {
                sdfOut = new SimpleDateFormat("HH:mm, dd MMM", Locale.US);
            }
            return sdfOut.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
