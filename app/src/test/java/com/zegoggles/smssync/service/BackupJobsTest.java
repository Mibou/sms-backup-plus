package com.zegoggles.smssync.service;

import android.os.Build;

import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.zegoggles.smssync.preferences.DataTypePreferences;
import com.zegoggles.smssync.preferences.Preferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class BackupJobsTest {
    private BackupJobs subject;
    private WorkManager workManager;

    @Mock private Preferences preferences;
    @Mock private DataTypePreferences dataTypePreferences;

    @Before public void before() {
        initMocks(this);
        WorkManagerTestInitHelper.initializeTestWorkManager(RuntimeEnvironment.application);
        workManager = WorkManager.getInstance(RuntimeEnvironment.application);
        subject = new BackupJobs(RuntimeEnvironment.application, preferences);
        when(preferences.getDataTypePreferences()).thenReturn(dataTypePreferences);
    }

    @Test public void shouldScheduleImmediate() throws Exception {
        subject.scheduleImmediate();
        assertScheduled(BackupType.BROADCAST_INTENT.name());
    }

    @Test public void shouldScheduleRegular() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getRegularTimeoutSecs()).thenReturn(2000);
        subject.scheduleRegular();
        assertScheduled(BackupType.REGULAR.name());
    }

    @Test public void shouldScheduleIncoming() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getIncomingTimeoutSecs()).thenReturn(2000);
        subject.scheduleIncoming();
        assertScheduled(BackupType.INCOMING.name());
    }

    @Test @Config(sdk = Build.VERSION_CODES.N)
    public void shouldScheduleContentTriggerJob() throws Exception {
        subject.scheduleContentTriggerJob();
        assertScheduled(BackupJobs.CONTENT_TRIGGER_TAG);
    }

    @Test @Config(sdk = Build.VERSION_CODES.M)
    public void shouldNotScheduleContentTriggerJobBeforeApi24() throws Exception {
        subject.scheduleContentTriggerJob();
        assertNotScheduled(BackupJobs.CONTENT_TRIGGER_TAG);
    }

    @Test public void shouldScheduleRegularJobAfterBoot() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(true);
        when(preferences.getRegularTimeoutSecs()).thenReturn(2000);
        subject.scheduleBootup();
        assertScheduled(BackupType.REGULAR.name());
    }

    @Test public void shouldCancelAllJobsAfterBootIfAutoBackupDisabled() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(false);
        subject.scheduleBootup();
        assertNotScheduled(BackupType.REGULAR.name());
    }

    @Test public void shouldNotScheduleRegularBackupIfAutoBackupIsDisabled() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(false);
        subject.scheduleRegular();
        assertNotScheduled(BackupType.REGULAR.name());
    }

    @Test public void shouldNotScheduleIncomingBackupIfAutoBackupIsDisabled() throws Exception {
        when(preferences.isAutoBackupEnabled()).thenReturn(false);
        subject.scheduleIncoming();
        assertNotScheduled(BackupType.INCOMING.name());
    }

    private void assertScheduled(String uniqueName) throws Exception {
        // Unconstrained work (the immediate backup) may already have run to SUCCEEDED under the
        // test scheduler, while delayed work stays ENQUEUED; either way it must not be cancelled.
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
}
