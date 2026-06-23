package sms.backup.plus.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import sms.backup.plus.App;
import sms.backup.plus.activity.events.AutoBackupSettingsChangedEvent;

import static sms.backup.plus.App.LOCAL_LOGV;
import static sms.backup.plus.App.TAG;

public class PackageReplacedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOCAL_LOGV) Log.v(TAG, "onReceive(" + context + "," + intent + ")");

        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            Log.d(TAG, "now installed version: " + App.getVersionCode(context));
            //  just post event and let application handle the rest
            App.post(new AutoBackupSettingsChangedEvent());
        } else {
            Log.w(TAG, "unhandled intent: "+intent);
        }
    }
}
