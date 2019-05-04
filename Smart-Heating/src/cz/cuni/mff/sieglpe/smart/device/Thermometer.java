/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.device;

import cz.cuni.mff.sieglpe.smart.core.Device;

/**
 * Device of type thermometer.
 * @author Petr Siegl 
 */
public interface Thermometer extends Device {

	/**
	 * Get current temperature of the thermometer.
	 * @return
	 */
	public float getTemperature();
}
