package gvscontrol;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.aby.ble.sample.util.HexString;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

import static com.trello.rxlifecycle.android.ActivityEvent.PAUSE;

public class MediaActivity extends RxAppCompatActivity {

    @BindView(R.id.connect)
    Button connectButton;
    @BindView(R.id.read_output)
    TextView readOutputView;
    @BindView(R.id.read_hex_output)
    TextView readHexOutputView;
    @BindView(R.id.write_input)
    TextView writeInput;
    @BindView(R.id.read)
    Button readButton;
    @BindView(R.id.write)
    Button writeButton;
    @BindView(R.id.notify)
    Button notifyButton;
    @BindView(R.id.check)
    TextView output ;
    @BindView(R.id.file)
    Button chooseFile ;
    @BindView(R.id.Triggers)
    TextView triggerTimeShow;
    @BindView(R.id.trig)
    Button trigger ;
    @BindView(R.id.play)
    Button playPause ;
    @BindView(R.id.seek)
    SeekBar seek1 ;
    @BindView(R.id.intensity_seek)
    SeekBar intSeek ;
    @BindView(R.id.intensity_seek_mot)
    SeekBar intSeekMot ;
    @BindView(R.id.delay_seek)
    SeekBar delSeek ;
    @BindView(R.id.delay_seek_mot)
    SeekBar delSeekMot ;
    @BindView(R.id.phdelay_seek_mot)
    SeekBar phSeekMot ;
    @BindView(R.id.intensity_val)
    TextView intVal ;
    @BindView(R.id.intensity_val_mot)
    TextView intValMot ;
    @BindView(R.id.delay_val)
    TextView delVal ;
    @BindView(R.id.delay_val_mot)
    TextView delValMot ;
    @BindView(R.id.ph_val)
    TextView phVal ;
    @BindView(R.id.test)
    Button testSend ;
    @BindView(R.id.triggerActive)
    Switch deviceActive ;

    private UUID characteristicReadUuid = UUID.fromString("00002221-0000-1000-8000-00805f9b34fb");
    private UUID characteristicUuid = UUID.fromString("00002222-0000-1000-8000-00805f9b34fb");
    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();
    private Observable<RxBleConnection> connectionObservable;
    private RxBleDevice bleDevice;

    MediaPlayer mp;
    private boolean isLoaded = false ;

