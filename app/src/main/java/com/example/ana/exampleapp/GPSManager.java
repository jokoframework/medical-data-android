package com.example.ana.exampleapp;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import static android.content.ContentValues.TAG;


/**
 * Handling GPS
 *
 * @author Niels Jacot
 */

 // For stablish GPS service if is not enable...
public class GPSManager extends Service implements LocationListener {

    private final Context mContext;
    boolean isGPSEnabled = false;
    boolean canGetLocation = false;

    protected LocationManager locationManager;

    public GPSManager(Context context) {
        this.mContext = context;
        getServices();
    }
    
    public void getServices() {
        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (isGPSEnabled) {
                this.canGetLocation = true;
            } else {
                // houston we have a GPS issue
                showSettingsAlert();
            }

        } catch (Exception e) {
            Log.e(TAG,String.format("GPS not enable, can't get location%s", e.getMessage()),e);
        }
    }

    /**
     * Function to check GPS/wifi enabled
     *
     * @return boolean
     */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    /**
     * Function to show settings alert dialog On pressing Settings button will
     * lauch Settings Options
     */

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setTitle("GPS is turned off");
        alertDialog.setMessage("Location is turned off! Do you want to turn it on ?");

        alertDialog.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        mContext.startActivity(intent);
                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}