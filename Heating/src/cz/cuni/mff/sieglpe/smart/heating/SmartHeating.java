/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.heating;

import com.google.auto.service.AutoService;
import cz.cuni.mff.sieglpe.smart.core.Device;
import cz.cuni.mff.sieglpe.smart.core.DeviceExistsException;
import cz.cuni.mff.sieglpe.smart.device.Relay;
import cz.cuni.mff.sieglpe.smart.device.RelayState;
import cz.cuni.mff.sieglpe.smart.core.SmartHandler;
import cz.cuni.mff.sieglpe.smart.core.SmartListener;
import cz.cuni.mff.sieglpe.smart.core.SmartProcess;
import cz.cuni.mff.sieglpe.smart.device.Thermometer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Manages smart heating system.
 *
 * @author Petr Siegl
 */
@AutoService(SmartProcess.class)
public class SmartHeating extends SmartProcess {

	protected static final int ID_MAX_LENGTH = 100;
	protected static final int ID_MIN_LENGTH = 1;
	protected static final String ID_REGEXP = String.format("[\\w-]{%d,%d}", ID_MIN_LENGTH, ID_MAX_LENGTH);

	private static String prefix = "";
	public static final String DB_PREFIX = "HEATING_";

	/**
	 * This allows dispatching pages to controller only when necessary.
	 */
	protected static final String CONTROLLERPREFIX = "/pages";

	/**
	 * Prefix to hidden pages.
	 */
	protected static final String PAGESPREFIX = "/WEB-INF";

	// Prefix of the process, used when redirecting page request.
	/**
	 * Database table name for heating units .
	 */
	public static final String DB_TABLE_HEATING_UNITS = DB_PREFIX + "HEATINGUNIT";

	/**
	 * Database table name for measurements .
	 */
	public static final String DB_TABLE_TEMPERATURES = DB_PREFIX + "TEMPERATURE";

	/**
	 * Database table names for days of week.
	 */
	public static final String[] DB_TABLE_DAYS = {
		"Monday",
		"Tuesday",
		"Wednesday",
		"Thursday",
		"Friday",
		"Saturday",
		"Sunday"};

	/**
	 * Table days column name for ID.
	 */
	protected static final String DB_DAYS_COL_UNIT_ID = "UnitID";

	/**
	 * Table days column name for Time.
	 */
	protected static final String DB_DAYS_COL_TIME = "Time";

	/**
	 * Table days column name for Temperature.
	 */
	protected static final String DB_DAYS_COL_TEMPERATURE = "Temperature";

	// interval of checking for correct temperature setting in seconds
	private static int interval = 1;

	// Maximal allowed deviation from correct temperature
	private static float tempBound = 0.5f;
	// Current mode of the smart heating process
	private static HeatingMode mode = HeatingMode.MANUAL;
	// ID and unit with that ID

	/**
	 * Stores all heating units with thir ID for quick access.
	 */
	protected static final Map<String, HeatingUnit> heatingUnits = new ConcurrentHashMap<>();

	// For time measurement purpose for storing temperatures
	private static boolean store_temps = true;
	private static long startTime;
	private static final long TEMP_STORE_INTERVAL = 30 * 60 * 1000;

	// HeatingListener for message from SmartHandler
	/**
	 * Listener used for removing units with wrong components.
	 */
	protected static SmartListener listener;

	/**
	 * Aso creates listener for the instance.
	 */
	public SmartHeating() {
		listener = new HeatingListener(this);
	}

	/**
	 * Initialize containers.
	 */
	@Override
	public void init() {
		initContainers();
	}

