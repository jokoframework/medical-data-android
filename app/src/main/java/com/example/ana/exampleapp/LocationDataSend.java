package com.example.ana.exampleapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.sql.Timestamp;
import java.util.Calendar;
import static android.content.ContentValues.TAG;


/**
 * Created by joaquin on 22/05/17.
 */

//In charge to store location data in "Locations" collections...

public class LocationDataSend extends AsyncTask<Context, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Context... contexts) {
        SharedPreferences settings =
                contexts[0].getSharedPreferences(Variables.PREFS_NAME, Context.MODE_PRIVATE);
        try {
            MongoClientURI mongoClientURI = new MongoClientURI(Variables.MONGO_URI);
            MongoClient mongoClient = new MongoClient(mongoClientURI);
            MongoDatabase dbMongo = mongoClient.getDatabase(mongoClientURI.getDatabase());
            MongoCollection<Document> coll = dbMongo.getCollection("locations");
            Calendar cal = Calendar.getInstance();
            Timestamp now = new Timestamp(cal.getTime().getTime());
            ObjectId userId = new ObjectId(settings.getString("user_id", ""));

            Document document = new Document();
            document.append("user_id", userId);
            document.append("latitud", GpsService.getNlocation().getLatitude());
            document.append("longitud", GpsService.getNlocation().getLongitude());
            document.append("onDate", now.toString());

            coll.insertOne(document);

            mongoClient.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG,String.format("Error en la carga de datos a la MongoDb %s", e.getMessage()),e);
            return false;
        }
    }

}
