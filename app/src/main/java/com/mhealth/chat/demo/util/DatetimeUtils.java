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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = sdf.parse(message.getTimeStamp());
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getDefault());
        return cal.getTime();
    }
}
