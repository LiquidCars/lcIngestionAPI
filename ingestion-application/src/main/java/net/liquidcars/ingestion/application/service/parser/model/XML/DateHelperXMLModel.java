package net.liquidcars.ingestion.application.service.parser.model.XML;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

@Component
public class DateHelperXMLModel {
    /****************************************************/
    /* SIMULATED CURRENT TIME CONFIGURATION - BEGINNING */
    /****************************************************/
    private static boolean isDevEnvironment;
    private static LocalDateTime currentSimDateTime;
    private static LocalDateTime originSimDateTime;
    private static boolean isSimRunningTime;

    /**********************************************/
    /* SIMULATED CURRENT TIME CONFIGURATION - END */
    /**********************************************/

    public static final String DEFAULT_DATE_FORMAT_SHORT_STR= "yyyy-MM-dd'T'HH:mm:ss";
    public static final DateFormat DEFAULT_DATE_FORMAT_SHORT = new SimpleDateFormat(DEFAULT_DATE_FORMAT_SHORT_STR);
    public static final DateTimeFormatter DEFAULT_DATE_FORMATTER_SHORT = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT_SHORT_STR);

    public static final String DEFAULT_DATE_FORMAT_STR = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";// DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT;// "yyyy-MM-dd HH:mm:ss.SSSZ";
    /*Note about date-time formats offsets:
        Z represents the timezone offset as 'Z' for UTC or as '+HH:mm' or '-HH:mm' for other timezones.
        ZZ represents the timezone offset as '+HHmm' or '-HHmm'.
        ZZZZZ represents the timezone offset as '+HH:mm' or '-HH:mm'.
    */
    public static final DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat(DEFAULT_DATE_FORMAT_STR);
    public static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT_STR);
    public static final String DEFAULT_DATE_ONLY_FORMAT_STR = "yyyy-MM-dd";
    public static final DateFormat DEFAULT_DATE_ONLY_FORMAT = new SimpleDateFormat(DEFAULT_DATE_ONLY_FORMAT_STR);
    public static final DateTimeFormatter DEFAULT_DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_ONLY_FORMAT_STR);
    public static final String DEFAULT_MIN_DATE = "2000-01-01T00:00:00.000+0000";
    public static final String DEFAULT_MAX_DATE = "9999-12-31T23:59:59.999+0000";
    public static final LocalDateTime MIN_DATE = LocalDateTime.MIN;
    public static final LocalDateTime MAX_DATE = LocalDateTime.MAX;

    private static long milisecondsFromSimOrigin(){
        if (DateHelperXMLModel.currentSimDateTime == null) {
            return 0;
        }
        else {
            return Instant.now().toEpochMilli() - DateHelperXMLModel.originSimDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
    }

    public static long currentTimeMillis(){
        if (DateHelperXMLModel.currentSimDateTime == null)  {
            return System.currentTimeMillis();
        }
        else {
            //return System.currentTimeMillis();
            // Step 1: Convert LocalDateTime to ZonedDateTime using the system default time zone
            ZonedDateTime zonedDateTime = now().atZone(ZoneId.systemDefault());
            // Step 2: Convert ZonedDateTime to Instant
            Instant instant = zonedDateTime.toInstant();
            // Step 3: Get the number of milliseconds since the epoch
            return instant.toEpochMilli();
        }
    }

    // Stop the timer and print the elapsed time
    public static String stopAndPrint(long startTime) {
        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;

        long minutes = elapsed / (60 * 1000);
        long seconds = (elapsed % (60 * 1000)) / 1000;
        long milliseconds = elapsed % 1000;

        return String.format("%dm %ds %dms", minutes, seconds, milliseconds);
    }

    public static LocalDateTime now(){
        return DateHelperXMLModel.currentSimDateTime!=null
                ? (DateHelperXMLModel.isSimRunningTime
                ? DateHelperXMLModel.currentSimDateTime.plusNanos(milisecondsFromSimOrigin() * 1_000_000)
                : DateHelperXMLModel.currentSimDateTime)
                : LocalDateTime.now();
    }

    public static String fromDateOnly(LocalDateTime dt){
        return  dt!=null ? dt.format(DateHelperXMLModel.DEFAULT_DATE_ONLY_FORMATTER) : null;
    }

    public static String nowDefault(){
        return DateHelperXMLModel.now().atZone(ZoneId.systemDefault()).format(DEFAULT_DATE_FORMATTER);
    }

    public static String nowUTCDefault(){
        return DateHelperXMLModel.now().atZone(ZoneOffset.UTC).format(DEFAULT_DATE_FORMATTER);
    }

    public static String dateUTCDefault(LocalDateTime dt){
        if (dt!=null) {
            ZonedDateTime utcDateTime = dt.atZone(ZoneOffset.UTC);
            return utcDateTime.format(DEFAULT_DATE_FORMATTER);
        }
        else {
            return nowUTCDefault();
        }
    }

    public static String dateDefault(LocalDateTime dt){
        if (dt!=null) {
            return dt.atZone(ZoneId.systemDefault()).format(DateHelperXMLModel.DEFAULT_DATE_FORMATTER);
        }
        else {
            return nowUTCDefault();
        }
    }

    public static LocalDateTime fromStringDefaultLDT(String s) {
        if (s != null && !s.isEmpty()) {
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(s, DEFAULT_DATE_FORMATTER);
                return zdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            } catch (Exception ex) {
                try {
                    OffsetDateTime odt = OffsetDateTime.parse(s, DEFAULT_DATE_FORMATTER);
                    return odt.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
                } catch (Exception ex1) {
                    try {
                        // último intento sin zona, se asume UTC
                        return LocalDateTime.parse(s);
                    } catch (Exception ex2) {
                        // ignorar o lanzar excepción según convenga
                    }
                }
            }
        }
        return null;
    }

    public static LocalDateTime fromStringLDT(String s, String format) {
        if (s!=null && !s.isEmpty()) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(format);
            try {
                return ZonedDateTime.parse(s, dateFormatter).toLocalDateTime();
            }
            catch (Exception ex) {
                try {
                    return OffsetDateTime.parse(s, DEFAULT_DATE_FORMATTER).toLocalDateTime();
                }
                catch (Exception ex1){
                    //ignore it
                }

            }
        }
        return null;
    }

    public static LocalDateTime fromStringLDT(String s, DateTimeFormatter dateTimeFormatter) {
        if (s!=null) {
            s = s.replaceAll(" ","");
            if (!s.isEmpty()) {
                try {
                    return ZonedDateTime.parse(s, dateTimeFormatter).toLocalDateTime();
                }
                catch (Exception ex) {
                    try {
                        return OffsetDateTime.parse(s, dateTimeFormatter).toLocalDateTime();
                    }
                    catch (Exception ex1){
                        //ignore it
                    }

                }
            }
        }
        return null;
    }

    public static LocalDateTime fromStringOnlyDateLDT(String s) {
        if (s!=null) {
            s = s.replaceAll(" ","");
            if (!s.isEmpty()) {
                try {
                    LocalDate dt =  LocalDate.parse(s, DEFAULT_DATE_ONLY_FORMATTER);
                    return  dt.atStartOfDay();
                }
                catch (Exception ex) {
                    //ignore it
                }
            }
        }
        return null;
    }

    public static Date fromLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime==null) return null;
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDateTime fromDate(Date date) {
        if (date==null) return null;
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
