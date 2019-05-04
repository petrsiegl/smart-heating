package cz.cuni.mff.sieglpe.smart.heating;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
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
 * Handler for GpioRelay device.
 * @author Petr Siegl
 */
public class GpioRelayHandler implements DeviceHandler {

	/**
	 * Get new GpioRelay instance.
	 *
	 * @param id identifier of the relay
	 * @param pins pins its connected to, uses only the first
	 * @return new instance of GpioRelay
	 */
	@Override
	public GpioRelay getInstance(String id, Pin[] pins) {
		return pins.length > 0 ? new GpioRelay(id, pins[0]) : null;
	}

	/**
	 * Store GpioRelay in database.
	 *
	 * @param connection connection to database
	 * @param tableName table name to store to
	 * @param device
	 * @throws SQLException Storing failed.
	 */
	@Override
	public void addDeviceToDB(Connection connection, String tableName, Device device) throws SQLException {
		if (device instanceof GpioRelay) {
			String query;
			PreparedStatement preparedStatement;

			query = "CREATE TABLE IF NOT EXISTS " + tableName + "("
					+ "ID varchar(255) PRIMARY KEY,"
					+ "PIN varchar(255))";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.executeUpdate();
			preparedStatement.close();

			// Insert data
			query = "INSERT INTO " + tableName + "(ID, Pin) "
					+ "VALUES (?,?)";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, device.getID());
			preparedStatement.setString(2, ((GpioRelay) device).getPinName());
			preparedStatement.execute();

			preparedStatement.close();
		}
	}

	/**
	 * Remove GpioRelay from database.
	 * @param connection connection to database
	 * @param tableName table to remove from
	 * @param device GpioRelay to remove.
	 * @throws SQLException Removing failed
	 */
	@Override
	public void removeDeviceFromDB(Connection connection, String tableName, Device device) throws SQLException {
		if (device instanceof GpioRelay) {
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
	}

	/**
	 * Load all GpioRelays from database.
	 * @param connection connection to database
	 * @param tableName table name
	 * @return All stored GpioRelays
	 * @throws SQLException Loading failed.
	 */
	@Override
	public Device[] loadAllFromDatabase(Connection connection, String tableName) throws SQLException {
		String query;
		PreparedStatement selectPS;
		ResultSet rs;
		Statement statement;
		statement = connection.createStatement();

		List<GpioRelay> list = new ArrayList<>();

		rs = statement.executeQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + tableName + "'");
		if (rs.next()) {
			query = "SELECT * FROM " + tableName;
			selectPS = connection.prepareStatement(query);
			rs = selectPS.executeQuery();

			while (rs.next()) {
				String id = rs.getString("ID");
				String pinName = rs.getString("Pin");
				Pin pin = RaspiPin.getPinByName(pinName);
				list.add(new GpioRelay(id, pin));
			}
			selectPS.close();
		}
		statement.close();
		return list.size() > 0 ? list.toArray(new GpioRelay[list.size()]) : null;
	}

}
