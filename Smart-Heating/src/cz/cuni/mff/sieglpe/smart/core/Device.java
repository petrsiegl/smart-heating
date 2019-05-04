/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

/**
 * All virtual devices are of this type.
 * @author Petr Siegl
 */
public interface Device {
	
	/**
	 * All devices  have unique ID.
	 * @return
	 */
	public String getID();

	/**
	 * All devices  have unique ID.
	 * @param id
	 */
	public void setID(String id);
}
