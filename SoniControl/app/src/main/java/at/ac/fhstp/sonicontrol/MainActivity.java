package at.ac.fhstp.sonicontrol;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;


public class MainActivity extends AppCompatActivity implements Scan.DetectionListener {
    private static final String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_ALL_PERMISSIONS = 42;
    private static final int NOTIFICATION_STATUS_REQUEST_CODE = 2;
    private static final int NOTIFICATION_DETECTION_REQUEST_CODE = 1;

    private static final String TAG = "MainActivity";
    static MainActivity mainIsMain;

    ImageButton btnStorLoc;
    ImageButton btnStart;
    ImageButton btnStop;
    ImageButton btnSettings;
    ImageButton btnExit;

    Button btnAlertStart;
    Button btnAlertSpoof;
    Button btnAlertDismissThisTime;
    Button btnAlertDismissAlways;

    Scan detector;
    Location locationFinder;
    JSONManager jsonMan = new JSONManager(this);

    AlertDialog alert;
    TextView txtSignalType;
    Technology sigType;
    View view;

    Random randomNotificationNumberGenerator = new Random();

    NotificationCompat.Builder detectionBuilder;
    NotificationManagerCompat mNotificationManager;
    int detectionNotificationId;

    NotificationCompat.Builder spoofingStatusBuilder;
    int spoofingStatusNotificationId = ConfigConstants.SPOOFING_NOTIFICATION_ID;
    NotificationCompat.Builder detectionAlertStatusBuilder;
    int detectionAlertStatusNotificationId = ConfigConstants.DETECTION_ALERT_STATUS_NOTIFICATION_ID;
    NotificationCompat.Builder onHoldStatusBuilder;
    int onHoldStatusNotificationId = ConfigConstants.ON_HOLD_NOTIFICATION_ID;
    NotificationCompat.Builder scanningStatusBuilder;
    int scanningStatusNotificationId = ConfigConstants.SCANNING_NOTIFICATION_ID;

    Notification notificationDetection;
    Notification notificationDetectionAlertStatus;
    Notification notificationSpoofingStatus;
    Notification notificationOnHoldStatus;
    Notification notificationScanningStatus;

    private boolean isInBackground = false;

    private boolean detectionNotitificationFirstBuild = true;
    private boolean spoofingStatusNotitificationFirstBuild = true;
    private boolean detectionAlertStatusNotitificationFirstBuild = true;
    private boolean onHoldStatusNotitificationFirstBuild = true;
    private boolean scanningStatusNotitificationFirstBuild = true;

    boolean isSignalPlayerGenerated;

    AudioTrack sigPlayer;

    boolean saveJsonFile;
    String usedBlockingMethod;
    boolean preventiveSpoof;

    protected LocationManager locationManager;
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    double[] lastPosition;

    // Thread handling
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    public static final ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(NUMBER_OF_CORES + 1);

    public Handler uiHandler = new Handler(Looper.getMainLooper());

    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainIsMain = this;

        detector = Scan.getInstance(); //Get Scan-object if no object is available yet make a new one
        detector.init(MainActivity.this); //initialize the detector with the main method
        detector.addDetectionListener(this); // MainActivity will be notified of detections (calls onDetection)

        locationFinder = Location.getInstanceLoc(); //Get LocationFinder-object if no object is available yet make a new one
        locationFinder.init(MainActivity.this); //initialize the location-object with the main method

        btnStop = (ImageButton) findViewById(R.id.btnStop); //Main button for stopping the whole process
        btnStart = (ImageButton) findViewById(R.id.btnPlay); //Main button for starting the whole process
        btnStorLoc = (ImageButton) findViewById(R.id.btnStorLoc); //button for getting into the storedLocations activity
        btnSettings = (ImageButton) findViewById(R.id.btnSettings); //button for getting into the settings activity
        btnExit = (ImageButton) findViewById(R.id.btnExit); //button for exiting the application

        btnStop.setEnabled(false); //after the start of the app set the stop button to false because nothing is there to stop yet

