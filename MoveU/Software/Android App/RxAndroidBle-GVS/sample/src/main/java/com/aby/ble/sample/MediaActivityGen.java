package com.aby.ble.sample;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.utils.ConnectionSharingAdapter;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

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

public class MediaActivityGen extends RxAppCompatActivity implements SensorEventListener{


    @BindView(R.id.Status_Info)
    TextView output ;
    @BindView(R.id.notify)
    Button notify;
    @BindView(R.id.dataView)
    TextView dataView;
    @BindView(R.id.saveTrigger)
    Switch saveTrigger ;
    @BindView(R.id.editText)
    TextView editText ;
    @BindView(R.id.play1)
    Button play1 ;
    @BindView(R.id.play2)
    Button play2 ;
    @BindView(R.id.l1)
    Button l1 ;
    @BindView(R.id.r1)
    Button r1 ;
    @BindView(R.id.stop)
    Button stop ;
    @BindView(R.id.status_dir)
    TextView status_dir;
    @BindView(R.id.status_rt)
    TextView status_rt;
    @BindView(R.id.strength_seek)
    SeekBar strength_seek ;
    @BindView(R.id.strength_status)
    TextView strength_status ;

    private UUID characteristicReadUuid = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private UUID characteristicUuid = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private PublishSubject<Void> disconnectTriggerSubject = PublishSubject.create();
    private Observable<RxBleConnection> connectionObservable;
    private RxBleDevice bleDevice;


    byte[] data = {20,0,0,0};
    // Start (Flag), Strength(L) , Strength(R) , Strength(C)

    boolean fileDefine = false ;
    boolean fileWrite = false ;

    byte strength  = 90 ;
    int x_ax = 0 ;
    int y_ax = 0 ;
    int y_ax_point = 0 ;
    boolean x_enable , y_enable ;
    double cosine ;
    boolean calibrate ;
    CSVWriter writer;


    private SensorManager mSensorManager;
    private Sensor accelerometer;

    private float[] gravityValues = null;
    private float[] magneticValues = null;

    LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
            new DataPoint(0, 0),
            new DataPoint(1, 0)
    });
    int graphx = 2 ;

    boolean gyro_t = false ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_gen);
        ButterKnife.bind(this);
        String macAddress = getIntent().getStringExtra(DeviceActivity.EXTRA_MAC_ADDRESS);
        bleDevice = GVSControl.getRxBleClient(this).getBleDevice(macAddress);
        connectionObservable = prepareConnectionObservable();
        //noinspection ConstantConditions
        getSupportActionBar().setSubtitle(getString(R.string.mac_address, macAddress));
        output.setText("Connecting");
        if(!isConnected()) onConnectToggleClick();
        //onNotifyClick();
        status_dir.setText("Neutral");
        initSeek(strength);
        x_enable = false ;
        y_enable = false ;
        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.addSeries(series);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(40);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-10000);
        graph.getViewport().setMaxY(10000);
        calibrate = false ;

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_NORMAL);
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
        //Snackbar.make(findViewById(R.id.main), "Write success", Snackbar.LENGTH_SHORT).show();

    }

    private void onWriteFailure(Throwable throwable) {
        //noinspection ConstantConditions
        Snackbar.make(findViewById(R.id.main), "Write error: " + throwable, Snackbar.LENGTH_SHORT).show();
    }

    private void onNotificationReceived(byte[] bytes) {
        //noinspection ConstantConditions
        int numValues = 8 ;
        int idx[] = new int[numValues] ;
        String str = " " ;
        for(int i = 0 ; i < numValues ; i++) {
            idx[i] += bytes[2*i] & 0xFF;
            idx[i] += (bytes[2*i + 1] & 0xFF) << 8;
            //idx[i] += (bytes[4*i + 2] & 0xFF) << 16;
            //idx[i] += (bytes[4*i + 3] & 0xFF) << 24;
            str += Integer.toString(idx[i]) + " " ;
        }

        dataView.setText(str);
        String[] dat = {Integer.toString(idx[0]),Integer.toString(idx[1]),Integer.toString(idx[2]),Integer.toString(idx[3]),Integer.toString(idx[4]),
                         Integer.toString(idx[5]),Integer.toString(idx[6]),Integer.toString(idx[7])};
        if(fileWrite) {writer.writeNext(dat);}
        //Assuming idx3 is X axis and idx4 is y
        x_ax = (short)idx[4];
        y_ax = (short)idx[5];
        y_ax_point = y_ax ;

        if(!calibrate) {
            cosine = x_ax / 16384.0;
            calibrate = true ;
        }
        y_ax = (int) ((double)(y_ax)*cosine );
        x_ax = (int) ((double)(x_ax)*cosine );
        if(gyro_t) y_ax = 10*(short)idx[7];
        //status_dir.setText("Working? " +  Double.toString(cosine));
        if(x_enable) series.appendData(new DataPoint(graphx++,x_ax),true,40);
        else series.appendData(new DataPoint(graphx++,y_ax),true,40);
        //feedback();
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




/*    @OnClick(R.id.play1)
    public void onClickPlay1() {
        if(!isConnected()) onConnectToggleClick();
        play1.setEnabled(false);
        x_enable = true ;
        play2.setEnabled(true);
        y_enable = false ;
        status_rt.setText("X-axis Feedback");
    }

    @OnClick(R.id.play2)
    public void onClickPlay2() {
        if(!isConnected()) onConnectToggleClick();
        play2.setEnabled(false);
        y_enable = true ;
        play1.setEnabled(true);
        x_enable = false ;
        status_rt.setText(" Y-axis Feedback");
    }*/

    @OnClick(R.id.play1)
    public void onClickPlay1() {
        if(!isConnected()) onConnectToggleClick();
        data[1] = 0 ;
        data[2] = 0 ;
        data[3] = 50 ;
        sendData();

    }

    @OnClick(R.id.play2)
    public void onClickPlay2() {
        if(!isConnected()) onConnectToggleClick();
        data[1] = 0 ;
        data[2] = 0 ;
        data[3] = 60 ;
        sendData();

    }


    @OnClick(R.id.stop)
    public void onClickStop() {
        if(!isConnected()) onConnectToggleClick();
        play1.setEnabled(true);
        play2.setEnabled(true);
        l1.setEnabled(true);
        r1.setEnabled(true);
        x_enable = false;
        y_enable  = false;
        data[1] = 0 ;
        data[2] = 0 ;
        data[3] = 0 ;
        sendData();
        status_rt.setText("No Feedback");
    }

    @OnClick(R.id.l1)
    public void onClickl1() {
        if(!isConnected()) onConnectToggleClick();
        data[1] = 100 ;
        data[2] = strength ;
        sendData();
        //status_rt.setText("Left " + Byte.toString(strength));
        status_rt.setText("Calibrated");
        status_dir.setText("IMU Point " +  Integer.toString(y_ax_point));
    }

    @OnClick(R.id.r1)
    public void onClickr1() {
        if(!isConnected()) onConnectToggleClick();
        data[1] = 0 ;
        data[2] = strength ;
        sendData();
        status_rt.setText("Right " + Byte.toString(strength));
        gyro_t = !gyro_t;
        if(gyro_t)status_dir.setText("Gyro");
        else status_dir.setText("Acc");
    }

    private void initSeek(int x){
        strength_seek.setMax(100);
        strength_seek.setProgress(x);
        strength_seek.setOnSeekBarChangeListener(mSeekBar);
        strength_seek.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        strength_status.setText("" + Integer.toString(x));
    }

    private SeekBar.OnSeekBarChangeListener mSeekBar = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            strength_status.setText("" + Integer.toString(progress));
            //seekTime = progress ;
            strength = (byte)progress ;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void feedback(){
        if(y_enable){
            if(y_ax < -3500) {
                data[1] = strength ;
                data[2] = 0 ;
            }
            else if(y_ax >=3500) {
                data[1] = 0 ;
                data[2] = strength ;
            }
            else {
                data[1] = 0 ;
                data[2] = 0 ;
            }
            sendData();
        }
        else if(x_enable){
            if(x_ax < -3500) {
                data[1] = strength ;
                data[2] = 0 ;
            }
            else if(x_ax >=3500) {
                data[1] = 0 ;
                data[2] = strength ;
            }
            else {
                data[1] = 0 ;
                data[2] = 0 ;
            }
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

    @Override
    public void onSensorChanged(SensorEvent event){
     /*  if ((gravityValues != null) && (magneticValues != null)
                && (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {

            float[] deviceRelativeAcceleration = new float[4];
            deviceRelativeAcceleration[0] = event.values[0];
            deviceRelativeAcceleration[1] = event.values[1];
            deviceRelativeAcceleration[2] = event.values[2];
            deviceRelativeAcceleration[3] = 0;

            // Change the device relative acceleration values to earth relative values
            // X axis -> East
            // Y axis -> North Pole
            // Z axis -> Sky

            float[] R = new float[16], I = new float[16], earthAcc = new float[16];

            SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);

            float[] inv = new float[16];

            android.opengl.Matrix.invertM(inv, 0, R, 0);
            android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);
            Log.d("Acceleration", "Values: (" + earthAcc[0] + ", " + earthAcc[1] + ", " + earthAcc[2] + ")");

        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravityValues = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = event.values;
        }*/
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}
