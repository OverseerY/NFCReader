package com.yaroslav.newfuckingtestapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.GsonBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "TEST";

    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 1f;

    private static final int PERMISSION_REQUEST_LOCATION = 0;

    private String currentTime;
    private String description;
    private String currentLatitude;
    private String currentLongitude;
    private String UniqID;

    private boolean isGpsEnabled;
    private boolean isNfcEnabled;
    private boolean isInternetEnabled;
    private boolean isServerAvailable;

    private boolean isGpsDialogShown;
    private boolean isNfcDialogShown;
    private boolean isNetDialogShown;
    private boolean isSrvDialogShown;

    private boolean isFileChecked;

    private boolean dontShowGps;
    private boolean dontShowNfc;
    private boolean dontShowNet;
    private boolean dontShowSrv;

    LocationManager locationManager;
    NfcManager nfcManager;
    NfcAdapter nfcAdapter;

    ArrayAdapter<Ticket> adapter;
    List<Ticket> points;
    List<Ticket> temp_points;
    ListView listView;

    private final String[][] techList = new String[][] {
            new String[] {
                    NfcA.class.getName(),
                    NfcB.class.getName(),
                    NfcV.class.getName(),
                    IsoDep.class.getName(),
                    MifareClassic.class.getName(),
                    MifareUltralight.class.getName(),
                    Ndef.class.getName()
            }
    };

    //#region Activity Lifetime Methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            isGpsDialogShown = savedInstanceState.getBoolean("isGpsDialogShown");
            isNfcDialogShown = savedInstanceState.getBoolean("isNfcDialogShown");
            isNetDialogShown = savedInstanceState.getBoolean("isNetDialogShown");
            isSrvDialogShown = savedInstanceState.getBoolean("isSrvDialogShown");

            isGpsEnabled = savedInstanceState.getBoolean("isGpsEnabled");
            isNfcEnabled = savedInstanceState.getBoolean("isNfcEnabled");
            isInternetEnabled = savedInstanceState.getBoolean("isInternetEnabled");
            isServerAvailable = savedInstanceState.getBoolean("isServerAvailable");

            dontShowGps = savedInstanceState.getBoolean("dontShowGps");
            dontShowNfc = savedInstanceState.getBoolean("dontShowNfc");
            dontShowNet = savedInstanceState.getBoolean("dontShowNet");
            dontShowSrv = savedInstanceState.getBoolean("dontShowSrv");

            isFileChecked = savedInstanceState.getBoolean("isFileChecked");

            UniqID = savedInstanceState.getString("uuid");
        } else {
            dontShowGps = false;
            dontShowNet = false;
            dontShowNfc = false;
            dontShowSrv = false;

            isGpsEnabled = false;
            isNfcEnabled = false;
            isInternetEnabled = false;
            isServerAvailable = false;
        }

        isFileChecked = false;

        description = null;
        currentLatitude = null;
        currentLongitude = null;
        currentTime = null;

        points = new ArrayList<>();
        temp_points = new ArrayList<>();
        listView = (ListView) findViewById(R.id.list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, points);
        listView.setAdapter(adapter);

        if (UniqID == null) {
            UniqID = getUniqueUserID();
        }

        if (!isFileChecked) {
            tryToSendFile();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        testGpsState();
        testNfcState();
        testInternetState();
        testServerState();

        delayBeforeInitState();
        if (isNfcEnabled) {
            listenForNfc();
        }
        if (isGpsEnabled) {
            initLocationProvider();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle onSavedInstanceState) {
        onSavedInstanceState.putBoolean("isGpsEnabled", isGpsEnabled);
        onSavedInstanceState.putBoolean("isNfcEnabled", isNfcEnabled);
        onSavedInstanceState.putBoolean("isInternetEnabled", isInternetEnabled);
        onSavedInstanceState.putBoolean("isServerAvailable", isServerAvailable);
        onSavedInstanceState.putBoolean("isGpsDialogShown", isGpsDialogShown);
        onSavedInstanceState.putBoolean("isNfcDialogShown", isNfcDialogShown);
        onSavedInstanceState.putBoolean("isNetDialogShown", isNetDialogShown);
        onSavedInstanceState.putBoolean("isSrvDialogShown", isSrvDialogShown);
        onSavedInstanceState.putString("uuid", UniqID);

        onSavedInstanceState.putBoolean("dontShowGps", dontShowGps);
        onSavedInstanceState.putBoolean("dontShowNfc", dontShowNfc);
        onSavedInstanceState.putBoolean("dontShowNet", dontShowNet);
        onSavedInstanceState.putBoolean("dontShowSrv", dontShowSrv);

        onSavedInstanceState.putBoolean("isFileChecked", isFileChecked);

        super.onSaveInstanceState(onSavedInstanceState);
    }


    //#endregion

    //#region Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.exit_item:
                nfcAdapter.disableForegroundDispatch(this);
                locationManager.removeUpdates(locationListener);
                disableWiFi();
                finishAndRemoveTask();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //#endregion  Creation

    //#region Sensors State Initialization


    static public boolean isURLReachable(String url) {
        if (get(url) != null) {
            return true;
        }
        return false;
    }

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


    private boolean initNFC() {
        nfcManager = (NfcManager) this.getSystemService(Context.NFC_SERVICE);
        isNfcEnabled = false;
        try {
            nfcAdapter = nfcManager.getDefaultAdapter();
            if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                isNfcEnabled = true;
            }
        } catch (NullPointerException e) {
            Log.e("initNFC", e.getLocalizedMessage());
        }
        return isNfcEnabled;
    }

    private boolean initGPS() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        isGpsEnabled = false;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                isGpsEnabled = true;
            }
        } catch (NullPointerException e) {
            Log.e("initLocationManager", e.getLocalizedMessage());
        }
        return isGpsEnabled;
    }


    public void testInternetState() {
        Timer netTimer = new Timer();
        final Handler netHandler = new Handler();
        final TextView netStat = (TextView) findViewById(R.id.net_stat);
        netTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final String result;
                final int textColor;
                if (isURLReachable("http://google.com")) {
                    result = getString(R.string.ok);
                    textColor = getResources().getColor(R.color.color_success);
                    isInternetEnabled = true;
                } else {
                    result = getString(R.string.unavailable);
                    textColor = getResources().getColor(R.color.color_fail);
                    isInternetEnabled = false;
                }
                netHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        netStat.setText(result);
                        netStat.setTextColor(textColor);
                    }
                });
            }
        }, 0L, 5L * 1000);
    }

    public void testGpsState() {
        Timer gpsTimer = new Timer();
        final Handler gpsHandler = new Handler();
        final TextView gpsStat = (TextView) findViewById(R.id.gps_stat);
        gpsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final String result;
                final int textColor;
                if (initGPS()) {
                    result = getString(R.string.ok);
                    textColor = getResources().getColor(R.color.color_success);
                    isNfcEnabled = true;
                } else {
                    result = getString(R.string.unavailable);
                    textColor = getResources().getColor(R.color.color_fail);
                    isNfcEnabled = false;
                }
                gpsHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        gpsStat.setText(result);
                        gpsStat.setTextColor(textColor);
                    }
                });
            }
        }, 0L, 5L * 1000);
    }

    public void testNfcState() {
        Timer nfcTimer = new Timer();
        final Handler nfcHandler = new Handler();
        final TextView nfcStat = (TextView) findViewById(R.id.nfc_stat);
        nfcTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final String result;
                final int textColor;
                if (initNFC()) {
                    result = getString(R.string.ok);
                    textColor = getResources().getColor(R.color.color_success);
                    isNfcEnabled = true;
                } else {
                    result = getString(R.string.unavailable);
                    textColor = getResources().getColor(R.color.color_fail);
                    isNfcEnabled = false;
                }
                nfcHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        nfcStat.setText(result);
                        nfcStat.setTextColor(textColor);
                    }
                });
            }
        }, 0L, 5L * 1000);

    }

    public void testServerState() {
        Timer myTimer = new Timer(); // Создаем таймер
        final Handler uiHandler = new Handler();
        final TextView txtResult = (TextView)findViewById(R.id.srv_stat);
        myTimer.schedule(new TimerTask() { // Определяем задачу
            @Override
            public void run() {
                final String result;
                final int textColor;

                if (isURLReachable("http://points.temirtulpar.com/api/values")) {
                    result = getString(R.string.ok);
                    textColor = getResources().getColor(R.color.color_success);
                    isServerAvailable = true;
                }
                else {
                    result = getString(R.string.unavailable);
                    textColor = getResources().getColor(R.color.color_fail);
                    isServerAvailable = false;
                }
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        txtResult.setText(result);
                        txtResult.setTextColor(textColor);
                    }
                });
            }
        }, 0L, 10L * 1000);
    }

    private void listenForNfc() {
        try {
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter filter = new IntentFilter();
            filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
            filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
            filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filter}, this.techList);
        } catch (Exception e) {
            Log.e("listenForNfc", e.getLocalizedMessage());
            customSnackbar(e.getLocalizedMessage(), getString(R.string.ok));
        }
    }

    private void statusInit() {
        if (!dontShowGps) {
            if (!isGpsEnabled && !isGpsDialogShown) {
                customDialog(getString(R.string.warning), getString(R.string.gps_disabled), getString(R.string.settings), 2);
            }
        }

        if (!dontShowNet) {
            if (!isInternetEnabled && !isNetDialogShown) {
                customDialog(getString(R.string.warning), getString(R.string.net_disabled), getString(R.string.settings), 3);
            }
        }

        if (!dontShowNfc) {
            if (!isNfcEnabled && !isNfcDialogShown) {
                customDialog(getString(R.string.warning), getString(R.string.nfc_disabled), getString(R.string.settings), 1);
            }
        }

        if (!dontShowSrv) {
            if (!isServerAvailable && !isSrvDialogShown) {
                customDialog(getString(R.string.warning), getString(R.string.srv_disabled), getString(R.string.ok), 4);
            }
        }
    }

    //#endregion

    //#region Reading Tag
    @Override
    protected void onNewIntent(Intent intent) {

        if (isNfcEnabled) {
            String tag_data = "";
            String tag_id = "";

            Parcelable[] data = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (data != null) {
                try {
                    for (int i = 0; i < data.length; i++) {
                        NdefRecord[] recs = ((NdefMessage) data[i]).getRecords();
                        for (int j = 0; j < recs.length; j++) {
                            if (recs[j].getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(recs[j].getType(), NdefRecord.RTD_TEXT)) {
                                byte[] payload = recs[j].getPayload();
                                String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
                                int langCodeLen = payload[0] & 0077;
                                tag_data += (new String(payload, langCodeLen + 1, payload.length - langCodeLen - 1, textEncoding));
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("TagDispatch", e.getLocalizedMessage());
                }
            }
            if (tag_data != "") {
                description = tag_data;
                currentTime = getCurTime();
                if (currentLatitude != null && currentLongitude != null) {
                    if (isInternetEnabled && isServerAvailable) {
                        addExtendedPoint(description, currentLatitude, currentLongitude, currentTime, UniqID);
                        new UploadJsonTask().execute();
                    } else {
                        addExtendedPoint(description, currentLatitude, currentLongitude, currentTime, UniqID);
                        savePoints();
                    }
                } else {
                    customSnackbar(getString(R.string.location_is_null), getString(R.string.ok));
                }
                autoCloseDialog(tag_data, getString(R.string.tag_success));
            } else {
                if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
                    String nfcid = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
                    tag_id = nfcid;
                    autoCloseDialog(tag_id, getString(R.string.tag_unknown));
                } else {
                    autoCloseDialog(getString(R.string.failure), getString(R.string.tag_failure));
                }
            }
        } else {
            customSnackbar(getString(R.string.nfc_notification),getString(R.string.ok));
        }
    }

    private String ByteArrayToHexString(byte [] inarray) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";
        for(j = 0 ; j < inarray.length ; ++j)
        {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    //#endregion

    //#region Dialogs

    public void customDialog(String title, String message, String positiveButton, final int settingsId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNeutralButton(getString(R.string.dont_show),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (settingsId == 1)
                            dontShowNfc = true;
                        if (settingsId == 2)
                            dontShowGps = true;
                        if (settingsId == 3)
                            dontShowNet = true;
                        if (settingsId == 4)
                            dontShowSrv = true;
                    }
                });

        builder.setPositiveButton(positiveButton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        switch(settingsId) {
                            case 1:
                                startActivityForResult(new Intent(Settings.ACTION_NFC_SETTINGS), 0);
                                isNfcDialogShown = false;
                                break;
                            case 2:
                                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
                                isGpsDialogShown = false;
                                break;
                            case 3:
                                startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 0);
                                isNetDialogShown = false;
                                break;
                            case 4:
                                isSrvDialogShown = false;
                                break;
                            default:
                                startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 0);
                        }
                    }
                });
        builder.show();
        if (settingsId == 1)
            isNfcDialogShown = true;
        if (settingsId == 2)
            isGpsDialogShown = true;
        if (settingsId == 3)
            isNetDialogShown = true;
        if (settingsId == 4)
            isNetDialogShown = true;
    }

    public void autoCloseDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this);
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

    public void customSnackbar(String message, String button) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setAction(button, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
        snackbar.show();
    }

    //#endregion

    //#region Get Location

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            currentLatitude = location.convert(location.getLatitude(), location.FORMAT_DEGREES);
            currentLongitude = location.convert(location.getLongitude(), location.FORMAT_DEGREES);
            //currentTime = String.valueOf(location.getTime());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private void initLocationProvider() {
        Log.i(TAG, "initializeLocationProvider");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, locationListener);
            } catch (java.lang.SecurityException ex) {
                Log.e(TAG, "Fail to request location update, ignore", ex);
                customSnackbar(getString(R.string.location_failure), getString(R.string.ok));
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "Network provider does not exist, " + ex.getMessage());
                customSnackbar(getString(R.string.network_provider_failure), getString(R.string.ok));
            }
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, locationListener);
            } catch (java.lang.SecurityException ex) {
                Log.e(TAG, "Fail to request location update, ignore", ex);
                customSnackbar(getString(R.string.location_failure), getString(R.string.ok));
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "Gps provider does not exist " + ex.getMessage());
                customSnackbar(getString(R.string.gps_provider_failure), getString(R.string.ok));
            }
        } else {
            requestLocationPermissions();
        }
    }

    private void requestLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(findViewById(android.R.id.content), R.string.location_permission_required, Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
                }
            }).show();
        } else {
            Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied, Snackbar.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_granted, Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    //#endregion

    //#region JSON Operations

    public static void executeJson(String description, String latitude, String longitude, String time, String uniqID) {
        Map<String, String> ticket = new HashMap<>();
        ticket.put("Description", description);
        ticket.put("Ltt", latitude);
        ticket.put("Lng", longitude);
        ticket.put("TimeMS", time);
        ticket.put("IMEI", uniqID);
        String json = new GsonBuilder().create().toJson(ticket, Map.class);
        makeRequest("http://points.temirtulpar.com/api/values", json);
    }

    public static void executeJsonFromObject(Ticket tag) {
        Map<String, String> ticket = new HashMap<>();
        ticket.put("Description", tag.getmDescription());
        ticket.put("Ltt", tag.getmLatitude());
        ticket.put("Lng", tag.getmLongitude());
        ticket.put("TimeMS", tag.getmTime());
        ticket.put("IMEI", tag.getmUid());
        String json = new GsonBuilder().create().toJson(ticket, Map.class);
        makeRequest("http://points.temirtulpar.com/api/values", json);
    }

    public static HttpResponse makeRequest(String uri, String json) {
        try {
            HttpPost httpPost = new HttpPost(uri);

            httpPost.setEntity(new StringEntity(json, "UTF-8"));
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            return new DefaultHttpClient().execute(httpPost);
        } catch (UnsupportedEncodingException e) {
            Log.e("makeRequest_uee", e.getLocalizedMessage());
            //e.printStackTrace();
        } catch (ClientProtocolException e) {
            Log.e("makeRequest_cpe", e.getLocalizedMessage());
            //e.printStackTrace();
        } catch (IOException e) {
            Log.e("makeRequest_io", e.getLocalizedMessage());
            //e.printStackTrace();
        }
        return null;
    }

    private class UploadJsonTask extends AsyncTask<URL, Integer, String> {
        @Override
        protected String doInBackground(URL... urls) {
            executeJson(description, currentLatitude, currentLongitude, currentTime, UniqID);
            return null;
        }
    }

    private class UploadOldJsonTask extends AsyncTask<Ticket, Integer, String>{
        @Override
        protected String doInBackground(Ticket... urls) {
            executeJsonFromObject(urls[0]);
            return null;
        }
    }


    //#endregion

    //#region Operations with Points


    private void addExtendedPoint(String name, String latit, String longit, String time, String uuid) {
        Ticket point = new Ticket(name, latit, longit, time, uuid);
        points.add(point);
        adapter.notifyDataSetChanged();
    }

    private void savePoints() {
        try {
            JSONHelper.exportToJSON(this, points);
        } catch (Exception e) {
            Log.e("savePoints", e.getLocalizedMessage());
        }
    }

    private void openPoints() {
        try {
            temp_points = JSONHelper.importFromJSON(this);
            if (temp_points != null) {
                points = temp_points;
                adapter.notifyDataSetChanged();
            } else {
                customSnackbar(getString(R.string.restore_failed), getString(R.string.ok));
            }
        } catch (Exception e) {
            Log.e("openPoints", e.getLocalizedMessage());
        }
    }

    private void deleteFile() {
        try {
            boolean result = JSONHelper.deleteThisFuckingFile(this);
            if (result) {
                points.clear();
                adapter.notifyDataSetChanged();
                customSnackbar(getString(R.string.file_deleted), getString(R.string.ok));
            } else {
                customSnackbar(getString(R.string.file_did_not_deleted), getString(R.string.ok));
            }
        } catch (Exception e) {
            Log.e("deleteFile", e.getLocalizedMessage());
        }
    }

    //#endregion

    //#region Miscellaneous

    private void checkFileExistance() {
        try {
            openPoints();
            if (points != null || points.size() < 1) {
                for (Ticket t : points) {
                    new UploadOldJsonTask().execute(t);
                }
                deleteFile();
                isFileChecked = true;
            }
        } catch (Exception e) {
            Log.e("checkFile", e.getLocalizedMessage());
        }
    }

    private String getCurTime() {
        long value = System.currentTimeMillis();
        String date = String.valueOf(value);
        return date;
    }

    private String converteTime(long value) {
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date(value);
        String formatted = format.format(date);
        return formatted;
    }

    private String getUniqueUserID() {
        String uuid;
        uuid = UUID.randomUUID().toString();
        return uuid;
    }

    public void tryToSendFile() {
        Timer sendTimer = new Timer();
        sendTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isInternetEnabled && isServerAvailable && !isFileChecked ) {
                    checkFileExistance();
                }
            }
        }, 0L, 15L * 1000);
    }

    public void delayBeforeInitState() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                statusInit();
            }
        }, 2000);
    }

    private void disableWiFi() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {
            wifi.setWifiEnabled(false);
        }
    }

    //#endregion

}




























