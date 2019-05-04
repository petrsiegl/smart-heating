/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

/**
 * Status codes and messages for view context.
 *
 * @author Petr Siegl
 */
public enum StatusCode {
	NONE(0, ""),
	PIN_EXISTS(1, "Device connected to that pin already exists."),
	ID_EXISTS(2, "Device with that ID already exists."),
	DEVICE_CREATED(3, "Device created."),
	PIN_FAIL(4, "Chosen pin can't be found on this device."),
	NO_HANDLER(5, "No handler for specified device type assigned, device can't be created."),
	ID_FAIL(6, "Please make sure your non-empty id contains only numbers, letters, underscore or hyphen and is less then 100 characters long."),
	CLASS_FAIL(7, "Specified device type not loaded in system."),
	CREATION_FAIL(8, "Error occured while creating device, please try again. In case of a reccurance refer to the system log."),
	ACC_FAIL(9, "Problem occurred, try again."),
	ACC_PASS_CHANGED(10, "Password has been changed."),
	USR_CREATE_FAIL(11, "User creation failed, try again."),
	USR_MOD_FAIL(12, "Modification of chosen user failed, try again."),
	USR_DEL_FAIL(13, "Deletion failed, try again."),
	USR_INP_FAIL(14, "Incorrect input values for username or password."),
	USR_PASS_FAIL(15, String.format("Please make sure your password is from %s to %s characters long.",
			ControllerServlet.PASS_MIN_LENGTH, ControllerServlet.PASS_MAX_LENGTH)),
	USR_NOT_FOUND(16, "User not found."),
	USR_MOD_SUCC(17, "User modified."),
	USR_DEL_SUCC(18, "User deleted."),
	USR_NAME_FAIL(19, String.format("Please make sure your non-empty username contains only numbers, letters, underscore or hyphen and is from %s to %s characters long.",
			ControllerServlet.USERNAME_MIN_LENGTH, ControllerServlet.USERNAME_MAX_LENGTH)),
	USR_CREATE_SUCC(20, "User created."),
	TRAVERSER_FAIL(21, "Modification failed."),
	TRAVERSER_CONN_FAIL(22, "There is a connectivity problem. This issue might stem from wrong address or there is another problem on the line."),
	DEVICE_DELETED(23, "Device deleted."),
	DEVICE_UNCHANGED(23, "Failed to change device's settings."),
	DEVICE_SET(23, "Device's settings altered..");

	private final int code;
	private final String description;

	private StatusCode(int code, String description) {
		this.code = code;
		this.description = description;
	}

	public int getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return code + ":" + description;
	}

}
