package com.example.ana.exampleapp;

import java.util.Calendar;
import java.util.Date;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

import static com.mongodb.client.model.Filters.eq;


/**
 * Main activity of the app. It shows a view that creates a {@link RegisterActivity} if it is the
 * first that the app is used. Otherwise a view to introduce the PIN is shown and a
 * {@link TestActivity} is created when the correct PIN is provided.
 *
 * @author Ana María Martínez Gómez
 */


public class MainActivity extends AppCompatActivity {
    //settings, mDbHelper, readable_db and projection are used repeatedly
    GPSManager gps;
    SharedPreferences settings;
    SQLiteDatabase readable_db;
    String[] projection = {FeedTestContract.FeedEntry.COLUMN_NAME_TIMESTAMP};
    long startTotalTime = -1;
    long startTime = -1;
    int pinTries = 0;
    EditText pinEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(Variables.PREFS_NAME, Context.MODE_PRIVATE);
        FeedTestDbHelper mDbHelper = new FeedTestDbHelper(this);
        readable_db = mDbHelper.getReadableDatabase();
        setMainView();
        runtimePermissions();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        startTotalTime = -1;
        startTime = -1;
        pinTries = 0;
        setMainView();
    }

    /**
     * Set the appropriate view taking into account if it is the first time that app is used and
     * the database last entry.
     */
    private void setMainView() {
        boolean firstTime = settings.getBoolean("firstTime", true);
        if (firstTime) {
            setContentView(R.layout.activity_main_first_time);
        } else {
            setContentView(R.layout.activity_main);

            Cursor c = readable_db.query(
                    FeedTestContract.FeedEntry.TABLE_NAME, projection,
                    null,
                    null,
                    null,
                    null,
                    FeedTestContract.FeedEntry._ID + " DESC", "1");
            boolean moved = c.moveToFirst(); // false if it is empty
            if (moved && FeedTestContract.isToday(c.getString(0))) {
                // Test has already been filled
                Button button = (Button) findViewById(R.id.button_start);
                button.setText(getString(R.string.start_change));
            }

            pinEditText = (EditText) findViewById(R.id.pin);
            TextWatcher tw = new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (startTotalTime == -1 && pinEditText.length() == 1) {
                        startTotalTime = System.nanoTime(); // current timestamp in nanoseconds
                        startTime = System.nanoTime(); // current timestamp in nanoseconds
                    } else if (startTime == -1 && pinEditText.length() == 1) {
                        startTime = System.nanoTime(); // current timestamp in nanoseconds
                    }
                }
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // TextWatcher need the declaration of this functions, but not used.
                }
                @Override
                public void afterTextChanged(Editable s) {
                    // TextWatcher need the declaration of this functions, but not used.
                }
            };
            pinEditText.addTextChangedListener(tw);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean firstTime = settings.getBoolean("firstTime", true);
        menu.findItem(R.id.profile).setVisible(!firstTime);
        menu.findItem(R.id.profile).setEnabled(!firstTime);
        menu.findItem(R.id.configuration).setVisible(!firstTime);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Inflate the menu; this adds items to the action bar if it isn't the first time the app is
        used
         */
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Intent intent;
        switch (item.getItemId()) {
            case R.id.profile:
                intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                return true;
            case R.id.configuration:
                intent = new Intent(this, ConfigurationActivity.class);
                startActivity(intent);
                return true;
            case R.id.information:
                intent = new Intent(this, InformationActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Check if the introduced PIN is correct and in that case saves the time spent to do it and
     * creates a {@link TestActivity}. Otherwise it set an error on the PIN {@link EditText}.
     *
     * @param view the {@link View} that calls the method
     */
    public void btnStart(View view) {
        int pin = settings.getInt("pin", 0);
        String pinText = pinEditText.getText().toString();
        pinTries++;
        if (!"".equals(pinText) && pin == Integer.parseInt(pinText)) {
            //The PIN is correct
            int pin_time = (int) ((System.nanoTime() - startTime) / 1000000); // in milliseconds
            int pin_time_total = (int) ((System.nanoTime() - startTotalTime) / 1000000); // in milliseconds
            // get service GPS...
            gps = new GPSManager(this);
            if (gps.canGetLocation){
                Intent intent = new Intent(this, TestActivity.class);
                intent.putExtra("PIN_TIME", pin_time);
                intent.putExtra("PIN_TIME_TOTAL", pin_time_total);
                intent.putExtra("PIN_TRIES", pinTries);
                startActivity(intent);
            }

        } else {
            startTime = -1;
            pinEditText.setText("");
            pinEditText.setError(getString(R.string.pin_error));
        }
    }

    /**
     * Checks if the email is in the database and the password is correct and, in that case,
     * download the user information from the database and creates a {@link FinishRegisterActivity}
     * to confirm that the sign up process has been completed successfully. It also checks that
     * there is internet connection before trying to connect with the database.If the data
     * introduced is not correct or there is any problem while connecting with the database the user
     * is informed using a {@link Toast} message.
     *
     * @param view the {@link View} clicked
     */
    public void btnSignIn(View view) {
        boolean error = false;
        EditText email = (EditText) findViewById(R.id.email_answer);
        String emailText = email.getText().toString();
        if ("".equals(emailText)) {
            email.setError(getString(R.string.email_blank));
            email.requestFocus();
            error = true;
        }
        EditText pin = (EditText) findViewById(R.id.pin_answer);
        String pinText = pin.getText().toString();
        if ("".equals(pinText)) {
            pin.setError(getString(R.string.pin_blank));
            if (!error) pin.requestFocus();
            error = true;
        }
            if (Variables.connection(this) < 0 && !error)
                Toast.makeText(this, R.string.no_internet, Toast.LENGTH_LONG).show();
            else {
                int pinNumber = Integer.parseInt(pin.getText().toString());
                User user = new User(emailText, pinNumber);

                //Save register in the server database
                try {
                    DownloadRegistration runner = new DownloadRegistration();
                    runner.execute(user);
                    int option = runner.get();
                    if (option == 0) {
                        //Save register in the app
                        user.save(this);
                        // Feedback: register has been completed
                        Intent intent = new Intent(this, FinishRegisterActivity.class);
                        startActivity(intent);
                    } else if (option == 1) {
                        Toast.makeText(this, R.string.wrong_data, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.mongodb_error, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e("Error en registro",String.format("The register couldn't be completed. Check that you have internet connexion and try it again later%s", e.getMessage()),e);
                    Toast.makeText(this, R.string.register_error, Toast.LENGTH_LONG).show();
                }
            }
    }

    /**
     * Creates a {@link RegisterActivity} if there is internet connection. Otherwise a {@link Toast}
     * message is showed to informed that internet connection is needed.
     *
     * @param view the {@link View} clicked
     */
    public void btnSignUp(View view) {
        if (Variables.connection(this) < 0)
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_LONG).show();
        else {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        }
    }


    /**
     * This class is used to check that a user exits in the MongoDB database and download his
     * information from database after signing in.
     *
     * @author Ana María Martínez Gómez
     */
    private class DownloadRegistration extends AsyncTask<User, Void, Integer> {
        @Override
        protected Integer doInBackground(User... params) {
            try {
                MongoClientURI mongoClientURI = new MongoClientURI(Variables.MONGO_URI);
                MongoClient mongoClient = new MongoClient(mongoClientURI);
                MongoDatabase dbMongo = mongoClient.getDatabase(mongoClientURI.getDatabase());
                MongoCollection<Document> coll = dbMongo.getCollection("users");
                User localUser = params[0];
                Document user = coll.find(eq("email", localUser.getEmail())).first();
                mongoClient.close();
                if (user == null || !(user.get("pin").equals(localUser.getPin()))) {
                    return 1; // Wrong data
                }
                Date d = (Date) user.get("birthDate");
                Calendar cal = Calendar.getInstance();
                cal.setTime(d);
                // WARNING: Calendar.MONTH starts in 0 Calendar.DAY_OF_MONTH starts in 1
                localUser.completeSignIn((String) user.get("name"), cal.get(Calendar.DAY_OF_MONTH) - 1, cal.get(Calendar.MONTH), cal.get(Calendar.YEAR), (Boolean) user.get("gender"), user.getObjectId("_id").toString());
                return 0; //Successfully saved
            } catch (Exception e) {
                Log.e("Error al registrar",String.format("Error en DownloadRegistration %s", e.getMessage()),e);
                return 2; // Error
            }
        }
    }

    private boolean runtimePermissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},100);

            return true;
        }
        return false;
    }


}
