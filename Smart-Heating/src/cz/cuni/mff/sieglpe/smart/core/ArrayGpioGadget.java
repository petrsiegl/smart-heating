/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

import com.pi4j.io.gpio.GpioPinDigitalOutput;

/**
 * Provides array of pins for devices working or more GPIOs.
 *
 * @author Petr Siegl
 */
public abstract class ArrayGpioGadget extends GpioGadget {

	/**
	 *
	 */
	protected GpioPinDigitalOutput[] pins;

	/**
	 * All names of all pins in use.
	 * @return names
	 */
	public String[] getPinNames() {

		String[] names = new String[pins.length];
		for (int i = 0; i < pins.length; i++) {
			if (pins[i]!=null) {
				names[i] = pins[i].getName();
			}
		}
		return names;
	}

	/**
	 * Finalize for all pins.
	 */
	@Override
	public void finalizeGpio() {
		for (GpioPinDigitalOutput pin : pins) {
			if (pin != null) {
				GPIO_CONTROLLER.unprovisionPin(pin);
			}
		}
	}

}
