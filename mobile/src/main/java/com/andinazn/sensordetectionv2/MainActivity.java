/*
*** Created by Andina Zahra Nabilla on 10 April 2018
*** Created by Astrid Gloria on 30 April 2018
*** Created by Kalista Umari on 2 May 2018
*
* Active Sensor: Heartrate & Accelerometer Monitor
* Build Gradle Version: 2.3.0
* Mobile SDK Version: 25.0.3
* Wear SDK Version: 25.0.0
*
* Activity berfungsi untuk:
* 1. Mengaktifkan Navigation Drawer Fragment
* 2. Mengaktifkan Remote Sensor Manager
* 3. Mengaktifkan Firebase
* 4. Penarikan Nilai Age ke MainActivity
* 5. Pengiriman START_TIME melalui Broadcast Intent
* 6. Upload HR Value, Fall State, dan HR State ke Firebase Dalam Bentuk Array
* 7. Mendeklarasikan Button Sebagai Trigger Activity Dimulai
* 8. Menerima Broadcast Intent Berisi Data Sensor Dari Sensor Receiver Service
* 9. Memproses Heart Rate Abnormality State
* 10. Menunjukkan Notifikasi Darurat Pada Aplikasi User
* 11. Pengiriman Pesan Darurat Melalui SMS Gateway
* 12. Fungsi getDate Sebagai TimeStamp Yang Ditunjukan Sebagai TextView
* 13. Activity Recognition dari data accelerometer yang didapat
* 14. Kesimpulan Fall
*
* Done right:
* + Hasil pengiriman sensor muncul di logcat
* + Sensor data heartrate muncul di TextView
* + Sensor accelerometer diterima bukan 0 kalau fall detected (accelerometer)
* + Fall detected processing (accelerometer)
* + SMS gateway (accelerometer)
* + Show notification fall detected (accelerometer)
* + Data heartrate masuk ke firebase (heartrate)
* + Heartrate processing (heartrate)
* + SMS gateway (heartrate abnormality)
* + Show notification heartrate abnormality & overtired (heartrate)
* + Accelerometer dan heartrate monitor bisa aktif bersamaan (intent broadcast)
* + Login registrasi sukses
* + Input umur ke firebase dan dipanggil kembali untuk processing
* + Upload state fall & heartrate abnormality detected ke firebase
* + Upload hr, state, gps ke firebase secara array child dari child (Astrid)
* + Timestamp udah bener
* + GPS
*
* Need fixing:
* - Delay penerimaan fall detected (seconds) = 45 60 122 - 28 78 45 27 10 59 (5x tangan biar efektif)
* - Input no HP ke firebase (astrid)
* - Max 590 records (20 menit)
 */

