/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

import static cz.cuni.mff.sieglpe.smart.net.HttpKind.GET;
import static cz.cuni.mff.sieglpe.smart.net.HttpKind.POST;
import cz.cuni.mff.sieglpe.smart.net.Message;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import static java.lang.Thread.sleep;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Utf8StringBuilder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Handles requests coming to the NAT traverser server. Actively polls the NAT
 * traverser for incoming requests via https interface. Synchronizes sessions
 * for the connections to the NAT server and creates requests with created http
 * clients on the local server.
 *
 * @author Petr Siegl
 */
public class TraverserConnector implements Runnable {

	// Session Identifier Of Connection on NAT traverser, associeted client
	/**
	 *
	 */
	protected Map<String, HttpClient> clients = new ConcurrentHashMap<>();

	private static volatile boolean failing = false;

	private static final String MESSAGES_URI = "/messages";
	private static final String PASSWORD_URI = "/password";
	private static final String INIT_FILE_NAME = "traverser.INI";
	private static final String PROTOCOL = "http://";

	private static final String VAR_ADDRESS = "address";
	private static final String VAR_PORT = "port";
	private static final String VAR_STATUS = "status";
	private static final String VAR_CONTEXT = "context";
	private static final String VAR_PASS = "password";

	// Default settings
	private static final String DEFAULT_ADDRESS = "localhost";
	private static final int DEFAULT_PORT = 8080;
	private static final boolean DEFAULT_STATUS = false;
	private static final String DEFAULT_PASS = "traverser";
	private static final String DEFAULT_CONTEXT = "";

	// Server settings
	private String address = DEFAULT_ADDRESS;
	private int port = DEFAULT_PORT;
	private String password = DEFAULT_PASS;
	private String context = DEFAULT_CONTEXT;


	/* Possible future implementation
	protected String getContext() {
		return context;
	}

	public void setContext(String context) throws IOException {
		this.context = context;
		saveSettings();
	}
	 */
	private static SslContextFactory sslContextFactory;
	// Client handling equests from and to traverser
	private static HttpClient mainClient;
	// xml handling objects
	private static JAXBContext jaxbContext;
	// object -> xml for class Message
	private static Marshaller marshaller;
	// xml -> object for class Message
	private static Unmarshaller unmarshaller;

	private volatile boolean running = DEFAULT_STATUS;
	private StringWriter writer = new StringWriter();
	private Message incomingMsg;

	private final Object lock = new Object();

	/**
	 * Prepare class for use.
	 *
	 * @throws JAXBException
	 * @throws Exception
	 */
	private void init() throws JAXBException {
		failing = false;
		// prepare xml handling objects
		jaxbContext = JAXBContext.newInstance(Message.class);
		marshaller = jaxbContext.createMarshaller();
		unmarshaller = jaxbContext.createUnmarshaller();
		// prepare client for sending https requests.
		sslContextFactory = new SslContextFactory(true);
		mainClient = new HttpClient(sslContextFactory);

		initServerSettings();
	}

	protected boolean isFailing() {
		return failing;
	}

	/**
	 * Change password used for authentication by traverser and home system.
	 *
	 * @param password new password for traverser setup
	 * @throws IOException Failed to save settings to disk.
	 * @throws InterruptedException Failed to send.
	 * @throws TimeoutException Failed to send.
	 * @throws ExecutionException Failed to send.
	 */
	protected void setPassword(String password) throws IOException, InterruptedException, TimeoutException, ExecutionException {
		String oldpassword = this.password;

		ContentResponse response = mainClient.newRequest(PROTOCOL + address + ":" + port + context + PASSWORD_URI)
				.method(HttpMethod.PUT)
				.param("password", oldpassword)
				.param("newPassword", password)
				.send();

		if (response.getStatus() == HttpServletResponse.SC_OK) {
			this.password = password;
			saveSettings();
		}
	}

	/**
	 *
	 * @param port
	 * @throws IOException
	 */
	public void setPort(int port) throws IOException {
		this.port = port;
		saveSettings();
	}

	/**
	 *
	 * @return
	 */
	public int getPort() {
		return port;
	}

	/**
	 *
	 * @param address
	 * @throws IOException
	 */
	public void setAddress(String address) throws IOException {
		this.address = address;
		saveSettings();
	}

	/**
	 *
	 * @return
	 */
	public String getAddress() {
		return address;
	}

