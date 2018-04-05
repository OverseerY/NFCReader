package com.yaroslav.newfuckingtestapp;

import android.app.DatePickerDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

import java.util.Calendar;

public class HistoryActivity extends AppCompatActivity {

    Calendar dateStart, dateFinish;
    EditText dateFromField;
    EditText dateToField;

    private long currentDateFromMillisec;
    private long currentDateToMillisec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        if (savedInstanceState != null) {
            currentDateFromMillisec = savedInstanceState.getLong("currentDateFromMillisec");
            currentDateToMillisec = savedInstanceState.getLong("currentDateToMillisec");
        }

        dateStart = Calendar.getInstance();
        dateFinish = Calendar.getInstance();

        dateFromField = (EditText) findViewById(R.id.date_from_field);
        dateToField = (EditText) findViewById(R.id.date_to_field);
        dateFromField.setFocusable(false);
        dateToField.setFocusable(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle onSavedInstanceState) {
        onSavedInstanceState.putLong("currentDateFromMillisec", currentDateFromMillisec);
        onSavedInstanceState.putLong("currentDateToMillisec", currentDateToMillisec);

        super.onSaveInstanceState(onSavedInstanceState);
    }

    public void onClickStartDate(View view) {
        setStartDate();
    }

    public void onClickFinishDate(View view) {
        setFinishDate();
    }

    private void setStartDate() {
        new DatePickerDialog(HistoryActivity.this, calendar_start,
                dateStart.get(Calendar.YEAR),
                dateStart.get(Calendar.MONTH),
                dateStart.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void setFinishDate() {
        new DatePickerDialog(HistoryActivity.this, calendar_finish,
                dateFinish.get(Calendar.YEAR),
                dateFinish.get(Calendar.MONTH),
                dateFinish.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    DatePickerDialog.OnDateSetListener calendar_start = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dateStart.set(Calendar.YEAR, year);
            dateStart.set(Calendar.MONTH, monthOfYear);
            dateStart.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            currentDateFromMillisec = dateStart.getTimeInMillis();
            setInitialDateFrom();
        }
    };

    DatePickerDialog.OnDateSetListener calendar_finish = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dateFinish.set(Calendar.YEAR, year);
            dateFinish.set(Calendar.MONTH, monthOfYear);
            dateFinish.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            currentDateToMillisec = dateFinish.getTimeInMillis();
            setInitialDateTo();
        }
    };

    private void setInitialDateTo() {
        dateToField.setText(DateUtils.formatDateTime(this, currentDateToMillisec,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR));
    }

    private void setInitialDateFrom() {
        dateFromField.setText(DateUtils.formatDateTime(this, currentDateFromMillisec,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR ));
    }
}
