/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

import com.pi4j.io.gpio.Pin;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Classes with this interface are used for manipulation with Devices.
 * They are responsible for database storing and loading, Device creation and loading of devices.
 * @author Petr Siegl
 */
public interface DeviceHandler {

	/**
	 * Get new instance of associated Device.
	 * @param id Id of new device.
	 * @param pins Pin for device to use.
	 * @return new instance of associated Device
	 */
	public Device getInstance(String id, Pin[] pins);

	/**
	 * Handles device database storing.
	 * @param connection Connection to database.
	 * @param tableName Name of the table to store in.
	 * @param device Device to store.
	 * @throws SQLException
	 */
	public void addDeviceToDB(Connection connection,String tableName, Device device) throws SQLException;

	/**
	 * Handles removing of the device form database.
	 * @param connection Connection to database.
	 * @param tableName Name of the table to delete from.
	 * @param device Device to delete.
	 * @throws SQLException
	 */
	public void removeDeviceFromDB(Connection connection,String tableName, Device device) throws SQLException;

	/**
	 * Load all devices of handler type from database.
	 * @param connection Connection to database.
	 * @param tableName Name of the table to load from.
	 * @return All found devices.
	 * @throws SQLException
	 */
	public Device[] loadAllFromDatabase(Connection connection,String tableName) throws SQLException;
}
