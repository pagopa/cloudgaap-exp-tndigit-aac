package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

public class NoSuchRoleException extends Exception {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public NoSuchRoleException() {
        super();
    }

    public NoSuchRoleException(String message) {
        super(message);
    }

    public NoSuchRoleException(Throwable cause) {
        super(cause);
    }

    public NoSuchRoleException(String message, Throwable cause) {
        super(message, cause);
    }
}
