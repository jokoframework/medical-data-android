package com.example.ana.exampleapp;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import static android.content.ContentValues.TAG;


public class GpsService extends Service implements LocationListener {


    private static Location nlocation;
    private LocationManager locationManager;

    @Override
    public void onCreate() {
        gpsManager();
        try {
            LocationDataSend runner = new LocationDataSend();
            runner.execute(this);
        } catch (Exception e) {
            Log.e(TAG,String.format("Error en ejecucion del servicio GpsService %s", e.getMessage()),e);
        }
        stopSelf();
    }


    public static Location getNlocation() {
        return nlocation;
    }

    public static void setNlocation(Location nlocation) {
        GpsService.nlocation = nlocation;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        setNlocation(location);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        // Not needed in application structure
    }

    @Override
    public void onProviderEnabled(String s) {
        // Not needed in application structure
    }

    @Override
    public void onProviderDisabled(String s) {
        Toast.makeText(getApplicationContext(), "Your GPS is disabled! Is Needed for better service", Toast.LENGTH_LONG).show();
        Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    public void gpsManager() {
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        //noinspection MissingPermission
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Variables.timeToUpdateLocationMilli, 0, this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        setNlocation(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
    }

}
