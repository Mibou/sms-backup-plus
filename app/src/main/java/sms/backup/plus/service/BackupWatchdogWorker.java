/* Copyright (c) 2017 Jan Berkel <jan.berkel@gmail.com>
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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import sms.backup.plus.preferences.Preferences;

import static sms.backup.plus.App.LOCAL_LOGV;
import static sms.backup.plus.App.TAG;

/**
 * Periodic safety net for the regular backup chain.
 *
 * <p>Regular backups are driven by a chain of one-time jobs where each run schedules the next
 * one when it finishes (see {@link BackupJobs} and {@link SmsBackupService#scheduleNextBackup}).
 * If a run is interrupted before it can reschedule — the process is killed mid-backup, the worker
 * is stopped when a constraint is lost, or the backup never reports a finished state — the chain
 * stops and backups silently cease until the app is next reconfigured or the device reboots.
 *
 * <p>This worker runs on a {@link androidx.work.PeriodicWorkRequest}, which WorkManager
 * re-enqueues itself and persists across process death and reboots. Each run simply re-primes the
 * regular chain if it is missing (see {@link BackupJobs#scheduleRegularIfMissing()}), so a broken
 * chain recovers within one watchdog interval without disturbing a chain that is still healthy.
 */
public class BackupWatchdogWorker extends Worker {

    public BackupWatchdogWorker(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    @NonNull @Override
    public Result doWork() {
        final Context context = getApplicationContext();
        final BackupJobs backupJobs = new BackupJobs(context);
        if (new Preferences(context).isAutoBackupEnabled()) {
            if (LOCAL_LOGV) Log.v(TAG, "watchdog: ensuring the regular backup is scheduled");
            backupJobs.scheduleRegularIfMissing();
        } else {
            // auto backup was turned off since the watchdog was scheduled; stop re-priming and
            // tear the periodic watchdog down.
            if (LOCAL_LOGV) Log.v(TAG, "watchdog: auto backup disabled, canceling");
            backupJobs.cancelRegular();
            backupJobs.cancelWatchdog();
        }
        return Result.success();
    }
}
