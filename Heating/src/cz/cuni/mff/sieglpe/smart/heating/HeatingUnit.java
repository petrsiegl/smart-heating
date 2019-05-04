/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.heating;

import cz.cuni.mff.sieglpe.smart.device.Relay;
import cz.cuni.mff.sieglpe.smart.device.Thermometer;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Smart heating unit connects thermometer and relay.
 * Relay status is changed accordingly to temperature from thermometer.
 * @author Petr Siegl
 */
public class HeatingUnit {

	/**
	 *
	 */
	private String id;
    /** Temperature to keep when in manual mode. */
    private float preferredTemp;

	/**
	 *
	 */
	private Thermometer thermometer;

	/**
	 *
	 */
	private Relay relay;

	/**
	 *
	 * @param id
	 * @param thermometer
	 * @param relay
	 */
	public HeatingUnit(String id, Thermometer thermometer, Relay relay) {
        this.id = id;
        this.thermometer = thermometer;
        this.relay = relay;
    }

	/**
	 *
	 * @return
	 */
	public String getID() {
        return id;
    }

	/**
	 *
	 * @return
	 */
	public  Thermometer getThermometer() {
        return thermometer;
    }

	/**
	 *
	 * @return
	 */
	public  String getThermometerID() {
        return thermometer.getID();
    }

	/**
	 *
	 * @param thermometer
	 */
	protected  void setThermometer(Thermometer thermometer) {
        this.thermometer = thermometer;
    }

	/**
	 *
	 * @return
	 */
	public  Relay getRelay() {
        return relay;
    }

	/**
	 *
	 * @return
	 */
	public  String getRelayID() {
        return relay.getID();
    }

	/**
	 *
	 * @param relay
	 */
	protected  void setRelay(Relay relay) {
        this.relay = relay;
    }

	/**
	 *
	 * @return
	 */
	public float getPreferredTemp() {
        return preferredTemp;
    }

	/**
	 *
	 * @param preferredTemp
	 */
	protected void setPreferredTemp(float preferredTemp) {
        this.preferredTemp = preferredTemp;
    }
	
	public JsonObject getJsonTempReading(){
		JsonObject jo = Json.createObjectBuilder()
				.add("id", id)
				//.add("relayId", getRelayID())
				//.add("thermometerId", getThermometerID())
				.add("preferredTemp", getPreferredTemp())
				.add("temperature", getThermometer().getTemperature())
				.build();
		
		return jo;
	}

}
