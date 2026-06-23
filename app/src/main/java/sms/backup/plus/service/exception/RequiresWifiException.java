package sms.backup.plus.service.exception;

import sms.backup.plus.R;

public class RequiresWifiException extends ConnectivityException {
    public RequiresWifiException() {
        super(null);
    }

    @Override
    public int errorResourceId() {
        return R.string.error_wifi_only_no_connection;
    }
}
