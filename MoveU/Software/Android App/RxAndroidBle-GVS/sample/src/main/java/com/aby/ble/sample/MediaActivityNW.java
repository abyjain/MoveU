package com.aby.ble.sample;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.aby.ble.sample.util.HexString;
import com.opencsv.CSVWriter;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

import static com.trello.rxlifecycle.android.ActivityEvent.PAUSE;

public class MediaActivityNW extends RxAppCompatActivity {


    @BindView(R.id.Status_Info)
    TextView output ;
    @BindView(R.id.notify)
    Button notify;
    @BindView(R.id.dataView)
    TextView dataView;
    @BindView(R.id.test)
    Button testSend ;
    @BindView(R.id.saveTrigger)
    Switch saveTrigger ;
    @BindView(R.id.editText)
    TextView editText ;
    @BindView(R.id.play)
    Button playPause ;
    @BindView(R.id.seek)
    SeekBar seek1 ;
    @BindView(R.id.check)
    TextView output1 ;
    @BindView(R.id.dir)
    TextView dir;

    private UUID characteristicReadUuid = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private UUID characteristicUuid = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();
    private Observable<RxBleConnection> connectionObservable;
    private RxBleDevice bleDevice;


    byte[] data = {20,0,0};
    // Start (Flag), Strength(L) , Strength(R)

    public int[] triggerTime = {6 , 13 , 14 , 17 , 52 , 53 , 55 , 58 , 59 , 61 , 62 , 66 , 67 , 68 , 70 , 71 , 73 , 75 , 78 , 80 , 85 , 92 , 95 , 98 , 100 , 101 , 104 ,
                                106 , 107 , 108 , 110 , 113 , 118 , 120 , 122 ,125 ,126 , 128 , 129 , 132 , 136 , 138 , 140} ;
    byte[][] multipleTriggers = {{100,0},{0,0},{0,100},{0,0},{0,50},{0,0},{0,100},{0,0},{100,0},{0,0},{0,100},{100,0},
                                 {0,0},{0,50},{0,0},{100,0},{0,0},{0,100},{0,0},{50,0},{0,0},{100,0},{0,0},{100,0},
                                 {0,0},{0,70},{100,0},{0,0},{0,100},{0,0},{0,50},{50,0},{0,0},{0,100},{0,0},{100,0},
                                 {0,0},{0,70},{0,0},{100,0},{0,0},{0,0},{0,0}};
    int triggerCount = 43;
    long startTime = 0;
    int indx = 0 ;

    boolean fileDefine = false ;
    boolean fileWrite = false ;

