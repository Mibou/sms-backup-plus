package sms.backup.plus.activity.events;

public class AccountConnectionChangedEvent {
    public final boolean connected;

    public AccountConnectionChangedEvent(boolean connect) {
        this.connected = connect;
    }
}