        final AlertDialog.Builder openScanner = new AlertDialog.Builder(MainActivity.this); //AlertDialog for getting the alert message after detection
        openScanner.setCancelable(false); //the AlertDialog cannot be canceled because you have to choose an option for the found signal
        LayoutInflater inflater = getLayoutInflater(); //inflator for getting the custom alertDialog over the main activity
        final ViewGroup viewGroup = (ViewGroup) findViewById(android.R.id.content);
        view = inflater.inflate(R.layout.alert_message, viewGroup , false); //put the alert_message layout on the inflator
        openScanner.setView(view); //set the view of the inflater
        alert = openScanner.create(); //create the AlertDialog

        txtSignalType = (TextView)view.findViewById(R.id.txtSignalType); //this line can be deleted it's only for debug in the alert

        btnAlertDismissAlways = (Button) view.findViewById(R.id.btnDismissAlwaysHere); //button of the alert for always dismiss the found signal
        btnAlertDismissAlways.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
            onAlertDismissAlways();
            }
        });

        btnAlertDismissThisTime = (Button) view.findViewById(R.id.btnDismissThisTime); //button of the alert for only dismiss the found signal this time
        btnAlertDismissThisTime.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
            onAlertDismissThisTime();
            }
        });

        btnAlertSpoof = (Button) view.findViewById(R.id.btnSpoof); //button of the alert for starting the spoofing process after finding a signal
        btnAlertSpoof.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
            onAlertSpoofDetectedSignal();
            }
        });

        btnAlertStart = (Button) view.findViewById(R.id.btnPlay); //button of the alert for playing the found signal with fs/3
        btnAlertStart.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
            onAlertPlayDetectedSignal();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                onBtnStartClick(v);
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                stopApplicationProcesses();
            }
        });

        btnStorLoc.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
            openStoredLocations();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
            openSettings();
            }
        });

        btnExit.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){onBtnExitClick(v);
            }
        });

        // Store the "active" status
        SharedPreferences sp = getSharedPreferences("appStatus", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("active", true);
        ed.commit();

        // savedInstanceState will only be null during the first run of the app, later we do not need to add notifications again.
        if(savedInstanceState == null) {
            mNotificationManager = NotificationManagerCompat.from(this);

            Intent resultIntent = new Intent(this, MainActivity.class); //the intent is still the main-activity
            resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent resultPendingIntent = PendingIntent.getActivity(
                    this,
                    MainActivity.NOTIFICATION_STATUS_REQUEST_CODE,
                    resultIntent,
                    PendingIntent.FLAG_NO_CREATE);


            PendingIntent detectionPendingIntent = PendingIntent.getActivity(
                    this,
                    MainActivity.NOTIFICATION_DETECTION_REQUEST_CODE,
                    resultIntent,
                    PendingIntent.FLAG_NO_CREATE);

            // This will be null if there is no pending intent pointing to the MainActivity, so if it does not run yet
            if (resultPendingIntent == null && detectionPendingIntent == null) {
                activateOnHoldStatusNotification();
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                // TODO: Anything else to initialize when we launch the app ?
            }
            else {
                // TODO: There are some initializations to add depending on the current status.


                // TODO: To change for onHold status
                // Depends on the status. (it should be the opposite if we are ON_HOLD
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);


                Intent intent = getIntent();
                if (intent.getExtras() != null || detectionPendingIntent != null) {
                    Technology technology = (Technology) intent.getExtras().get(ConfigConstants.EXTRA_TECHNOLOGY_DETECTED);
                    if (technology != null) {
                        activateAlert(technology);
                    }
                    else if (detectionPendingIntent != null) {
                        try {
                            detectionPendingIntent.send();
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    // TODO: ?

                }
/*
                PendingIntent detectionPendingIntent = PendingIntent.getActivity(
                        this,
                        MainActivity.NOTIFICATION_DETECTION_REQUEST_CODE,
                        resultIntent,
                        PendingIntent.FLAG_NO_CREATE);
                // This will be null if there is no detection notification
                if (detectionPendingIntent != null) {
                    activateAlert(detectionPendingIntent.get);
                }
*/
            }

            getUpdatedSettings(); //get the settings
        }

    }

    private void onBtnExitClick(View v) {
        mNotificationManager.cancelAll(); //cancel all notifications

        // Cancel the pending intent corresponding to the notification
        Intent resultIntent = new Intent(MainActivity.this, MainActivity.class); //the intent is still the main-activity
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                MainActivity.this,
                MainActivity.NOTIFICATION_STATUS_REQUEST_CODE,
                resultIntent,
                0);

        if (resultPendingIntent != null)
            resultPendingIntent.cancel();

        PendingIntent detectionPendingIntent = PendingIntent.getActivity(
                this,
                MainActivity.NOTIFICATION_DETECTION_REQUEST_CODE,
                resultIntent,
                PendingIntent.FLAG_NO_CREATE);
        if (detectionPendingIntent != null)
            detectionPendingIntent.cancel();

        // Stop all the background threads
        threadPool.shutdownNow();
        System.exit(0); //exit the application
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.open_help:
                openHelp();
                break;
            // action with ID action_settings was selected
            case R.id.open_about_us:
                openAboutUs();
                break;
            default:
                break;
        }

        return true;
    }

    public void openHelp(){
        Uri uri = Uri.parse("http://sonicontrol.fhstp.ac.at"); // missing 'http://' will cause crashed
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void openAboutUs(){
        Intent myIntent = new Intent(MainActivity.this, AboutUs.class); //redirect to the stored locations activity
        startActivityForResult(myIntent, 0);
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void onBtnStartClick(View v) {
        if(!hasPermissions(MainActivity.this, PERMISSIONS)){
            // If an explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO)) {

                // TODO: Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast toast = Toast.makeText(MainActivity.this, R.string.permissionRequestExplanation, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER,0,0);
                toast.show();
                //showRequestPermissionExplanation();

                uiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_ALL_PERMISSIONS);
                    }
                }, 2000);
            } else {
                // First time, no explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_ALL_PERMISSIONS);
            }
        }
        else {
            startDetection();
        }
    }

    private void showRequestPermissionExplanation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.permissionRequestExplanation);
        builder.setPositiveButton("Open the Permission Menu",new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                }
        );
        builder.setNegativeButton("Back to the main menu", null);
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ALL_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length == 0) {
                    //we will show an explanation next time the user click on start
                    showRequestPermissionExplanation();
                }
                else {
                    for (int i = 0; i < permissions.length; i++) {
                        if (Manifest.permission.RECORD_AUDIO.equals(permissions[i])) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                startDetection();
                            }
                            else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                                showRequestPermissionExplanation();
                            }
                        }
                    }
                }
            }
            case ConfigConstants.REQUEST_GPS_PERMISSION:{
                if (grantResults.length == 0) {
                    Toast.makeText(MainActivity.this, R.string.toastLocationAccessDenied, Toast.LENGTH_LONG).show();
                }
                else {
                    for (int i = 0; i < permissions.length; i++) {
                        if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                locationFinder.requestGPSUpdates();
                            }
                            else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                                Toast.makeText(MainActivity.this, R.string.toastLocationAccessDenied, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
            }
        }
    }


    public void activateAlert(Technology signalType){
        SharedPreferences settings = getSettingsObject(); //get the settings
        preventiveSpoof = settings.getBoolean(ConfigConstants.SETTING_PREVENTIVE_SPOOFING, ConfigConstants.SETTING_PREVENTIVE_SPOOFING_DEFAULT);
        if(preventiveSpoof) {
            activateSpoofingStatusNotification();
            usedBlockingMethod = locationFinder.blockMicOrSpoof();
        }
        sigType = signalType; //set the technology variable to the latest detected one

        SharedPreferences sp = getSharedPreferences("appStatus", MODE_PRIVATE);
        boolean activityExists = sp.getBoolean("active", false);
        if (activityExists)
            uiHandler.post(displayAlert);
    }

    private Runnable displayAlert = new Runnable() {
        public void run() {
            alert.show(); //open the alert
        }
    };

    /***
     *
     */
    private void stopAutomaticBlockingMethodOnAction(){
        cancelSpoofingStatusNotification();
        if(usedBlockingMethod.equals(ConfigConstants.USED_BLOCKING_METHOD_SPOOFER)){
            Spoofer spoofBlock = Spoofer.getInstance();
            spoofBlock.stopSpoofingComplete();
        }else if(usedBlockingMethod.equals(ConfigConstants.USED_BLOCKING_METHOD_MICROPHONE)){
            MicCapture micBlock = MicCapture.getInstance();
            micBlock.stopMicCapturingComplete();
        }
    }
