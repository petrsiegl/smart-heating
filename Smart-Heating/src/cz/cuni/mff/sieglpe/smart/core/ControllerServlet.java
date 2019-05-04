/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague.
 */
package cz.cuni.mff.sieglpe.smart.core;

import cz.cuni.mff.sieglpe.smart.device.RelayState;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.exception.GpioPinExistsException;
import static cz.cuni.mff.sieglpe.smart.core.UserHandler.changePassword;
import static cz.cuni.mff.sieglpe.smart.core.UserHandler.changeUserSettings;
import static cz.cuni.mff.sieglpe.smart.core.UserHandler.getUsers;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static cz.cuni.mff.sieglpe.smart.core.ServerStarter.CONTROLLER_PREFIX;
import static cz.cuni.mff.sieglpe.smart.core.ServerStarter.PAGES_PREFIX;

/**
 * Front controller. Manages all requests and sets correct data to the pages,
 * before dispatching.
 *
 * @author Petr Siegl
 */
public class ControllerServlet extends HttpServlet {

	// "GPIO " the beginning of GPIO pin name
	protected static final int PIN_PREFIX_LENGTH = "GPIO ".length();
	protected static final int USERNAME_MAX_LENGTH = 100;
	protected static final int USERNAME_MIN_LENGTH = 1;
	protected static final int PASS_MAX_LENGTH = 100;
	protected static final int PASS_MIN_LENGTH = 8;
	protected static final int ID_MAX_LENGTH = 100;
	protected static final int ID_MIN_LENGTH = 1;
	protected static final String USERNAME_REGEXP = String.format("[\\w-]{%d,%d}", USERNAME_MIN_LENGTH, USERNAME_MAX_LENGTH);
	protected static final String ID_REGEXP = String.format("[\\w-]{%d,%d}", ID_MIN_LENGTH, ID_MAX_LENGTH);
	protected static final String PASS_REGEXP = String.format(".{%d,%d}", PASS_MIN_LENGTH, PASS_MAX_LENGTH);

