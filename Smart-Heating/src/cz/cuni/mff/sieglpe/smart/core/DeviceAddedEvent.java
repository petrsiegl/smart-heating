/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

/**
 * Sends information about device being added form smart platform.
 * @author Petr Siegl
 */
public class DeviceAddedEvent {
    /** Id of the device, that was removed. */
    private final Device device;
    
    /**
     * Identify the device being removed by id.
	 * @param device
     */
    public DeviceAddedEvent(Device device) {
        this.device = device;
    }
    
	/**
	 *
	 * @return
	 */
	public Device getDevice(){
        return device;
    }
    
}
