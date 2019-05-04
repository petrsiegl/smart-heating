/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.wiringpi.GpioUtil;
import java.util.logging.Logger;

/**
 * All devices working with GPIO extend this class.
 *
 * @author Petr Siegl
 */
public abstract class GpioGadget {

	/**
	 * Handles all GPIO pin interactions.
	 */
	protected static final GpioController GPIO_CONTROLLER;

	static {
		if (GpioUtil.isPrivilegedAccessRequired()) {
			Logger.getLogger(GpioGadget.class.getName()).severe("Enabling access to GPIO without root priviliges failed.");
			System.out.println("Enabling access to GPIO without root priviliges failed.\n"
					+ "Privileged access is required if any of the the following conditions are not met:\n"
					+ "- You are running with Linux kernel version 4.1.7 or greater\n"
					+ "- The Device Tree is enabled\n"
					+ "- The 'bcm2835_gpiomem' kernel module loaded.\n"
					+ "- Udev rules are configured to permit write access to '/sys/class/gpio/**' ");
			//System.exit(1);
			System.out.println("System will try to continue as if with root priviledges.");
		} else {
			// No need for root rights this way.
			// WARNING! Needs to be some newer Raspbian version.
			// Must be invoked before creating GPIO controller.
			GpioUtil.enableNonPrivilegedAccess();
		}

		GPIO_CONTROLLER = GpioFactory.getInstance();
	}

	/**
	 * Subclass need to unprovision used pins. 
	 * This class is called before destruction of class.
	 */
	public abstract void finalizeGpio();
}
