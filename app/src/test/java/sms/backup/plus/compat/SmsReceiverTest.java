package sms.backup.plus.compat;

import android.content.Intent;
import android.os.Build;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public class SmsReceiverTest {
    private SmsReceiver subject;

    @Before
    public void setUp() throws Exception {
        subject = new SmsReceiver();
    }

    // minSdk is 21, so the pre-KitKat (< 19) branch is unreachable; LOLLIPOP is the lowest
    // supported SDK and exercises the pre-Q default-SMS-package branch.
    @Test @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void testOnReceive() {
        subject.onReceive(RuntimeEnvironment.application, new Intent());
    }

    @Test @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void testIsSmsBackupNotDefaultSmsApp() {
        assertThat(SmsReceiver.isSmsBackupDefaultSmsApp(RuntimeEnvironment.application)).isFalse();
    }
}
