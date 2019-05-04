/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.heating;

import cz.cuni.mff.sieglpe.smart.device.Thermometer;
import com.google.auto.service.AutoService;
import cz.cuni.mff.sieglpe.smart.core.Device;
import cz.cuni.mff.sieglpe.smart.core.MethodKind;
import cz.cuni.mff.sieglpe.smart.core.SingleGpioGadget;
import cz.cuni.mff.sieglpe.smart.core.SmartHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import cz.cuni.mff.sieglpe.smart.core.HtmlGetter;

/**
 * Virtual device for D18B20 thermometer.
 *
 * @author Petr Siegl
 */
@AutoService(Device.class)
public class DS18B20 extends SingleGpioGadget implements Thermometer {

	/**
	 * Directory where information about D18B20 thermometers are stored.
	 */
	public static final String DIR = Paths.get(File.separator, "sys", "bus", "w1", "devices").toString();
	/**
	 * This value is returned when correct temperature couldn't have been
	 * loaded.
	 */
	public static final float WRONG = -250;

	static {
		SmartHandler.registerDeviceHandler(DS18B20.class.getCanonicalName(), new DS18B20Handler());
		SmartHandler.registerDeviceLoader(new DS18B20Loader());
	}
	
	//public D18B20(){}
	/**
	 * Set id as device id. The used id should be unique.
	 *
	 * @param id Id to use for this device.
	 */
	public DS18B20(String id) {
		this.id = id;
	}

	/**
	 * Get current temperature in degrees Celsius. Value WRONG means correct one
	 * couldn't have been loaded.
	 *
	 * @return Currently measured temperature.
	 */
	@Override
	@HtmlGetter(name = "Temperature", kind = MethodKind.TEXT)
	public float getTemperature() {
		// Reading this file catches current temperature from thermometer.
		
		Path pathToDir = Paths.get(DIR, id, "w1_slave");

		if (Files.exists(pathToDir)) {
			String firstLine;
			String secondLine;
			int numberOfTries = 0;

			// Repeat until checksum is correct "YES" or we tried 5 times
			while (true) {
				numberOfTries++;
				try (BufferedReader br = new BufferedReader(Files.newBufferedReader(pathToDir))) {

					firstLine = br.readLine();
					secondLine = br.readLine();
					if (firstLine.contains("YES") || numberOfTries > 5) {
						break;
					}

				} catch (FileNotFoundException ex) {
					Logger.getLogger(DS18B20.class.getName()).log(Level.SEVERE, null, ex);
				} catch (IOException ex) {
					Logger.getLogger(DS18B20.class.getName()).log(Level.SEVERE, null, ex);
				}
			}

			if (firstLine.contains("YES")) {
				// xx xx xx xx xx xx xx xx xx t=OurTemperature
				// OurTemperature is in Celsius *1000
				float temp = Float.parseFloat(secondLine.split("=", 2)[1]);
				temp = temp / 1000;
				return temp;
			} else {
				return WRONG;
			}

		} else {
			return WRONG;
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
