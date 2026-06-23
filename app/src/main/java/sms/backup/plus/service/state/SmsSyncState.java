package sms.backup.plus.service.state;

public enum SmsSyncState {
    INITIAL,
    CALC,
    LOGIN,
    BACKUP,
    RESTORE,
    ERROR,
    CANCELED_BACKUP,
    CANCELED_RESTORE,
    FINISHED_BACKUP,
    FINISHED_RESTORE,
    UPDATING_THREADS
}
