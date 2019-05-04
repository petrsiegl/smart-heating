/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

/**
 * Process can register smart listener to get information about devices in core.
 * @author Petr Siegl
 */
public interface SmartListener {

	/**
	 * Device removed.
	 * @param event
	 */
	public void dispatchDeviceRemoved (DeviceRemovedEvent event);

	/**
	 * New device added.
	 * @param event
	 */
	public void dispatchDeviceAdded (DeviceAddedEvent event);
}