package com.andinazn.sensordetectionv2;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.andinazn.sensordetectionv2.database.DatabaseHandler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private static RemoteSensorManager remoteSensorManager;
    FirebaseAuth auth;
    FirebaseUser user;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //2. Mengaktifkan Remote Sensor Manager
        remoteSensorManager = RemoteSensorManager.getInstance(this);

        //1. Mengaktifkan Navigation Drawer Fragment
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
    }

    //amira position 1
    //amira signout
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (position == 0) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                    .commit();
        } else if (position == 1) {

            Intent intent = new Intent(MainActivity.this, MyGPS.class);
            startActivity(intent);
        }
        else if (position == 2) {
            if (user != null) {
                auth.signOut();
                finish();
                Intent intent = new Intent(MainActivity.this, PilihanMenuAwal.class);
                startActivity(intent);
            }
        }

    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        TextView hrTxt;
        TextView falldetectionTxt;
        TextView lastSyncTxt;
        Switch mSwitch;

        long lastMeasurementTime = 0L;
        boolean isRunning = false;
        boolean isStop = false;

        boolean fallconfirmation = false;

        int stateactivity;



        FirebaseAuth auth;
        FirebaseUser user;

        //astrid v-1
        DatabaseReference databaseUser;
        DatabaseReference ref;
        DatabaseReference ref1;
        DatabaseReference ref2;
        List<CreateHR> hrlist;
        List<CreateHRState> hrstatelist;
        List<CreateFallState> fallstatelist;
        Calendar calendar;
        String Date, Time, Timestamp, Time2, Timestamp2, Time3, Timestamp3;
        SimpleDateFormat simpleDateFormat, simpleDateFormat1, simpleDateFormat2, simpleDateFormat3, simpleDateFormat4, simpleDateFormat5, simpleDateFormat6;


        //Input age for heartrate processing
        public int ageValue2;
        public String eN1, eN2, eN3, eN4, eN5;
        ValueEventListener listener;
        DatabaseReference dref;

        ArrayList<String> familyNumber;


        //astrid
        private final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            mSwitch = (Switch) rootView.findViewById(R.id.hrSwitch);
            lastSyncTxt = (TextView) rootView.findViewById(R.id.lastSyncTxt);

            //Show to textview
            hrTxt = (TextView) rootView.findViewById(R.id.hrTxt);
            falldetectionTxt = (TextView) rootView.findViewById(R.id.falldetectionTxt);

            familyNumber = new ArrayList<>();



            //6. Upload HR Value, Fall State, dan HR State ke Firebase Dalam Bentuk Array
            databaseUser = FirebaseDatabase.getInstance().getReference().child("Users");
            auth = FirebaseAuth.getInstance();
            user = auth.getCurrentUser();
            ref = databaseUser.child(user.getUid()).child("hrvalue");
            ref1 = databaseUser.child(user.getUid()).child("fallstate");
            ref2= databaseUser.child(user.getUid()).child("hrstate");

            hrlist = new ArrayList<>();
            hrstatelist = new ArrayList<>();
            fallstatelist = new ArrayList<>();
            mSwitch.setOnCheckedChangeListener(checkBtnChange);
            dref = databaseUser.child(user.getUid());

            listener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot ds) {
                    ageValue2 = ds.child("age").getValue(Integer.class);
                    Log.i("user age frag", "user age " + ageValue2);
                    eN1 = ds.child("emergencynumber1").getValue(String.class);
                    Log.i("emergency number 1", "eN1 " + eN1);
                    eN2 = ds.child("emergencynumber2").getValue(String.class);
                    Log.i("emergency number 2", "eN2 " + eN2);
                    eN3 = ds.child("emergencynumber3").getValue(String.class);
                    Log.i("emergency number 3", "eN3 " + eN3);
                    eN4 = ds.child("emergencynumber4").getValue(String.class);
                    Log.i("emergency number 4", "eN2 " + eN4);
                    eN5 = ds.child("emergencynumber5").getValue(String.class);
                    Log.i("emergency number 5", "eN5 " + eN5);

                    if ((ageValue2 != 0) && (eN1 != null) && (eN2 != null) && (eN3 != null) && (eN4 != null) && (eN5 != null)) {
                        dref.removeEventListener(listener);
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            dref.addValueEventListener(listener);





            //5. Pengiriman START_TIME melalui Broadcast Intent
            getActivity().registerReceiver(mMessageReceiver, new IntentFilter("com.example.Broadcast"));
            Intent intent = new Intent();
            intent.setAction("com.example.Broadcast");
            intent.putExtra("START_TIME", 0L); // clear millisec time
            getActivity().sendBroadcast(intent);
            return rootView;





        }

        //7. Mendeklarasikan Button Sebagai Trigger Activity Dimulai
        CompoundButton.OnCheckedChangeListener checkBtnChange = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    lastMeasurementTime = System.currentTimeMillis();
                    remoteSensorManager.startMeasurement();
                    Intent intent = new Intent();
                    intent.setAction("com.example.Broadcast1");
                    intent.putExtra("START_TIME", lastMeasurementTime); // get current millisec time
                    getActivity().sendBroadcast(intent);
                    lastSyncTxt.setText(String.valueOf(getDate()));
                    SharedPreferences pref = getActivity().getSharedPreferences("START_TIME", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putLong("START_TIME", lastMeasurementTime);
                    editor.apply();


                } else {
                    Intent intent = new Intent();
                    intent.setAction("com.example.Broadcast1");
                    intent.putExtra("START_TIME", 0L); // clear millisec time
                    getActivity().sendBroadcast(intent);
                    lastSyncTxt.setText("");
                    remoteSensorManager.stopMeasurement();
                    SharedPreferences pref = getActivity().getSharedPreferences("START_TIME", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putLong("START_TIME", 0L);
                    editor.apply();
                }
            }
        };


        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }

        // handler for received Intents for the "my-event" event
        private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Extract data included in the Intent
                try {

                    isRunning = intent.getBooleanExtra("IS_RUNNING",false);
                    if (!isRunning) {
                        //Thread.sleep(3000);
                        isStop = true;
                    } else {
                        isStop = false;
                    }
                    if (mSwitch != null) {
                        if (isStop) {
                            mSwitch.setChecked(isRunning);
                        }
                    }

                    //8. Menerima Broadcast Intent Berisi Data Sensor Dari Sensor Receiver Service
                    int message2 = intent.getIntExtra("ACCR", 0);
                    int sensorType = intent.getIntExtra("SENSOR_TYPE", 0);

                    fallconfirmation = intent.getBooleanExtra("FALLSTATE",false);

                    //ACCELEROMETER DATA

                    //Receive accelerometer data
                    float[] message3 = intent.getFloatArrayExtra("CURRENT");
                    //Accelerometer processing into fall detection conclusion
                    if (sensorType == 1) {
                        float tmpX = (int)Math.ceil(message3[0]);
                        float tmpY = (int)Math.ceil(message3[1]);
                        float tmpZ = (int)Math.ceil(message3[2]);

                        float currentacc [] = {tmpX, tmpY, tmpZ};
                        Log.d("Receiver", "Got Accelerometer: " + Arrays.toString(currentacc) + ". Got Accuracy: " + message2);

                        if ((tmpX == 1000)&&(tmpY == 1000)&&(tmpZ == 1000)) {
                            stateactivity = 1; //Resting
                        }

                        if ((tmpX == 2000)&&(tmpY == 2000)&&(tmpZ == 2000)) {
                            stateactivity = 2; //Moderate
                        }

                        if ((tmpX == 3000)&&(tmpY == 3000)&&(tmpZ == 3000)) {
                            stateactivity = 3; //Vigorous
                        }

                        //14. Kesimpulan Fall

                        if (fallconfirmation == false) {

                            falldetectionTxt.setText("No fall Detected.");

                        } else {
                            Log.d("Fall", "fall detected");
                            falldetectionTxt.setText("Fall Detected.");


                            //10. Menunjukkan Notifikasi Darurat Pada Aplikasi User
                            showNotification();

                            String fallstate = "FALL DETECTED";

                            //6. Upload HR Value, Fall State, dan HR State ke Firebase Dalam Bentuk Array
                            CreateFallState kondisifall = new CreateFallState(fallstate, Date, Time, Timestamp);
                            ref1.child("nilaifallstate").push().setValue(kondisifall);
                            calendar = Calendar.getInstance();
                            simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                            simpleDateFormat1 = new SimpleDateFormat("dd-MM-yyyy");
                            simpleDateFormat2 = new SimpleDateFormat("HH:mm:ss");
                            Timestamp = simpleDateFormat.format(calendar.getTime());
                            Time = simpleDateFormat2.format(calendar.getTime());
                            Date = simpleDateFormat1.format(calendar.getTime());


                            //11. Pengiriman Pesan Darurat Melalui SMS Gateway

                            //Check permission for SMS gateway
                            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
                            } else {
                                SmsManager sms = SmsManager.getDefault();
                                String message = "FALL DETECTED! A fall has been detected on your family member, open your Ambient Assisted Living app to see their location!";

                                Log.i("family number", "fall familynumber size" + familyNumber.size());
                                Log.i("family number", "fall familynumber value " + familyNumber);
                                if ((eN1 != null) && (eN1.length() > 3)){
                                    sms.sendTextMessage(eN1, null, message, null, null);}
                                if ((eN2 != null) && (eN2.length() > 3)){
                                    sms.sendTextMessage(eN2, null, message, null, null);}
                                if ((eN3 != null) && (eN3.length() > 3)){
                                    sms.sendTextMessage(eN3, null, message, null, null);}
                                if ((eN4 != null) && (eN4.length() > 3)){
                                    sms.sendTextMessage(eN4, null, message, null, null);}
                                if ((eN5 != null) && (eN5.length() > 3)){
                                    sms.sendTextMessage(eN5, null, message, null, null);}

                            }

                        }

                        DatabaseHandler db = new DatabaseHandler(getActivity());
                        long timeStamp = intent.getLongExtra("TIME", 0)/1000000L;
                        lastSyncTxt.setText(String.valueOf(getDate()) + " / " + db.getAllUserMonitorDataByLastMeasurementTime(lastMeasurementTime).size() + " records");
                    }



                    //HEARTRATE DATA
                    //Receive heartrate data
                    float[] message1 = intent.getFloatArrayExtra("HR");
                    if ((message1 != null ) && (sensorType == 21)) {
                        Log.d("Receiver", "Got HR: " + message1[0] + ". Got Accuracy: " + message2);
                        int tmpHr = (int)Math.ceil(message1[0] - 0.5f);
                        if (tmpHr > 0) {
                            hrTxt.setText(String.valueOf(tmpHr));


                            //6. Upload HR Value, Fall State, dan HR State ke Firebase Dalam Bentuk Array
                            CreateHR hrval = new CreateHR(tmpHr, Date, Time, Timestamp);
                            ref.child("nilaihr").push().setValue(hrval);
                            calendar = Calendar.getInstance();
                            simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                            simpleDateFormat3 = new SimpleDateFormat("dd-MM-yyyy");
                            simpleDateFormat4 = new SimpleDateFormat("HH:mm:ss");
                            Timestamp = simpleDateFormat.format(calendar.getTime());
                            Date = simpleDateFormat3.format(calendar.getTime());
                            Time = simpleDateFormat4.format(calendar.getTime());


                            //9. Memproses Heart Rate Abnormality State
                            if (tmpHr < 60.0) {
                                Log.i("abnormality detected", "HEART EMERGENCY, Got HR: " + tmpHr);

                                //10. Menunjukkan Notifikasi Darurat Pada Aplikasi User
                                showNotificationHRemergency();
                                String hrstate = "HEARTRATE ABNORMAL";

                                //6. Upload HR Value, Fall State, dan HR State ke Firebase Dalam Bentuk Array
                                CreateHRState kondisihr = new CreateHRState(tmpHr, hrstate, Date, Time, Timestamp);
                                ref2.child("nilaihrstate").push().setValue(kondisihr);
                                calendar = Calendar.getInstance();
                                simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                                simpleDateFormat5 = new SimpleDateFormat("dd-MM-yyyy");
                                simpleDateFormat6 = new SimpleDateFormat("HH:mm:ss");
                                Timestamp = simpleDateFormat.format(calendar.getTime());
                                Time = simpleDateFormat6.format(calendar.getTime());
                                Date = simpleDateFormat5.format(calendar.getTime());


                                //11. Pengiriman Pesan Darurat Melalui SMS Gateway
                                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(getActivity(),
                                            new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
                                } else {
                                    SmsManager sms = SmsManager.getDefault();
                                    String message = "HEARTRATE ABNORMALITY DETECTED! A heart rate abnormality on your family member has been detected, open your Ambient Assisted Living app to check their location!";

                                    Log.i("family number", "familynumber size" + familyNumber.size());
                                    if ((eN1 != null) && (eN1.length() > 3)){
                                        sms.sendTextMessage(eN1, null, message, null, null);}
                                    if ((eN2 != null) && (eN2.length() > 3)){
                                        sms.sendTextMessage(eN2, null, message, null, null);}
                                    if ((eN3 != null) && (eN3.length() > 3)){
                                        sms.sendTextMessage(eN3, null, message, null, null);}
                                    if ((eN4 != null) && (eN4.length() > 3)){
                                        sms.sendTextMessage(eN4, null, message, null, null);}
                                    if ((eN5 != null) && (eN5.length() > 3)){
                                        sms.sendTextMessage(eN5, null, message, null, null);}


                                }
                            }

                            //13. Activity Recognition dari data accelerometer yang didapat

                            if (stateactivity == 1) {
                                Log.i("AI Activity conclusion", "Activity Conclusion: Resting Activity, State: " + stateactivity);
                                //9. Memproses Heart Rate Abnormality State
                                if ((tmpHr < 60.0) || (tmpHr > 100.0)) {
                                    Log.i("abnormality detected", "HEART EMERGENCY, Got HR: " + tmpHr);

                                    //10. Menunjukkan Notifikasi Darurat Pada Aplikasi User
                                    showNotificationHRemergency();
                                    String hrstate = "HEARTRATE ABNORMAL";

                                    //6. Upload HR Value, Fall State, dan HR State ke Firebase Dalam Bentuk Array
                                    CreateHRState kondisihr = new CreateHRState(tmpHr, hrstate, Date, Time, Timestamp);
                                    ref2.child("nilaihrstate").push().setValue(kondisihr);
                                    calendar = Calendar.getInstance();
                                    simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                                    simpleDateFormat5 = new SimpleDateFormat("dd-MM-yyyy");
                                    simpleDateFormat6 = new SimpleDateFormat("HH:mm:ss");
                                    Timestamp = simpleDateFormat.format(calendar.getTime());
                                    Time = simpleDateFormat6.format(calendar.getTime());
                                    Date = simpleDateFormat5.format(calendar.getTime());



                                    //11. Pengiriman Pesan Darurat Melalui SMS Gateway
                                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                        ActivityCompat.requestPermissions(getActivity(),
                                                new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
                                    } else {
                                        SmsManager sms = SmsManager.getDefault();
                                        String message = "HEARTRATE ABNORMALITY DETECTED! A heart rate abnormality on your family member has been detected, open your Ambient Assisted Living app to check their location!";

                                        Log.i("family number", "familynumber size" + familyNumber.size());
                                        if ((eN1 != null) && (eN1.length() > 3)){
                                            sms.sendTextMessage(eN1, null, message, null, null);}
                                        if ((eN2 != null) && (eN2.length() > 3)){
                                            sms.sendTextMessage(eN2, null, message, null, null);}
                                        if ((eN3 != null) && (eN3.length() > 3)){
                                            sms.sendTextMessage(eN3, null, message, null, null);}
                                        if ((eN4 != null) && (eN4.length() > 3)){
                                            sms.sendTextMessage(eN4, null, message, null, null);}
                                        if ((eN5 != null) && (eN5.length() > 3)){
                                            sms.sendTextMessage(eN5, null, message, null, null);}


                                    }

                                }
                            }

                            if (stateactivity == 2) {
                                Log.i("AI Activity conclusion", "Activity Conclusion: Moderate Activity, State: " + stateactivity);
                                //9. Memproses Heart Rate Abnormality State
                                if (ageValue2 != 0) {
                                    if (tmpHr > ((220 - ageValue2) * 0.7)) {
                                        Log.i("user age process", "user age " + ageValue2 + ", hr max " + ((220 - ageValue2) * 0.7));
                                        Log.i("abnormality detected", "YOU ARE TIRED, Got HR: " + tmpHr);

                                        //10. Menunjukkan Notifikasi Darurat Pada Aplikasi User
                                        showNotificationHRwarning();
                                    }
                                }

                            }

                            if (stateactivity == 3) {
                                Log.i("AI Activity conclusion", "Activity Conclusion: Vigorous Activity, State: " + stateactivity);
                                //9. Memproses Heart Rate Abnormality State
                                if (ageValue2 != 0) {
                                    if (tmpHr > ((220 - ageValue2) * 0.85)) {
                                        Log.i("user age process", "user age " + ageValue2 + ", hr max " + ((220 - ageValue2) * 0.85));
                                        Log.i("abnormality detected", "YOU ARE TIRED, Got HR: " + tmpHr);

                                        //10. Menunjukkan Notifikasi Darurat Pada Aplikasi User
                                        showNotificationHRwarning();
                                    }
                                }
                            }


                        }

                        DatabaseHandler db = new DatabaseHandler(getActivity());
                        long timeStamp = intent.getLongExtra("TIME", 0)/1000000L;
                        lastSyncTxt.setText(String.valueOf(getDate()) + " / " + db.getAllUserMonitorDataByLastMeasurementTime(lastMeasurementTime).size() + " records");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        public void showNotification() {
            final NotificationManager mgr = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder note = new NotificationCompat.Builder(getContext());
            note.setContentTitle("FALL ALERT!");
            note.setContentText("A fall has been detected.");
            note.setTicker("FALL ALERT!");
            note.setAutoCancel(true);
            note.setPriority(Notification.PRIORITY_HIGH);
            note.setVibrate(new long[] {0, 100, 100, 100});
            note.setDefaults(Notification.DEFAULT_SOUND);
            note.setSmallIcon(R.mipmap.ic_launcher);
            PendingIntent pi = PendingIntent.getActivity(getContext(), 0, new Intent(getActivity(), MainActivity.class), 0);
            note.setContentIntent(pi);
            mgr.notify(693020, note.build());
        }

        public void showNotificationHRemergency() {
            final NotificationManager mgr1 = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder note = new NotificationCompat.Builder(getContext());
            note.setContentTitle("HEART RATE EMERGENCY!");
            note.setContentText("An abnormality in your heart rate has been detected");
            note.setTicker("HEART RATE EMERGENCY!");
            note.setAutoCancel(true);
            note.setPriority(Notification.PRIORITY_HIGH);
            note.setVibrate(new long[] {0, 100, 100, 100});
            note.setDefaults(Notification.DEFAULT_SOUND);
            note.setSmallIcon(R.mipmap.ic_launcher);
            PendingIntent pi = PendingIntent.getActivity(getContext(), 0, new Intent(getActivity(), MainActivity.class), 0);
            note.setContentIntent(pi);
            mgr1.notify(693020, note.build());
        }

        public void showNotificationHRwarning() {
            final NotificationManager mgr2 = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder note = new NotificationCompat.Builder(getContext());
            note.setContentTitle("PLEASE TAKE A REST!");
            note.setContentText("You've overworked yourself. Please take a rest!");
            note.setTicker("PLEASE TAKE A REST!");
            note.setPriority(Notification.PRIORITY_HIGH);
            note.setVibrate(new long[] {0, 100, 100, 100});
            note.setDefaults(Notification.DEFAULT_SOUND);
            note.setSmallIcon(R.mipmap.ic_launcher);
            PendingIntent pi = PendingIntent.getActivity(getContext(), 0, new Intent(getActivity(), MainActivity.class), 0);
            note.setContentIntent(pi);
            mgr2.notify(960302, note.build());
        }

        //12. Fungsi getDate Sebagai TimeStamp Yang Ditunjukan Sebagai TextView
        private String getDate(){
            try{
                DateFormat sdf = new SimpleDateFormat("dd cc yy HH:mm a");
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
                Date netDate = (new Date());
                return sdf.format(netDate);
            }
            catch(Exception ex){
                return "7:00";
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        //BusProvider.getInstance().register(this);
        //List<Sensor> sensors = RemoteSensorManager.getInstance(this).getSensors();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //BusProvider.getInstance().unregister(this);
    }
}