	/**
	 * Initial setup of the class. Load heating units from the database. Create
	 * table for heating units and for every day of a week (is used by when
	 * working in the predefined regime).
	 */
	public static void initContainers() {

		try (Connection connection = SmartHandler.getDBConnection()) {

			String query;
			PreparedStatement selectPS;
			ResultSet rs;
			Statement statement;
			statement = connection.createStatement();

			// Heating unit loading
			// --first check for existence
			rs = statement.executeQuery("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + DB_TABLE_HEATING_UNITS + "'");
			if (rs.next()) {
				query = "SELECT * FROM " + DB_TABLE_HEATING_UNITS;
				selectPS = connection.prepareStatement(query);
				rs = selectPS.executeQuery();

				HeatingUnit heatingUnit;
				while (rs.next()) {
					String id = rs.getString("ID");
					String relayID = rs.getString("RelayID");
					String thermoID = rs.getString("ThermoID");
					float temperature = rs.getFloat("Temperature");

					Device relay = SmartHandler.getDevice(relayID);
					Device thermo = SmartHandler.getDevice(thermoID);
					if (relay != null && thermo != null) {
						if (relay instanceof Relay) {
							if (thermo instanceof Thermometer) {
								heatingUnit = new HeatingUnit(id, (Thermometer) thermo, (Relay) relay);
								heatingUnit.setPreferredTemp(temperature);
								heatingUnits.put(id, heatingUnit);
							}
						}
					}

				}
			}

			// Create Heating Units table
			query = "CREATE TABLE IF NOT EXISTS " + DB_TABLE_HEATING_UNITS + "("
					+ "ID varchar(255) PRIMARY KEY,"
					+ "RelayID varchar(255) ,"
					+ "ThermoID varchar(255) ,"
					+ "Temperature FLOAT)";
			statement.executeUpdate(query);

			// Predefined temperature tables
			for (String day : DB_TABLE_DAYS) {
				query = "CREATE TABLE IF NOT EXISTS " + day + "("
						+ DB_DAYS_COL_UNIT_ID + " varchar(255),"
						+ DB_DAYS_COL_TIME + " TIME,"
						+ DB_DAYS_COL_TEMPERATURE + " FLOAT,"
						+ "PRIMARY KEY (" + DB_DAYS_COL_UNIT_ID + ", " + DB_DAYS_COL_TIME + "))";
				statement.executeUpdate(query);
			}

			statement.close();

		} catch (SQLException | SecurityException ex) {
			Logger.getLogger(SmartHandler.class
					.getName()).log(Level.SEVERE, "Failed to initialize smartheating properly.", ex);

		} catch (IllegalArgumentException ex) {
			Logger.getLogger(SmartHandler.class
					.getName()).log(Level.SEVERE, "Failed to initialize smartheating properly.", ex);
		}

		// Register for device removing events.
		SmartHandler.addSmartListener(listener);
	}

	/**
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	public void doGet(HttpServletRequest request,
			HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Main URL parsing and dispatching method.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public void processRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String path = request.getPathInfo();

		if (path == null) {
			dispatchMain(request, response);
		} else {

			String[] tokens = path.split("/");
			if (tokens.length > 1) {
				switch (tokens[1]) {
					case "":
					case "home":
					case "smartheating.jsp":
						dispatchMain(request, response);
						break;
					case "changeregime":
						dispatchChangeRegime(request, response);
						break;
					case "mode":
						handleModeChange(request, response);
						break;
					case "predefined":
					case "predefined.jsp":
						dispatchPredefined(request, response);
						break;
					case "createunit":
					case "createunit.jsp":
						dispatchCreateUnit(request, response);
						break;
					case "thermostat":
					case "thermostat.jsp":
						dispatchThermostat(request, response);
						break;
					case "unit":
						dispatchUnit(request, response);
						break;
					default:
						dispatch(request, response, path);
						break;

				}
			} else {
				dispatchMain(request, response);
			}

		}

	}

	/**
	 * General dispatch for pages.
	 *
	 * @param request
	 * @param response
	 * @param path What page to dispatch.
	 * @throws IOException
	 * @throws ServletException
	 */
	protected static void dispatch(HttpServletRequest request,
			HttpServletResponse response, String path) throws IOException, ServletException {
		// Get prefix given by AuthenticationFilter
		String prefix = (String) request.getAttribute("prefix");
		request.getRequestDispatcher(PAGESPREFIX + "/" + prefix + path).forward(request, response);
	}

	/**
	 * Dispatching of unit request.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected static void handleModeChange(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException, IllegalArgumentException {

		String newMode = request.getParameter("mode");

		if (newMode != null && !newMode.isEmpty()) {
			try {
				SmartHeating.changeMode(HeatingMode.valueOf(newMode));
				response.setStatus(HttpServletResponse.SC_OK);
			} catch (IllegalArgumentException ex) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				throw ex;
			}
		}
	}

	/**
	 * Dispatching of unit request.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected static void dispatchMode(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException, IllegalArgumentException {
		handleModeChange(request, response);
	}

	/**
	 * Change heating mode to given one.
	 *
	 * @param mode Mode to change to
	 */
	protected static void changeMode(HeatingMode mode) {
		SmartHeating.mode = mode;
	}

	/**
	 * Help method for dispatching main page of heating.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected static void dispatchChangeRegime(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
			handleModeChange(request, response);
		} catch (IllegalArgumentException ex) {
			Logger.getLogger(SmartHeating.class.getName()).log(Level.WARNING, "Unknown mode name.", ex);
		}
		dispatchMain(request, response);
	}

	/**
	 * Dispatching of unit request.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected static void dispatchUnit(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		if (request.getMethod().equalsIgnoreCase("PUT")) {
			putUnitsDispatch(request, response);
		} else {
			getUnitsDispatch(request, response);
		}

	}

	/**
	 * GET units request dispatch.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private static void getUnitsDispatch(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String[] tokens = request.getPathInfo().split("/");
		// path 
		String format = tokens.length > 2 ? tokens[2] : null;
		String id = tokens.length > 3 ? tokens[3] : null;

		String jsonUnits = null;

		if (format != null && !format.isEmpty()) {
			if (id != null && id.isEmpty()) {
				id = null;
			}

			switch (format.toLowerCase()) {

				case "json":
					jsonUnits = getJsonUnits(id);
					break;

				default:
					jsonUnits = getJsonUnits(id);
					break;
			}

		}

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.print(jsonUnits);
		out.flush();
	}

	/**
	 * PUT request units dispatch. Updates units based on specified JSON.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private static void putUnitsDispatch(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String[] tokens = request.getPathInfo().split("/");
		// path 
		String format = tokens.length > 2 ? tokens[2] : null;

		String jsonUnits = request.getReader().lines().collect(Collectors.joining());

		if (format != null && !format.isEmpty()) {

			switch (format.toLowerCase()) {

				case "json":
					putUnitsUpdate(jsonUnits);
					break;

				default:
					putUnitsUpdate(jsonUnits);
					break;
			}

		}

		response.setStatus(HttpServletResponse.SC_OK);
	}

	/**
	 * Update
	 * @param jsonUnits json format for heating untis change 
	 */
	private static void putUnitsUpdate(String jsonUnits) {

		JsonReader reader = Json.createReader(new StringReader(jsonUnits));
		JsonArray arr = reader.readArray();

		HeatingUnit unit = null;

		for (JsonValue val : arr) {
			JsonObject obj = val.asJsonObject();
			unit = SmartHeating.getHeatingUnit(obj.getString("id"));
			if (unit != null) {

				String numb = obj.getString("preferredTemp");
				if (numb != null) {
					try {
						unit.setPreferredTemp(Float.parseFloat(numb));
					} catch (NumberFormatException ex) {
						Logger.getLogger(SmartHeating.class.getName()).log(Level.SEVERE, "Incorrect preffered temperature input.", ex);
					}
				}
			}
		}
	}

	/**
	 * Get temperature reading from all heating units or just selected one.
	 *
	 * @param id if null get all heating unit, otherwise return only specified
	 * unit.
	 * @return
	 */
	private static String getJsonUnits(String id) {
		String jsonStr = null;
		if (id == null) {
			JsonArrayBuilder builder = Json.createArrayBuilder();
			for (HeatingUnit unit : heatingUnits.values()) {
				builder.add(unit.getJsonTempReading());
			}
			jsonStr = builder.build().toString();
		} else {
			HeatingUnit unit = getHeatingUnit(id);
			if (unit != null) {
				jsonStr = unit.getJsonTempReading().toString();
			}
		}
		return jsonStr;
	}

	/**
	 * Dispatching of createunit.jsp file.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected static void dispatchCreateUnit(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String id = request.getParameter("id");
		StatusCode status = StatusCode.NONE;

		if (request.getParameter("create_button") != null) {
			String idRelay = request.getParameter("idrelay");
			String idThermometer = request.getParameter("idthermo");

			if (idRelay != null && idThermometer != null) {
				Device relay = SmartHandler.getDevice(idRelay);
				Device thermo = SmartHandler.getDevice(idThermometer);

				if (relay != null && thermo != null) {
					if (relay instanceof Relay) {
						if (thermo instanceof Thermometer) {
							if (id != null && id.matches(ID_REGEXP)) {
								HeatingUnit heatingUnit = new HeatingUnit(id, (Thermometer) thermo, (Relay) relay);
								try {
									try {
										SmartHeating.addHeatingUnit(id, heatingUnit);
										status = StatusCode.CREATE_UNIT_SUCC;
									} catch (SQLException ex) {
										Logger.getLogger(SmartHeating.class.getName()).log(Level.SEVERE, null, ex);
										status = StatusCode.CREATE_DB_FAIL;
									}
								} catch (DeviceExistsException ex) {
									status = StatusCode.CREATE_UNIT_EXISTS;
									Logger.getLogger(SmartHeating.class.getName()).log(Level.SEVERE, null, ex);
								}
							} else {
								status = StatusCode.CREATE_ID_FAIL;
							}
						} else {
							status = StatusCode.CREATE_NOT_FOUND;
						}
					} else {
						status = StatusCode.CREATE_NOT_FOUND;
					}
				} else {
					status = StatusCode.CREATE_NOT_FOUND;
				}
			}
		}

		List<String> relayIDList = new ArrayList<>();
		List<String> thermoIDList = new ArrayList<>();

		for (String devID : SmartHandler.getDevicesIDs()) {
			Device device = SmartHandler.getDevice(devID);
			if (device instanceof Relay) {
				relayIDList.add(devID);
			} else if (device instanceof Thermometer) {
				thermoIDList.add(devID);
			}
		}

		request.setAttribute("relays", relayIDList);
		request.setAttribute("thermometers", thermoIDList);
		request.setAttribute("status", status.getDescription());

		dispatch(request, response, "/createunit.jsp");
	}

	/**
	 * Add new predefined record.
	 *
	 * @param day Day of record.
	 * @param heatingUnitID ID of the unit
	 * @param time time of the record
	 * @param temperature Chosen temperature
	 * @return Info about processing for user.
	 */
	protected static StatusCode addPredefinedRecord(String day,
			String heatingUnitID, String time, float temperature) {

		if (day != null && heatingUnitID != null
				&& time != null) {
			// check if day is actual name of the table
			if (Arrays.asList(DB_TABLE_DAYS).contains(day)) {

				String query;
				PreparedStatement preparedStatement;
				try (Connection connection = SmartHandler.getDBConnection()) {

					SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
					long ms = sdf.parse(time).getTime();

					// ex. Monday (Time,UnitID,Temperature)
					query = String.format("MERGE INTO %1$s (%2$s,%3$s,%4$s) KEY(%2$s,%3$s) VALUES (?,?,?)",
							day, DB_DAYS_COL_TIME, DB_DAYS_COL_UNIT_ID, DB_DAYS_COL_TEMPERATURE);
					preparedStatement = connection.prepareStatement(query);

					preparedStatement.setTime(1, new Time(ms));
					preparedStatement.setString(2, heatingUnitID);
					preparedStatement.setFloat(3, temperature);
					preparedStatement.executeUpdate();
					preparedStatement.close();

					return StatusCode.PREDEF_ADD_SUCC;
				} catch (SQLException ex) {
					Logger.getLogger(SmartHeating.class.getName()).log(Level.SEVERE, "Predefined statement saving failed.", ex);
					return StatusCode.PREDEF_ADD_FAIL;
				} catch (ParseException ex) {
					Logger.getLogger(SmartHeating.class.getName()).log(Level.SEVERE, "Time parsing failed.", ex);
					return StatusCode.PREDEF_ADD_FAIL;
				}
			}
		}

		return StatusCode.NONE;

	}

	/**
	 * Removed predefined record from database
	 *
	 * @param day Day to remove from
	 * @param heatingUnitID ID of unit
	 * @param time time of record
	 * @return Info about processing for use.
	 */
	protected static StatusCode removePredefinedRecord(String day,
			String heatingUnitID, String time) {

		if (day != null && heatingUnitID != null
				&& time != null) {
			// check if day is actual name of the table
			if (Arrays.asList(DB_TABLE_DAYS).contains(day)) {

				String query;
				PreparedStatement preparedStatement;
				try (Connection connection = SmartHandler.getDBConnection()) {

					SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
					long ms = sdf.parse(time).getTime();

					// ex. Monday (Time,UnitID,Temperature)
					query = String.format("DELETE FROM %1$s WHERE %2$s=? AND %3$s=?",
							day, DB_DAYS_COL_TIME, DB_DAYS_COL_UNIT_ID);
					preparedStatement = connection.prepareStatement(query);

					preparedStatement.setTime(1, new Time(ms));
					preparedStatement.setString(2, heatingUnitID);
					preparedStatement.executeUpdate();
					preparedStatement.close();

					return StatusCode.PREDEF_DEL_SUCC;
				} catch (SQLException ex) {
					Logger.getLogger(SmartHeating.class.getName()).log(Level.SEVERE, "Predefined statement removing failed.", ex);
					return StatusCode.PREDEF_DEL_FAIL;
				} catch (ParseException ex) {
					Logger.getLogger(SmartHeating.class.getName()).log(Level.SEVERE, "Invalid time input.", ex);
					return StatusCode.PREDEF_DEL_FAIL;
				}
			}
		}

		return StatusCode.NONE;
	}

	/**
	 * Set parameters and dispatch for smart-heating index page.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected static void dispatchMain(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// Collect all Relay and Thermometer devices

		String id = request.getParameter("id");
		// reqtype defines what should servlet do
		StatusCode status = StatusCode.NONE;

		// Either change settings of heating unit or delete heating unit.
		if (request.getParameter("set_button") != null) {
			// Change prefered temperature on unit
			if (id != null) {
				try {
					float temperature = Float.parseFloat(request.getParameter("temp"));
					HeatingUnit heatingUnit = SmartHeating.getHeatingUnit(id);
					if (heatingUnit != null) {
						synchronized (heatingUnit) {
							heatingUnit.setPreferredTemp(temperature);
						}
					}
				} catch (NumberFormatException ex) {
					status = StatusCode.TEMP_INP_FAIL;
				}
			}

		} else if (request.getParameter("delete_button") != null) {
			if (id != null) {
				SmartHeating.removeHeatingUnit(id);
				status = StatusCode.UNIT_DEL_SUCC;
			}
		} else if (request.getParameter("change_button") != null) {
			// Change current heating mode.
			String mode;
			if ((mode = request.getParameter("mode")) != null) {
				try {
					SmartHeating.changeMode(HeatingMode.valueOf(mode));
				} catch (IllegalArgumentException ex) {
					Logger.getLogger(SmartHeating.class.getName()).log(Level.INFO,
							"{0} is not a heating mode", mode);
					status = StatusCode.MODE_CHANGE_FAIL;
				}
			}
		}

		request.setAttribute("status", status.getDescription());
		request.setAttribute("currentMode", SmartHeating.mode);
		request.setAttribute("modes", HeatingMode.values());
		request.setAttribute("units", heatingUnits);

		dispatch(request, response, "/smartheating.jsp");
	}

	/**
	 * Set parameters and dispatch for smart-heating index page.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected static void dispatchThermostat(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// Collect all Relay and Thermometer devices

		String id = request.getParameter("id");
		// reqtype defines what should servlet do
		StatusCode status = StatusCode.NONE;

		// Either change settings of heating unit or delete heating unit.
		if (request.getParameter("set_button") != null) {
			// Change prefered temperature on unit
			if (id != null) {
				try {
					float temperature = Float.parseFloat(request.getParameter("temp"));
					HeatingUnit heatingUnit = SmartHeating.getHeatingUnit(id);
					if (heatingUnit != null) {
						synchronized (heatingUnit) {
							heatingUnit.setPreferredTemp(temperature);
						}
					}
				} catch (NumberFormatException ex) {
					status = StatusCode.TEMP_INP_FAIL;
				}
			}

		} else if (request.getParameter("delete_button") != null) {
			if (id != null) {
				SmartHeating.removeHeatingUnit(id);
				status = StatusCode.UNIT_DEL_SUCC;
			}
		} else if (request.getParameter("change_button") != null) {
			// Change current heating mode.
			String mode;
			if ((mode = request.getParameter("mode")) != null) {
				try {
					SmartHeating.changeMode(HeatingMode.valueOf(mode));
				} catch (IllegalArgumentException ex) {
					Logger.getLogger(SmartHeating.class.getName()).log(Level.INFO,
							"{0} is not a heating mode", mode);
					status = StatusCode.MODE_CHANGE_FAIL;
				}
			}
		}

		request.setAttribute("status", status.getDescription());
		request.setAttribute("currentMode", SmartHeating.mode);
		request.setAttribute("modes", HeatingMode.values());
		request.setAttribute("units", heatingUnits);

		dispatch(request, response, "/thermostat.jsp");
	}

	/**
	 * Set attributes for predefined page.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected static void dispatchPredefined(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// Collect all heating units
		List<String> heatingUnitsIDList = getHeatingUnitsIDs();
		StatusCode status = StatusCode.NONE;
		List<DBPredefinedRecord> tableRecords = null;
		String day = null;
		String heatingUnitID = null;
		// Show predefined table to user
		boolean show = false;

		if (request.getParameter("add_button") != null) {
			show = true;
			heatingUnitID = request.getParameter("addUnitID");
			day = request.getParameter("addDay");
			String time = request.getParameter("addTime");
			String temperature = request.getParameter("temperature");
			try {
				float temp = Float.parseFloat(temperature);
				status = addPredefinedRecord(day, heatingUnitID, time, temp);
			} catch (NumberFormatException ex) {
				Logger.getLogger(SmartHeating.class.getName()).log(Level.INFO,
						"Not correct temperature");
				status = StatusCode.TEMP_INP_FAIL;
			}

			tableRecords = getRecords(day, heatingUnitID, status);

		} else if (request.getParameter("show_button") != null) {
			show = true;
			day = request.getParameter("day");
			heatingUnitID = request.getParameter("heatingUnitID");

			// Load records from the database
			tableRecords = getRecords(day, heatingUnitID, status);

		} else if (request.getParameter("delete_button") != null) {
			show = true;
			heatingUnitID = request.getParameter("id");
			day = request.getParameter("day");
			String time = request.getParameter("time");

			if (heatingUnitID != null && day != null & time != null) {
				status = removePredefinedRecord(day, heatingUnitID, time);

			}
			tableRecords = getRecords(day, heatingUnitID, status);
		}

		// Show only if records have been requested
		if (tableRecords != null && day != null && heatingUnitID != null) {
			tableRecords.sort((r1, r2) -> r1.getTime().compareTo(r2.getTime()));
			request.setAttribute("day", day);
			request.setAttribute("unitID", heatingUnitID);
			request.setAttribute("tableRecords", tableRecords);
		}

		// Get all times options availible to choose from
		List<String> availibleTimes = getAvailibleTimes();

		request.setAttribute("show", show);
		request.setAttribute("availibleTimes", availibleTimes);
		
		request.setAttribute("days", DB_TABLE_DAYS);
		request.setAttribute("heatingUnits", heatingUnitsIDList);
		request.setAttribute("status", status.getDescription());
		dispatch(request, response, "/predefined.jsp");
	}

	/**
	 * Get all records for chosen attributes.
	 *
	 * @param day Day of record.
	 * @param heatingUnitID Id of the unit
	 * @param status Status of processing for user info.
	 * @return List of all found records.
	 */
	private static List<DBPredefinedRecord> getRecords(String day, String heatingUnitID, StatusCode status) {
		List<DBPredefinedRecord> tableRecords = new ArrayList<>();
		if (day != null && heatingUnitID != null) {
			// Check if day is valid table name, also prevents SQL injection
			if (Arrays.asList(DB_TABLE_DAYS).contains(day)) {
				// Check if Heating Unit with that ID actually exists.
				if (heatingUnits.containsKey(heatingUnitID)) {
					String query;
					PreparedStatement preparedStatement;

					try (Connection connection = SmartHandler.getDBConnection()) {
						ResultSet rs;
						query = String.format("SELECT * FROM %s WHERE %s=?",
								day, DB_DAYS_COL_UNIT_ID);

						preparedStatement = connection.prepareStatement(query);
						preparedStatement.setString(1, heatingUnitID);
						rs = preparedStatement.executeQuery();

						DBPredefinedRecord record;
						while (rs.next()) {
							record = new DBPredefinedRecord();
							record.setHeatingUnitID(rs.getString(DB_DAYS_COL_UNIT_ID));
							record.setTime(rs.getTime(DB_DAYS_COL_TIME));
							record.setTemperature(rs.getFloat(DB_DAYS_COL_TEMPERATURE));
							tableRecords.add(record);
						}
						preparedStatement.close();
						rs.close();
					} catch (SQLException ex) {
						Logger.getLogger(SmartHeating.class.getName()).log(Level.SEVERE, null, ex);
					}
				} else {
					status = StatusCode.PREDEF_UNIT_FAIL;
				}
			} else {
				status = StatusCode.PREDEF_DAY_FAIL;
			}
		}
		return tableRecords;
	}

	/**
	 * Get all times possible to store record. Increases by 15 minutes.
	 *
	 * @return all day times by 15 min increments.
	 */
	private static List<String> getAvailibleTimes() {
		List<String> availibleTimes = new ArrayList<>();
		String hour;
		String minute;
		for (int i = 0; i < 24; i++) {
			for (int j = 0; j < 60; j += 15) {
				if (i < 10) {
					hour = "0%d";
				} else {
					hour = "%d";
				}
				if (j == 0) {
					minute = "%d0";
				} else {
					minute = "%d";
				}

				availibleTimes.add(String.format(hour + ":" + minute, i, j));
			}
		}
		return availibleTimes;
	}

	/**
	 * Remove unit with id from DB.
	 *
	 * @param id ID of unit
	 * @throws SQLException
	 */
	private static void removeHeatingUnitFromDB(String id) throws SQLException {
		String deleteQuery;
		PreparedStatement deletePS;

		try (Connection connection = SmartHandler.getDBConnection()) {
			deleteQuery = "DELETE FROM " + DB_TABLE_HEATING_UNITS + " WHERE ID=?";

			deletePS = connection.prepareStatement(deleteQuery);
			deletePS.setString(1, id);
			deletePS.executeUpdate();
			deletePS.close();
			removeAllPredefinedData(connection, id);
		} catch (SQLException ex) {
			throw ex;
		}
	}

	/**
	 * Remove all predefined data of the unit with id. Used for removing records
	 * after unit deletion.
	 *
	 * @param connection
	 * @param id ID of unit
	 * @throws SQLException
	 */
	private static void removeAllPredefinedData(Connection connection, String id) throws SQLException {
		String deleteQuery;
		PreparedStatement deletePS;
		for (String day : DB_TABLE_DAYS) {

			deleteQuery = "DELETE FROM " + day + " WHERE " + DB_DAYS_COL_UNIT_ID + "=?";

			deletePS = connection.prepareStatement(deleteQuery);
			deletePS.setString(1, id);
			deletePS.executeUpdate();
			deletePS.close();
		}
	}

	/**
	 * Removes Heating Unit from running application and from database.
	 *
	 * @param id ID of device to remove.
	 */
	protected static void removeHeatingUnit(String id) {
		synchronized (heatingUnits) {
			if (heatingUnits.containsKey(id)) {
				try {
					removeHeatingUnitFromDB(id);
					heatingUnits.remove(id);

				} catch (SQLException ex) {
					Logger.getLogger(SmartHandler.class
							.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	/**
	 * Add new heating unit to database.
	 *
	 * @param heatingUnit Unit to add.
	 * @throws SQLException
	 */
	private static void addHeatingUnitToDB(HeatingUnit heatingUnit) throws SQLException {
		String query;
		PreparedStatement preparedStatement;

		try (Connection connection = SmartHandler.getDBConnection()) {

			// Insert data
			query = "INSERT INTO " + DB_TABLE_HEATING_UNITS + "(ID ,RelayID, ThermoID, Temperature) "
					+ "VALUES (?,?,?,?)";
			preparedStatement = connection.prepareStatement(query);
			synchronized (heatingUnit) {
				preparedStatement.setString(1, heatingUnit.getID());
				preparedStatement.setString(2, heatingUnit.getRelayID());
				preparedStatement.setString(3, heatingUnit.getThermometerID());
				preparedStatement.setFloat(4, heatingUnit.getPreferredTemp());
			}

			preparedStatement.executeUpdate();
			preparedStatement.close();
		}
	}

	/**
	 * Add heating unit to the in memory and database storage.
	 *
	 * @param id ID of the unit
	 * @param heatingUnit Unit to register
	 * @throws DeviceExistsException If ID is already taken
	 */
	protected static void addHeatingUnit(String id, HeatingUnit heatingUnit) throws DeviceExistsException, SQLException {
		synchronized (heatingUnits) {
			if (!heatingUnits.containsKey(id)) {
				try {
					addHeatingUnitToDB(heatingUnit);
					heatingUnits.put(id, heatingUnit);

				} catch (SQLException ex) {
					Logger.getLogger(SmartHeating.class
							.getName()).log(Level.SEVERE, null, ex);
					throw ex;
				}
			} else {
				throw new DeviceExistsException("Heating unit with id " + id + " already exists.");
			}
		}
	}

	/**
	 * Returns unit id or null if it isn't registered.
	 *
	 * @param id
	 * @return
	 */
	protected synchronized static HeatingUnit getHeatingUnit(String id) {
		return heatingUnits.get(id);
	}

	/**
	 * Help method for quick access to IDs of units.
	 *
	 * @return IDs of all heating units.
	 */
	public synchronized static List<String> getHeatingUnitsIDs() {
		List<String> heatingUnitsList = new ArrayList<>();

		heatingUnits.forEach((id, unit) -> {
			heatingUnitsList.add(id);
		});

		return heatingUnitsList;
	}

	/**
	 * Process menu name.
	 *
	 * @return name
	 */
	@Override
	public String getName() {
		return "Smart Heating";
	}

	/**
	 * Actual heating checking logic.
	 */
	@Override
	public void run() {
		startTime = System.currentTimeMillis();
		while (true) {
			if (null != mode) {
				switch (mode) {
					case MANUAL:
						checkManual();
						break;
					case PREDEFINED:
						checkPredefined();
						break;
					default:
						break;
				}
			}
			try {
				// Check only every interval (s)
				Thread.sleep(interval * 1000);

			} catch (InterruptedException ex) {
				Logger.getLogger(SmartHeating.class
						.getName()).log(Level.SEVERE, "Heating process interrupted.", ex);
				break;
			}
		}
	}

	/**
	 * For every heating unit adjust heating accordingly to the manual
	 * temperature value.
	 */
	private static void checkManual() {
		// Check every smartheating if temperature isnt
		// too far from prefered value.
		//Set relay state accordingly
		heatingUnits.forEach((id, unit) -> {
			checkTemperature(unit, unit.getPreferredTemp());
		});
	}

	/**
	 * Check if current temperature is not too far from preferred value. The
	 * maximal possible difference is appointed by tempBound variable.
	 *
	 * @param unit Heating Unit to check on.
	 * @param preferredTemperature value to compare to
	 */
	private static void checkTemperature(HeatingUnit unit, float preferredTemperature) {
		float currentTemperature = unit.getThermometer().getTemperature();
		if (currentTemperature < (preferredTemperature - tempBound)) {
			unit.getRelay().setState(RelayState.ON);
		} else if (currentTemperature > (preferredTemperature + tempBound)) {
			unit.getRelay().setState(RelayState.OFF);
		}

		// longer than interval
		if (store_temps && (System.currentTimeMillis() - startTime) >= TEMP_STORE_INTERVAL) {
			startTime = System.currentTimeMillis();
			saveMeasurement(unit.getID(), currentTemperature);
		}

	}

	/**
	 * Temperature measurements storing. Mainly for possible automated learning
	 * implementations.
	 *
	 * @param id Id of unit.
	 * @param temperature Measured temperature
	 */
	private static void saveMeasurement(String id, float temperature) {
		String query;
		PreparedStatement preparedStatement;
		Statement statement;
		try (Connection connection = SmartHandler.getDBConnection()) {
			statement = connection.createStatement();

			// Create storing table
			query = "CREATE TABLE IF NOT EXISTS " + DB_TABLE_TEMPERATURES + "("
					+ "ID varchar(255) NOT NULL,"
					+ "TIME TIMESTAMP NOT NULL,"
					+ "TEMPERATURE FLOAT,"
					+ "PRIMARY KEY (ID, TIME))";

			statement.executeUpdate(query);
			statement.close();

			// Insert data
			query = "INSERT INTO " + DB_TABLE_TEMPERATURES + "(ID, TIME ,TEMPERATURE) "
					+ "VALUES (?,?,?)";
			preparedStatement = connection.prepareStatement(query);
			preparedStatement.setString(1, id);
			preparedStatement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			preparedStatement.setFloat(3, temperature);

			preparedStatement.executeUpdate();
			preparedStatement.close();
		} catch (SQLException ex) {
			Logger.getLogger(SmartHeating.class.getName()).log(Level.INFO, "Measurment wasn't stored.", ex);
		}

	}

	/**
	 * Set, for specified day for every heating unit, heating accordingly to
	 * predefined temperature for current time.
	 *
	 * @param day For which day to check.
	 */
	private static void checkPredefinedDay(String day) {
		String query;
		Statement statement;
		ResultSet rs;

		try (Connection connection = SmartHandler.getDBConnection()) {
			float temperature;
			String heatingUnitID;
			HeatingUnit heatingUnit;

			statement = connection.createStatement();

			ZonedDateTime zdt = ZonedDateTime.now();
			String currentTime = zdt.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

			for (Map.Entry<String, HeatingUnit> entry : heatingUnits.entrySet()) {
				heatingUnitID = entry.getKey();
				heatingUnit = entry.getValue();

				// Check latest setting
				query = String.format("Select TOP 1 * FROM %s WHERE %s BETWEEN '00:00:00' and '%s'"
						+ "AND %s = '%s'"
						+ "ORDER BY %s DESC", day, DB_DAYS_COL_TIME, currentTime, DB_DAYS_COL_UNIT_ID, heatingUnitID, DB_DAYS_COL_TIME);
				rs = statement.executeQuery(query);

				// If none temperature for the day is set, use prefferedTemp for that heating unit
				if (rs.next()) {
					try {
						temperature = rs.getFloat(DB_DAYS_COL_TEMPERATURE);
						heatingUnit.setPreferredTemp(temperature);
						checkTemperature(heatingUnit, temperature);
					} catch (SQLException ex) {
						Logger.getLogger(SmartHeating.class.getName()).log(Level.SEVERE, "Getting predefined value failed, reverting to preffered value.", ex);
						checkTemperature(heatingUnit, heatingUnit.getPreferredTemp());
					}
				} else {
					checkTemperature(heatingUnit, heatingUnit.getPreferredTemp());
				}
			}
		} catch (SQLException ex) {
			Logger.getLogger(SmartHeating.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	/**
	 * Set day on week cycle to check settings for.
	 */
	private static void checkPredefined() {
		Calendar calendar = Calendar.getInstance();
		int day = calendar.get(Calendar.DAY_OF_WEEK);

		// Careful this is dependant on the order of the elements in the array.
		switch (day) {
			case Calendar.MONDAY:
				checkPredefinedDay(DB_TABLE_DAYS[0]);
				break;
			case Calendar.TUESDAY:
				checkPredefinedDay(DB_TABLE_DAYS[1]);
				break;
			case Calendar.WEDNESDAY:
				checkPredefinedDay(DB_TABLE_DAYS[2]);
				break;
			case Calendar.THURSDAY:
				checkPredefinedDay(DB_TABLE_DAYS[3]);
				break;
			case Calendar.FRIDAY:
				checkPredefinedDay(DB_TABLE_DAYS[4]);
				break;
			case Calendar.SATURDAY:
				checkPredefinedDay(DB_TABLE_DAYS[5]);
				break;
			case Calendar.SUNDAY:
				checkPredefinedDay(DB_TABLE_DAYS[6]);
				break;
			default:
				break;
		}
	}

	/**
	 * Context prefix of process.
	 *
	 * @param prefix
	 */
	@Override
	public void setPrefix(String prefix) {
		SmartHeating.prefix = prefix;
	}
}
