/*
 * Copyright (c) 2018, 2019, 2020. Peter Kopciak, Kevin Pirner, Alexis Ringot, Florian Taurer, Matthias Zeppelzauer.
 *
 * This file is part of SoniControl app.
 *
 *     SoniControl app is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SoniControl app is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with SoniControl app.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.ac.fhstp.sonicontrol;


import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class Spoofer {

    static Spoofer instance;

    int helpCounter = 0;

    MainActivity main;

    double[] positionLatest;
    double[] positionOld;
    private Scan detector = Scan.getInstance();
    private Location locFinder = Location.getInstanceLoc();
    NoiseGenerator genNoise;
    double distance;

    private AudioTrack audioTrack;
    private boolean playingGlobal;
    private boolean playingHandler;
    private boolean isFirstPlay;
    private boolean noiseGenerated = false;
    private int playtime = 0;
    private int locationRadius;
    private long startTime;
    private long stopTime;

    //private Technology signalType;

    private boolean stopped = false;

    private Spoofer(){
    }

    public static Spoofer getInstance() { //getInstance method for Singleton pattern
        if(instance == null) { //if no instance of Scan is there create a new one, otherwise return the existing
            instance = new Spoofer();
        }
        return instance;
    }

    public void init(boolean playingGlobal, boolean playingHandler){  //initialize the Scan with a main object
        // TODO: init() could be called only once. We create a new NoiseGenerator object every time we want to spoof.
        this.genNoise = new NoiseGenerator();
        this.playingGlobal = playingGlobal;
        this.playingHandler = playingHandler;
    }

    public void startSpoofing(MainActivity main){
        this.main = main;
        onPulsing(main); //start the onPulsing method
    }


    public void playWhitenoise(){
        audioTrack.play(); //start the player
    }

    public void stopWhitenoise(){
        audioTrack.stop(); //stop the player
    }

    public void startStop(boolean playStatus){ //method for dynamically start and stop the spoofer depending on the playStatus
        if(playStatus){
            playWhitenoise();
        } else{
            stopWhitenoise();
        }
    }

    public void onPulsing(MainActivity main) {
        MainActivity.threadPool.schedule(spoofRun, playtime, TimeUnit.MILLISECONDS);
    }

    private Runnable spoofRun = new Runnable() {
        public void run() {
            if (stopped) {
                // TODO: Do we need to reinitialize something ?
            }
            else {
                Context context = main.getApplicationContext();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND); //set the handler thread to background
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                // not used ? int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(3, (int) Math.round((audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 0.70D)), 0);

                if (playingHandler) {
                    playtime = genNoise.getPlayertime(); //get the playertime depending on the generated whitenoise
                } else {
                    playtime = Integer.valueOf(sp.getString(ConfigConstants.SETTING_PAUSE_DURATION, ConfigConstants.SETTING_PAUSE_DURATION_DEFAULT)); //get the pause value from the settings
                }
                if (!noiseGenerated) { //if no whitenoise is available generate a new one
                    String technologyName = sp.getString(ConfigConstants.LAST_DETECTED_TECHNOLOGY_SHARED_PREF,Technology.UNKNOWN.toString());
                    Technology detectedTechnology = null;
                    try {
                        detectedTechnology = Technology.fromString(technologyName);
                    }
                    catch (IllegalArgumentException e) {
                        Log.d("Spoofer", "spoofRun - technology string stored not recognized: " + e.getMessage());
                        detectedTechnology = Technology.UNKNOWN;
                    }
                    genNoise.generateWhitenoise(detectedTechnology, main); //generate noise
                    audioTrack = genNoise.getGeneratedPlayer(); //get the generated player
                    noiseGenerated = true; //noise and player are generated so its true
                    startTime = Calendar.getInstance().getTimeInMillis(); //get the starttime
                }
                if (isFirstPlay) { //if it is the first play after starting the spoofer
                    startStop(playingHandler); //starting it depending on the playingHandler boolean
                    playingHandler = !playingHandler; //change the variable for the next run
                    isFirstPlay = false; //set the first play to false
                    onPulsing(main); //execute the method again
                } else {
                    if (playingGlobal) {
                        startStop(playingHandler); //starting it depending on the playingHandler boolean
                        playingHandler = !playingHandler; //change the variable for the next run
                        SharedPreferences sharedPref = main.getSettingsObject(); //get the settings
                        locationRadius = Integer.valueOf(sharedPref.getString(ConfigConstants.SETTING_LOCATION_RADIUS, ConfigConstants.SETTING_LOCATION_RADIUS_DEFAULT)); //get the location radius in metres
                        int spoofingTime = Integer.valueOf(sharedPref.getString(ConfigConstants.SETTING_BLOCKING_DURATION, ConfigConstants.SETTING_BLOCKING_DURATION_DEFAULT)); //get the spoofingtime in minutes
                        stopTime = Calendar.getInstance().getTimeInMillis(); //get the stoptime
                        Long logLong = (stopTime - startTime) / 1000; //get the difference of the start- and stoptime
                        String logTime = String.valueOf(logLong);
                        if (logLong > (spoofingTime * 60)) { //check if its over the spoofing time from the settings
                            executeRoutineAfterExpiredTime(main);
                        } else {
                            onPulsing(main); //start the pulsing again
                        }
                    } else {
                    }
                }
            }
        }
    };

    public boolean isNoiseGenerated(){
        return noiseGenerated;
    }

    public void setNoiseGeneratedFalse(){
        noiseGenerated = false;
    }

    public void setFirstPlayTrue(){
        isFirstPlay = true;
    }

    public void setPlaytimeZero(){
        playtime = 0;
    }

    public void setInstanceNull(){
        instance = null;
    }

    public void stopSpoofingComplete(){
        stopped = true;
        if(instance != null) { //if there is an instance
            // TODO: Is this safe ? Could other classes have kept the reference to the instance ? ...
            setInstanceNull(); //set the instance of the spoofer null
        }
        if(audioTrack != null){ //if there is an audioplayer
            audioTrack.stop(); //stop playing
            audioTrack.release(); //release the player resources
            audioTrack = null; //set the player to null
            // TODO: shouldnt we keep it in case we can reuse it ?
            genNoise.setGeneratedPlayerToNull(); //set the player of the generator null
            genNoise = null; //set the NoiseGenerator object to null
        }
    }

    private void executeRoutineAfterExpiredTime(MainActivity main){
        stopped = true;
        startStop(false); //stop the spoofer
        playingGlobal = false; //set to false because its not playing anymore
        boolean locationTrack = false;
        SharedPreferences sharedPref = main.getSettingsObject(); //get the settings
        boolean locationTrackGps = sharedPref.getBoolean(ConfigConstants.SETTING_GPS, ConfigConstants.SETTING_GPS_DEFAULT);
        boolean locationTrackNet = sharedPref.getBoolean(ConfigConstants.SETTING_NETWORK_USE, ConfigConstants.SETTING_NETWORK_USE_DEFAULT);
        if(locationTrackGps||locationTrackNet){
            locationTrack = true;
        }
        if((locFinder.getDetectedDBEntry()[0]!=0&&locFinder.getDetectedDBEntry()[1]!=0)) {
            positionLatest = locFinder.getLocation(main); //get the latest position
            positionOld = locFinder.getDetectedDBEntry(); //get the position saved in the json-file
            distance = locFinder.getDistanceInMetres(positionOld, positionLatest); //calculate the distance
            if (distance < locationRadius) { //if we are still in the locationRadius
                setSpoofingNoiseToNullAndTryGettingMicAccessAgain(main);
            } else {
                startScanningAgain(main);
            }
        }else {
            startScanningAgain(main);
        }
    }

    private void startScanningAgain(MainActivity main){
        setInstanceNull(); //set the NoiseGenerator instance to null
        NotificationHelper.activateScanningStatusNotification(main.getApplicationContext()); //activate the scanning status notification
        detector.getTheOldSpoofer(Spoofer.this); //update the spoofer object in the detector
        detector.startScanning(main); //start scanning again
    }

    private void setSpoofingNoiseToNullAndTryGettingMicAccessAgain(MainActivity main){
        // TODO: Keep the generated noise in case of reuse ?
        genNoise.setGeneratedPlayerToNull(); //set the noiseplayer to null
        audioTrack.release(); //release the player resources
        audioTrack = null; //set the player to null
        genNoise = null; //set the the noisegenerator object to null
        setInstanceNull(); //set the NoiseGenerator instance to null
        locFinder.blockMicOrSpoof(main); //try again to get access to the microphone and then choose the spoofing method
    }
}
