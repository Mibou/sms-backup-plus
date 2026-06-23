package sms.backup.plus.activity.fragments;

import android.os.Bundle;
import android.os.Handler;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import sms.backup.plus.App;
import sms.backup.plus.R;
import sms.backup.plus.activity.events.AutoBackupSettingsChangedEvent;
import sms.backup.plus.preferences.Preferences;

public abstract class SMSBackupPreferenceFragment extends PreferenceFragmentCompat {
    protected Preferences preferences;
    private Handler handler;

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        preferences = new Preferences(getContext(), getPreferenceManager().getSharedPreferences());
        handler = new Handler();
    }

    void addPreferenceListener(String... prefKeys) {
        addPreferenceListener(new AutoBackupSettingsChangedEvent(), prefKeys);
    }

    void addPreferenceListener(final Object event, String... prefKeys) {
        for (String prefKey : prefKeys) {
            findPreference(prefKey).setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        public boolean onPreferenceChange(Preference preference, final Object newValue) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    App.post(event);
                                }
                            });
                            return true;
                        }
                    });
        }
    }
}
