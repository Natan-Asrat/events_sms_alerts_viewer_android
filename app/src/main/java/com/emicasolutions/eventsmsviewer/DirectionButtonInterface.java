package com.emicasolutions.eventsmsviewer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

public class DirectionButtonInterface {
    private LoadingDialog loadingDialog;
    Context mContext;


    DirectionButtonInterface(Context c, LoadingDialog loadingDialog) {
        mContext = c;
        this.loadingDialog= loadingDialog;
        
    }

    @JavascriptInterface
    public void direction(String location, String name) {
        // Code to be executed when the function is called from JavaScript

        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + location + "(" + name + ")");

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        mContext.startActivity(mapIntent);
        Toast.makeText(mContext, "Click on Directions!", Toast.LENGTH_SHORT).show();
    }
    @JavascriptInterface
    public boolean checkLocation() {
        return ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    @JavascriptInterface
    public void finishedLoading() {
        Toast.makeText(mContext, "Finished Loading!", Toast.LENGTH_SHORT).show();
        loadingDialog.dismiss();
    }
}

