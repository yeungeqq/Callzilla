package com.example.callzilla;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;

import de.codecrafters.tableview.TableView;
import de.codecrafters.tableview.toolkit.SimpleTableDataAdapter;
import de.codecrafters.tableview.toolkit.SimpleTableHeaderAdapter;

public class TextMode extends AppCompatActivity{
    private String startDate;
    private String endDate;
    Button datePicker;
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.refreshLayout);
        Button dateTitle = findViewById(R.id.chooseDateText);
        DBHelper db = new DBHelper(this, true, true);
        TableView<String[]> table = findViewById(R.id.table);
        loadCallData(db.phoneCall);
        loadDateTable(db.phoneCall, table);
        TabLayout tabs = findViewById(R.id.tabs);
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
                if (tabs.getSelectedTabPosition() == 0) {loadDateTable(db.phoneCall, table);}
                if (tabs.getSelectedTabPosition() == 1) {loadTimeTable(db.phoneCall, table);}
                dateTitle.setText(startDate + " - " + endDate);
                loadCallData(db.phoneCall);
            }
        }
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {loadDateTable(db.phoneCall, table);}
                if (tab.getPosition() == 1) {loadTimeTable(db.phoneCall, table);}
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

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
                        startDate = format.format(selection.first);
                        endDate = format.format(selection.second);
                        updateDb(db.phoneCall);
                        Intent intent = getIntent();
                        finish();
                        intent.putExtra("Start Date", startDate);
                        intent.putExtra("End Date", endDate);
                        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                    }
                });
    }

    private void loadTimeTable(SQLiteDatabase phoneCall, TableView<String[]> table) {
        table.setColumnCount(2);
        long tableRow = phoneCall.compileStatement("SELECT COUNT(*) FROM time_table").simpleQueryForLong();
        String[][] timeData = new String[(int) tableRow][2];
        Cursor c = phoneCall.rawQuery("SELECT * FROM time_table", null);
        String[] header = {"Hour of Day", "Duration"};
        table.setHeaderAdapter(new SimpleTableHeaderAdapter(this, header));
        int row = 0;
        if (c != null ) {
            if  (c.moveToFirst()) {
                do {
                    int timeColumn = c.getColumnIndex("time");
                    String time = c.getString(timeColumn);
                    int durationColumn = c.getColumnIndex("total_duration");
                    String duration = c.getString(durationColumn);
                    timeData[row][0] = time;
                    timeData[row][1] = duration + " sec";
                    row++;
                } while (c.moveToNext());
            }
        }
        table.setDataAdapter(new SimpleTableDataAdapter(this, timeData));
        assert c != null;
        c.close();
    }

    private void loadDateTable(SQLiteDatabase phoneCall, TableView<String[]> table) {
        table.setColumnCount(3);
        long tableRow = phoneCall.compileStatement("SELECT COUNT(*) FROM day_table").simpleQueryForLong();
        String[][] dateData = new String[(int) tableRow][3];
        Cursor c = phoneCall.rawQuery("SELECT * FROM day_table", null);
        String[] header = {"Date", "Frequency", "Duration"};
        table.setHeaderAdapter(new SimpleTableHeaderAdapter(this, header));
        int row = 0;
        if (c != null ) {
            if  (c.moveToFirst()) {
                do {
                    int dateColumn = c.getColumnIndex("date");
                    String date = c.getString(dateColumn);
                    int frequencyColumn = c.getColumnIndex("frequency");
                    String frequency = c.getString(frequencyColumn);
                    int durationColumn = c.getColumnIndex("total_duration");
                    String duration = c.getString(durationColumn);
                    dateData[row][0] = date;
                    dateData[row][1] = frequency;
                    dateData[row][2] = duration + " sec";
                    row++;
                } while (c.moveToNext());
            }
        }
        table.setDataAdapter(new SimpleTableDataAdapter(this, dateData));
        assert c != null;
        c.close();
    }

    public void switchMode(View view) {
        Intent intent = new Intent(this, VisualMode.class);
        intent.putExtra("Start Date", startDate);
        intent.putExtra("End Date", endDate);
        startActivity(intent);
    }

    public void loadCallData(SQLiteDatabase db) {
        long responded = db.compileStatement("SELECT responded FROM response_table WHERE id = 1").simpleQueryForLong();
        long dialed = db.compileStatement("SELECT dialed FROM response_table WHERE id = 1").simpleQueryForLong();
        long missed = db.compileStatement("SELECT missed FROM response_table WHERE id = 1").simpleQueryForLong();
        long rejected = db.compileStatement("SELECT rejected FROM response_table WHERE id = 1").simpleQueryForLong();
        long total = responded + dialed + missed + rejected;
        TextView totalNo = (TextView) findViewById(R.id.total);
        totalNo.setText(String.valueOf((total)));
        TextView respondedNo = (TextView) findViewById(R.id.responded);
        respondedNo.setText(String.valueOf(responded));
        TextView dialedNo = (TextView) findViewById(R.id.dialed);
        dialedNo.setText(String.valueOf(dialed));
        TextView missedNo = (TextView) findViewById(R.id.missed);
        missedNo.setText(String.valueOf(missed));
        TextView rejectedNo = (TextView) findViewById(R.id.rejected);
        rejectedNo.setText(String.valueOf(rejected));
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