/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague.
 */
package cz.cuni.mff.sieglpe.smart.core;

import static cz.cuni.mff.sieglpe.smart.core.UserHandler.initUsers;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base of the smart system. Manages and stores all the important data.
 *
 * @author Petr Siegl
 */
public class SmartHandler extends HttpServlet implements ServletContextListener {

	/**
	 * Default database values for driver.
	 */
	protected static final String DEF_DRIVER = "org.h2.Driver";

	/**
	 * Default database values for connection.
	 */
	protected static final String DEF_CONNECTION = "jdbc:h2:./smartheating";

	/**
	 * Default database values for user.
	 */
	protected static final String DEF_USER = "sa";

	/**
	 * Default database values for password.
	 */
	protected static final String DEF_PASSWORD = "";

	// Database setup
	/**
	 * Current database values for driver.
	 */
	protected static String DB_DRIVER = DEF_DRIVER;

	/**
	 * Current database values for connection.
	 */
	protected static String DB_CONNECTION = DEF_CONNECTION;

	/**
	 * Current database values for user.
	 */
	protected static String DB_USER = DEF_USER;

	/**
	 * Current database values for password.
	 */
	protected static String DB_PASSWORD = DEF_PASSWORD;

	// 
	/**
	 * Database table name for users.
	 */
	public static final String USERS_TABLE_NAME = "USER";

	/**
	 * Traverser connection handler.
	 */
	protected static TraverserConnector traverserConnector;

	// All loaded devies in core.
	private static final Map<String, Device> devices = new HashMap<>();

	/**
	 * Context prefix as key and associated prefix.
	 */
	protected static final Map<String, SmartProcess> processes = new HashMap<>();

	/**
	 * Threads running processes with context prefix as key.
	 */
	protected static Map<String, Thread> threads = new HashMap<>();

	/**
	 * Canonical class name as key and associated device handle capable to
	 * manipulate.
	 */
	protected static Map<String, DeviceHandler> handlers = new HashMap<>();
	private static List<SmartListener> listeners = new ArrayList<>();
	private static List<DeviceLoader> loaders = new ArrayList<>();

	/**
	 * Set default db settings.
	 */
	private static void setDBDefault() {
		DB_DRIVER = DEF_DRIVER;
		DB_CONNECTION = DEF_CONNECTION;
		DB_USER = DEF_USER;
		DB_PASSWORD = DEF_PASSWORD;
	}

