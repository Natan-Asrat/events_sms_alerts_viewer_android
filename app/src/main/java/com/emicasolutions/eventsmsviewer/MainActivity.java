package com.emicasolutions.eventsmsviewer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView latestSmsTextView, detectedCodeTextView;
    private ImageView smsImageView;
    private RecyclerView recyclerView;
    private SmsAdapter smsAdapter;
    private List<SMS> smsList;
    private String lastSmsHash;
    private static final int SMS_PERMISSION_CODE = 100;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    public SharedPreferences sharedPreferences;
    public static WebView locationRequesterWebview = null;
    LoadingDialog loadingDialog;
    Button requestPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        latestSmsTextView = findViewById(R.id.latestSmsTextView);
        detectedCodeTextView = findViewById(R.id.detected_code);
        smsImageView = findViewById(R.id.smsImageView);
        recyclerView = findViewById(R.id.recyclerView);
        loadingDialog = new LoadingDialog();
        requestPermission = findViewById(R.id.request_permission);
        // Set up RecyclerView
        Button settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
        requestPermission.setOnClickListener(view -> {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS},
                    SMS_PERMISSION_CODE);


        });
        if(checkSmsPermission()){
            loadSMS();

        };



    }
    @Override
    protected void onStart() {
        super.onStart();
        if(checkSmsPermission()){
            loadSMS();

        };

    }
    private void registerSmsReceiver() {
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, filter);
    }
    private void loadSMS(){
        smsList = SMS.getSmsWithDetectedCodeKeyword(this);
        smsAdapter = new SmsAdapter(smsList, this, loadingDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(smsAdapter);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);

        // Register the BroadcastReceiver to listen for incoming SMS
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, filter);

        // Load last SMS hash to avoid duplicates
        lastSmsHash = sharedPreferences.getString("last_sms_hash", null);

    }


    private boolean checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {

            // Request both permissions (READ and RECEIVE SMS)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS}, SMS_PERMISSION_CODE);

            // Show permission request button if permission is not yet granted
            requestPermission.setVisibility(View.VISIBLE);
            return false;
        } else {
            // Permissions already granted, proceed with SMS handling
            requestPermission.setVisibility(View.INVISIBLE);
            registerSmsReceiver(); // Ensure you register the SMS receiver after permissions are granted
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // SMS permissions granted
                registerSmsReceiver();
                loadSMS(); // Only load SMS after permission is granted
            } else {
                // Permission denied
                Toast.makeText(this, "SMS permissions are required to read SMS messages", Toast.LENGTH_SHORT).show();
                requestPermission.setOnClickListener(view -> {
                    openAppSettings();
                    Toast.makeText(this, "Click on permissions > SMS, Location", Toast.LENGTH_SHORT).show();

                });
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (locationRequesterWebview != null) {
                    locationRequesterWebview.reload();
                }
            } else {
                // Location permission denied
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    // BroadcastReceiver to listen for incoming SMS
    private final BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String messageBody = smsMessage.getMessageBody();

                        // Process the SMS content and extract necessary info (e.g., the YAML format you mentioned)
                        processNewSms(messageBody);
                    }
                }
            }
        }
    };

    // Process the incoming SMS, forward if necessary, and update the UI
    private void processNewSms(String messageBody) {
        // Example: Extract detected code, hash, and other info from the message body (YAML parsing logic should go here)
        SMS newSms = SMS.parseSmsMessage(messageBody);

        // Check if this SMS is new based on its hash
        if (newSms != null && !newSms.getHash().equals(lastSmsHash)) {
            // Forward SMS based on settings
            checkAndForwardNewAlert(newSms);

            // Save this SMS hash to prevent future forwarding
            lastSmsHash = newSms.getHash();
            sharedPreferences.edit().putString("last_sms_hash", lastSmsHash).apply();

            // Update UI with the new SMS
            updateUIWithNewSms(newSms);
        }
    }




    // Check and forward only if the latest alert is new
    private void checkAndForwardNewAlert(SMS sms) {
        String forwardNumbers = sharedPreferences.getString("forward_numbers", null);
        boolean forwardSms = sharedPreferences.getBoolean("forward_sms", false);

        if (forwardSms && forwardNumbers != null) {
            // Forward SMS logic here
            forwardSmsToNumbers(forwardNumbers, sms);
        }
    }

    // Forward SMS to the provided numbers
    private void forwardSmsToNumbers(String numbers, SMS sms) {
        // Implement actual SMS forwarding logic here (use SMSManager or appropriate APIs)
        Toast.makeText(this, "Forwarding SMS to: " + numbers, Toast.LENGTH_SHORT).show();
    }
    private void loadImageFromAssets(String imageId, ImageView imageView) {
        AssetManager assetManager = imageView.getContext().getAssets();

        try {
            // Check if the GIF exists in the assets folder
            InputStream inputStream = assetManager.open("images/" + imageId + ".jpg");
            inputStream.close(); // Close stream if found

            // Construct the path to the GIF in assets
            String imagePath = "file:///android_asset/images/" + imageId + ".jpg";

            // Load the GIF using Glide
            Glide.with(imageView.getContext())
                    .load(imagePath)
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(24)))
                    .transition(DrawableTransitionOptions.withCrossFade()) // Optional: add a cross-fade animation

                    .into(imageView);
        } catch (IOException e) {
            // Handle the case where the GIF is not found (optional)
            Toast.makeText(imageView.getContext(), "Respective image not found! ImageId: " + imageId, Toast.LENGTH_SHORT).show();
        }
    }

    // Update the UI with the new SMS details
    private void updateUIWithNewSms(SMS sms) {
        detectedCodeTextView.setText(sms.getDetectedSound());
        String code = sms.getDetectedCode();
        // Set the image (replace with actual image logic)
        loadImageFromAssets(code, smsImageView);

        // Show the latest alert details
        findViewById(R.id.latest_alert_container).setVisibility(android.view.View.VISIBLE);

        // Add to list and notify the adapter
        smsList.add(0, sms); // Add the new SMS to the top of the list
        smsAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the SMS BroadcastReceiver when the activity is destroyed
        unregisterReceiver(smsReceiver);
    }
}
