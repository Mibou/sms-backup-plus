/* Copyright (c) 2009 Christoph Studer <chstuder@gmail.com>
 * Copyright (c) 2010 Jan Berkel <jan.berkel@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sms.backup.plus.service;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import sms.backup.plus.mail.DataType;
import sms.backup.plus.preferences.Preferences;

import java.util.concurrent.TimeUnit;

import static sms.backup.plus.App.LOCAL_LOGV;
import static sms.backup.plus.App.TAG;
import static sms.backup.plus.Consts.CALLLOG_PROVIDER;
import static sms.backup.plus.Consts.SMS_PROVIDER;
import static sms.backup.plus.service.BackupType.BROADCAST_INTENT;
import static sms.backup.plus.service.BackupType.INCOMING;
import static sms.backup.plus.service.BackupType.REGULAR;

/**
 * Schedules backups using {@link WorkManager}. WorkManager transparently uses
 * {@code JobScheduler} on API 23+ and {@code AlarmManager} on older platforms, replacing the
 * (now unmaintained) firebase-jobdispatcher and its hand-rolled AlarmManager driver.
 */
public class BackupJobs {
    private static final int BOOT_BACKUP_DELAY = 60;
    // initial backoff, exponential: [ 30, 60, 120, 240, ... ] seconds
    private static final long BACKOFF_DELAY_SECONDS = 30;

    static final String CONTENT_TRIGGER_TAG = "contentTrigger";
    static final String DATA_BACKUP_TYPE = "backup_type";
    static final String DATA_CONTENT_TRIGGER = "content_trigger";

    private final Context context;
    private final Preferences preferences;

    public BackupJobs(Context context) {
        this(context, new Preferences(context));
    }

    BackupJobs(Context context, Preferences preferences) {
        this.context = context.getApplicationContext();
        this.preferences = preferences;
    }

    private WorkManager workManager() {
        // resolved lazily so that merely constructing BackupJobs (e.g. during Application#onCreate)
        // does not force WorkManager to initialize
        return WorkManager.getInstance(context);
    }

    public void scheduleIncoming() {
        schedule(preferences.getIncomingTimeoutSecs(), INCOMING, false);
    }

    public void scheduleRegular() {
        schedule(preferences.getRegularTimeoutSecs(), REGULAR, false);
    }

    public void scheduleBootup() {
        if (!preferences.isAutoBackupEnabled()) {
            Log.d(TAG, "auto backup no longer enabled, canceling all jobs");
            cancelAll();
        } else {
            schedule(BOOT_BACKUP_DELAY, REGULAR, false);
        }
    }

    public void scheduleImmediate() {
        schedule(-1, BROADCAST_INTENT, true);
    }

    public void scheduleContentTriggerJob() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (LOCAL_LOGV) Log.v(TAG, "content uri triggers not supported on this platform");
            return;
        }
        final OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SmsBackupWorker.class)
            .addTag(CONTENT_TRIGGER_TAG)
            .setConstraints(contentTriggerConstraints())
            .setInputData(new Data.Builder()
                .putString(DATA_BACKUP_TYPE, INCOMING.name())
                .putBoolean(DATA_CONTENT_TRIGGER, true)
                .build())
            .build();
        enqueue(CONTENT_TRIGGER_TAG, request);
    }

    public void cancelAll() {
        cancelRegular();
        cancelContentUriTrigger();
    }

    public void cancelRegular() {
        cancel(REGULAR.name());
    }

    private void cancelContentUriTrigger() {
        cancel(CONTENT_TRIGGER_TAG);
    }

    private void cancel(String uniqueName) {
        if (LOCAL_LOGV) Log.v(TAG, "cancel(" + uniqueName + ")");
        workManager().cancelUniqueWork(uniqueName);
    }

    private void schedule(int inSeconds, BackupType backupType, boolean force) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "scheduleBackup(" + inSeconds + ", " + backupType + ", " + force + ")");
        }

        if (force || (preferences.isAutoBackupEnabled() && inSeconds > 0)) {
            enqueue(backupType.name(), createRequest(inSeconds, backupType));
            if (LOCAL_LOGV) {
                Log.v(TAG, "Scheduled backup job " + backupType + " due " +
                        (inSeconds > 0 ? "in " + inSeconds + " seconds" : "now"));
            }
        } else {
            if (LOCAL_LOGV) Log.v(TAG, "Not scheduling backup because auto backup is disabled.");
        }
    }

    private void enqueue(String uniqueName, OneTimeWorkRequest request) {
        workManager().enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request);
    }

    private @NonNull OneTimeWorkRequest createRequest(int inSeconds, BackupType backupType) {
        final OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(SmsBackupWorker.class)
            .addTag(backupType.name())
            .setConstraints(jobConstraints(backupType))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
            .setInputData(new Data.Builder().putString(DATA_BACKUP_TYPE, backupType.name()).build());

        if (inSeconds > 0) {
            builder.setInitialDelay(inSeconds, TimeUnit.SECONDS);
        }
        return builder.build();
    }

    private Constraints jobConstraints(BackupType backupType) {
        if (backupType == BROADCAST_INTENT) {
            return Constraints.NONE;
        }
        return new Constraints.Builder()
            .setRequiredNetworkType(networkType())
            .build();
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private Constraints contentTriggerConstraints() {
        final Constraints.Builder builder = new Constraints.Builder()
            .setRequiredNetworkType(networkType())
            .addContentUriTrigger(SMS_PROVIDER, true);

        if (preferences.getDataTypePreferences().isBackupEnabled(DataType.CALLLOG)
                && preferences.isCallLogBackupAfterCallEnabled()) {
            builder.addContentUriTrigger(CALLLOG_PROVIDER, true);
        }
        return builder.build();
    }

    private NetworkType networkType() {
        return preferences.isWifiOnly() ? NetworkType.UNMETERED : NetworkType.CONNECTED;
    }
}
