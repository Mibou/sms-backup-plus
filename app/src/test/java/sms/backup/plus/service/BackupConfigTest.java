package sms.backup.plus.service;

import sms.backup.plus.contacts.ContactGroup;
import sms.backup.plus.mail.BackupImapStore;
import sms.backup.plus.mail.DataType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.EnumSet;

import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class BackupConfigTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckForDataTypesEmpty() throws Exception {
        new BackupConfig(mock(BackupImapStore.class),
                0,
                -1,
                ContactGroup.EVERYBODY,
                BackupType.MANUAL,
                EnumSet.noneOf(DataType.class),
                false);

    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckForDataTypesNull() throws Exception {
        new BackupConfig(mock(BackupImapStore.class),
                0,
                -1,
                ContactGroup.EVERYBODY,
                BackupType.MANUAL,
                null,
                false);
    }


    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckForPositiveTry() throws Exception {
        new BackupConfig(mock(BackupImapStore.class),
                -1,
                -1,
                ContactGroup.EVERYBODY,
                BackupType.MANUAL,
                EnumSet.of(DataType.MMS),
                false);
    }
}
