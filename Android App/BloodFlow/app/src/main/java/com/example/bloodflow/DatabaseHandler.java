package com.example.bloodflow;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.bloodflow.model.Test;
import com.example.bloodflow.utils.Db;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

    public DatabaseHandler(Context context) {
        super(context, Db.DATABASE_NAME, null, Db.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String CREATE_TESTS_TABLE = "CREATE TABLE " + Db.TABLE_NAME + " (" + Db.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                Db.KEY_BPM + " INTEGER," + Db.KEY_SPO2 + " INTEGER," + Db.KEY_DATE + " TEXT)";

        sqLiteDatabase.execSQL(CREATE_TESTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + Db.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    public void addTest(Test test) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(Db.KEY_BPM, test.getBpm());
        contentValues.put(Db.KEY_SPO2, test.getSpo2());
        contentValues.put(Db.KEY_DATE, test.getDate().toString());
        database.insert(Db.TABLE_NAME, null, contentValues);
        database.close();
    }

    public Test getTest(int id) {
        SQLiteDatabase database = this.getReadableDatabase();
        Test test;
        Cursor cursor = database.query(Db.TABLE_NAME,
                new String[]{Db.KEY_ID, Db.KEY_BPM, Db.KEY_SPO2, Db.KEY_DATE},
                Db.KEY_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            test = new Test(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), new FormatCorrector().stringToDate(cursor.getString(3)));
        } else {
            test = null;
        }
        cursor.close();
        database.close();
        return test;
    }

    public List<Test> getAllTests() {
        SQLiteDatabase database = this.getReadableDatabase();
        List<Test> testList = new ArrayList<>();
        String getAll = "SELECT * FROM " + Db.TABLE_NAME;
        Cursor cursor = database.rawQuery(getAll ,null);

        if (cursor.getCount() > 0) {
            cursor.moveToLast();
            do{
                Test test = new Test();
                test.setId(cursor.getInt(0));
                test.setBpm(cursor.getInt(1));
                test.setSpo2(cursor.getInt(2));
                test.setDate(new FormatCorrector().stringToDate(cursor.getString(3)));
                testList.add(test);
            }while (cursor.moveToPrevious());
        } else {
            testList = null;
        }
        cursor.close();
        database.close();
        return testList;
    }

    public boolean isEmpty() {
        SQLiteDatabase database = this.getReadableDatabase();
        String selectAll = "SELECT * FROM " + Db.TABLE_NAME;
        Cursor cursor = database.rawQuery(selectAll ,null);
        if (cursor.getCount() == 0) {
            cursor.close();
            database.close();
            return true;
        } else {
            cursor.close();
            database.close();
            return false;
        }
    }

    public int getCount() {
        SQLiteDatabase database = this.getReadableDatabase();
        String selectAll = "SELECT * FROM " + Db.TABLE_NAME;
        Cursor cursor = database.rawQuery(selectAll ,null);
        int c = cursor.getCount();
        cursor.close();
        database.close();
        return c;
    }

    public List<Test> getLast(float inDays) {
        SQLiteDatabase database = this.getReadableDatabase();
        List<Test> testList = new ArrayList<>();
        String getAll = "SELECT * FROM " + Db.TABLE_NAME;
        Cursor cursor = database.rawQuery(getAll ,null);
        Date now = Calendar.getInstance().getTime();

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do{
                Date date = new FormatCorrector().stringToDate(cursor.getString(3));
                long different = now.getTime() - date.getTime();
                float days = (float) different / 86400000;
                if (days <= inDays) {
                    Test test = new Test();
                    test.setId(cursor.getInt(0));
                    test.setBpm(cursor.getInt(1));
                    test.setSpo2(cursor.getInt(2));
                    test.setDate(date);
                    testList.add(test);
                }
            }while (cursor.moveToNext());
        } else {
            testList = null;
        }
        cursor.close();
        database.close();
        return testList;
    }
}