    CSVWriter writer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_nw);
        ButterKnife.bind(this);
        String macAddress = getIntent().getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS);
        bleDevice = GVSControl.getRxBleClient(this).getBleDevice(macAddress);
        connectionObservable = prepareConnectionObservable();
        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(getString(R.string.mac_address, macAddress));
        output.setText("Connecting");
        if(!isConnected()) onConnectToggleClick();
        //onNotifyClick();
        triggerCount = 0 ;
        dir.setText("Neutral");
        initSeek(150);

    }

    private Observable<RxBleConnection> prepareConnectionObservable() {
        return bleDevice
                .establishConnection(false)
                .takeUntil(disconnectTriggerSubject)
                .compose(bindUntilEvent(PAUSE))
                .compose(new ConnectionSharingAdapter());
    }


    public void onConnectToggleClick() {

        if (isConnected()) {
            triggerDisconnect();
        } else {
            connectionObservable
                    .flatMap(RxBleConnection::discoverServices)
                    .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(characteristicUuid))
                    .observeOn(AndroidSchedulers.mainThread())
                    //.doOnSubscribe(() -> connectButton.setText(R.string.connecting))
                    .subscribe(
                            characteristic -> {
                                updateUI();
                                Log.i(getClass().getSimpleName(), "Hey, connection has been established!");
                            },
                            this::onConnectionFailure,
                            this::onConnectionFinished
                    );
        }
    }



    @OnClick(R.id.test)
    public void testSend() {
        if(!isConnected()) onConnectToggleClick();
        else sendData();
    }


    public void sendData() {

        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUuid, data ))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> onWriteSuccess(),
                            this::onWriteFailure
                    );
        }
    }

    @OnClick(R.id.notify)
    public void onNotifyClick() {

        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.setupNotification(characteristicReadUuid))
                    .doOnNext(notificationObservable -> runOnUiThread(this::notificationHasBeenSetUp))
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onNotificationReceived, this::onNotificationSetupFailure);

        }
    }


    private boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
        updateUI();
    }

    private void onConnectionFinished() {
        updateUI();
    }

    private void onReadFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Read error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void onWriteSuccess() {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Write success", Snackbar.LENGTH_SHORT).show();
    }

    private void onWriteFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Write error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void onNotificationReceived(byte[] bytes) {
        //noinspection ConstantConditions
        int numValues = 5 ;
        int idx[] = new int[numValues] ;
        String str = " " ;
        for(int i = 0 ; i < numValues ; i++) {
            idx[i] += bytes[4*i] & 0xFF;
            idx[i] += (bytes[4*i + 1] & 0xFF) << 8;
            idx[i] += (bytes[4*i + 2] & 0xFF) << 16;
            idx[i] += (bytes[4*i + 3] & 0xFF) << 24;
            str += Integer.toString(idx[i]) + " " ;
        }

        dataView.setText(str);

        String[] dat = {Integer.toString(idx[0]),Integer.toString(idx[1]),Integer.toString(idx[2]),Integer.toString(idx[3]),Integer.toString(idx[4])};
        if(fileWrite) {writer.writeNext(dat);
        }
    }

    private void onNotificationSetupFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Notifications error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void notificationHasBeenSetUp() {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Notifications has been set up", Snackbar.LENGTH_SHORT).show();
    }

    private void triggerDisconnect() {
        disconnectTriggerSubject.onNext(null);
    }

    /**
     * This method updates the UI to a proper state.
     *
     */
    private void updateUI() {
        if(fileWrite) output.setText("Writing");
        else if(isConnected()) output.setText("Connected");
        else output.setText("Disconnected");

    }

    private boolean hasProperty(BluetoothGattCharacteristic characteristic, int property) {
        return characteristic != null && (characteristic.getProperties() & property) > 0;
    }


    @OnClick(R.id.saveTrigger)
    public void onToggle(){
        if(!fileDefine){
            String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
            String fileName = "AnalysisData-"+ editText.getText() +".csv";
            String filePath = baseDir + File.separator + fileName;
            File f = new File(filePath );
            Log.d("Filepath :",  baseDir);
            // File exist
            if(f.exists() && !f.isDirectory()){

                try {
                    FileWriter mFileWriter = new FileWriter(filePath, true);
                    writer = new CSVWriter(mFileWriter);
                }
                catch (IOException e) {
                    Log.d("Exception","An IOException was caught :" + e.getMessage());
                }

            }
            else {
                try {
                    writer = new CSVWriter(new FileWriter(filePath));
                }
                catch (IOException e) {
                    Log.d("Exception","An IOException was caught :" + e.getMessage());
                }

        }

            fileDefine = true ;
        }

        fileWrite = saveTrigger.isChecked() ;

        if(!fileWrite) {
            try{
                 writer.close();
            }
            catch (IOException e) {
                Log.d("Exception","An IOException was caught :" + e.getMessage());
            }
            fileDefine = false ;
        }
        updateUI();
    }

    private void initSeek(int x){
        seek1.setMax(x);
        seek1.setProgress(0);
        seek1.setOnSeekBarChangeListener(mSeekBar);
        output1.setText("Start");
        seek1.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);


    }

    private SeekBar.OnSeekBarChangeListener mSeekBar = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            output1.setText("" + milliSecondsToTimer((int) progress));
            //seekTime = progress ;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    @OnClick(R.id.play)
    public void onClickPlay() {
        if(!isConnected()) onConnectToggleClick();

        seek1.getProgressDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
        seek1.setProgress(0);
        startTime = System.currentTimeMillis();
        indx = 0 ;
        timerHandler.postDelayed(timerRunnable, 0);

    }


    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            int secondsep = 60*minutes+seconds ;
            seek1.setProgress(secondsep);
            output1.setText(String.format("%d:%02d", minutes, seconds));
            checkPlayer(secondsep);
            if(secondsep >= 150) timerHandler.removeCallbacks(timerRunnable);
            else timerHandler.postDelayed(this, 500);
        }
    };


    private void checkPlayer(int currentDuration){
        if(triggerTime[indx] == currentDuration) {
                data[1] = multipleTriggers[indx][0];
                data[2] = multipleTriggers[indx][1];
                if(data[1]>data[2]) dir.setText("Left");
                else if(data[1] < data[2])dir.setText("Right");
                else dir.setText("Neutral");
                if(indx < 41) indx += 1 ;
                sendData();
        }
    }

    public  String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

}
