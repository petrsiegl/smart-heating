/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

/**
 * Sends information about device being removed form smart platform.
 * @author Petr Siegl
 */
public class DeviceRemovedEvent {
    /** Id of the device, that was removed. */
    private final String id;
    
    /**
     * Identify the device being removed by id.
     * @param id Id of the removed device.
     */
    public DeviceRemovedEvent(String id) {
        this.id = id;
    }
    
	/**
	 *
	 * @return
	 */
	public String getID(){
        return id;
    }
    
}
