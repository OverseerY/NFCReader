package com.yaroslav.newfuckingtestapp;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class JSONParsing {

    private class ParseTask extends AsyncTask<Void, Void, String> {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String resultJson = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                //URL url = new URL(url_json);

                //urlConnection = (HttpURLConnection) url.openConnection();
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

            Log.d("JSONParse", strJson);

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
                    //tagItems.add(tagItem);
                    //adapter.notifyDataSetChanged();
                }

            } catch (JSONException e) {
                Log.e("onPostExecute", e.getLocalizedMessage());
                //Log.e("onPostExecute_extended", e.getStackTrace().toString());
            }
        }
    }
}
