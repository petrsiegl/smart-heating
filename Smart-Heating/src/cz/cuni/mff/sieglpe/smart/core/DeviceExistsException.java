/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

/**
 * Situation when trying to create already existing device.
 * @author Petr Siegl
 */
public class DeviceExistsException extends Exception {

	/**
	 *
	 */
	public DeviceExistsException() {
    }

	/**
	 *
	 * @param message
	 */
	public DeviceExistsException(String message) {
        super(message);
    }

	/**
	 *
	 * @param cause
	 */
	public DeviceExistsException(Throwable cause) {
        super(cause);
    }

	/**
	 *
	 * @param message
	 * @param cause
	 */
	public DeviceExistsException(String message, Throwable cause) {
        super(message, cause);
    }

}