	/**
	 *
	 * @param address
	 * @param port
	 * @throws IOException
	 */
	public void setServer(String address, int port) throws IOException {
		setPort(port);
		setAddress(address);
		saveSettings();
	}

	/**
	 * Initialize address and port setting form init file.
	 */
	private void initServerSettings() {

		try (BufferedReader reader = new BufferedReader(new FileReader(INIT_FILE_NAME))) {

			String line;
			String[] parts;
			while ((line = reader.readLine()) != null) {
				parts = line.split(":", 2);
				if (parts.length > 1) {
					switch (parts[0].toLowerCase()) {
						case VAR_STATUS:
							if (parts[1].equalsIgnoreCase("ON")) {
								start();
							} else {
								running = false;
							}
							break;
						case VAR_ADDRESS:
							address = parts[1];
							break;
						case VAR_PORT:
							port = Integer.parseInt(parts[1]);
							break;
						/*
						case VAR_CONTEXT:
							context = parts[1];
							break;
						 */
						case VAR_PASS:
							password = parts[1].trim();
							break;
						default:
							// ignore
							break;
					}
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(TraverserConnector.class.getName()).log(Level.INFO,
					"Loading setting of traverser connector from init file failed. Using defaults");
			address = DEFAULT_ADDRESS;
			port = DEFAULT_PORT;
			running = DEFAULT_STATUS;
			password = DEFAULT_PASS;
			try {
				saveSettings();
			} catch (IOException ex1) {
				//Logger.getLogger(TraverserConnector.class.getName()).log(Level.SEVERE, null, ex1);
			}
		}
	}

	/**
	 * Save current settings for traverser connector to init file.
	 */
	private synchronized void saveSettings() throws IOException {
		try (PrintWriter fileWriter = new PrintWriter(new FileWriter(INIT_FILE_NAME, false))) {
			if (isRunning()) {
				fileWriter.println(VAR_STATUS + ":ON");
			} else {
				fileWriter.println(VAR_STATUS + ":OFF");
			}
			fileWriter.println(VAR_ADDRESS + ":" + address);
			fileWriter.println(VAR_PORT + ":" + port);
			//fileWriter.println(VAR_CONTEXT + ":" + context);
			fileWriter.println(VAR_PASS + ":" + password);
		} catch (IOException ex) {
			Logger.getLogger(TraverserConnector.class.getName()).log(Level.SEVERE, "Error while saving current traverser connector settings.", ex);
			throw ex;
		}
	}

	Request handlePost(HttpClient httpClient, Message message) throws InterruptedException, TimeoutException, ExecutionException {
		// Prepare parameters and send POST request.
		Request request = httpClient.POST("http://localhost:" + ServerStarter.httpPort + message.URI);
		String value;
		for (String key : message.parameters.keySet()) {
			value = message.parameters.get(key)[0];
			request.param(key, value);
		}
		return request;
	}

	/**
	 * Stop all activities.
	 */
	private synchronized void stopClients() {
		if (running) {
			clients.forEach((id, client) -> {
				try {
					client.stop();
				} catch (Exception ex) {
					Logger.getLogger(TraverserConnector.class.getName()).log(Level.SEVERE, "Client {0} couldn't have been stopped.", id);
				}
			});

			try {
				running = false;
				mainClient.stop();
			} catch (Exception ex) {
				mainClient = new HttpClient(sslContextFactory);
				Logger.getLogger(TraverserConnector.class.getName()).log(Level.SEVERE, "Main client couldn't have been stopped.");
			}
		}
	}

	/**
	 *
	 * @return
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Stop NAT traversing function.
	 */
	protected void stop() {
		failing = false;
		running = false;
	}

	/**
	 * Start NAT traversing function.
	 */
	protected synchronized void start() {
		if (!running) {
			clients.clear();
			try {
				mainClient.start();
				running = true;
				synchronized (lock) {
					lock.notify();
				}
			} catch (Exception ex) {
				mainClient = new HttpClient(sslContextFactory);
				Logger.getLogger(TraverserConnector.class.getName()).log(Level.SEVERE, "Main client {0} couldn't have been started.");
			}
		}
	}

	@Override
	public void run() {
		try {
			init();

			ContentResponse response;
			while (true) {

				// Stop this thread
				if (!running) {
					stopClients();
					synchronized (lock) {
						do {
							lock.wait();
						} while (!running);
					}
				}

				try {
					// Check for new requests on the traverser
					response = mainClient.newRequest(PROTOCOL + address + ":" + port + context + MESSAGES_URI)
							.method(HttpMethod.GET)
							.param("password", password)
							.send();

					// Empty queue means no new requests.
					if (response.getStatus() == HttpServletResponse.SC_OK && !response.getContentAsString().isEmpty()) {
						try {
							synchronized (unmarshaller) {
								incomingMsg = (Message) unmarshaller.unmarshal(new StringReader(response.getContentAsString()));
							}

							// Find client associated with session or
							// create new one.
							HttpClient httpClient;
							if ((httpClient = clients.get(incomingMsg.sessionId)) == null) {
								httpClient = new HttpClient();
								httpClient.start();
								clients.put(incomingMsg.sessionId, httpClient);
							}

							// What kind of request to process
							switch (incomingMsg.kind) {
								case POST:
									Request request = handlePost(httpClient, incomingMsg);
									request.send(new AsyncRequestHandler());
									break;

								case GET:
								default:
									httpClient.newRequest("http://localhost:" + ServerStarter.httpPort + incomingMsg.URI)
											.send(new AsyncRequestHandler());
									break;
							}

						} catch (JAXBException ex) {
							Logger.getLogger(TraverserConnector.class.getName()).log(Level.INFO, "Failed to receive message.");
						}
					}

				} catch (InterruptedException | ExecutionException | TimeoutException ex) {
					// Couldn't reach the traverser or local
					Logger.getLogger(TraverserConnector.class.getName()).log(Level.WARNING,
							"Can't connect to the traverser on " + PROTOCOL + address + ":" + port + context);
					/* Probably some connectivity problem, or possibly wrong
					   adress. Either way we should wait some time, as not to
					   unnecessarly spam the server. 
					 */

					failing = true;
					// Sleep before trying again, as not to spam the server.
					sleep(30000);

				}

			}
		} catch (JAXBException ex) {
			Logger.getLogger(TraverserConnector.class.getName()).log(Level.SEVERE,
					"Failed to unmarshall message.");
		} catch (Exception ex) {
			Logger.getLogger(TraverserConnector.class.getName()).log(Level.INFO,
					"Http client start for new request failed.", ex);
		} finally {

			// End user's clients
			clients.forEach((id, client) -> {
				try {
					client.stop();
				} catch (Exception ex) {
					Logger.getLogger(TraverserConnector.class.getName()).log(Level.SEVERE,
							"Traverser client {0} did not end properly.", id);
				}
			});

			// End main client
			try {
				mainClient.stop();
			} catch (Exception ex) {
				Logger.getLogger(TraverserConnector.class.getName()).log(Level.SEVERE,
						"Traverser client did not end properly.");
			}

		}
	}

	/**
	 * Asynchronous request handler. When all content is received, sends it to
	 * the NAT traverser.
	 */
	private class AsyncRequestHandler extends Response.Listener.Adapter {

		private final Utf8StringBuilder contentBuilder = new Utf8StringBuilder();

		@Override
		public void onContent(Response response, ByteBuffer content) {
			byte[] bytes = BufferUtil.toArray(content);
			contentBuilder.append(bytes, 0, bytes.length);
		}

		@Override
		public void onComplete(Result result) {
			if (!result.isFailed()) {
				try {
					Message msg = new Message();
					msg.sessionId = incomingMsg.sessionId;
					msg.requestId = incomingMsg.requestId;
					msg.content = contentBuilder.toReplacedString();

					// Clear writer and store message as xml
					writer.getBuffer().setLength(0);
					synchronized (marshaller) {
						marshaller.marshal(msg, writer);
					}

					// Send response back to NAT traverser
					ContentResponse cr;

					cr = mainClient.newRequest(PROTOCOL + address + ":" + port + context + MESSAGES_URI)
							.method(HttpMethod.PUT)
							.content(new StringContentProvider(writer.toString()))
							.param("password", password)
							.send();

				} catch (JAXBException | InterruptedException | TimeoutException | ExecutionException ex) {
					Logger.getLogger(TraverserConnector.class.getName()).log(Level.SEVERE,
							"Request answer failed to send. Server may be unreachable.", ex);
				}
			}
		}
	}

}
