package sms.backup.plus.activity.events;

public class FallbackAuthEvent {
    public final boolean showDialog;

    public FallbackAuthEvent(boolean showDialog) {
        this.showDialog = showDialog;
    }
}
