/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.heating;

import cz.cuni.mff.sieglpe.smart.device.RelayState;
import cz.cuni.mff.sieglpe.smart.device.Relay;
import com.google.auto.service.AutoService;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.exception.GpioPinExistsException;
import cz.cuni.mff.sieglpe.smart.core.Device;
import cz.cuni.mff.sieglpe.smart.core.HtmlSetter;
import cz.cuni.mff.sieglpe.smart.core.MethodKind;
import cz.cuni.mff.sieglpe.smart.core.SingleGpioGadget;
import cz.cuni.mff.sieglpe.smart.core.HtmlGetter;
import cz.cuni.mff.sieglpe.smart.core.SmartHandler;

/**
 * Virtual object representing hardware GPIO relay.
 *
 * @author Petr Siegl
 */
@AutoService(Device.class)
public class GpioRelay extends SingleGpioGadget implements Relay {

	static {
		SmartHandler.registerDeviceHandler(GpioRelay.class.getCanonicalName(), new GpioRelayHandler());
	}

	/**
	 * Create relay with id on chosen pin.
	 *
	 * @param id Id to set for this relay.
	 * @param pinToConnect Pin to set for this relay.
	 */
	public GpioRelay(String id, Pin pinToConnect) throws GpioPinExistsException {
		this.id = id;
		// Basic state depends on hardware settings.
		pin = GPIO_CONTROLLER.provisionDigitalOutputPin(pinToConnect, pinToConnect.getName(), PinState.LOW);
	}

	/**
	 * Set state of the relay.
	 *
	 * @param state RelayState to set device to.
	 */
	@Override
	public void setState(RelayState state) {
		switch (state) {
			case ON:
				pin.high();
				break;
			case OFF:
				pin.low();
				break;
			default:
				break;
		}
	}

	/**
	 *
	 * @param state
	 */
	@HtmlSetter(name = "state")
	public void setState(String state) {
		switch (state) {
			case "ON":
				setState(RelayState.ON);
				break;
			case "OFF":
				setState(RelayState.OFF);
				break;
			default:
				break;
		}
	}

	/**
	 * Get current state of the relay.
	 *
	 * @return current state
	 */
	@Override
	@HtmlGetter(name = "state", kind = MethodKind.RADIO, radioValues = {"ON", "OFF"})
	public RelayState getState() {
		switch (pin.getState()) {
			case HIGH:
				return RelayState.ON;
			case LOW:
				return RelayState.OFF;
			default:
				return null;

		}
	}

	/**
	 *
	 * @return
	 */
	@Override
	public String getID() {
		return id;
	}

	/**
	 *
	 * @param id
	 */
	@Override
	public void setID(String id) {
		this.id = id;
	}

}