/*
    public void initDetectionNotification(){
        detectionBuilder = //create a builder for the detection notification
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.hearing_found) //adding the icon
                        .setContentTitle(getString(R.string.NotificationDetectionTitle)) //adding the title
                        .setContentText(getString(R.string.NotificationDetectionMessage)) //adding the text
                        .setOngoing(true) //can't be canceled
                        .setPriority(Notification.PRIORITY_HIGH) //high priority in the notification system
                        .setCategory(Notification.CATEGORY_STATUS)
                        .setAutoCancel(true); //it's canceled when tapped on it

        Intent resultIntent = new Intent(this, MainActivity.class); //the intent is still the main-activity

        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this,
                MainActivity.NOTIFICATION_STATUS_REQUEST_CODE,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        detectionBuilder.setContentIntent(resultPendingIntent);

        notificationDetection = detectionBuilder.build(); //build the notiviation

    }

    public void activateDetectionNotification(){
        if(detectionNotitificationFirstBuild){ //if it's the first time that it's built
            initDetectionNotification(); //initialize the notification
        }
        mNotificationManager.notify(detectionNotificationId, notificationDetection); //activate the notification with the notification itself and its id
        detectionNotitificationFirstBuild = false; //notification is created
    }

    public void cancelDetectionNotification(){
        mNotificationManager.cancel(detectionNotificationId); //Cancel the notification with the help of the id
    }
*/
    private void initSpoofingStatusNotification(){
        spoofingStatusBuilder = //create a builder for the detection notification
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.hearing_block) //adding the icon
                        .setContentTitle(getString(R.string.StatusNotificationSpoofingTitle)) //adding the title
                        .setContentText(getString(R.string.StatusNotificationSpoofingMesssage)) //adding the text
                        .setCategory(Notification.CATEGORY_SERVICE)
                        .setOngoing(true); //it's canceled when tapped on it

        Intent resultIntent = new Intent(this, MainActivity.class); //the intent is still the main-activity

        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this,
                MainActivity.NOTIFICATION_STATUS_REQUEST_CODE,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        spoofingStatusBuilder.setContentIntent(resultPendingIntent);

        notificationSpoofingStatus = spoofingStatusBuilder.build(); //build the notiviation
    }

    public void activateSpoofingStatusNotification(){
        if(spoofingStatusNotitificationFirstBuild){ //if it's the first time that it's built
            initSpoofingStatusNotification(); //initialize the notification
        }
        mNotificationManager.notify(spoofingStatusNotificationId, notificationSpoofingStatus); //activate the notification with the notification itself and its id
        spoofingStatusNotitificationFirstBuild = false; //notification is created
    }

    public void cancelSpoofingStatusNotification(){
        mNotificationManager.cancel(spoofingStatusNotificationId); //Cancel the notification with the help of the id
    }

    private void initDetectionAlertStatusNotification(Technology technology){
        detectionAlertStatusBuilder = //create a builder for the detection notification
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.hearing_found)
                        .setContentTitle(getString(R.string.StatusNotificationDetectionAlertTitle))
                        .setContentText(getString(R.string.StatusNotificationDetectionAlertMessage))
                        .setCategory(Notification.CATEGORY_STATUS)
                        .setOngoing(true) // cannot be dismissed
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setCategory(Notification.CATEGORY_STATUS)
                        .setAutoCancel(true); //it's canceled when tapped on it

        Intent resultIntent = new Intent(this, MainActivity.class); //the intent is still the main-activity
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        resultIntent.putExtra(ConfigConstants.EXTRA_TECHNOLOGY_DETECTED, technology);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this,
                MainActivity.NOTIFICATION_DETECTION_REQUEST_CODE,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        detectionAlertStatusBuilder.setContentIntent(resultPendingIntent);

        notificationDetectionAlertStatus = detectionAlertStatusBuilder.build(); //build the notiviation
    }

    public void activateDetectionAlertStatusNotification(Technology technology){
        initDetectionAlertStatusNotification(technology); //initialize the notification

        mNotificationManager.notify(detectionAlertStatusNotificationId, notificationDetectionAlertStatus); //activate the notification with the notification itself and its id

    }

    public void cancelDetectionAlertStatusNotification(){
        mNotificationManager.cancel(detectionAlertStatusNotificationId); //Cancel the notification with the help of the id
    }

    private void initOnHoldStatusNotification(){
        onHoldStatusBuilder = //create a builder for the detection notification
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.hearing_pause) //adding the icon
                        .setContentTitle(getString(R.string.StatusNotificationOnHoldTitle)) //adding the title
                        .setContentText(getString(R.string.StatusNotificationOnHoldMessage)) //adding the text
                        .setCategory(Notification.CATEGORY_STATUS)
                        .setOngoing(true); //it's canceled when tapped on it

        Intent resultIntent = new Intent(this, MainActivity.class); //the intent is still the main-activity
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this,
                MainActivity.NOTIFICATION_STATUS_REQUEST_CODE,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        onHoldStatusBuilder.setContentIntent(resultPendingIntent);

        notificationOnHoldStatus = onHoldStatusBuilder.build(); //build the notiviation
    }

    public void activateOnHoldStatusNotification(){
        if(onHoldStatusNotitificationFirstBuild){ //if it's the first time that it's built
            initOnHoldStatusNotification(); //initialize the notification
        }
        mNotificationManager.notify(onHoldStatusNotificationId, notificationOnHoldStatus); //activate the notification with the notification itself and its id
        onHoldStatusNotitificationFirstBuild = false; //notification is created
    }

    public void cancelOnHoldStatusNotification(){
        mNotificationManager.cancel(onHoldStatusNotificationId); //Cancel the notification with the help of the id
    }

    private void initScanningStatusNotification(){
        scanningStatusBuilder = //create a builder for the detection notification
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_hearing_white_48dp) //adding the icon
                        .setContentTitle(getString(R.string.StatusNotificationScanningTitle)) //adding the title
                        .setContentText(getString(R.string.StatusNotificationScanningMessage)) //adding the text
                        .setCategory(Notification.CATEGORY_SERVICE)
                        .setOngoing(true); //it's canceled when tapped on it

        Intent resultIntent = new Intent(this, MainActivity.class); //the intent is still the main-activity

        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this,
                MainActivity.NOTIFICATION_STATUS_REQUEST_CODE,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        scanningStatusBuilder.setContentIntent(resultPendingIntent);

        notificationScanningStatus = scanningStatusBuilder.build(); //build the notiviation
    }

    public void activateScanningStatusNotification(){
        if(scanningStatusNotitificationFirstBuild){ //if it's the first time that it's built
            initScanningStatusNotification(); //initialize the notification
        }
        mNotificationManager.notify(scanningStatusNotificationId, notificationScanningStatus); //activate the notification with the notification itself and its id
        scanningStatusNotitificationFirstBuild = false; //notification is created
    }

    public void cancelScanningStatusNotification(){
        mNotificationManager.cancel(scanningStatusNotificationId); //Cancel the notification with the help of the id
    }

    public SharedPreferences getSettingsObject(){
        if (sharedPref == null)
            sharedPref = PreferenceManager.getDefaultSharedPreferences(this); //get the object with the settings
        return sharedPref;
    }

    public void getUpdatedSettings(){
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume(){ //override the onResume method for setting a variable for checking the background-status
        super.onResume();
        isInBackground = false;

        SharedPreferences settings = getSettingsObject(); //get the settings
        saveJsonFile = settings.getBoolean(ConfigConstants.SETTING_SAVE_DATA_TO_JSON_FILE, ConfigConstants.SETTING_SAVE_DATA_TO_JSON_FILE_DEFAULT);

        if(saveJsonFile) {
            if (!jsonMan.checkIfJsonFileIsAvailable()) { //check if a JSON File is already there in the storage
                jsonMan.createJsonFile(); //create a JSON file
            }
            if (!jsonMan.checkIfSavefolderIsAvailable()) { //check if a folder for the audio files is already there in the storage
                jsonMan.createSaveFolder(); //create a folder for the audio files
            }
        }
    }

    @Override
    public void onPause(){ //override the onPause method for setting a variable for checking the background-status
        super.onPause();
        isInBackground = true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Store our shared preference
        SharedPreferences sp = getSharedPreferences("appStatus", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("active", false);
        ed.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // TODO: Release resources not released yet in onStop()
        // Maybe threads, microphone, ... ?
        //threadPool.shutdownNow();
    }

    public static MainActivity getMainIsMain(){
        return mainIsMain;
    }

    public boolean getBackgroundStatus(){
        return isInBackground;
    } //get the background-status

    public boolean[] checkJsonAndLocationPermissions() {
        boolean[] saveJsonAndLocation = new boolean[2];
        SharedPreferences settings = getSettingsObject(); //get the settings
        saveJsonFile = settings.getBoolean(ConfigConstants.SETTING_SAVE_DATA_TO_JSON_FILE, ConfigConstants.SETTING_SAVE_DATA_TO_JSON_FILE_DEFAULT);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        boolean locationTrack = false;
        boolean locationTrackGps = settings.getBoolean(ConfigConstants.SETTING_GPS, ConfigConstants.SETTING_GPS_DEFAULT);
        boolean locationTrackNet = settings.getBoolean(ConfigConstants.SETTING_NETWORK_USE, ConfigConstants.SETTING_NETWORK_USE_DEFAULT);

        if((locationTrackGps&&isGPSEnabled)||(locationTrackNet&&isNetworkEnabled)){
            locationTrack = true;
        }

        saveJsonAndLocation[0] = saveJsonFile;
        saveJsonAndLocation[1] = locationTrack;

        return saveJsonAndLocation;
    }

    @Override
    public void onDetection(final Technology technology) {
        //TODO: someHandler.post(checkTechnologyAndDoAccordingly(technology));
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                checkTechnologyAndDoAccordingly(technology);
            }
        });
    }


    private void checkTechnologyAndDoAccordingly(Technology detectedTechnology){
        if(detectedTechnology == null) {
            // TODO: Needed ? detector.startScanning();
            Log.d(TAG, "checkTechnologyAndDoAccordingly: detectedTechnology is null !?");
        }
        else {
            switch (detectedTechnology) {
                case GOOGLE_NEARBY:
                    handleSignal(Technology.GOOGLE_NEARBY);
                case LISNR:
                    handleSignal(Technology.LISNR);
                case PRONTOLY:
                    handleSignal(Technology.PRONTOLY);
                case UNKNOWN:
                    Log.d("Detected", "Unknown ultrasonic signal");
                    handleSignal(Technology.UNKNOWN);
            }
        }
    }

    private void handleSignal(Technology technology) {
        boolean locationTrack;
        locationFinder.saveSignalTypeForLater(technology);
        locationTrack = this.checkJsonAndLocationPermissions()[1];

        if (locationTrack) {
            lastPosition = locationFinder.getLocation(); //get the latest position
        }

        if (this.getSettingsObject().getBoolean(ConfigConstants.SETTING_CONTINOUS_SPOOFING, false)) { //check if the settings are set to continous spoofing
            /*if (!this.getBackgroundStatus()) { //if the app is not in the background
                this.cancelDetectionNotification(); //cancel the detection notification
            }*/
            Log.d("Spoof", "I spoof oontinuous");
            if (locationTrack) {
                locationFinder.setPositionForContinuousSpoofing(lastPosition); //set the position for distance calculation to the latest position
            }
            this.cancelScanningStatusNotification(); //cancel the scanning-status notification
            this.activateSpoofingStatusNotification(); //activate the spoofing-status notification

            saveJsonFile = this.checkJsonAndLocationPermissions()[0];

            JSONManager jsonMan = new JSONManager(this);
            if (saveJsonFile && locationTrack) {
                jsonMan.addJsonObject(locationFinder.getDetectedDBEntry(), technology.toString(), 1, locationFinder.getDetectedDBEntryAddres()); //adding the found signal in the JSON file
            }
            if (saveJsonFile && !locationTrack) {
                double[] noLocation = new double[2];
                noLocation[0] = 0;
                noLocation[1] = 0;
                jsonMan.addJsonObject(noLocation, technology.toString(), 1, this.getResources().getString(R.string.addressData));
            }

            locationFinder.blockMicOrSpoof(); //try for microphone access and choose the blocking method
            //resetHandler(); // Should be handled by the cpp (just stop scanning)
        } else {
            if (!jsonMan.checkIfJsonFileIsAvailable()) { //check if the user has a JSON file
                /*if (this.getBackgroundStatus()) { //if the app is in the background
                    this.activateDetectionNotification(); //activate the notification for a detection
                }*/
                this.cancelScanningStatusNotification(); //cancel the scanning-status notification
                this.activateDetectionAlertStatusNotification(technology);
                this.activateAlert(technology); //open the alert dialog
            } else {
                if (locationTrack) {
                    locationFinder.checkExistingLocationDB(lastPosition, technology); //if a JSON file is available we check if the signal is a new one with position and technologytype
                } else {
                    /*if (this.getBackgroundStatus()) { //if the app is in the background
                        this.activateDetectionNotification(); //activate the notification for a detection
                    }*/
                    this.cancelScanningStatusNotification(); //cancel the scanning-status notification
                    this.activateDetectionAlertStatusNotification(technology); //activate the onHold-status notification
                    this.activateAlert(technology); //open the alert dialog
                    //resetHandler(); // Should be handled by the cpp (just stop scanning)
                }
            }
        }
    }

    public void startDetection(){
        runOnUiThread(new Runnable() {
        @Override
        public void run() {
                SharedPreferences settings = getSettingsObject(); //get the settings
                saveJsonFile = settings.getBoolean(ConfigConstants.SETTING_SAVE_DATA_TO_JSON_FILE, ConfigConstants.SETTING_SAVE_DATA_TO_JSON_FILE_DEFAULT);

                if(saveJsonFile) {
                    if (!jsonMan.checkIfJsonFileIsAvailable()) { //check if a JSON File is already there in the storage
                        jsonMan.createJsonFile(); //create a JSON file
                    }
                    if (!jsonMan.checkIfSavefolderIsAvailable()) { //check if a folder for the audio files is already there in the storage
                        jsonMan.createSaveFolder(); //create a folder for the audio files
                    }
                }



                cancelOnHoldStatusNotification(); //cancel the onHold notification
                activateScanningStatusNotification(); //start the scanning-status notification
                detector.startScanning(); //start scanning for signals
                btnStart.setEnabled(false); //disable the start button
                btnStop.setEnabled(true); //enable the stop button
        }
        });
    }

    public void stopApplicationProcesses(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNotificationManager.cancelAll(); //cancel all active notifications
                activateOnHoldStatusNotification(); //activate only the onHold-status notification again
                detector.pause(); // stop scanning
                alert.cancel();
                Spoofer spoof = Spoofer.getInstance(); //get a spoofing object
                spoof.stopSpoofingComplete(); //stop the whole spoofing process
                MicCapture micCap = MicCapture.getInstance(); //get a microphone capture object
                micCap.stopMicCapturingComplete(); //stop the whole capturing process via the microphone
                btnStart.setEnabled(true); //enable the start button again
                btnStop.setEnabled(false); //disable the stop button
            }
        });
    }

    public void openStoredLocations(){
        Intent myIntent = new Intent(MainActivity.this, StoredLocations.class); //redirect to the stored locations activity
        startActivityForResult(myIntent, 0);
    }

    public void openSettings(){
        Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class); //redirect to the settings activity
        startActivityForResult(myIntent, 0);
        String uniqueID = UUID.randomUUID().toString();
        Log.d("UUID", uniqueID);
    }

    public void onAlertChoice(int spoofDecision) {
        if(usedBlockingMethod != null) {
            stopAutomaticBlockingMethodOnAction();
        }

        // TODO: Function to be called in a thread, for IO (save json entry)

        saveJsonFile = checkJsonAndLocationPermissions()[0];
        boolean locationTrack = checkJsonAndLocationPermissions()[1];

        if(saveJsonFile && locationTrack) {
            jsonMan.addJsonObject(locationFinder.getDetectedDBEntry(), sigType.toString(), spoofDecision, locationFinder.getDetectedDBEntryAddres()); //adding the found signal in the JSON file
        }
        if(saveJsonFile&&!locationTrack){
            double[] noLocation = new double[2];
            noLocation[0] = 0;
            noLocation[1] = 0;
            jsonMan.addJsonObject(noLocation, sigType.toString(), spoofDecision, getString(R.string.noAddressForJsonFile));
        }
        alert.cancel(); //cancel the alert dialog
        txtSignalType.setText(""); //can be deleted it's only for debugging
        //cancelDetectionNotification(); //cancel the detection notification
        cancelDetectionAlertStatusNotification(); //canceling the onHold notification
        //Why this ? : cancelSpoofingStatusNotification();
    }

    public void onAlertPlayDetectedSignal(){
        if (sigPlayer == null && !isSignalPlayerGenerated){ //if no player for the signal is created yet and the boolean for generating is also false
            btnAlertStart.setText(R.string.ButtonStopSignal); //set the button for playing/stopping to "stop"
            sigPlayer = locationFinder.generatePlayer(); //create a new player
            isSignalPlayerGenerated = true; //player is generated so it's true
            sigPlayer.play(); //start the player
        }else if(sigPlayer!=null && isSignalPlayerGenerated){ //if a player for the signal is created and the boolean for generating is true
            sigPlayer.stop(); //stop the player
            sigPlayer.release(); //release the resources of the player
            sigPlayer = null; //set the player variable to null
            btnAlertStart.setText(R.string.ButtonPlaySignal); //set the button for playing/stopping to "play"
            isSignalPlayerGenerated = false; //now there is no player anymore so it's false
        }
        txtSignalType.setText(sigType.toString()); //can be deleted it's only for debugging
    }

    public void onAlertSpoofDetectedSignal(){
        onAlertChoice(1);
        locationFinder.blockMicOrSpoof(); //try to get the microphone access for choosing the blocking method
        activateSpoofingStatusNotification(); //activates the notification for the spoofing process
    }

    public void onAlertDismissAlways(){
        onAlertChoice(0);
        detector.startScanning(); //start scanning again
        activateScanningStatusNotification(); //activates the notification for the scanning process
    }

    public void onAlertDismissThisTime(){
        onAlertChoice(2);
        detector.startScanning(); //start scanning again
        activateScanningStatusNotification(); //activates the notification for the scanning process
    }
/*
    public void updateDistance(final double distance) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView txtDistance = (TextView) mainIsMain.findViewById(R.id.txtDistance); //can be deleted only for debugging
                txtDistance.setText(String.valueOf(distance*1000)); //can be deleted only for debugging
            }
        });
    }*/

}
