package com.example.callzilla;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.anychart.anychart.Animation;
import com.anychart.anychart.AnyChart;
import com.anychart.anychart.AnyChartView;
import com.anychart.anychart.Cartesian;
import com.anychart.anychart.DataEntry;
import com.anychart.anychart.Pie;
import com.anychart.anychart.ValueDataEntry;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import java.text.SimpleDateFormat;
import java.util.*;

public class VisualMode extends AppCompatActivity {
    private static final String RESPONDED = "responded";
    private static final String DIALED = "dialed";
    private static final String MISSED = "missed";
    private static final String REJECTED = "rejected";
    private String startDate;
    private String endDate;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    private Pie pie = AnyChart.pie();
    private Cartesian line = AnyChart.line();
    private Cartesian bar = AnyChart.column();
    List<DataEntry> responseData;
    List<DataEntry> durationData;
    List<DataEntry> frequencyData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visual);
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.refreshLayout);
        Button dateTitle = findViewById(R.id.chooseDateVisual);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE,
                            Manifest.permission.READ_CALL_LOG,
                            Manifest.permission.WRITE_CALL_LOG}, 1);
        }
        DBHelper db = new DBHelper(this, true, true);
        AnyChartView anyChartView = (AnyChartView) findViewById(R.id.response);
        AnyChartView anyChartView2 = (AnyChartView) findViewById(R.id.duration);
        AnyChartView anyChartView3 = (AnyChartView) findViewById(R.id.frequency);
        pie.setAnimation(true, 500.0);
        pie.setBackground("#D9E5E6");
        line.getXAxis().setTitle("Time");
        line.getYAxis().setTitle("Duration (sec)");
        line.setAnimation(true, 500.0);
        line.setBackground("#D9E5E6");
        bar.getXAxis().setTitle("Time");
        bar.getYAxis().setTitle("Frequency");
        bar.setAnimation(true, 500.0);
        bar.setBackground("#D9E5E6");
        setCharts(db.phoneCall, anyChartView, anyChartView2, anyChartView3);
        // Refresh the layout
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        startDate = null;
                        endDate = null;
                        Intent intent = getIntent();
                        intent.putExtra("Start Date", startDate);
                        intent.putExtra("End Date", endDate);
                        finish();
                        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
        );
        Bundle b = getIntent().getExtras();
        if (b != null) {
            startDate = (String) b.get("Start Date");
            endDate = (String) b.get("End Date");
            if (startDate != null && endDate != null) {
                updateDb(db.phoneCall);
                responseData = getResponseData(db.phoneCall);
                pie.setData(responseData);
                durationData = getDurationData(db.phoneCall);
                line.setData(durationData);
                frequencyData = getFrequencyData(db.phoneCall);
                bar.setData(frequencyData);
                dateTitle.setText(startDate + " - " + endDate);
            }
        }
        if (startDate != null && endDate != null) dateTitle.setText(startDate + " - " + endDate);
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("SELECT A DATE RANGE");
        final MaterialDatePicker<Pair<Long, Long>> datePickerDialog = builder.build();

        dateTitle.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        datePickerDialog.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
                    }
                }
        );
        datePickerDialog.addOnPositiveButtonClickListener(
                new MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>>() {
                    @Override
                    public void onPositiveButtonClick(Pair<Long, Long> selection) {
                        startDate = format.format(new Date(selection.first));
                        endDate = format.format(new Date(selection.second));
                        updateDb(db.phoneCall);
                        Intent intent = getIntent();
                        finish();
                        intent.putExtra("Start Date", startDate);
                        intent.putExtra("End Date", endDate);
                        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                    }
                });
    }

    private void setCharts(SQLiteDatabase db, AnyChartView anyChartView,
                           AnyChartView anyChartView2, AnyChartView anyChartView3) {
        responseData = getResponseData(db);
        pie.setData(responseData);
        anyChartView.setChart(pie);
        durationData = getDurationData(db);
        line.line(durationData).setName("Duration");
        anyChartView2.setChart(line);
        frequencyData = getFrequencyData(db);
        bar.column(frequencyData).setName("Frequency");
        anyChartView3.setChart(bar);
    }

    private List<DataEntry> getFrequencyData(SQLiteDatabase db) {
        List<DataEntry> data = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM time_table", null);
        if (c != null ) {
            if  (c.moveToFirst()) {
                do {
                    int timeColumn = c.getColumnIndex("time");
                    String time = c.getString(timeColumn);
                    int frequencyColumn = c.getColumnIndex("frequency");
                    String frequency = c.getString(frequencyColumn);
                    data.add(new ValueDataEntry(time, Integer.parseInt(frequency)));
                } while (c.moveToNext());
            }
        }
        return data;
    }

    private List<DataEntry> getDurationData(SQLiteDatabase db) {
        List<DataEntry> data = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM time_table", null);
        if (c != null ) {
            if  (c.moveToFirst()) {
                do {
                    int timeColumn = c.getColumnIndex("time");
                    String time = c.getString(timeColumn);
                    int durationColumn = c.getColumnIndex("total_duration");
                    String duration = c.getString(durationColumn);
                    data.add(new ValueDataEntry(time, Integer.parseInt(duration)));
                } while (c.moveToNext());
            }
        }
        assert c != null;
        c.close();
        return data;
    }

    private List<DataEntry> getResponseData(SQLiteDatabase db) {
        List<DataEntry> data = new ArrayList<>();
        long responded = db.compileStatement("SELECT responded FROM response_table WHERE id = 1").simpleQueryForLong();
        long dialed = db.compileStatement("SELECT dialed FROM response_table WHERE id = 1").simpleQueryForLong();
        long missed = db.compileStatement("SELECT missed FROM response_table WHERE id = 1").simpleQueryForLong();
        long rejected = db.compileStatement("SELECT rejected FROM response_table WHERE id = 1").simpleQueryForLong();
        data.add(new ValueDataEntry(RESPONDED, (int) responded));
        data.add(new ValueDataEntry(DIALED, (int) dialed));
        data.add(new ValueDataEntry(MISSED, (int) missed));
        data.add(new ValueDataEntry(REJECTED, (int) rejected));
        return data;
    }

    public void switchMode(View view) {
        Intent intent = new Intent(this, TextMode.class);
        intent.putExtra("Start Date", startDate);
        intent.putExtra("End Date", endDate);
        startActivity(intent);
    }

    public void updateDb(SQLiteDatabase db) {
        db.execSQL("UPDATE response_table SET responded = 0, dialed = 0, missed = 0, rejected = 0");
        db.execSQL("UPDATE time_table set frequency = 0, total_duration = 0");
        db.execSQL("DELETE FROM day_table");
        db.execSQL(
                "INSERT INTO day_table (date)" +
                        "  WITH RECURSIVE dates(date) AS (\n" +
                        "    VALUES('" + startDate + "')\n" +
                        "    UNION ALL\n" +
                        "    SELECT date(date, '+1 day')\n" +
                        "    FROM dates\n" +
                        "    WHERE date < '" + endDate + "'\n" +
                        "  )\n" +
                        "SELECT date FROM dates;"
        );
        db.execSQL("DROP TABLE IF EXISTS sub_log");
        db.execSQL("CREATE TABLE sub_log AS " +
                "SELECT * FROM log WHERE date BETWEEN '" + startDate + "' AND '" + endDate + "'");
        Cursor c = db.rawQuery("SELECT * FROM sub_log", null);
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    int typeColumn = c.getColumnIndex("type");
                    String type = c.getString(typeColumn);
                    switch (type) {
                        case "responded":
                            db.execSQL("UPDATE response_table SET responded = responded + 1 WHERE id = 1");
                            break;
                        case "dialed":
                            db.execSQL("UPDATE response_table SET dialed = dialed + 1 WHERE id = 1");
                            break;
                        case "missed":
                            db.execSQL("UPDATE response_table SET missed = missed + 1 WHERE id = 1");
                            break;
                        case "rejected":
                            db.execSQL("UPDATE response_table SET rejected = rejected + 1 WHERE id = 1");
                            break;
                    }
                    int dateColumn = c.getColumnIndex("date");
                    String date = c.getString(dateColumn);
                    int timeColumn = c.getColumnIndex("time");
                    String time = c.getString(timeColumn);
                    int durationColumn = c.getColumnIndex("duration");
                    String duration = c.getString(durationColumn);
                    db.execSQL("UPDATE day_table SET frequency = frequency + 1, " +
                            "total_duration = total_duration + " + duration +
                            " WHERE date = '" + date + "'");
                    db.execSQL("UPDATE time_table SET frequency = frequency + 1, " +
                            "total_duration = total_duration + " + duration +
                            " WHERE time = " + time);
                } while (c.moveToNext());
            }
        }
    }

}