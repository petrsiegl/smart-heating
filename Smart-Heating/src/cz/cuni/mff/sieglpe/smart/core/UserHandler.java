/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

import static cz.cuni.mff.sieglpe.smart.core.SmartHandler.USERS_TABLE_NAME;
import static cz.cuni.mff.sieglpe.smart.core.SmartHandler.getDBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Petr Siegl
 */
public final class UserHandler {

	private UserHandler() {
	}

	/**
	 * Prepare tables.
	 *
	 * @param connection
	 * @throws SQLException
	 */
	protected static void initUsers(Connection connection) throws SQLException {
		String query;
		ResultSet rs;
		Statement statement;
		statement = connection.createStatement();

		// Create table for storing user data
		// username,password
		query = "CREATE TABLE IF NOT EXISTS " + USERS_TABLE_NAME + "("
				+ "USERNAME varchar(255) PRIMARY KEY,"
				+ "PASSWORD varchar(255),"
				+ "LEVEL int(255))";
		statement.executeUpdate(query);

		// Create default account if none exists
		query = "SELECT * FROM " + USERS_TABLE_NAME;
		rs = statement.executeQuery(query);
		if (!rs.next()) {
			String pass;
			try {
				pass = PasswordHasher.createHash("admin");

				query = "INSERT INTO " + USERS_TABLE_NAME + "(USERNAME, PASSWORD,LEVEL) VALUES('admin','" + pass
						+ "','100')";
				statement.execute(query);
			} catch (PasswordHasher.CannotPerformOperationException ex) {
				Logger.getLogger(UserHandler.class.getName()).log(Level.SEVERE, "Admin user account creation failed.", ex);
			}
		}

		statement.close();
	}

	/**
	 * Get all stored users.
	 *
	 * @return List of all users in database.
	 * @throws SQLException
	 */
	protected static List<User> getUsers() throws SQLException {
		List<User> users = new ArrayList<>();
		String query;

		try (Connection connection = SmartHandler.getDBConnection()) {
			query = "SELECT * FROM " + SmartHandler.USERS_TABLE_NAME;

			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(query);

			String username;
			int level;
			while (rs.next()) {
				username = rs.getString("USERNAME");
				level = rs.getInt("LEVEL");
				users.add(new User(username, level));
			}
		}

		return users;
	}



	/**
	 * Change just password for user.
	 *
	 * @param user
	 * @param newPassword
	 * @throws SQLException
	 * @throws PasswordHasher.CannotPerformOperationException
	 */
	protected static synchronized void changePassword(User user, String newPassword)
			throws SQLException, PasswordHasher.CannotPerformOperationException {

		try (Connection connection = SmartHandler.getDBConnection()) {
			PreparedStatement insertPS;
			ResultSet rs;
			String query;

			query = "UPDATE " + SmartHandler.USERS_TABLE_NAME
					+ " SET PASSWORD=? "
					+ "WHERE USERNAME=?; ";
			insertPS = connection.prepareStatement(query);

			insertPS.setString(1, PasswordHasher.createHash(newPassword));
			insertPS.setString(2, user.getName());

			insertPS.executeUpdate();
			insertPS.close();
		}

	}

	/**
	 * Change more attributes.
	 *
	 * @param username
	 * @param password not changed if null
	 * @param level
	 * @return Status of the action.
	 * @throws SQLException
	 * @throws PasswordHasher.CannotPerformOperationException
	 */
	protected static StatusCode changeUserSettings(String username, String password, int level)
			throws SQLException, PasswordHasher.CannotPerformOperationException {

		if (username != null) {

			try (Connection connection = SmartHandler.getDBConnection()) {
				PreparedStatement insertPS;
				ResultSet rs;
				StringBuilder query = new StringBuilder();
				int parameterIndex = 1;

				query.append("UPDATE ").append(SmartHandler.USERS_TABLE_NAME).append(" SET");
				if (password != null) {
					query.append(" PASSWORD=?,");
				}
				query.append(" LEVEL=? ").append("WHERE USERNAME=?");

				insertPS = connection.prepareStatement(query.toString());
				if (password != null) {
					insertPS.setString(parameterIndex++, PasswordHasher.createHash(password));
				}
				insertPS.setInt(parameterIndex++, level);
				insertPS.setString(parameterIndex++, username);

				insertPS.executeUpdate();
				insertPS.close();
			}
			return StatusCode.USR_MOD_SUCC;
		} else {
			return StatusCode.USR_MOD_FAIL;
		}
	}

	/**
	 * Delete user with given username.
	 *
	 * @param username
	 * @throws SQLException
	 * @throws PasswordHasher.CannotPerformOperationException
	 */
	protected static void deleteUser(String username)
			throws SQLException, PasswordHasher.CannotPerformOperationException {
		if (username != null) {

			try (Connection connection = SmartHandler.getDBConnection()) {
				PreparedStatement insertPS;
				StringBuilder query = new StringBuilder();

				query.append("DELETE FROM ").append(SmartHandler.USERS_TABLE_NAME);
				query.append(" WHERE USERNAME=?");

				insertPS = connection.prepareStatement(query.toString());
				insertPS.setString(1, username);

				insertPS.executeUpdate();
				insertPS.close();
			}
		}
	}

	/**
	 * Add new user to system.
	 *
	 * @param username
	 * @param password
	 * @param level
	 * @throws SQLException
	 * @throws PasswordHasher.CannotPerformOperationException
	 */
	protected static void addUser(String username, String password, String level) throws SQLException, PasswordHasher.CannotPerformOperationException {
		addUser(username, password, UserLevel.stringNameToInt(level));
	}

	/**
	 * Add new user to system.
	 *
	 * @param username
	 * @param password
	 * @param level
	 * @throws SQLException
	 * @throws PasswordHasher.CannotPerformOperationException
	 */
	protected static void addUser(String username, String password, int level) throws SQLException, PasswordHasher.CannotPerformOperationException {
		try (Connection connection = getDBConnection()) {
			String hashPassword = PasswordHasher.createHash(password);
			String query = "INSERT INTO " + USERS_TABLE_NAME + "(USERNAME, PASSWORD, LEVEL) VALUES(?,?,?)";
			try (PreparedStatement ps = connection.prepareStatement(query)) {
				ps.setString(1, username);
				ps.setString(2, hashPassword);
				ps.setInt(3, level);

				ps.execute();
			}
		} catch (SQLException | PasswordHasher.CannotPerformOperationException ex) {
			throw ex;
		}
	}
}
