package sms.backup.plus.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import sms.backup.plus.service.BackupJobs;

import static sms.backup.plus.App.LOCAL_LOGV;
import static sms.backup.plus.App.TAG;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOCAL_LOGV) Log.v(TAG, "onReceive(" + context + "," + intent + ")");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            bootup(context);
        } else {
            Log.w(TAG, "unhandled intent: "+intent);
        }
    }

    private void bootup(Context context) {
        Log.i(TAG, "bootup");
        getBackupJobs(context).scheduleBootup();
    }

    protected BackupJobs getBackupJobs(Context context) {
        return new BackupJobs(context);
    }
}
