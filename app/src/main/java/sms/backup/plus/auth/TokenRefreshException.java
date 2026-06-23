package sms.backup.plus.auth;

public class TokenRefreshException extends Exception {
    public TokenRefreshException(String detailMessage) {
        super(detailMessage);
    }

    public TokenRefreshException(Throwable e) {
        super(e);
    }
}
