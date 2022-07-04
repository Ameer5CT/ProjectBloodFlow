package com.example.bloodflow;

import android.annotation.SuppressLint;

import com.example.bloodflow.utils.RandomUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class FormatCorrector {

    public Date stringToDate(String s) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
        Date date = null;
        try { date = sdf.parse(s); } catch (ParseException e) { e.printStackTrace(); }
        return date;
    }
    public String formatTime(Calendar cal) {
        String amPm; String mZeroOrNot; String h00to12;
        if (cal.get(Calendar.AM_PM) == Calendar.AM) { amPm = "AM"; } else { amPm = "PM"; }
        if (cal.get(Calendar.HOUR) == 0) { h00to12 = "12"; } else {
            String hZeroOrNot;
            if (cal.get(Calendar.HOUR) < 10) { hZeroOrNot = "0"; } else { hZeroOrNot = ""; }
            h00to12 = hZeroOrNot + cal.get(Calendar.HOUR);
        }
        if (cal.get(Calendar.MINUTE) < 10) { mZeroOrNot = "0"; } else { mZeroOrNot = ""; }
        return h00to12 + ":" + mZeroOrNot + cal.get(Calendar.MINUTE) + " " + amPm;
    }
    public String formatDate(Calendar cal) {
        return cal.get(Calendar.DAY_OF_MONTH) + " / " + (cal.get(Calendar.MONTH) + 1);
    }

    public String messageUrl(String doctorID, String userName, int bpm, int spo2, Calendar cal) {
        return RandomUtils.BOT_URL + RandomUtils.TOKEN + "/sendMessage?chat_id=" + doctorID + "&text=" +
                "<b>%23" + userName.replace(" ", "_") + "</b>" +
                "%0A" + "BPM: " + bpm +
                "%0A" + "Spo2: " + spo2 +
                "%0A" + new FormatCorrector().formatTime(cal) +
                "%0A" + new FormatCorrector().formatDate(cal) + "&parse_mode=html";
    }

    public String filterBPM(String s) {
        if (s == null || s.equals("Beat!")) {
            return null;
        }
        int ind1 = s.indexOf("BPM:");
        int ind2 = s.indexOf("-----");
        if (ind1 > 0 && ind2 > 0) {
            return s.substring(ind1 + 4, ind2);
        } else {
            return null;
        }
    }
    public String filterSpo2(String s) {
        if (s == null || s.equals("Beat!")) {
            return null;
        }
        int ind11 = s.indexOf("Percent:");
        int ind22 = s.indexOf("*");
        if (ind11 > 0 && ind22 > 0) {
            return s.substring(ind11 + 8, ind22);
        } else {
            return null;
        }
    }
    public String filterAverageBPM(String s) {
        if (s == null || s.equals("Beat!")) {
            return null;
        }
        int ind1 = s.indexOf("BPM-AVG:");
        int ind2 = s.indexOf("-----");
        if (ind1 > 0 && ind2 > 0) {
            return s.substring(ind1 + 8, ind2);
        } else {
            return null;
        }
    }
    public String filterAverageSpo2(String s) {
        if (s == null || s.equals("Beat!")) {
            return null;
        }
        int ind11 = s.indexOf("Percent-AVG:");
        int ind22 = s.indexOf("*");
        if (ind11 > 0 && ind22 > 0) {
            return s.substring(ind11 + 12, ind22);
        } else {
            return null;
        }
    }
}
