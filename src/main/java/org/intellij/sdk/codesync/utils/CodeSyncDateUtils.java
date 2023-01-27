package org.intellij.sdk.codesync.utils;

import org.jetbrains.annotations.Nullable;

import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;

import static org.intellij.sdk.codesync.Constants.DATE_TIME_FORMAT;

public class CodeSyncDateUtils {
    @Nullable
    public static Date parseDate(String dateString, String format) {
        SimpleDateFormat pattern = new SimpleDateFormat(format);
        try {
            return new Date(pattern.parse(dateString).getTime());
        } catch (ParseException pe) {
            return null;
        }
    }

    @Nullable
    public static Date parseDate(String dateString) {
        return parseDate(dateString, DATE_TIME_FORMAT);
    }

    /*
        Using instant because it handles time zone correctly.
         */
    @Nullable
    public static Instant parseDateToInstant(String dateString, String format) {
        Date date = parseDate(dateString, format);
        if (date != null) {
            return date.toInstant();
        }
        return null;
    }

    @Nullable
    public static Instant parseDateToInstant(String dateString) {
        return parseDateToInstant(dateString, DATE_TIME_FORMAT);
    }

    public static String formatDate(Instant instant) {
        return formatDate(Date.from(instant), DATE_TIME_FORMAT);
    }

    public static String formatDate(Date date) {
        return formatDate(date, DATE_TIME_FORMAT);
    }

    public static String formatDate(Date date, String format) {
        SimpleDateFormat pattern = new SimpleDateFormat(format);
        pattern.setTimeZone(TimeZone.getTimeZone("UTC"));
        return pattern.format(date);
    }

    public static String formatDate(FileTime date) {
        SimpleDateFormat pattern = new SimpleDateFormat(DATE_TIME_FORMAT);
        pattern.setTimeZone(TimeZone.getTimeZone("UTC"));
        return pattern.format(date.toMillis());
    }

    /*
        Convert Date object to POSIX time.
    */
    public static Long getPosixTime(Date date) {
        return date.getTime() / 1000L;
    }

    /*
        Convert Date String to POSIX time.
    */
    public static Long getPosixTime(String dateString) {
        Date date = parseDate(dateString);
        if (date ==  null) {
            return null;
        }
        return date.getTime() / 1000L;
    }

    public static String getCurrentDatetime()  {
        return formatDate(new Date());
    }

    /*
        Return an Instant for 4:30 PM for tomorrow (tomorrow here means 1 day in the future of the time when this function is called.)
        Instant would be in the local time zone.
    */
    public static Instant getTomorrowAlertInstant() {
        ZoneId timeZone = ZoneId.systemDefault();
        LocalDateTime reminderDateTime = LocalDateTime.now(timeZone)
            .withHour(16)
            .withMinute(30)
            .withSecond(0)
            .plusDays(1);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(reminderDateTime, timeZone);
        return zonedDateTime.toInstant();
    }

    /*
        Return an Instant for 4:30 PM for tomorrow (tomorrow here means 1 day in the future of the time when this function is called.)
        Instant would be in the local time zone.
    */
    public static Instant getYesterdayInstant() {
        ZoneId timeZone = ZoneId.systemDefault();
        LocalDateTime yesterdayDateTime = LocalDateTime.now(timeZone)
            .withHour(16)
            .withMinute(30)
            .withSecond(0)
            .minusDays(1);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(yesterdayDateTime, timeZone);

        return zonedDateTime.toInstant();
    }

    public static Instant getTodayInstant() {
        ZoneId timeZone = ZoneId.systemDefault();
        LocalDateTime yesterdayDateTime = LocalDateTime.now(timeZone);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(yesterdayDateTime, timeZone);

        return zonedDateTime.toInstant();
    }

    /*
        Get today's instant for the given hour, minute and second.
    */
    public static Instant getTodayInstant(int hour, int minute, int second) {
        ZoneId timeZone = ZoneId.systemDefault();
        LocalDateTime yesterdayDateTime = LocalDateTime.now(timeZone);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(yesterdayDateTime, timeZone);
        zonedDateTime = zonedDateTime
            .withHour(hour)
            .withMinute(minute)
            .withSecond(second);

        return zonedDateTime.toInstant();
    }
}
