/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.heating;

import com.google.common.hash.Hasher;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

/**
 * Database record object for smart-heating predefined mode.
 *
 * @author Petr Siegl
 */
public class DBPredefinedRecord {

	private Time time;
	private float temperature;
	private String heatingUnitID;

	/**
	 *
	 * @return
	 */
	public Time getTime() {
		return time;
	}

	/**
	 *
	 * @param time
	 */
	public void setTime(Time time) {
		this.time = time;
	}

	/**
	 *
	 * @return
	 */
	public String getFormatedTime() {
		return new SimpleDateFormat("HH:mm").format(time);
	}

	/**
	 *
	 * @return
	 */
	public float getTemperature() {
		return temperature;
	}

	/**
	 *
	 * @param temperature
	 */
	public void setTemperature(float temperature) {
		this.temperature = temperature;
	}

	/**
	 *
	 * @return
	 */
	public String getHeatingUnitID() {
		return heatingUnitID;
	}

	/**
	 *
	 * @param heatingUnitID
	 */
	public void setHeatingUnitID(String heatingUnitID) {
		this.heatingUnitID = heatingUnitID;
	}

}
