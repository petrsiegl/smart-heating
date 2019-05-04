package cz.cuni.mff.sieglpe.smart.core;

import com.pi4j.io.gpio.GpioPinDigitalOutput;

/**
 * GpioGadget for gadgets with one GPIO pin.
 * @author Petr Siegl
 */
public abstract class SingleGpioGadget extends GpioGadget {

	/**
	 * Pin which is the device connected to.
	 */
	protected GpioPinDigitalOutput pin;

	/**
	 * ID of the device.
	 */
	protected String id;

	/**
	 *
	 * @return
	 */
	public String getPinName(){
		return pin.getName();
	}
	
	/**
	 * Unprovision pin.
	 */
	@Override
	public void finalizeGpio() {
		if (pin != null) {
			GPIO_CONTROLLER.unprovisionPin(pin);
		}
	}
}
