/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.NATtraverser;

import cz.cuni.mff.sieglpe.smart.net.HttpKind;
import cz.cuni.mff.sieglpe.smart.net.Message;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Traverser - maps incoming requests on home system web interface.
 * Home system asks
 * @author Petr Siegl
 */
public class IncomingRequestServlet extends HttpServlet {


	/**
	 * Stores coming in requests for home server. Stored in XML.
	 */
	protected static Queue<String> outgoingMessages = new ConcurrentLinkedDeque<>();

	/**
	 * Stores request asynchronous handlers with unique id. Used for incoming answers.
	 */
	protected static Map<String, AsyncContext> requests = new ConcurrentHashMap<>();
	private static JAXBContext jaxbContext;
	private static Marshaller marshaller;
	private static Unmarshaller unmarshaller;

	private static final String DEFAULT_PASS = "traverser";
	private static String password = "traverser";

	// No big traffic expected so for collision avoidance is number enough.
	private static final AtomicInteger requestNumber = new AtomicInteger(0);

	/**
	 * Initialize XML processors and authentication file.
	 */
	@Override
	public void init() {
		try {
			jaxbContext = JAXBContext.newInstance(Message.class);
			marshaller = jaxbContext.createMarshaller();
			unmarshaller = jaxbContext.createUnmarshaller();
			loadAuthentication();
		} catch (JAXBException ex) {
			Logger.getLogger(IncomingRequestServlet.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Load password for communication with home server from file, or use default.
	 */
	private void loadAuthentication() {
		try (BufferedReader br = new BufferedReader(new FileReader("auth.txt"))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty() && line.charAt(0) != '#') {
					password = line;
				}
			}
		} catch (IOException ex) {
			password = DEFAULT_PASS;
		}
	}

	/**
	 * Check if application is authorized to read incoming message and send
	 * responses.
	 *
	 * @param user username to check on
	 * @param password password for username
	 * @return true if username is valid and correct password is included; false
	 * otherwise
	 */
	private boolean isAuthorized(String password) {
		// database can be added for more complex setups eventually
		return password != null && password.equals(this.password);
	}

	/**
	 * For receiving incoming messages
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	public void doPut(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String path = request.getPathInfo();

		// Authentication
		String password = request.getParameter("password");

		if (isAuthorized(password)) {
			if (path.startsWith("/messages")) {
				try {
					Message message;
					synchronized (unmarshaller) {
						message = (Message) unmarshaller.unmarshal(request.getReader());
					}
					AsyncContext ctx;
					// Only if request fot that sesison exist
					if ((ctx = requests.get(message.sessionId + message.requestId)) != null) {
						ctx.getResponse().getWriter().print(message.content);
						ctx.complete();
					}
				} catch (JAXBException ex) {
					Logger.getLogger(IncomingRequestServlet.class.getName()).log(Level.SEVERE, null, ex);
				}
			} else if (path.startsWith("/password")) {
				String newPass = request.getParameter("newPassword");
				if (newPass != null && !newPass.isEmpty()) {
					try {
						changePassword(newPass);
					} catch (IOException ex) {
						Logger.getLogger(IncomingRequestServlet.class.getName()).log(Level.SEVERE, "Auth file not created, using default", ex);
						IncomingRequestServlet.password = DEFAULT_PASS;
						response.sendError(HttpServletResponse.SC_CONFLICT);
					}
				} 
			}

		} else {
			// Authenticaiton failed
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}

	}

	/**
	 * Change password in file and in use.
	 * @param password
	 * @throws IOException 
	 */
	private void changePassword(String password) throws IOException {
		password = IncomingRequestServlet.password;

		File file = new File("auth.txt");

		if (!file.exists()) {
			file.createNewFile();
		}

		try (PrintWriter pw = new PrintWriter("auth.txt")) {
			pw.println(password);
		} catch (IOException ex) {
			throw ex;
		}

	}

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
		doPostOrig(request, response);
	}

	/**
	 *
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response
	) throws ServletException, IOException {

		String path = request.getPathInfo();
		if (path.startsWith("/messages")) {

			String password = request.getParameter("password");
			if (isAuthorized(password)) {
				// Send first message in queue
				String msg;
				// Sent nothing if empty
				if ((msg = outgoingMessages.poll()) != null) {
					response.getWriter().write(msg);
					response.setStatus(HttpServletResponse.SC_OK);
				}
			} else {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			}
		} else {
			doGetOrig(request, response);
		}
	}

	/**
	 * Create new async context for request and put it in requests.
	 * @param request
	 * @param response 
	 */
	private void handleAsyncContext(HttpServletRequest request,
			HttpServletResponse response) {
		// Tomcat setting, web.xml should be sufficient for some reason didn't work
		// for Tomcat
		request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
		AsyncContext as = request.startAsync(request, response);
		requests.put(request.getSession().getId() + requestNumber, as);
	}

	
	/**
	 * Handles all normal requests.
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException 
	 */
	private void doGetOrig(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		handleAsyncContext(request, response);

		Message message = new Message();
		message.kind = HttpKind.GET;
		if (request.getQueryString() != null) {
			// URL format querry string separated by '?'
			message.URI = request.getPathInfo() + "?" + request.getQueryString();
		} else {
			message.URI = request.getPathInfo();
		}

		messageInit(request, message);
		queueMessage(message);
	}

	private void doPostOrig(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		handleAsyncContext(request, response);

		Message message = new Message();
		message.kind = HttpKind.POST;
		message.parameters = request.getParameterMap();
		message.URI = request.getPathInfo();
		messageInit(request, message);
		queueMessage(message);
	}

	/**
	 * Generate id for message.
	 * @param request
	 * @param message 
	 */
	private void messageInit(HttpServletRequest request, Message message) {
		message.sessionId = request.getSession().getId();
		message.requestId = requestNumber.getAndIncrement();
	}

	/**
	 * Queue message for send.
	 * @param msg 
	 */
	private void queueMessage(Message msg) {
		try {
			StringWriter sw = new StringWriter();
			synchronized (marshaller) {
				marshaller.marshal(msg, sw);
			}
			outgoingMessages.add(sw.toString());

		} catch (JAXBException ex) {
			Logger.getLogger(IncomingRequestServlet.class
					.getName()).log(Level.SEVERE, null, ex);
		}
	}

}