	/**
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	public void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
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
	public void doPut(HttpServletRequest request,
			HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Process incoming HTTP request.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public void processRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String path = request.getRequestURI();
		// Remove CONTROLLERPREFIX
		path = path.substring(CONTROLLER_PREFIX.length());
		// Parse path.
		if (!path.startsWith("/servlet")) {
			String[] tokens = path.split("/");
			if (tokens.length > 1) {
				if (!SmartHandler.isLoadedProcessPrefix(tokens[1])) {
					switch (tokens[1].toLowerCase()) {
						case "users":
							dispatchUsers(request, response);
							break;
						case "account":
							dispatchAccount(request, response);
							break;
						case "create.jsp":
						case "create-device":
							dispatchCreate(request, response);
							break;
						case "devices.jsp":
						case "devices":
							dispatchDevices(request, response);
							break;
						case "traverser.jsp":
						case "traverser":
							dispatchTraverser(request, response);
							break;
						case "home":
						case "index.jsp":
							dispatch(request, response, "/index.jsp");
							break;
						default:
							dispatch(request, response, path);
							break;

					}
				} else {
					// Is some loaded process job to respond -> forward to it
					request.getRequestDispatcher(path).forward(request, response);
				}
			} else {
				dispatch(request, response, path);
			}
		} else {
			request.getRequestDispatcher(path).forward(request, response);
		}
	}

	/**
	 * Dispatch request for device showing page.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private void dispatchDevices(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String id;
		StatusCode status = StatusCode.NONE;

		String[] tokens = request.getPathInfo().split("/");
		id = tokens.length > 2 ? tokens[2] : null;
		if (id == null) {
			id = request.getParameter("id");
		}

		if (id != null) {
			request.setAttribute("id", id);

			// Found if device with wanted id exists.
			Device device = SmartHandler.getDevice(id);
			if (device != null) {
				request.setAttribute("found", true);
				if (request.getParameter("set_button") != null) {
					try {
						setHtmlDevice(request, device);
						status = StatusCode.DEVICE_SET;
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
						Logger.getLogger(ControllerServlet.class.getName()).log(Level.SEVERE, null, ex);
						status = StatusCode.DEVICE_UNCHANGED;
					}
				} else if (request.getParameter("delete_button") != null) {
					// Request for removing device
					SmartHandler.removeDevice(id);
					status = StatusCode.DEVICE_DELETED;
				}

				Map<String, String> info = new HashMap<>();
				Map<String, String[]> radios = new HashMap<>();
				Map<String, String> checked = new HashMap<>();

				getDeviceInfo(device, info, radios, checked);

				request.setAttribute("checked", checked);
				request.setAttribute("radios", radios);
				request.setAttribute("info", info);
			}

		}

		// Get all ids of devices in the system
		List<String> ids = SmartHandler.getDevicesIDs();
		request.setAttribute("ids", ids);
		request.setAttribute("status", status.getDescription());
		dispatch(request, response, "/devices.jsp");
	}

	/**
	 * Set all HtmlSetter annotations possible to gather from request.
	 *
	 * @param request
	 * @param device
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private static void setHtmlDevice(HttpServletRequest request, Device device)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (device != null) {
			Method[] methods = device.getClass().getDeclaredMethods();
			HtmlSetter annotation;
			String valueToSet;

			for (Method method : methods) {
				annotation = method.getAnnotation(HtmlSetter.class);
				if (annotation != null) {
					// Find  the associeted HtmlGetter 
					valueToSet = request.getParameter(annotation.name());
					if (valueToSet != null) {
						// Try to set the method
						method.invoke(device, valueToSet);
					}
				}
			}
		}

	}

	/**
	 * Get all possible information from HtmlGetter annotations of device.
	 *
	 * @param device Device to get info from.
	 * @param info Stores static text.
	 * @param radios HTML radio view.
	 * @param checked Checked options for radios.
	 */
	private static void getDeviceInfo(Device device,
			Map<String, String> info,
			Map<String, String[]> radios,
			Map<String, String> checked) {
		// Device is in the database.

		Class<? extends Device> clazz = (Class<? extends Device>) device.getClass();

		HtmlGetter annot;
		Method[] methods = clazz.getDeclaredMethods();

		// For every method with HtmlField annotation create item in form.
		String name;
		String value;

		for (Method m : methods) {
			annot = m.getAnnotation(HtmlGetter.class);
			if (annot != null) {
				name = annot.name();

				String text;
				switch (annot.kind()) {

					case TEXT:
						try {
							text = m.invoke(device).toString();
							value = text;
							info.put(name, value);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
							Logger.getLogger(ControllerServlet.class.getName()).log(Level.SEVERE, null, ex);
						}
						break;

					case RADIO:
						// Create radiobox for every possible state
						try {
							RelayState rs = (RelayState) m.invoke(device);
							checked.put(name, rs.toString());
							radios.put(name, annot.radioValues());

						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
							Logger.getLogger(ControllerServlet.class.getName()).log(Level.SEVERE, null, ex);
						}
						break;

					default:
						break;
				}
			}
		}
	}

	/**
	 * Dispatch request for device creation page.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private void dispatchCreate(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		StatusCode status = StatusCode.NONE;
		// Names of all currently accessible pins stored in pins.
		List<String> pins = new ArrayList<>();
		// Names of all currently accessible devices stored in types.
		List<String> types = new ArrayList<>();

		for (Pin p : RaspiPin.allPins()) {
			pins.add(p.getName());
		}

		// which pin to add
		String pinName = request.getParameter("pin");
		// ID to add
		String id = request.getParameter("id");
		// what type of device to add
		String type = request.getParameter("type");

		if (pinName != null && id != null && type != null) {
			id = id.trim();
			if (id.matches(ID_REGEXP)) {
				try {
					status = handleAdding(pinName, type, id);
				} catch (DeviceExistsException ex) {
					status = StatusCode.ID_EXISTS;
				} catch (GpioPinExistsException ex) {
					status = StatusCode.PIN_EXISTS;
				} catch (SecurityException | IllegalArgumentException ex) {
					Logger.getLogger(ControllerServlet.class.getName()).log(Level.SEVERE, null, ex);
					status = StatusCode.CREATION_FAIL;
				}
			} else {
				status = StatusCode.ID_FAIL;
			}
		}

		// Sort GPIO #number so that the serie is ascending
		pins.sort((first, second) -> {
			// Take away GPIO and sort just by number
			return Integer.parseInt(first.substring(PIN_PREFIX_LENGTH)) - Integer.parseInt(second.substring(PIN_PREFIX_LENGTH));
		});

		for (String className : SmartHandler.handlers.keySet()) {
			types.add(className);
		}

		request.setAttribute("types", types);
		request.setAttribute("pins", pins);
		request.setAttribute("status", status.getDescription());
		dispatch(request, response, "/create.jsp");
	}

	/**
	 * Help method for adding of a new device.
	 *
	 * @param pinName pin used by device
	 * @param type type of device
	 * @param id id of the device
	 * @return State of processing for user info
	 * @throws DeviceExistsException
	 */
	private static StatusCode handleAdding(String pinName, String type, String id) throws DeviceExistsException {
		Pin pin = RaspiPin.getPinByName(pinName);
		if (pin == null) {
			return StatusCode.PIN_FAIL;
		}

		DeviceHandler handler = SmartHandler.getHandler(type);

		if (handler == null) {
			try {
				// Call static initialization for possible loading of handler.
				Class.forName(type);
				handler = SmartHandler.getHandler(type);
				if (handler != null) {
					addGpioDevice(id, new Pin[]{pin}, handler);
					return StatusCode.DEVICE_CREATED;
				} else {
					return StatusCode.NO_HANDLER;
				}
			} catch (ClassNotFoundException ex) {
				Logger.getLogger(ControllerServlet.class.getName()).log(Level.INFO, "Requested class not found: " + type);
				return StatusCode.CLASS_FAIL;
			}
		} else {
			addGpioDevice(id, new Pin[]{pin}, handler);
			return StatusCode.DEVICE_CREATED;
		}
	}

