/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.heating;

import cz.cuni.mff.sieglpe.smart.core.DeviceAddedEvent;
import cz.cuni.mff.sieglpe.smart.core.DeviceRemovedEvent;
import cz.cuni.mff.sieglpe.smart.core.SmartListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Smart Heating process event listener.
 * @author Petr Siegl
 */
public class HeatingListener implements SmartListener {

    private final SmartHeating smartHeating;

	/**
	 * Store connected instance of SmartHeating.
	 * @param smartHeating
	 */
	public HeatingListener(SmartHeating smartHeating) {
        this.smartHeating = smartHeating;
    }

    /**
     * Remove all heating units that contains the removed device.
     * @param event Info about added device.
     */
    @Override
    public void dispatchDeviceRemoved(DeviceRemovedEvent event) {
        List<String> idsToRemove = new ArrayList<>();
        
        // Collect ids of heating units to remove
        SmartHeating.heatingUnits.forEach((id,unit) -> {
            if (unit.getRelayID().equals(event.getID())) {
                idsToRemove.add(id);
            } else if (unit.getThermometerID().equals(event.getID())) {
                idsToRemove.add(id);
            }
        });

		// Removing
        idsToRemove.forEach((id) -> {
            SmartHeating.removeHeatingUnit(id);
        });
    }

	/**
	 * No implementation necessary.
	 * @param event Info about removed device.
	 */
	@Override
	public void dispatchDeviceAdded(DeviceAddedEvent event) {
		//empty
	}
}