    public long[] triggerTime = {-1 , -1 , -1 , -1 , -1 , -1} ;
    public int triggerCount = 0 ;
    int seekTime = 0 ;
    byte[] data = {20,100,10,100,10,0};
    // Start (Flag), Strength , Duration(*100ms)
    byte[][] multipleTriggers = {{100,10},{100,10},{100,10},{100,10},{100,10},{100,10}};
    byte[][] multipleTriggersMot = {{100,10},{100,10},{100,10},{100,10},{100,10},{100,10}};
    byte[] multipleTriggersPh = {0,0,0,0,0,0};
    boolean triggerActive = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        ButterKnife.bind(this);
        String macAddress = getIntent().getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS);
        bleDevice = GVSControl.getRxBleClient(this).getBleDevice(macAddress);
        connectionObservable = prepareConnectionObservable();
        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(getString(R.string.mac_address, macAddress));
        output.setText("Select File");
        mp = new MediaPlayer();
        triggerCount = 0 ;
        intVal.setText("Intensity: " +Integer.toString(100));
        delVal.setText("Duration: " +Integer.toString(10*100)+" ms");
        intValMot.setText("Intensity: " +Integer.toString(100));
        delValMot.setText("Duration: " +Integer.toString(10*100)+" ms");
        phVal.setText("Phase delay: "+Integer.toString(0*100)+" ms");
        intSeek.setMax(255);
        intSeek.setProgress(100);
        intSeek.setOnSeekBarChangeListener(intSeekListener);
        intSeekMot.setMax(255);
        intSeekMot.setProgress(100);
        intSeekMot.setOnSeekBarChangeListener(intSeekMotListener);
        delSeek.setMax(100);
        delSeek.setProgress(10);
        delSeek.setOnSeekBarChangeListener(delSeekListener);
        delSeekMot.setMax(100);
        delSeekMot.setProgress(10);
        delSeekMot.setOnSeekBarChangeListener(delSeekMotListener);
        phSeekMot.setMax(40);
        phSeekMot.setProgress(20);
        phSeekMot.setOnSeekBarChangeListener(phSeekListener);
        triggerTimeShow.setLines(6);
        if(!isConnected()) onConnectToggleClick();
        //mp.setAudioStreamType(AudioManager.STREAM_MUSIC);

    }

    private Observable<RxBleConnection> prepareConnectionObservable() {
        return bleDevice
                .establishConnection(false)
                .takeUntil(disconnectTriggerSubject)
                .compose(bindUntilEvent(PAUSE))
                .compose(new ConnectionSharingAdapter());
    }



    @OnClick(R.id.file)
    @TargetApi(Build.VERSION_CODES.M)
    public void chooseFile(){

        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 10);
        if(this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

        triggerCount = 0 ;
        //triggerTimeShow.setText("Triggers");
        trigger.setEnabled(true);
        trigger.setText("Set Triggers");
        if(mp.isPlaying()) mp.stop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == RESULT_OK && requestCode == 10){
            Uri uriSound=data.getData();
            output.setText(uriSound.getEncodedPath());
            mp = MediaPlayer.create(this,uriSound);
            isLoaded = true ;
            output.setText("Duration " + milliSecondsToTimer(mp.getDuration()));
            //output.append("Hello");
            initSeek(mp.getDuration());
        }
    }

    private void initSeek(int x){
        seek1.setMax(x);
        seek1.setProgress(0);
        seek1.setOnSeekBarChangeListener(mSeekBar);
        output.setText("Set Triggers");
        seek1.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);


    }

    private SeekBar.OnSeekBarChangeListener mSeekBar = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        output.setText("" + milliSecondsToTimer((int) progress));
        seekTime = progress ;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        if(mp.isPlaying()) mp.seekTo(seekTime);
        }
    };


    @OnClick(R.id.trig)
    public void onClickSetTime() {
        if(triggerCount >5)  Toast.makeText(this, "Only 5 triggers Allowed", Toast.LENGTH_SHORT).show();
        else if(isLoaded) {
            triggerTime[triggerCount] = seek1.getProgress();
            multipleTriggers[triggerCount][0] = data[1];
            multipleTriggers[triggerCount][1] = data[2];
            multipleTriggersMot[triggerCount][0] = data[3];
            multipleTriggersMot[triggerCount][1] = data[4];
            multipleTriggersPh[triggerCount] = data[5];
            triggerTimeShow.append("\nTrigger on -" + milliSecondsToTimer((long) triggerTime[triggerCount]));
            triggerCount++;

        }
    }

    @OnClick(R.id.play)
    public void onClickPlay() {
        if(!isConnected()) onConnectToggleClick();
        if(isLoaded) {
            if(mp.isPlaying()) {
                mp.pause();
                trigger.setEnabled(true);
                trigger.setText("Set Triggers");
            }
            else {
                mp.start();
                seek1.getProgressDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
                seek1.setProgress(0);
                output.post(mUpdateTime);
                trigger.setEnabled(false);
            }
        }
        else
            Toast.makeText(this, "Choose A Song", Toast.LENGTH_SHORT).show();
    }

    public void onPause(){
        super.onPause();
        if(mp.isPlaying())mp.stop();
    }

    private Runnable mUpdateTime = new Runnable() {
        public void run() {
            int currentDuration;
            if (mp.isPlaying()) {
                currentDuration = mp.getCurrentPosition();
                if(triggerActive) checkPlayer(currentDuration);
                seek1.setProgress(currentDuration);
                output.postDelayed(this, 900);
            }else {
                output.removeCallbacks(this);
            }
        }
    };

    private void checkPlayer(int currentDuration){
       for(int i = 0 ; i<triggerCount ; i++)
       {
           if((int)triggerTime[i]/1000 == (int)currentDuration/1000) {
               trigger.setText("Sending " + Integer.toString(i + 1));
               data[1] = multipleTriggers[i][0];
               data[2] = multipleTriggers[i][1];
               data[3] = multipleTriggersMot[i][0];
               data[4] = multipleTriggersMot[i][1];
               data[5] = multipleTriggersPh[i];
               sendData();
           }
       }
    }



    @OnClick(R.id.connect)
    public void onConnectToggleClick() {

        if (isConnected()) {
            triggerDisconnect();
        } else {
            connectionObservable
                    .flatMap(RxBleConnection::discoverServices)
                    .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(characteristicUuid))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(() -> connectButton.setText(R.string.connecting))
                    .subscribe(
                            characteristic -> {
                                updateUI(characteristic);
                                Log.i(getClass().getSimpleName(), "Hey, connection has been established!");
                            },
                            this::onConnectionFailure,
                            this::onConnectionFinished
                    );
        }
    }


    @OnClick(R.id.read)
    public void onReadClick() {

        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.readCharacteristic(characteristicReadUuid))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bytes -> {
                        readOutputView.setText(new String(bytes));
                        readHexOutputView.setText(HexString.bytesToHex(bytes));
                        writeInput.setText(HexString.bytesToHex(bytes));
                    }, this::onReadFailure);
        }
    }

    @OnClick(R.id.write)
    public void onWriteClick() {

        if (isConnected()) {
            connectionObservable
                    .flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUuid, getInputBytes()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            bytes -> onWriteSuccess(),
                            this::onWriteFailure
                    );
        }
    }

    @OnClick(R.id.test)
    public void testSend() {
        if(!isConnected()) onConnectToggleClick();
        sendData() ;
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

    @OnClick(R.id.triggerActive)
    public void onToggle(){
        triggerActive = deviceActive.isChecked() ;

    }

    private boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void onConnectionFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Connection error: " + throwable, Snackbar.LENGTH_SHORT).show();
        updateUI(null);
    }

    private void onConnectionFinished() {
        updateUI(null);
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
        //Snackbar.make(findViewById(R.id.main), "Change: " + HexString.bytesToHex(bytes), Snackbar.LENGTH_SHORT).show();
        output.setText(HexString.bytesToHex(bytes));
      //  if(isLoaded && mp.isPlaying())
      //  {
      //      long x = mp.getCurrentPosition() ;
      //  }
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
     * @param characteristic a nullable {@link BluetoothGattCharacteristic}. If it is null then UI is assuming a disconnected state.
     */
    private void updateUI(BluetoothGattCharacteristic characteristic) {
        connectButton.setText(characteristic != null ? R.string.disconnect : R.string.connect);
        //readButton.setEnabled(hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ));
        readButton.setEnabled(isConnected());
        writeButton.setEnabled(hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE));
        notifyButton.setEnabled(isConnected());
        chooseFile.setEnabled(true);
    }

    private boolean hasProperty(BluetoothGattCharacteristic characteristic, int property) {
        return characteristic != null && (characteristic.getProperties() & property) > 0;
    }

    private byte[] getInputBytes() {
        return HexString.hexToBytes(writeInput.getText().toString());
    }

    private SeekBar.OnSeekBarChangeListener intSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            data[1] = (byte)progress ;
            intVal.setText("Intensity T: " +Integer.toString(progress));

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(mp.isPlaying()) mp.seekTo(seekTime);
        }
    };

    private SeekBar.OnSeekBarChangeListener intSeekMotListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            data[3] = (byte)progress ;
            intValMot.setText("Intensity M: " +Integer.toString(progress));

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(mp.isPlaying()) mp.seekTo(seekTime);
        }
    };

    private SeekBar.OnSeekBarChangeListener delSeekListener= new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            data[2] = (byte)progress ;
            delVal.setText("Duration: " +Integer.toString(progress*100)+" ms");

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(mp.isPlaying()) mp.seekTo(seekTime);
        }
    };

    private SeekBar.OnSeekBarChangeListener delSeekMotListener= new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            data[4] = (byte)progress ;
            delValMot.setText("Duration: " +Integer.toString(progress*100)+" ms");

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(mp.isPlaying()) mp.seekTo(seekTime);
        }
    };

    private SeekBar.OnSeekBarChangeListener phSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            data[5] = (byte)progress ;
            phVal.setText("Phase delay: "+Integer.toString((progress-20)*100)+" ms");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if(mp.isPlaying()) mp.seekTo(seekTime);
        }
    };

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
