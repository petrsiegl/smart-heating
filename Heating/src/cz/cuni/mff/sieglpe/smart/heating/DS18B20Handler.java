/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.sieglpe.smart.heating;

import com.pi4j.io.gpio.Pin;
import cz.cuni.mff.sieglpe.smart.core.Device;
import cz.cuni.mff.sieglpe.smart.core.DeviceHandler;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler for DS18B20 device.
 * @author Petr Siegl
 */
public class DS18B20Handler implements DeviceHandler {

	/**
	 * Get new instance of DS18B20 device.
	 * @param id id of the new device
	 * @param pins pins device is connected to
	 * @return new instance
	 */
	@Override
	public DS18B20 getInstance(String id, Pin[] pins) {
		return new DS18B20(id);
	}

	/**
	 * Store DS18B20 to database with given table.
	 * @param connection connection to database
	 * @param tableName table name to store to
	 * @param device device
	 * @throws SQLException Storing failed.
	 */
	@Override
	public void addDeviceToDB(Connection connection, String tableName, Device device) throws SQLException {
		String query;

		PreparedStatement preparedStatement;
		// Create database
		query = "CREATE TABLE IF NOT EXISTS " + tableName + "("
				+ "ID varchar(255) PRIMARY KEY)";
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.executeUpdate();
		preparedStatement.close();
		// Insert data
		query = "INSERT INTO "+tableName+"(ID) "
				+ "VALUES (?)";
		preparedStatement = connection.prepareStatement(query);
		preparedStatement.setString(1, device.getID());

		preparedStatement.executeUpdate();
		preparedStatement.close();
	}

	/**
	 * Remove given device from storage.
	 * @param connection connection to database
	 * @param tableName table name to deleted from
	 * @param device device to delete
	 * @throws SQLException Removing failed.
	 */
	@Override
	public void removeDeviceFromDB(Connection connection, String tableName, Device device) throws SQLException {
		String deleteQuery;
		PreparedStatement deletePS;

		// delete data
		deleteQuery = "DELETE FROM "
				+ tableName
				+ " WHERE ID=?";

		deletePS = connection.prepareStatement(deleteQuery);
		deletePS.setString(1, device.getID());
		deletePS.executeUpdate();
		deletePS.close();
	}

	/**
	 * Load all stored DS18B20 from database.
	 * @param connection connection to database
	 * @param tableName table name to load from
	 * @return All stored devices
	 * @throws SQLException Loading failed.
	 */
	@Override
	public DS18B20[] loadAllFromDatabase(Connection connection, String tableName) throws SQLException {
		String query;
		PreparedStatement selectPS;
		ResultSet rs;
		Statement statement;
		statement = connection.createStatement();

		List<DS18B20> list = new ArrayList<>();
		
		rs = statement.executeQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='"+tableName+"'");
		if (rs.next()) {
			query = "SELECT * FROM "+tableName;
			selectPS = connection.prepareStatement(query);
			rs = selectPS.executeQuery();

			while (rs.next()) {
				String id = rs.getString("ID");
				list.add(new DS18B20(id));
			}
			selectPS.close();
		}
		
		statement.close();
		return  list.size() > 0 ? list.toArray(new DS18B20[list.size()]) : null;
	}

}
