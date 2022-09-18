package com.example.callzilla;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.CallLog;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DBHelper extends SQLiteOpenHelper{
    private static final String name = "phoneCall";
    private static final int version = 1;

    private static final String RESPONSE_TABLE = "response_table";
    private static final String ID = "id";
    private static final String RESPONDED = "responded";
    private static final String DIALED = "dialed";
    private static final String MISSED = "missed";
    private static final String REJECTED = "rejected";

    private static final String LOG = "log";
    private static final String DATE_TIME = "date_time";
    private static final String DATE = "date";
    private static final String TIME = "time";
    private static final String TYPE = "type";
    private static final String DURATION = "duration";

    private static final String TIME_TABLE = "time_table";
    private static final String T_DURATION = "total_duration";

    private static final String DAY_TABLE = "day_table";
    private static final String FREQUENCY = "frequency";

    public SQLiteDatabase phoneCall;

    private Context context;

    public DBHelper(Context context, boolean clear, boolean load) {
        super(context, name, null, version);
        this.context = context;
        if (clear) this.context.deleteDatabase(name);
        phoneCall = getWritableDatabase();
        if (load) loadLog(phoneCall);;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("DB", "dbOnCreate");
        String LOG_CREATE =
            "CREATE TABLE IF NOT EXISTS " + LOG + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    TYPE + " TEXT, " +
                    DATE + " TEXT, " +
                    TIME + " INTEGER, " +
                    DURATION + " INTEGER)";

        String RESPONSE_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + RESPONSE_TABLE + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    RESPONDED + " INTEGER, " +
                    DIALED + " INTEGER, " +
                    MISSED + " INTEGER, " +
                    REJECTED + " INTEGER)";

        String TIME_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TIME_TABLE + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    TIME + " INTEGER, " +
                    FREQUENCY + " INTEGER DEFAULT 0, " +
                    T_DURATION + " INTEGER DEFAULT 0)";

        String DAY_TABLE_CREATE =
                "CREATE TABLE IF NOT EXISTS " + DAY_TABLE + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DATE_TIME + " TEXT, " +
                    DATE + " TEXT, " +
                    FREQUENCY + " INTEGER DEFAULT 0, " +
                    T_DURATION + " INTEGER DEFAULT 0)";

        db.execSQL(LOG_CREATE);
        db.execSQL(RESPONSE_TABLE_CREATE);
        db.execSQL(DAY_TABLE_CREATE);
        db.execSQL(TIME_TABLE_CREATE);
        if (db.compileStatement("SELECT COUNT(*) FROM time_table").simpleQueryForLong() == 0) {
            for (int i = 0; i < 24; i++) {
                db.execSQL("INSERT INTO " + TIME_TABLE + " (time) VALUES (" + i + ")");
            }
            ContentValues values = new ContentValues();
            values.put(RESPONDED, 0);
            values.put(DIALED, 0);
            values.put(MISSED, 0);
            values.put(REJECTED, 0);
            db.insert(RESPONSE_TABLE, "", values);
        }
        // loadLog(db);
    }

    public void loadLog(SQLiteDatabase phoneCall) {
        Uri contacts = CallLog.Calls.CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(contacts, null,
                null, null, null);
        int duration = cursor.getColumnIndex(CallLog.Calls.DURATION);
        int date = cursor.getColumnIndex(CallLog.Calls.DATE);
        int type = cursor.getColumnIndex(CallLog.Calls.TYPE);
        int id = cursor.getColumnIndex(CallLog.Calls._ID);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String firstDate = "";
        String todayDate = format.format(new Date());
        // retrieve all call history
        while (cursor.moveToNext()) {
            if (Integer.parseInt(cursor.getString(id)) == 1) {
                firstDate = format.format(new Date(Long.valueOf(cursor.getString(date))));
                phoneCall.execSQL(
                        "INSERT INTO " + DAY_TABLE + "(date)" +
                                "  WITH RECURSIVE dates(date) AS (\n" +
                                "    VALUES('" + firstDate + "')\n" +
                                "    UNION ALL\n" +
                                "    SELECT date(date, '+1 day')\n" +
                                "    FROM dates\n" +
                                "    WHERE date < '" + todayDate + "'\n" +
                                "  )\n" +
                                "SELECT date FROM dates;"
                );
            }
            /*
            long count = phoneCall.compileStatement("SELECT COUNT(*) FROM log").simpleQueryForLong();
            if (count > 0) {
                long uid_check = phoneCall.compileStatement("SELECT uid FROM log ORDER BY id DESC LIMIT 1").simpleQueryForLong();
                if (uid_check == uid) return;
            }

             */
            long callDuration = Long.parseLong(cursor.getString(duration));
            String callDateTime = new Date(Long.valueOf(cursor.getString(date))).toString();
            String callDate = format.format(new Date(Long.valueOf(cursor.getString(date))));
            int time = Integer.parseInt(callDateTime.substring(11, 13));
            ContentValues values = new ContentValues();
            int callType = Integer.parseInt(cursor.getString(type));
            // update database based on the call type
            switch (callType) {
                case CallLog.Calls.INCOMING_TYPE:
                    updateDatabase("responded", values, phoneCall, callDuration, time, callDate);
                    phoneCall.execSQL("UPDATE day_table SET frequency = frequency + 1 WHERE date = '" + callDate + "'");
                    phoneCall.execSQL("UPDATE day_table SET total_duration = total_duration + " +
                            callDuration + " WHERE date = '" + callDate + "'");
                    break;
                case CallLog.Calls.OUTGOING_TYPE:
                    updateDatabase("dialed", values, phoneCall, callDuration, time, callDate);
                    phoneCall.execSQL("UPDATE day_table SET frequency = frequency + 1 WHERE date = '" + callDate + "'");
                    phoneCall.execSQL("UPDATE day_table SET total_duration = total_duration + " +
                            callDuration + " WHERE date = '" + callDate + "'");
                    break;
                case CallLog.Calls.MISSED_TYPE:
                    updateDatabase("missed", values, phoneCall, callDuration, time, callDate);
                    break;
                case CallLog.Calls.REJECTED_TYPE:
                    updateDatabase("rejected", values, phoneCall, callDuration, time, callDate);
                    break;
            }
            values.put("date",callDate);
            values.put("time", time);
            values.put("duration",callDuration);
            phoneCall.insert("log","",values);
        }
        cursor.close();
    }

    public void updateDatabase(String typeString, ContentValues values, SQLiteDatabase db,
                               long callDuration, int time, String callDate) {
        values.put("type", typeString);
        db.execSQL("UPDATE response_table SET " + typeString +
                " = " + typeString + " + 1 WHERE id = 1");
        db.execSQL("UPDATE time_table SET " +
                "total_duration = total_duration + " + callDuration + ", " +
                "frequency = frequency + 1" +
                " WHERE time = " + time);
        db.execSQL("UPDATE day_table SET " +
                "total_duration = total_duration + " + callDuration + ", " +
                "frequency = frequency + 1" +
                " WHERE date = '" + callDate + "'");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + RESPONSE_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + TIME_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + DAY_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + LOG);
        onCreate(db);
    }

}

