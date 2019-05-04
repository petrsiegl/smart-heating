/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.device;

import cz.cuni.mff.sieglpe.smart.core.Device;

/**
 * Device of relay type.
 * @author Petr Siegl 
 */
public interface Relay extends Device {

	/**
	 *
	 * @param state
	 */
	public void setState(RelayState state);

	/**
	 *
	 * @return
	 */
	public RelayState getState();
}
