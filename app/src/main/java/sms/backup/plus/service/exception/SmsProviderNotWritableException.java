package sms.backup.plus.service.exception;

import sms.backup.plus.R;

public class SmsProviderNotWritableException extends Exception implements LocalizableException {
    @Override public int errorResourceId() {
        return R.string.error_sms_provider_not_writable;
    }
}
