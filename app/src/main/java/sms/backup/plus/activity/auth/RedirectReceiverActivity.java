package sms.backup.plus.activity.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import sms.backup.plus.App;
import sms.backup.plus.auth.OAuth2Client;

import static sms.backup.plus.App.TAG;

public class RedirectReceiverActivity extends Activity {
    static class BrowserAuthResult {
        final String code;
        final String error;

        BrowserAuthResult(String code, String error) {
            this.code = code;
            this.error = error;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: " +savedInstanceState);
        super.onCreate(savedInstanceState);
        handleRedirectIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleRedirectIntent(intent);
    }

    private void handleRedirectIntent(Intent intent) {
        if (OAuth2Client.REDIRECT_URL.getScheme().equals(intent.getScheme())) {
            final String code = intent.getData().getQueryParameter("code");
            final String error = intent.getData().getQueryParameter("error");

            App.post(new BrowserAuthResult(code, error));
        }
        finish();
    }
}
