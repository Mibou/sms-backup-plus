package sms.backup.plus.activity.fragments;

import static sms.backup.plus.preferences.Preferences.Keys.INCOMING_TIMEOUT_SECONDS;
import static sms.backup.plus.preferences.Preferences.Keys.REGULAR_TIMEOUT_SECONDS;
import static sms.backup.plus.preferences.Preferences.Keys.WIFI_ONLY;

public class AutoBackupSettings extends SMSBackupPreferenceFragment {
    @Override
    public void onResume() {
        super.onResume();

        addPreferenceListener(
            INCOMING_TIMEOUT_SECONDS.key,
            REGULAR_TIMEOUT_SECONDS.key,
            WIFI_ONLY.key
        );
    }
}