	/**
	 * Load database setting from file "database.INI".
	 */
	protected static void loadDBConfig() {
		File file = new File("database.INI");
		try {
			if (!file.createNewFile()) {
				// File already exists -> load it.
				try (BufferedReader br = new BufferedReader(new FileReader(file))) {
					// Fromat: name=value
					String line;
					String[] splitLine;
					String name;
					String value;
					while ((line = br.readLine()) != null) {
						if (!line.isEmpty() && line.charAt(0) != '#') {
							splitLine = line.split("=", 2);
							name = splitLine[0].trim();
							value = splitLine[1].trim();
							switch (name.toLowerCase()) {
								case "driver":
									DB_DRIVER = value;
									break;
								case "connection":
									DB_CONNECTION = value;
									break;
								case "user":
									DB_USER = value;
									break;
								case "password":
									DB_PASSWORD = value;
									break;
								default:
									break;
							}
						}
					}
				} catch (IOException ex) {
					throw ex;
				}
			} else {
				// File doestn exist and was just created.
				try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
					// Fromat: name=value
					pw.println("DB_DRIVER = " + DEF_DRIVER);
					pw.println("DB_CONNECTION = " + DEF_CONNECTION);
					pw.println("DB_USER = " + DEF_USER);
					pw.println("DB_PASSWORD = " + DEF_PASSWORD);
				} catch (IOException ex) {
					throw ex;
				} finally {
					// Default values.
					setDBDefault();
				}
			}
		} catch (IOException ex) {
			// Default values if.
			setDBDefault();
			Logger.getLogger(SmartHandler.class.getName()).log(Level.SEVERE, "Database loading failed", ex);
		}
	}

	/**
	 * Initializes all main containers.
	 */
	protected static void initContainers() {

		loadDBConfig();
		try (Connection connection = getDBConnection()) {

			initUsers(connection);
			initStoredDevices(connection);
			initLoaders();

		} catch (SQLException | IllegalArgumentException | SecurityException ex) {
			Logger.getLogger(SmartHandler.class
					.getName()).log(Level.SEVERE, "Containers initialization failed.", ex);

		}

		initProcesses();
	}

	/**
	 * Start all processes.
	 */
	private static void initProcesses() {

		for (Map.Entry<String, SmartProcess> entry : processes.entrySet()) {
			String prefix = entry.getKey();
			try {
				entry.getValue().init();

				Thread thread = new Thread(entry.getValue());
				threads.put(prefix, thread);
				thread.start();
			} catch (ServletException ex) {
				Logger.getLogger(SmartHandler.class.getName()).log(Level.SEVERE, "Initialization of process" + prefix + "failed", ex);
			}
		}
	}

	/**
	 * Load all devices in db.
	 *
	 * @param connection
	 * @throws SQLException
	 */
	private static void initStoredDevices(Connection connection) throws SQLException {
		Device[] storedDevices = null;
		for (Map.Entry<String, DeviceHandler> entry : handlers.entrySet()) {
			storedDevices = entry.getValue().loadAllFromDatabase(connection, classNameToTableName(entry.getKey()));
			if (storedDevices != null) {
				for (Device storedDevice : storedDevices) {
					devices.put(storedDevice.getID(), storedDevice);
				}
			}
		}
	}

	/**
	 * Load all devices trough registered loaders.
	 *
	 * @throws SQLException
	 */
	private static void initLoaders() throws SQLException {
		for (DeviceLoader loader : loaders) {
			List<? extends Device> list = loader.load();
			if (list != null) {
				list.forEach((device) -> {
					try {
						addDevice(device.getID(), device);
					} catch (DeviceExistsException ex) {
						Logger.getLogger(SmartHandler.class.getName()).info("Device with this id already exists: " + device.getID());
					}

				});
			}
		}
	}

	/**
	 * Register new Device handler.
	 *
	 * @param className Canonical name of device to associate to.
	 * @param handler Handler of device.
	 */
	public static void registerDeviceHandler(String className, DeviceHandler handler) {
		if (className != null && !className.isEmpty() && handler != null) {
			handlers.put(className, handler);
		}
	}

	/**
	 * Register new loader.
	 *
	 * @param loader
	 */
	public static void registerDeviceLoader(DeviceLoader loader) {
		loaders.add(loader);
	}

	/**
	 * Get handler for class of classname.
	 *
	 * @param className
	 * @return
	 */
	public static DeviceHandler getHandler(String className) {
		return handlers.get(className);
	}

	/**
	 * Get connection to database.
	 *
	 * @return Connection to current database.
	 * @throws SQLException
	 */
	public static Connection getDBConnection() throws SQLException {
		Connection connection = null;
		try {
			Class.forName(DB_DRIVER);

			connection = DriverManager.getConnection(
					DB_CONNECTION,
					DB_USER,
					DB_PASSWORD);

		} catch (ClassNotFoundException ex) {
			Logger.getLogger(SmartHandler.class
					.getName()).log(Level.SEVERE, null, ex);
		}
		return connection;
	}

	/**
	 * Add device to database through associated handler.
	 *
	 * @param id id of device to add
	 * @param device device to add
	 * @throws SQLException
	 */
	private static void addDeviceToDB(String id, Device device) throws SQLException {
		if (device != null) {
			try (Connection connection = getDBConnection()) {
				DeviceHandler handler = handlers.get(device.getClass().getCanonicalName());
				String tableName = classNameToTableName(device.getClass().getCanonicalName());
				handler.addDeviceToDB(connection, tableName, device);
			} catch (SQLException ex) {
				throw ex;
			}
		}
	}

	/**
	 * Add device to running application and database.
	 *
	 * @param id ID of device to add
	 * @param device device to add.
	 * @throws cz.cuni.mff.sieglpe.smart.core.DeviceExistsException
	 */
	protected static void addDevice(String id, Device device) throws DeviceExistsException {
		if (!devices.containsKey(id)) {
			try {
				addDeviceToDB(id, device);
				devices.put(id, device);
				fireDeviceAdded(new DeviceAddedEvent(device));
			} catch (SQLException ex) {
				Logger.getLogger(SmartHandler.class
						.getName()).log(Level.SEVERE, null, ex);
			}
		} else {
			throw new DeviceExistsException("Device with id " + id + " already exists.");
		}
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public static Device getDevice(String id) {
		return devices.get(id);
	}

	/**
	 * Remove device form db through associated handler.
	 *
	 * @param id if of device to remove
	 * @param device
	 * @throws SQLException
	 */
	private static void removeDeviceFromDB(String id, Device device) throws SQLException {
		if (device != null) {
			try (Connection connection = getDBConnection()) {
				DeviceHandler handler = handlers.get(device.getClass().getCanonicalName());
				String tableName = classNameToTableName(device.getClass().getCanonicalName());
				handler.removeDeviceFromDB(connection, tableName, device);
			} catch (SQLException ex) {
				throw ex;
			}
		}
	}

	/**
	 * Converts class name to name used as table name in database.
	 *
	 * @param className name to convert
	 * @return table name
	 */
	private static String classNameToTableName(String className) {
		return className.replace('.', '_').toUpperCase();
	}

	/**
	 * Removes device from running application and from database.
	 *
	 * @param id ID of device to remove.
	 */
	protected static void removeDevice(String id) {
		synchronized (devices) {
			if (devices.containsKey(id)) {
				try {
					Device dev = devices.get(id);
					removeDeviceFromDB(id, dev);
					devices.remove(id);
					if (dev instanceof GpioGadget) {
						((GpioGadget) dev).finalizeGpio();
					}
					fireDeviceRemoved(new DeviceRemovedEvent(id));

				} catch (SQLException ex) {
					Logger.getLogger(SmartHandler.class
							.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	/**
	 * Add new listener.
	 *
	 * @param listener
	 */
	public static void addSmartListener(SmartListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove listener.
	 *
	 * @param listener
	 */
	public static void removeSmartListener(SmartListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Send information about device being removed.
	 *
	 * @param event
	 */
	private static void fireDeviceRemoved(DeviceRemovedEvent event) {
		listeners.forEach((listener) -> {
			listener.dispatchDeviceRemoved(event);
		});
	}

	/**
	 * Send information about device being removed.
	 *
	 * @param event
	 */
	private static void fireDeviceAdded(DeviceAddedEvent event) {
		listeners.forEach((listener) -> {
			listener.dispatchDeviceAdded(event);
		});
	}

	/**
	 * Get IDs of all loaded devices.
	 *
	 * @return
	 */
	public static List<String> getDevicesIDs() {
		List<String> ids = new ArrayList<>();
		devices.forEach((id, device) -> {
			ids.add(id);
		});
		return ids;
	}

	/**
	 * Get names of all loaded processes.
	 *
	 * @return
	 */
	public static List<String> getProcessesNames() {
		List<String> names = new ArrayList<>();
		processes.forEach((id, process) -> {
			names.add(process.getName());
		});
		return names;
	}

	/**
	 * Get all active context prefixes.
	 *
	 * @return
	 */
	public static List<String> getProcessesPrefixes() {
		List<String> prefixes = new ArrayList<>();
		processes.forEach((prefix, process) -> {
			prefixes.add(prefix);
		});
		return prefixes;
	}

	/**
	 * Check whether prefix is currently loaded.
	 *
	 * @param prefix context to check
	 * @return true if it is loaded.
	 */
	public static boolean isLoadedProcessPrefix(String prefix) {
		return processes.containsKey(prefix);
	}

	/**
	 *
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@Override
	public void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		// Set response content type
		response.setContentType("text/html");
		// Actual logic goes here.
		response.sendRedirect("/home");
	}

	/**
	 * Pair prefix with process.
	 *
	 * @param prefix
	 * @param process
	 */
	protected static void addProcess(String prefix, SmartProcess process) {
		if (process != null) {
			processes.put(prefix, process);
		}
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	protected static SmartProcess getProcess(String id) {
		return processes.get(id);
	}

	/**
	 * Stop all threads
	 */
	@Override
	public void destroy() {
		for (Thread thread : threads.values()) {
			try {
				thread.interrupt();
			} catch (SecurityException e) {
				Logger.getLogger(SmartHandler.class.getName()).log(Level.SEVERE, "Process interruption failed.", e);
			}
		}
	}

	/**
	 *
	 * @param sce
	 */
	@Override
	public void contextInitialized(ServletContextEvent sce) {

	}

	/**
	 *
	 * @param sce
	 */
	@Override
	public void contextDestroyed(ServletContextEvent sce) {

	}

}
