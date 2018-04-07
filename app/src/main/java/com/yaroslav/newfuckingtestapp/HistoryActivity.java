package com.yaroslav.newfuckingtestapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HistoryActivity extends AppCompatActivity {

    ArrayAdapter<Ticket> adapter;
    List<Ticket> tagItems;
    ListView listView;

    public static String LOG_TAG = "JsonLog";

    Calendar dateStart, dateFinish;
    EditText dateFromField;
    EditText dateToField;

    TextView test_history;

    ProgressDialog progDailog;

    private boolean isInternetEnabled = false;

    private long currentDateFromMillisec;
    private long currentDateToMillisec;

    private static final long dateCorrection = 1000 * 60 * 60 * 24;
    private static final long timeCorrection = 1000 * 60 * 60 * 12 ;

    private static long todayDateTime;

    private String url_json = "http://points.temirtulpar.com/api/values";

    //#region Activity Lifetime Methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        if (savedInstanceState != null) {
            currentDateFromMillisec = savedInstanceState.getLong("currentDateFromMillisec");
            currentDateToMillisec = savedInstanceState.getLong("currentDateToMillisec");
        }

        testConnectionState();

        todayDateTime = new Date().getTime();

        dateStart = Calendar.getInstance();
        dateFinish = Calendar.getInstance();

        dateFromField = (EditText) findViewById(R.id.date_from_field);
        dateToField = (EditText) findViewById(R.id.date_to_field);
        dateFromField.setFocusable(false);
        dateToField.setFocusable(false);

        test_history = (TextView) findViewById(R.id.test_history);

        //Array of scanned tags (object type Ticket)
        tagItems = new ArrayList<>();
        listView = (ListView) findViewById(R.id.history_list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tagItems);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle onSavedInstanceState) {
        onSavedInstanceState.putLong("currentDateFromMillisec", currentDateFromMillisec);
        onSavedInstanceState.putLong("currentDateToMillisec", currentDateToMillisec);

        super.onSaveInstanceState(onSavedInstanceState);
    }

    //#endregion

    //#region Calendar

    public void onClickStartDate(View view) {
        setStartDate();
    }

    public void onClickFinishDate(View view) {
        setFinishDate();
    }

    private void setStartDate() {
        DatePickerDialog dpd = new DatePickerDialog(HistoryActivity.this, calendar_start,
                dateStart.get(Calendar.YEAR),
                dateStart.get(Calendar.MONTH),
                dateStart.get(Calendar.DAY_OF_MONTH));
        dpd.getDatePicker().setMaxDate(todayDateTime);
        dpd.show();
    }

    private void setFinishDate() {
        DatePickerDialog dpd = new DatePickerDialog(HistoryActivity.this, calendar_finish,
                dateFinish.get(Calendar.YEAR),
                dateFinish.get(Calendar.MONTH),
                dateFinish.get(Calendar.DAY_OF_MONTH));
        dpd.getDatePicker().setMaxDate(todayDateTime + dateCorrection);
        dpd.show();
    }

    DatePickerDialog.OnDateSetListener calendar_start = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dateStart = setDefaultTime(dateStart);
            dateStart.set(Calendar.YEAR, year);
            dateStart.set(Calendar.MONTH, monthOfYear);
            dateStart.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            setInitialDateFrom();

            tagItems.clear();
            adapter.notifyDataSetChanged();
            setNewUrl();
            new ParseTask().execute();
        }
    };

    DatePickerDialog.OnDateSetListener calendar_finish = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dateFinish = setDefaultTime(dateFinish);
            dateFinish.set(Calendar.YEAR, year);
            dateFinish.set(Calendar.MONTH, monthOfYear);
            dateFinish.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            setInitialDateTo();

            if (currentDateFromMillisec != 0) {
                tagItems.clear();
                adapter.notifyDataSetChanged();
                setNewUrl();
                new ParseTask().execute();
            }
        }
    };

    public static Calendar setDefaultTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private void setInitialDateTo() {
        currentDateToMillisec = dateFinish.getTimeInMillis();
        if (currentDateFromMillisec != 0 && currentDateToMillisec < currentDateFromMillisec) {
            long temp = currentDateFromMillisec;
            currentDateFromMillisec = currentDateToMillisec;
            currentDateToMillisec = temp;

            dateFromField.setText(DateUtils.formatDateTime(this, currentDateFromMillisec,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR ));
            dateToField.setText(DateUtils.formatDateTime(this, currentDateToMillisec,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR));
        } else {
            dateToField.setText(DateUtils.formatDateTime(this, currentDateToMillisec, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR));
        }
    }

    private void setInitialDateFrom() {
        currentDateFromMillisec = dateStart.getTimeInMillis();
        if (currentDateToMillisec != 0 && currentDateToMillisec < currentDateFromMillisec) {
            long temp = currentDateFromMillisec;
            currentDateFromMillisec = currentDateToMillisec;
            currentDateToMillisec = temp;

            dateFromField.setText(DateUtils.formatDateTime(this, currentDateFromMillisec,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR ));
            dateToField.setText(DateUtils.formatDateTime(this, currentDateToMillisec,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR));
        } else {
            dateFromField.setText(DateUtils.formatDateTime(this, currentDateFromMillisec, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR));
        }
    }

    private void getLastDay() {
        long from = getCalculatedDate(-1);
        long to = getCalculatedDate(0);
        dateFromField.setText(DateUtils.formatDateTime(this, from,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR ));
        dateToField.setText(DateUtils.formatDateTime(this, to,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR));
        setFixedUrl(from, to);
    }

    private void getLastWeek() {
        long from = getCalculatedDate(-7);
        long to = getCalculatedDate(0);
        dateFromField.setText(DateUtils.formatDateTime(this, from,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR ));
        dateToField.setText(DateUtils.formatDateTime(this, to,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR));
        setFixedUrl(from, to);
    }

    private void getLastMonth() {
        long from = getCalculatedDate(-30);
        long to = getCalculatedDate(0);
        dateFromField.setText(DateUtils.formatDateTime(this, from,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR ));
        dateToField.setText(DateUtils.formatDateTime(this, to,DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR));
        setFixedUrl(from, to);
    }

    public static long getCalculatedDate(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTimeInMillis();
    }

    //#endregion

    //#region Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.history_day:
                if (isInternetEnabled) {
                    tagItems.clear();
                    adapter.notifyDataSetChanged();
                    getLastDay();
                    new ParseTask().execute();
                } else {
                    autoCloseDialog(getString(R.string.notification), getString(R.string.connection_lost));
                }
                return true;
            case R.id.history_week:
                if (isInternetEnabled) {
                    tagItems.clear();
                    adapter.notifyDataSetChanged();
                    getLastWeek();
                    new ParseTask().execute();
                } else {
                    autoCloseDialog(getString(R.string.notification), getString(R.string.connection_lost));
                }
                return true;
            case R.id.history_month:
                if (isInternetEnabled) {
                    tagItems.clear();
                    adapter.notifyDataSetChanged();
                    getLastMonth();
                    new ParseTask().execute();
                } else {
                    autoCloseDialog(getString(R.string.notification), getString(R.string.connection_lost));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void autoCloseDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                HistoryActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(true);


        final AlertDialog closedialog = builder.create();

        closedialog.show();

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                closedialog.dismiss();
                timer.cancel();
            }
        }, 2000);

    }

    //#endregion

    //#region JSON Parse
    private class ParseTask extends AsyncTask<Void, Void, String> {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String resultJson = "";
        public ProgressBar dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            /*ProgressDialog*/ progDailog = new ProgressDialog(HistoryActivity.this);
            progDailog.setMessage(getString(R.string.progress_message));
            progDailog.setIndeterminate(false);
            progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progDailog.setCancelable(true);
            progDailog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                URL url = new URL(url_json);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                resultJson = buffer.toString();
            } catch (Exception e) {
                Log.e("ParseTask", e.getLocalizedMessage());
                //Log.e("ParseTask_extended", e.getStackTrace().toString());
            }
            return resultJson;
        }

        @Override
        protected void onPostExecute(String strJson) {
            super.onPostExecute(strJson);

            progDailog.dismiss();

            Log.d(LOG_TAG, strJson);

            JSONObject dataJsonObj = null;

            try {
                dataJsonObj = new JSONObject(strJson);
                JSONArray items = dataJsonObj.getJSONArray("Items");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);

                    String description = item.getString("Description");
                    String cardId = item.getString("CardId");
                    String uuid = item.getString("UID");
                    String lat = item.getString("Ltt");
                    String lon = item.getString("Lng");
                    String time = item.getString("TimeMS");

                    Ticket tagItem = new Ticket(description, cardId, lat, lon, time, uuid);
                    tagItems.add(tagItem);
                    adapter.notifyDataSetChanged();
                }

            } catch (JSONException e) {
                Log.e("onPostExecute", e.getLocalizedMessage());
                //Log.e("onPostExecute_extended", e.getStackTrace().toString());
            }
        }
    }

    private void setFixedUrl(long from, long to) {
        url_json = "http://points.temirtulpar.com/api/values/dt/" + String.valueOf(from) + "/" + String.valueOf(to);
        //test_history.setText("" + todayDateTime);
    }

    private void setNewUrl() {
        if (currentDateFromMillisec != 0) {
            url_json = "http://points.temirtulpar.com/api/values/dt/" + String.valueOf(currentDateFromMillisec);
            if (currentDateToMillisec != 0) {
                if (currentDateFromMillisec != currentDateToMillisec) {
                    url_json += "/" + String.valueOf(currentDateToMillisec);
                } else {
                    long wholeDayTime = currentDateToMillisec + dateCorrection;
                    url_json += "/" + String.valueOf(wholeDayTime);
                }
            }
        }
        //test_history.setText(url_json);
    }

    //#endregion

    //#region Check Internet State

    public void testConnectionState() {
        Timer netTimer = new Timer();
        netTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isURLReachable("http://points.temirtulpar.com/api/values")) {
                    isInternetEnabled = true;
                } else {
                    isInternetEnabled = false;
                }
            }
        }, 0L, 5L * 1000);
    }

    //Get URl as argument to check it's availability and return result
    static public boolean isURLReachable(String url) {
        if (get(url) != null) {
            return true;
        }
        return false;
    }

    //Method of URL check that prevents memory leak (as AndroidStudio says)
    static String get(String url) {
        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder().url(url).get().build();
        try {
            Response resp = client.newCall(req).execute();
            ResponseBody body = resp.body();
            if (resp.isSuccessful()) {
                return body.string(); // Closes automatically.
            } else {
                body.close();
                return null;
            }
        } catch (IOException e) {
            Log.e("get_HTTP_Response", e.getLocalizedMessage());
            return null;
        }
    }

    //#endregion
}






























