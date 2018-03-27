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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
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
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TEST";

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private static final int FILE_FOR_LISTVIEW = 1;

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

    TextView gps_result;
    TextView nfc_result;
    TextView internet_result;

    LocationManager locationManager;
    NfcManager nfcManager;
    NfcAdapter nfcAdapter;

    ArrayAdapter<Ticket> adapter;
    List<Ticket> points;
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

        changeStateIfDisabled();

        if (savedInstanceState != null) {
            isGpsDialogShown = savedInstanceState.getBoolean("isGpsDialogShown");
            isNfcDialogShown = savedInstanceState.getBoolean("isNfcDialogShown");
            isNetDialogShown = savedInstanceState.getBoolean("isNetDialogShown");
            isSrvDialogShown = savedInstanceState.getBoolean("isSrvDialogShown");

            isGpsEnabled = savedInstanceState.getBoolean("isGpsEnabled");
            isNfcEnabled = savedInstanceState.getBoolean("isNfcEnabled");
            isInternetEnabled = savedInstanceState.getBoolean("isInternetEnabled");
            isServerAvailable = savedInstanceState.getBoolean("isServerAvailable");

            UniqID = savedInstanceState.getString("uuid");
        }

        gps_result = (TextView) findViewById(R.id.gps_stat);
        nfc_result = (TextView) findViewById(R.id.nfc_stat);
        internet_result = (TextView) findViewById(R.id.net_stat);

        description = null;
        currentLatitude = null;
        currentLongitude = null;
        currentTime = null;

        points = new ArrayList<>();
        listView = (ListView) findViewById(R.id.list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, points);
        listView.setAdapter(adapter);

        if (UniqID == null) {
            UniqID = getUniqueUserID();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        testGpsState();
        testNfcState();
        testInternetState();
        testServerState();

        statusInit();
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

        super.onSaveInstanceState(onSavedInstanceState);
    }


    @Override
    protected void onPause() {
        super.onPause();
        /*
        if (isNfcEnabled) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            nfcAdapter.disableForegroundDispatch(this);
        }*/
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
            case R.id.open_item:
                openPoints();
                return true;
            case R.id.save_item:
                savePoints();
                return true;
            case R.id.delete_item:
                deleteFile();
                //deleteTicket();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //#endregion  Creation

    //#region Sensors State Initialization

    static public boolean isURLReachable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            try {
                URL url = new URL("http://points.temirtulpar.com/api/values");   // Change to "http://google.com" for www  test.
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(10 * 1000);          // 10 s.
                urlc.connect();
                if (urlc.getResponseCode() == 200) {        // 200 = "OK" code (http connection is fine).
                    Log.wtf("Connection", "Success !");
                    return true;
                } else {
                    return false;
                }
            } catch (MalformedURLException ex) {
                Log.e("isURLReachable", ex.getLocalizedMessage());
                return false;
            } catch (IOException e) {
                Log.e("isURLReachable", e.getLocalizedMessage());
                return false;
            }
        }
        return false;
    }

    static public boolean initInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            try {
                URL url = new URL("http://google.com");   // Change to "http://google.com" for www  test.
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(5 * 1000);          // 5 s.
                urlc.connect();
                if (urlc.getResponseCode() == 200) {        // 200 = "OK" code (http connection is fine).
                    Log.wtf("Connection", "Success !");
                    return true;
                } else {
                    return false;
                }
            } catch (MalformedURLException ex) {
                Log.e("isURLReachable", ex.getLocalizedMessage());
                return false;
            } catch (IOException e) {
                Log.e("isURLReachable", e.getLocalizedMessage());
                return false;
            }
        }
        return false;
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
                if (initInternet(getApplicationContext())) {
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
                if (isURLReachable(getApplicationContext())) {
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

    private void changeStateIfDisabled() {
        WifiManager wifi;
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifi.isWifiEnabled())
            wifi.setWifiEnabled(true);//Turn on Wifi
    }

    private void statusInit() {
        if (!isGpsEnabled && !isGpsDialogShown) {
            customDialog(getString(R.string.warning), getString(R.string.gps_disabled), getString(R.string.settings), 2);
        }

        if (!isInternetEnabled && !isNetDialogShown) {
            customDialog(getString(R.string.warning), getString(R.string.net_disabled), getString(R.string.settings), 3);
        }

        if (!isNfcEnabled && !isNfcDialogShown) {
            customDialog(getString(R.string.warning), getString(R.string.nfc_disabled), getString(R.string.settings), 1);
        }

        if (!isServerAvailable && !isSrvDialogShown) {
            customDialog(getString(R.string.warning), getString(R.string.srv_disabled), getString(R.string.dont_show), 4);
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
                if (currentLatitude != null && currentLongitude != null) {
                    if (isInternetEnabled && isServerAvailable) {
                        addPoint(description, converteTime(Long.parseLong(currentTime)));
                        new UploadJsonTask().execute();
                    } else {
                        addPoint(description, converteTime(Long.parseLong(currentTime)));
                        //addTicket(description, currentLatitude, currentLongitude, currentTime, IMEI);
                        savePoints();
                        //saveTickets();
                        //customSnackbar("Tag <" + description + "> has been saved", getString(R.string.ok));
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

    private String getCurTime() {
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        String formatted = format.format(date);
        return formatted;
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
    //#endregion

    //#region Dialogs

    public void customDialog(String title, String message, String positiveButton, final int settingsId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNeutralButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (settingsId == 1)
                            isNfcDialogShown = true;
                        if (settingsId == 2)
                            isGpsDialogShown = true;
                        if (settingsId == 3)
                            isNetDialogShown = true;
                        if (settingsId == 4)
                            isNetDialogShown = true;
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

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            currentLatitude = location.convert(location.getLatitude(), location.FORMAT_DEGREES);
            currentLongitude = location.convert(location.getLongitude(), location.FORMAT_DEGREES);
            currentTime = String.valueOf(location.getTime());
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
        Map<String, String> ticket = new HashMap<String, String>();
        ticket.put("Description", description);
        ticket.put("Ltt", latitude);
        ticket.put("Lng", longitude);
        ticket.put("TimeMS", time);
        ticket.put("IMEI", uniqID);
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
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class UploadJsonTask extends AsyncTask<URL, Integer, String> {
        @Override
        protected String doInBackground(URL... urls) {
            executeJson(description, currentLatitude, currentLongitude, currentTime, UniqID);
            //customSnackbar("Tag <" + description + "> has been sent", getString(R.string.ok));
            return null;
        }
    }

    //#endregion

    //#region Operations with Points

    private void addPoint(String name, String time) {
        Ticket point = new Ticket(name, time);
        points.add(point);
        adapter.notifyDataSetChanged();
    }

    private void savePoints() {
        boolean result = JSONHelper.exportToJSON(this, points, FILE_FOR_LISTVIEW);
        if (result) {
            customSnackbar(getString(R.string.file_written), getString(R.string.ok));
        } else {
            customSnackbar(getString(R.string.file_not_written), getString(R.string.ok));
        }
    }

    private void openPoints() {
        points = JSONHelper.importFromJSON(this, FILE_FOR_LISTVIEW);
        if (points != null) {
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, points);
            listView.setAdapter(adapter);
            customSnackbar(getString(R.string.data_restored), getString(R.string.ok));
        } else {
            customSnackbar(getString(R.string.restore_failed), getString(R.string.ok));
        }
    }

    private void deleteFile() {
        boolean result = JSONHelper.deleteThisFuckingFile(this, FILE_FOR_LISTVIEW);
        if (result) {
            customSnackbar(getString(R.string.file_deleted), getString(R.string.ok));
        } else {
            customSnackbar(getString(R.string.file_did_not_deleted), getString(R.string.ok));
        }
    }

    //#endregion

}




























