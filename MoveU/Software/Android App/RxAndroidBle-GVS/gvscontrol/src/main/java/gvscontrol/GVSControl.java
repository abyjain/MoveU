package gvscontrol;

import android.app.Application;
import android.content.Context;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.internal.RxBleLog;


public class GVSControl extends Application {

    private RxBleClient rxBleClient;
    /**
     * In practise you will use some kind of dependency injection pattern.
     */
    public static RxBleClient getRxBleClient(Context context) {
        GVSControl application = (GVSControl) context.getApplicationContext();
        return application.rxBleClient;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        rxBleClient = RxBleClient.create(this);
        RxBleClient.setLogLevel(RxBleLog.VERBOSE);
    }

}
