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
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker.Result;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;
import androidx.work.testing.TestListenableWorkerBuilder;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.google.common.util.concurrent.ListenableFuture;
import com.squareup.otto.Subscribe;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

import sms.backup.plus.App;
import sms.backup.plus.service.exception.MissingPermissionException;
import sms.backup.plus.service.state.BackupState;
import sms.backup.plus.service.state.SmsSyncState;

import static com.google.common.truth.Truth.assertThat;
import static sms.backup.plus.service.BackupJobs.CONTENT_TRIGGER_TAG;
import static sms.backup.plus.service.BackupJobs.DATA_BACKUP_TYPE;
import static sms.backup.plus.service.BackupJobs.DATA_CONTENT_TRIGGER;
import static sms.backup.plus.service.BackupType.INCOMING;
import static sms.backup.plus.service.BackupType.REGULAR;
import static sms.backup.plus.service.state.SmsSyncState.ERROR;
import static sms.backup.plus.service.state.SmsSyncState.FINISHED_BACKUP;

@RunWith(RobolectricTestRunner.class)
public class SmsBackupWorkerTest {
    private WorkManager workManager;

    @Before public void before() {
        WorkManagerTestInitHelper.initializeTestWorkManager(RuntimeEnvironment.application);
        workManager = WorkManager.getInstance(RuntimeEnvironment.application);
    }

    @Test @Config(sdk = Build.VERSION_CODES.N)
    public void shouldRearmContentTriggerAndSucceedForContentTriggerRun() throws Exception {
        SmsBackupWorker worker = buildWorker(new Data.Builder()
            .putString(DATA_BACKUP_TYPE, INCOMING.name())
            .putBoolean(DATA_CONTENT_TRIGGER, true)
            .build());

        Result result = worker.startWork().get();

        // a content trigger run only re-arms the single-shot observer and finishes immediately
        assertThat(result).isEqualTo(Result.success());
        assertScheduled(CONTENT_TRIGGER_TAG);
    }

    @Test public void shouldSkipRegularBackupWhenAutoBackupDisabled() throws Exception {
        // auto backup defaults to disabled, so a scheduled REGULAR run must be skipped, not executed
        SmsBackupWorker worker = buildWorker(new Data.Builder()
            .putString(DATA_BACKUP_TYPE, REGULAR.name())
            .build());

        Result result = worker.startWork().get();

        assertThat(result).isEqualTo(Result.success());
        assertNotScheduled(REGULAR.name());
    }

    @Test public void shouldPostSystemCancelEventWhenStopped() {
        SmsBackupWorker worker = buildWorker(new Data.Builder()
            .putString(DATA_BACKUP_TYPE, REGULAR.name())
            .build());
        RecordingListener listener = new RecordingListener();
        App.register(listener);
        try {
            worker.onStopped();
        } finally {
            App.unregister(listener);
        }

        assertThat(listener.cancelEvent).isNotNull();
        // SYSTEM origin -> the running backup may be interrupted
        assertThat(listener.cancelEvent.mayInterruptIfRunning()).isTrue();
    }

    @Test public void shouldIgnoreBackupStateBeforeWorkStarted() {
        // no startWork() call means there is no completer yet; stray state events must be a no-op
        SmsBackupWorker worker = buildWorker(new Data.Builder()
            .putString(DATA_BACKUP_TYPE, REGULAR.name())
            .build());

        worker.backupStateChanged(new BackupState()); // INITIAL, not finished
        worker.backupStateChanged(state(FINISHED_BACKUP, INCOMING, null)); // finished but wrong type
        // reaching here without a NullPointerException is the assertion
    }

    @Test public void shouldSucceedWhenBackupFinishesSuccessfully() throws Exception {
        TestWorker worker = startedWorker(INCOMING);

        worker.backupStateChanged(state(FINISHED_BACKUP, INCOMING, null));

        assertThat(worker.future.isDone()).isTrue();
        assertThat(worker.future.get()).isEqualTo(Result.success());
    }

    @Test public void shouldRetryWhenBackupFinishesWithError() throws Exception {
        TestWorker worker = startedWorker(INCOMING);

        worker.backupStateChanged(state(ERROR, INCOMING, new RuntimeException("boom")));

        assertThat(worker.future.get()).isEqualTo(Result.retry());
    }

    @Test public void shouldNotRetryWhenBackupFailsWithMissingPermission() throws Exception {
        TestWorker worker = startedWorker(INCOMING);

        worker.backupStateChanged(state(ERROR, INCOMING,
            new MissingPermissionException(Collections.<String>emptySet())));

        // a missing permission will not be fixed by retrying, so the run is just marked done
        assertThat(worker.future.get()).isEqualTo(Result.success());
    }

    @Test public void shouldStayPendingForUnrelatedBackupState() {
        TestWorker worker = startedWorker(INCOMING);

        worker.backupStateChanged(state(FINISHED_BACKUP, REGULAR, null)); // wrong type
        worker.backupStateChanged(new BackupState());                     // not finished

        assertThat(worker.future.isDone()).isFalse();
    }

    private SmsBackupWorker buildWorker(Data inputData) {
        return TestListenableWorkerBuilder
            .from(RuntimeEnvironment.application, SmsBackupWorker.class)
            .setInputData(inputData)
            .build();
    }

    /** Builds a worker whose backup is stubbed out, then starts it so the completer is armed. */
    private TestWorker startedWorker(BackupType backupType) {
        TestWorker worker = TestListenableWorkerBuilder
            .from(RuntimeEnvironment.application, TestWorker.class)
            .setInputData(new Data.Builder().putString(DATA_BACKUP_TYPE, backupType.name()).build())
            .build();
        worker.future = worker.startWork();
        return worker;
    }

    private BackupState state(SmsSyncState syncState, BackupType type, Exception exception) {
        return new BackupState(syncState, 0, 0, type, null, exception);
    }

    private void assertScheduled(String uniqueName) throws Exception {
        final List<WorkInfo> infos = workManager.getWorkInfosForUniqueWork(uniqueName).get();
        assertThat(infos).hasSize(1);
        assertThat(infos.get(0).getState()).isNotEqualTo(WorkInfo.State.CANCELLED);
        assertThat(infos.get(0).getTags()).contains(uniqueName);
    }

    private void assertNotScheduled(String uniqueName) throws Exception {
        final List<WorkInfo> infos = workManager.getWorkInfosForUniqueWork(uniqueName).get();
        for (WorkInfo info : infos) {
            assertThat(info.getState()).isNotEqualTo(WorkInfo.State.ENQUEUED);
        }
    }

    private static class RecordingListener {
        CancelEvent cancelEvent;

        @Subscribe public void onCancel(CancelEvent event) {
            cancelEvent = event;
        }
    }

    /**
     * Stubs out the heavyweight {@link SmsBackupService} so the completion logic in
     * {@link SmsBackupWorker#backupStateChanged} can be driven directly. {@code startWork()}
     * still arms the completer; the future it returns is captured for assertions.
     */
    public static class TestWorker extends SmsBackupWorker {
        ListenableFuture<Result> future;

        public TestWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @Override protected void startBackup(String backupType) {
            // do nothing: leave the future pending until a BackupState is delivered
        }
    }
}
