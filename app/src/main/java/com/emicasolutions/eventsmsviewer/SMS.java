package com.emicasolutions.eventsmsviewer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SMS {
    private String detectedSound;
    private String detectedCode;
    private String hash;
    private String forwardedFrom;
    private boolean isForwarded;
    private boolean hasLocation;
    String  latitude;
    String  longitude;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    long id;

    public SMS(String detectedSound, String detectedCode, String hash, boolean isForwarded, String forwardedFrom, boolean hasLocation, String latitude, String longitude) {
        this.detectedSound = detectedSound;
        this.detectedCode = detectedCode;
        this.hash = hash;
        this.isForwarded = isForwarded;
        this.forwardedFrom = forwardedFrom;
        this.hasLocation = hasLocation;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getDetectedSound() {
        return detectedSound;
    }

    public String getDetectedCode() {
        return detectedCode;
    }

    public String getHash() {
        return hash;
    }

    public boolean isForwarded() {
        return isForwarded;
    }

    public String getForwardedFrom() {
        return forwardedFrom;
    }

    public boolean hasLocation() {
        return hasLocation;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }
    public static List<SMS> getSmsWithDetectedCodeKeyword(Context context) {
        List<SMS> smsList = new ArrayList<>();

        // Define the URI for SMS content
        Uri smsUri = Uri.parse("content://sms/inbox");

        // Define the projection (columns you want to fetch)
        String[] projection = new String[]{"_id", "address", "body", "date"};

        // Define the selection (query condition) to look for the "detected_code" string
        String selection = "body LIKE ?";
        String[] selectionArgs = new String[]{"%detected_code%"};

        // Query the SMS inbox using ContentResolver
        Cursor cursor = context.getContentResolver().query(smsUri, projection, selection, selectionArgs, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Extract the SMS details
                long id = cursor.getLong(cursor.getColumnIndex("_id"));

                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

                // Extract the relevant data from the SMS body (e.g., YAML parsing)
                SMS sms = SMS.parseSmsMessage(body);

                if (sms != null) {
                    sms.setId(id);
                    smsList.add(sms);
                }
            } while (cursor.moveToNext());

            cursor.close();
        }

        return smsList;
    }
    public static SMS parseSmsMessage(String messageBody) {
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(messageBody);

        String detectedSound = (String) data.getOrDefault("detected_sound", "Unknown");
        String detectedCode = String.valueOf(data.getOrDefault("detected_code", ""));
        String hash = (String) data.getOrDefault("hash", "");

        boolean isForwarded = data.containsKey("forwarded_from");
        String forwardedFrom = isForwarded ? (String) data.get("forwarded_from") : null;

        boolean hasLocation = false;
        String latitude = "0.0";
        String longitude = "0.0";

        Object locationObj = data.get("location");
        if (locationObj instanceof Map) {
            Map<String, Object> locationMap = (Map<String, Object>) locationObj;
            hasLocation = !locationMap.isEmpty();
            latitude = locationMap.getOrDefault("latitude", "0.0").toString();
            longitude = locationMap.getOrDefault("longitude", "0.0").toString();
        }

        return new SMS(detectedSound, detectedCode, hash, isForwarded, forwardedFrom, hasLocation, latitude, longitude);
    }


}
