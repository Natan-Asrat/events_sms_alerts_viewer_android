package com.emicasolutions.eventsmsviewer;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.SmsViewHolder> {

    private List<SMS> smsList;
    private Context context;
    LoadingDialog loadingDialog;
    private LinearLayout lastDropdown=null;


    public SmsAdapter(List<SMS> smsList, Context context, LoadingDialog loadingDialog) {
        this.smsList = smsList;
        this.loadingDialog = loadingDialog;
        this.context = context;
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sms_item, parent, false);
        return new SmsViewHolder(view);
    }
    public void direction(String location, String name) {
        // Code to be executed when the function is called from JavaScript

        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + location + "(" + name + ")");

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        context.startActivity(mapIntent);
        Toast.makeText(context, "Click on Directions!", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        SMS sms = smsList.get(position);

        // Load Image from Assets based on detectedCode
        loadImageFromAssets(sms.getDetectedCode(), holder.smsImageView);

        // Set detected sound text
        holder.detectedSoundTextView.setText("Detected " + sms.getDetectedSound());
        // Handle location and directions button
        if (sms.hasLocation()) {
            holder.showMapButton.setEnabled(true);
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.directionButton.setOnClickListener(view1 -> {
                        String location = sms.getLatitude() + "," + sms.getLongitude();
                        direction(location, sms.getDetectedSound());

                    });
                    if(lastDropdown!=null){
                        lastDropdown.setVisibility(View.GONE);
                    }
                    holder.dropdownIcon.setRotation(lastDropdown == holder.mapContainer ? 0 : 180);  // Rotate the dropdown icon if expanded

                    if (lastDropdown == holder.mapContainer) {
                        holder.mapContainer.setVisibility(View.GONE);  // Hide the image
                        lastDropdown = null;

                    } else {
                        holder.mapContainer.setVisibility(View.VISIBLE); // Show the image
                        loadingDialog.show(((AppCompatActivity) context).getSupportFragmentManager(), "loading");
                        holder.locationWebView.setVisibility(View.VISIBLE);
                        holder.locationWebView.getSettings().setJavaScriptEnabled(true);
                        holder.locationWebView.addJavascriptInterface(new DirectionButtonInterface(context, loadingDialog), "Android");
                        holder.locationWebView.getSettings().setAllowFileAccess(true);
                        holder.locationWebView.getSettings().setAllowContentAccess(true);
                        holder.locationWebView.setWebViewClient(new WebViewClient());
                        holder.locationWebView.getSettings().setGeolocationEnabled(true);
                        String link = "file:///android_asset/nahom-map.html?lat=" + sms.getLatitude() + "&lng=" + sms.getLongitude() + "&sound=" + sms.getDetectedSound();
                        holder.locationWebView.loadUrl(link);
                        holder.locationWebView.setWebChromeClient(new WebChromeClient() {
                            @Override
                            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                                // Check if location permission is granted
                                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    // Request the permission
                                    MainActivity.locationRequesterWebview = holder.locationWebView;
                                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MainActivity.LOCATION_PERMISSION_REQUEST_CODE);
                                } else {
                                    // If permission is granted, allow location
                                    callback.invoke(origin, true, false);
                                }
                            }

                            @Override
                            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                                Log.d("console", consoleMessage.message() + " -- From line "
                                        + consoleMessage.lineNumber() + " of "
                                        + consoleMessage.sourceId());
                                return true;
                            }
                        });
                        lastDropdown = holder.mapContainer;

                        // You can load the image here, e.g., using Glide or Picasso, or set a specific image
                    }
                }
            };
            holder.itemView.setOnClickListener(listener);
            // On click, show location on map in WebView
            holder.showMapButton.setOnClickListener(listener);
        } else {
            // If no location, disable directions button
            holder.showMapButton.setEnabled(false);
            holder.showMapButton.setText("No Location Sent");
            holder.showMapButton.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.showMapButton.setOnClickListener(v ->
                    Toast.makeText(context, "No location associated with alert SMS", Toast.LENGTH_SHORT).show()
            );
        }

        // Reset WebView visibility based on saved state (initially hidden)
        holder.locationWebView.setVisibility(View.GONE);


    }

    @Override
    public int getItemCount() {
        return smsList.size();
    }

    // ViewHolder class
    public static class SmsViewHolder extends RecyclerView.ViewHolder {
        ImageView smsImageView;
        TextView detectedSoundTextView;
        Button showMapButton;
        WebView locationWebView;
        ImageView dropdownIcon;
        Button directionButton;
        LinearLayout mapContainer;

        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            smsImageView = itemView.findViewById(R.id.smsImageView);
            detectedSoundTextView = itemView.findViewById(R.id.detectedSoundTextView);
            showMapButton = itemView.findViewById(R.id.showMapButton);
                    directionButton = itemView.findViewById(R.id.directionsButton);
            locationWebView = itemView.findViewById(R.id.locationWebView);
            dropdownIcon = itemView.findViewById(R.id.dropdownIcon);
            mapContainer = itemView.findViewById(R.id.map_container);
        }
    }
    public void openSpecificSms(Context context, long smsId) {
        Uri uri = Uri.parse("content://sms/inbox/" + smsId);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        context.startActivity(intent);
    }

    // Method to load image from assets
    private void loadImageFromAssets(String imageId, ImageView imageView) {
        try {
            // Check if the image exists in the assets folder
            InputStream inputStream = context.getAssets().open("images/" + imageId + ".jpg");
            inputStream.close(); // Close stream if found

            // Load the image using Glide
            String imagePath = "file:///android_asset/images/" + imageId + ".jpg";
            Glide.with(imageView.getContext())
                    .load(imagePath)
                    .into(imageView);
        } catch (IOException e) {
            Toast.makeText(imageView.getContext(), "Respective image not found! ImageId: " + imageId, Toast.LENGTH_SHORT).show();
        }
    }

    // Method to generate Leaflet map URL based on latitude and longitude
    private String generateLeafletMapUrl(String latitude, String longitude) {
        return "https://www.openstreetmap.org/?mlat=" + latitude + "&mlon=" + longitude + "#map=16/" + latitude + "/" + longitude;
    }
}
