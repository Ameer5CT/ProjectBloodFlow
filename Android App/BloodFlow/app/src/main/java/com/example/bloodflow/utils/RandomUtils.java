package com.example.bloodflow.utils;

import android.os.Environment;

import java.util.UUID;

public class RandomUtils {

    public static final UUID M_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String BOT_URL = "https://api.telegram.org/bot";
    public static final String TOKEN = "5209962730:AAHVX-HxZIPhCxOgsaK9AJAsnU5phrF9hQs";
    public static final String AVERAGE = "Average: ";
    public static final String KEY = "KEY";
    public static final String EMPTY = "empty";
    public static final String FOLDER = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/Blood Flow App/";
    public static final String TEMP = FOLDER + ".temp/";

}
