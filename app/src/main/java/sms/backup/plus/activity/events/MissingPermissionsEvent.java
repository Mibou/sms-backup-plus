package sms.backup.plus.activity.events;

import sms.backup.plus.activity.AppPermission;

import java.util.List;

public class MissingPermissionsEvent {
    public final List<AppPermission> permissions;

    public MissingPermissionsEvent(List<AppPermission> permissions) {
        this.permissions = permissions;
    }
}