	private static void addGpioDevice(String id, Pin[] pins, DeviceHandler handler) throws DeviceExistsException {
		Device device = handler.getInstance(id, pins);
		if (device != null) {
			SmartHandler.addDevice(id, device);
		}
	}

	/**
	 * Dispatch request for users page.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private void dispatchUsers(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		List<User> users;
		StatusCode status = StatusCode.NONE;

		String username;
		String level;
		String password = null;

		String[] tokens = request.getPathInfo().split("/");
		username = tokens.length > 2 ? tokens[2] : null;
		if (username == null) {
			username = request.getParameter("username");
		}

		level = request.getParameter("level");
		password = request.getParameter("password");

		if (request.getParameter("modify_button") != null) {
			status = handleUserModification(username, password, level);
		} else if (request.getParameter("delete_button") != null || request.getMethod().equals("DELETE")) {
			status = handleUserDeletion(username);
		} else if (request.getParameter("create_button") != null || request.getMethod().equals("PUT")) {
			status = handleUserCreation(username, password, level);
		}

		try {
			users = getUsers();
			request.setAttribute("users", users);
		} catch (SQLException ex) {
			request.setAttribute("loadFail", true);
			Logger.getLogger(ControllerServlet.class.getName()).log(Level.SEVERE, null, ex);
		}

		Map<Integer, String> levels = new HashMap<>();

		levels.put(UserLevel.USER, "User");
		levels.put(UserLevel.MODERATOR, "Moderator");
		levels.put(UserLevel.ADMIN, "Administrator");
		request.setAttribute("levels", levels);
		request.setAttribute("status", status.getDescription());
		dispatch(request, response, "/users.jsp");
	}

	/**
	 * Handle account modification request.
	 *
	 * @param username user to modify
	 * @param password password to change; do nothing if null
	 * @param level level to set
	 * @return status of action
	 */
	private StatusCode handleUserModification(String username, String password, String level) {

		if (level != null) {
			level = level.trim();
		}

		// If the password is empty change just level => null password
		if (password != null) {
			password = password.trim();
			if (password.isEmpty()) {
				password = null;
			}
		}

		try {
			int intLevel = Integer.parseInt(level);
			if (password == null || password.matches(PASS_REGEXP)) {
				return changeUserSettings(username, password, intLevel);
			} else {
				return StatusCode.USR_PASS_FAIL;
			}
		} catch (SQLException | NumberFormatException | PasswordHasher.CannotPerformOperationException ex) {
			Logger.getLogger(ControllerServlet.class.getName())
					.log(Level.SEVERE, "Modification of user " + username + " failed.", ex);
			return StatusCode.USR_MOD_FAIL;
		}
	}

	/**
	 * Handle account deletion.
	 *
	 * @param username username of account to delete
	 * @return Status of action
	 */
	private StatusCode handleUserDeletion(String username) {
		try {

			UserHandler.deleteUser(username);
			return StatusCode.USR_DEL_SUCC;

		} catch (SQLException | PasswordHasher.CannotPerformOperationException ex) {
			Logger.getLogger(ControllerServlet.class.getName())
					.log(Level.SEVERE, "Deletion of user" + username + " failed", ex);
			return StatusCode.USR_DEL_FAIL;
		}
	}

