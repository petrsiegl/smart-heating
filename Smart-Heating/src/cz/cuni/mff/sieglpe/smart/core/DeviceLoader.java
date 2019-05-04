/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

import java.util.List;

/**
 * This interface is used at the start of the system for loading of devices.
 * It saves user from inputting some of them manually
 * and handles more complex loading.
 * @author Petr Siegl
 */
public interface DeviceLoader {

	/**
	 * Load new devices to system.
	 * @return
	 */
	public List<? extends Device> load();
}
