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
import android.os.Environment;
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
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
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

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TEST";

    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 1f;

    private static final int PERMISSION_REQUEST_LOCATION = 0;
    private static final int PERMISSION_REQUEST_IMEI = 0;
    //private static final int PERMISSION_REQUEST_STORAGE_WRITE = 0;
    //private static final int PERMISSION_REQUEST_STORAGE_READ = 0;

    private String currentTime;
    private String description;
    private String currentLatitude;
    private String currentLongitude;
    private String IMEI;

    private boolean isGpsEnabled;
    private boolean isNfcEnabled;
    private boolean isInternetEnabled;

    private boolean isGpsDialogShown;
    private boolean isNfcDialogShown;
    private boolean isNetDialogShown;

    //private static final String FILENAME = "tags_list.dat";

    TextView gps_result;
    TextView nfc_result;
    TextView internet_result;

    TextView tag_info;

    LocationManager locationManager;
    NfcManager nfcManager;
    NfcAdapter nfcAdapter;
    ConnectivityManager connectivityManager;

    ArrayAdapter<Ticket> adapter;
    //ArrayList<Ticket> tickets = new ArrayList();
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tag_info = (TextView) findViewById(R.id.tag_info);

        gps_result = (TextView) findViewById(R.id.gps_stat);
        nfc_result = (TextView) findViewById(R.id.nfc_stat);
        internet_result = (TextView) findViewById(R.id.net_stat);

        isGpsDialogShown = false;
        isNfcDialogShown = false;
        isNetDialogShown = false;

        description = null;
        currentLatitude = null;
        currentLongitude = null;
        currentTime = null;

        points = new ArrayList<>();
        listView = (ListView) findViewById(R.id.list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, points);
        listView.setAdapter(adapter);

        IMEI = null;
        IMEI = getImei();

        openPoints();
    }

    @Override
    protected void onResume() {
        super.onResume();

        statusInit();
        initLocationProvider();
        listenForNfc();
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onStop(){
        super.onStop();
        savePoints();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        //savedInstanceState.putStringArrayList("tickets", tickets);
    }

    private void listenForNfc() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[] {filter}, this.techList);
    }

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
                    if (!isInternetEnabled) {
                        //tickets.add(new Ticket(description, currentLatitude, currentLongitude, currentTime, IMEI));
                        addPoint(description, converteTime(Long.parseLong(currentTime)));
                        //customSnackbar("Tag <" + description + "> has been saved", getString(R.string.ok));
                    } else {
                        //new UploadJsonTask().execute();
                        addPoint(description, converteTime(Long.parseLong(currentTime))); /**TO DO: delete this row before release */
                        //customSnackbar("Tag <" + description + "> has been sent", getString(R.string.ok));
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

    private void statusInit() {
        if (initializeLocationManager()) {
            gps_result.setText(R.string.ok);
        } else {
            gps_result.setText(R.string.fail);
            if (!isGpsDialogShown)
                customDialog(getString(R.string.warning), getString(R.string.gps_disabled), getString(R.string.cancel), getString(R.string.settings), 2);
        }

        if (initializeInternet()) {
            internet_result.setText(R.string.ok);
        } else {
            internet_result.setText(R.string.fail);
            if (!isNetDialogShown)
                customDialog(getString(R.string.warning), getString(R.string.net_disabled), getString(R.string.cancel), getString(R.string.settings), 3);
        }

        if (initializeNfc()) {
            nfc_result.setText(R.string.ok);
        } else {
            nfc_result.setText(R.string.fail);
            if (!isNfcDialogShown)
                customDialog(getString(R.string.warning), getString(R.string.nfc_disabled), getString(R.string.cancel), getString(R.string.settings), 1);
        }
    }

    private boolean initializeLocationManager() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                isGpsEnabled = true;
            } else {
                isGpsEnabled = false;
            }
        } catch (NullPointerException e) {
            Log.e("initLocationManager", e.getLocalizedMessage());
            isGpsEnabled = false;
        }
        return isGpsEnabled;
    }

    private boolean initializeNfc() {
        nfcManager = (NfcManager) this.getSystemService(Context.NFC_SERVICE);
        try {
            nfcAdapter = nfcManager.getDefaultAdapter();
            if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                isNfcEnabled = true;
            } else {
                isNfcEnabled = false;
            }
        } catch (NullPointerException e) {
            Log.e("initNFC", e.getLocalizedMessage());
            isNfcEnabled = false;
        }
        return isNfcEnabled;
    }

    private boolean initializeInternet() {
        connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        String netAddress = null;
        isInternetEnabled = false;
        try {
            if (connectivityManager.getActiveNetworkInfo() != null) {
                try {
                    netAddress = new NetTask().execute("www.google.com").get();
                    if (netAddress != null) {
                        isInternetEnabled = true;
                    }
                } catch (Exception e) {
                    Log.e("initializeInternet", e.getLocalizedMessage());
                    isInternetEnabled = false;
                }
            }
        } catch (NullPointerException e) {
            Log.e("initInternet", e.getLocalizedMessage());
            isInternetEnabled = false;
        }
        return isInternetEnabled;
    }

    public static class NetTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            InetAddress address = null;
            try {
                address = InetAddress.getByName(params[0]);
            } catch (UnknownHostException e) {
                Log.e("UnknownHostException", e.getLocalizedMessage());
            }
            return address.getHostAddress();
        }
    }

    //=============================================================================


    public void customDialog(String title, String message, String negativeButton, String positiveButton, final int settingsId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                MainActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNegativeButton(negativeButton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        if (settingsId == 1)
                            isNfcDialogShown = false;
                        if (settingsId == 2)
                            isGpsDialogShown = false;
                        if (settingsId == 3)
                            isNetDialogShown = false;
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
    }

    //================================================================================

    public void customSnackbar(String message, String button) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setAction(button, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
        snackbar.show();
    }

    //=====================================================================================

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
        Log.e(TAG, "initializeLocationProvider");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, locationListener);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "Fail to request location update, ignore", ex);
                customSnackbar(getString(R.string.location_failure), getString(R.string.ok));
                //Toast.makeText(this, getString(R.string.location_failure), Toast.LENGTH_SHORT).show();
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "Network provider does not exist, " + ex.getMessage());
                customSnackbar(getString(R.string.provider_failure), getString(R.string.ok));
                //Toast.makeText(this, "Network provider does not exist", Toast.LENGTH_SHORT);
            }
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, locationListener);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "Fail to request location update, ignore", ex);
                customSnackbar(getString(R.string.location_failure), getString(R.string.ok));
                //Toast.makeText(this, "Fail to request location update", Toast.LENGTH_SHORT).show();
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "Gps provider does not exist " + ex.getMessage());
                customSnackbar(getString(R.string.provider_failure), getString(R.string.ok));
                //Toast.makeText(this, "Gps provider does not exist", Toast.LENGTH_SHORT).show();
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

    private String getImei() {
        String imei = null;
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(getApplicationContext().TELEPHONY_SERVICE);
            imei = telephonyManager.getDeviceId();
        } else {
            requestImeiPermission();
        }
        return imei;
    }


    private void requestImeiPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_PHONE_STATE)) {
            Snackbar.make(findViewById(android.R.id.content), R.string.imei_permission_required, Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_IMEI);
                }
            }).show();
        } else {
            Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_IMEI);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_IMEI) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_granted, Snackbar.LENGTH_SHORT).show();
                IMEI = getImei();
            } else {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied, Snackbar.LENGTH_SHORT).show();
            }
        }
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_granted, Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied, Snackbar.LENGTH_SHORT).show();
            }
        }
        /*
        if (requestCode == PERMISSION_REQUEST_STORAGE_WRITE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_granted, Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied, Snackbar.LENGTH_SHORT).show();
            }
        }
        if (requestCode == PERMISSION_REQUEST_STORAGE_READ) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_granted, Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied, Snackbar.LENGTH_SHORT).show();
            }
        }
        */
    }

    public static void executeJson(String description, String latitude, String longitude, String time, String imei) {
        Map<String, String> ticket = new HashMap<String, String>();
        ticket.put("Description", description);
        ticket.put("Ltt", latitude);
        ticket.put("Lng", longitude);
        ticket.put("TimeMS", time);
        ticket.put("IMEI", imei);
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
            executeJson(description, currentLatitude, currentLongitude, currentTime, IMEI);
            return null;
        }
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

    //========================================================================================
    /**
    private void serializeThat() {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        File file = new File(path, "/" + FILENAME);
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            Log.i(TAG, "serializeThat");
            if (!tickets.isEmpty()) {
                oos.writeObject(tickets);
                customSnackbar(getString(R.string.file_written), getString(R.string.ok));
            } else {
                customSnackbar(getString(R.string.list_empty), getString(R.string.ok));
            }
        } catch (Exception e) {
            Log.e("serializeThat", e.getLocalizedMessage());
            autoCloseDialog(getString(R.string.error),e.getLocalizedMessage());
            //tagReadDialog(getString(R.string.error),e.getLocalizedMessage(),getString(R.string.ok));
        }
    }


    private void deserializeThat() {
        ArrayList<Ticket> newtickets;
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        File file = new File(path, "/" + FILENAME);
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            newtickets = (ArrayList<Ticket>) ois.readObject();
            for (Ticket t : newtickets) {
                String temp = tag_info.getText().toString();
                tag_info.setText(temp + "\n" + t.getmDescription() + "; " + converteTime(Long.parseLong(t.getmTime())) + "\n");
            }
        } catch (Exception e) {
            Log.e("deserializeThat", e.getMessage());
            autoCloseDialog(getString(R.string.error), e.getLocalizedMessage());
            //tagReadDialog(getString(R.string.error), e.getLocalizedMessage(), getString(R.string.ok));
        }
    }


    public void onClickWrite(View view) {
        if (tickets != null) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                serializeThat();
            } else {
                requestStorageWritePermissions();
            }
        } else {
            customSnackbar(getString(R.string.empy_obj), getString(R.string.ok));
        }
    }
    public void onClickRead(View view) {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            try {
                deserializeThat();
            } catch (Exception e) {
                Log.e("onClickRead", e.getLocalizedMessage());
                //tagReadDialog(getString(R.string.error), getString(R.string.file_empty), getString(R.string.ok));
                //autoCloseDialog(getString(R.string.error), getString(R.string.file_empty));
                customSnackbar(getString(R.string.file_empty),getString(R.string.ok));
            }
        } else {
           requestStorageReadPermissions();
        }
    }


    private void requestStorageWritePermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(findViewById(android.R.id.content), R.string.write_permission_required, Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE_WRITE);
                }
            }).show();
        } else {
            Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied, Snackbar.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE_WRITE);
        }
    }

    private void requestStorageReadPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Snackbar.make(findViewById(android.R.id.content), R.string.read_permission_required, Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE_READ);
                }
            }).show();
        } else {
            Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied, Snackbar.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE_READ);
        }
    }
    */

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

    private void addPoint(String name, String time) {
        Ticket point = new Ticket(name, time);
        points.add(point);
        adapter.notifyDataSetChanged();
    }

    private void savePoints() {
        boolean result = JSONHelper.exportToJSON(this, points);
        if (result) {
            customSnackbar(getString(R.string.file_written), getString(R.string.ok));
        } else {
            customSnackbar(getString(R.string.file_not_written), getString(R.string.ok));
        }
    }

    private void openPoints() {
        points = JSONHelper.importFromJSON(this);
        if (points != null) {
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, points);
            listView.setAdapter(adapter);
            customSnackbar(getString(R.string.data_restored), getString(R.string.ok));
        } else {
            customSnackbar(getString(R.string.restore_failed), getString(R.string.ok));
        }
    }


    private void deleteFile() {
       boolean result = JSONHelper.deleteThisFuckingFile(this);
        if (result) {
            customSnackbar(getString(R.string.file_deleted), getString(R.string.ok));
        } else {
            customSnackbar(getString(R.string.file_did_not_deleted), getString(R.string.ok));
        }
    }

    public void onClickSave(View view) {
        savePoints();
    }

    public void onClickOpen(View view) {
        openPoints();
    }

    public void onClickClear(View view) {
        deleteFile();
    }
}




