	/**
	 * Check and handle input for user creation.
	 *
	 * @param username
	 * @param password
	 * @param level
	 * @return None if any parameter is nul, action status otherwise
	 */
	private StatusCode handleUserCreation(String username, String password, String level) {

		if (username == null || level == null || password == null) {
			return StatusCode.NONE;
		}

		String trimUsername = username.trim();
		if (!trimUsername.matches(USERNAME_REGEXP)) {
			return StatusCode.USR_NAME_FAIL;
		}

		if (!password.matches(PASS_REGEXP)) {
			return StatusCode.USR_INP_FAIL;
		}

		try {
			UserHandler.addUser(trimUsername, password, level);
			return StatusCode.USR_CREATE_SUCC;
		} catch (SQLException | PasswordHasher.CannotPerformOperationException ex) {
			Logger.getLogger(ControllerServlet.class.getName()).log(Level.SEVERE, null, ex);
			return StatusCode.USR_CREATE_FAIL;
		}
	}

	/**
	 * Dispatch request for account settings page.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private void dispatchAccount(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		StatusCode status = StatusCode.NONE;

		String newPassword = request.getParameter("password");
		if (newPassword != null && !newPassword.isEmpty()) {
			Object tmpUser = request.getSession().getAttribute("user");
			User user = (tmpUser instanceof User) ? (User) tmpUser : null;

			if (user != null) {
				try {
					changePassword(user, newPassword);
					status = StatusCode.ACC_PASS_CHANGED;
				} catch (SQLException | PasswordHasher.CannotPerformOperationException ex) {
					status = StatusCode.ACC_FAIL;
					Logger
							.getLogger(ControllerServlet.class
									.getName()).log(Level.SEVERE, null, ex);
				}
			} else {
				status = StatusCode.ACC_FAIL;
			}
		}
		request.setAttribute("status", status.getDescription());
		dispatch(request, response, "/account.jsp");

	}

	/**
	 * Traverser connector dispatching.
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private void dispatchTraverser(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		StatusCode status = StatusCode.NONE;
		if (request.getParameter("save_button") != null) {
			// Save new settings.
			String travStatus = request.getParameter("status");
			if (travStatus != null) {
				if (travStatus.equals("ON")) {
					SmartHandler.traverserConnector.start();
				} else {
					SmartHandler.traverserConnector.stop();
				}
			}
			String address = request.getParameter("address");
			if (address != null) {
				SmartHandler.traverserConnector.setAddress(address);
			}
			String port = request.getParameter("port");
			if (port != null) {
				try {
					int portNumber = Integer.parseInt(port);
					if (portNumber >= 0 && portNumber <= 65535) {
						SmartHandler.traverserConnector.setPort(portNumber);
					}
				} catch (NumberFormatException ex) {
					status = StatusCode.TRAVERSER_FAIL;
				}
			}

			/*			
			Possible Future implementation
			String context = request.getParameter("context");
			if (context != null) {
				SmartHandler.traverserConnector.setContext(context);
			}
			 */
			String password = request.getParameter("password");
			if (password != null && !password.isEmpty()) {
				try {
					SmartHandler.traverserConnector.setPassword(password);

				} catch (InterruptedException | TimeoutException | ExecutionException ex) {
					Logger.getLogger(ControllerServlet.class
							.getName()).log(Level.SEVERE, "Password change failed.", ex);
					status = StatusCode.TRAVERSER_FAIL;
				}
			}

		}

		String travStatus;
		if (SmartHandler.traverserConnector.isRunning()) {
			travStatus = "ON";
		} else {
			travStatus = "OFF";
		}

		//request.setAttribute("context", SmartHandler.traverserConnector.getContext());
		request.setAttribute("failing", SmartHandler.traverserConnector.isFailing() ? StatusCode.TRAVERSER_CONN_FAIL.getDescription() : "");
		request.setAttribute("status", status.getDescription());
		request.setAttribute("travStatus", travStatus);
		request.setAttribute("address", SmartHandler.traverserConnector.getAddress());
		request.setAttribute("port", SmartHandler.traverserConnector.getPort());
		dispatch(request, response, "/traverser.jsp");
	}

	/**
	 * Dispatch to chosen path.
	 *
	 * @param request
	 * @param response
	 * @param path where to dispatch to
	 * @throws ServletException
	 * @throws IOException
	 */
	public void dispatch(HttpServletRequest request,
			HttpServletResponse response, String path) throws ServletException, IOException {
		request.getRequestDispatcher(PAGES_PREFIX + path).forward(request, response);

	}
}
