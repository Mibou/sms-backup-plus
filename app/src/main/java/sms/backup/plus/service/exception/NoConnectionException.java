package sms.backup.plus.service.exception;

import sms.backup.plus.R;

public class NoConnectionException extends ConnectivityException {
    public NoConnectionException() {
        super(null);
    }

    @Override public int errorResourceId() {
        return R.string.error_no_connection;
    }
}
