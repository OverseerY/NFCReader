package com.yaroslav.newfuckingtestapp;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class JSONHelper {
    private static final String FILE_POINTS = "data.json";
    private static final String FILE_TAGS = "tags.json";

    private static class DataItems {
        private List<Ticket> tickets;

        List<Ticket> getTickets() {
            return tickets;
        }

        void setTickets(List<Ticket> tickets) {
            this.tickets = tickets;
        }
    }

    static boolean deleteThisFuckingFile(Context context, int flag) {
        try {
            if (flag == 1) {
                context.deleteFile(FILE_POINTS);
                return true;
            }
            if (flag == 2) {
                context.deleteFile(FILE_TAGS);
                return true;
            }
        } catch (Exception e) {
            Log.e("json_del_file", e.getLocalizedMessage());
            //e.printStackTrace();
        }
        return false;
    }

    static boolean exportToJSON(Context context, List<Ticket> dataList, int flag) {
        Gson gson = new Gson();
        DataItems dataItems = new DataItems();
        dataItems.setTickets(dataList);
        String jsonString = gson.toJson(dataItems);

        FileOutputStream fileOutputStream = null;

        try {
            if (flag == 1) {
                fileOutputStream = context.openFileOutput(FILE_POINTS, Context.MODE_PRIVATE);
                fileOutputStream.write(jsonString.getBytes());
                return true;
            }
            if (flag == 2) {
                fileOutputStream = context.openFileOutput(FILE_TAGS, Context.MODE_PRIVATE);
                fileOutputStream.write(jsonString.getBytes());
                return true;
            }
        } catch (Exception e) {
            Log.e("json_export", e.getLocalizedMessage());
            //e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                }  catch (IOException e) {
                    Log.e("json_export_fos", e.getLocalizedMessage());
                    //e.printStackTrace();
                }
            }
        }
        return false;
    }

    static List<Ticket> importFromJSON(Context context, int flag) {
        InputStreamReader streamReader = null;
        FileInputStream fileInputStream = null;
        try {
            if (flag == 1) {
                fileInputStream = context.openFileInput(FILE_POINTS);
                streamReader = new InputStreamReader(fileInputStream);
                Gson gson = new Gson();
                DataItems dataItems = gson.fromJson(streamReader, DataItems.class);
                return dataItems.getTickets();
            }
            if (flag == 2) {
                fileInputStream = context.openFileInput(FILE_TAGS);
                streamReader = new InputStreamReader(fileInputStream);
                Gson gson = new Gson();
                DataItems dataItems = gson.fromJson(streamReader, DataItems.class);
                return dataItems.getTickets();
            }
        } catch (IOException ex) {
            Log.e("json_import", ex.getLocalizedMessage());
            //ex.printStackTrace();
        } finally {
            if (streamReader != null) {
                try {
                    streamReader.close();
                } catch (IOException e) {
                    Log.e("json_import_sr", e.getLocalizedMessage());
                    //e.printStackTrace();
                }
            }
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    Log.e("json_import_fis", e.getLocalizedMessage());
                    //e.printStackTrace();
                }
            }
        }
        return null;
    }
}









































