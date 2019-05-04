package cz.cuni.mff.sieglpe.smart.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.DispatcherType;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Template;
import org.openide.util.lookup.Lookups;

/**
 * Initializes and starts the web server.
 *
 * @author Petr Siegl
 */
public class ServerStarter {

	/**
	 * Controller context prefix to allow mapping other servlets.
	 */
	public static final String CONTROLLER_PREFIX = "/pages";

	/**
	 * Hidden pages prefix.
	 */
	public static final String PAGES_PREFIX = "/WEB-INF";

	/**
	 * HTTP port of server
	 */
	protected static int httpPort = 8888;

	/**
	 * HTTPS port of server
	 */
	protected static int httpsPort = 8443;

	private static String keystore_pass = "123456";
	private static String keymanager_pass = "123456";
	private static String truststore_pass = "123456";

	/**
	 * Variable names of Server.INI
	 */
	private static final String VAR_HTTP = "http_port";
	private static final String VAR_HTTPS = "https_port";
	private static final String VAR_KEYSTORE = "keystore_pass";
	private static final String VAR_KEYMANAGER = "keyman_pass";
	private static final String VAR_TRUSTSTORE = "truststore_pass";

	/**
	 * JSP initialization.
	 *
	 * @return
	 */
	private static List<ContainerInitializer> jspInitializers() {
		JettyJasperInitializer sci = new JettyJasperInitializer();
		ContainerInitializer initializer = new ContainerInitializer(sci, null);
		List<ContainerInitializer> initializers = new ArrayList<>();
		initializers.add(initializer);
		return initializers;
	}

	/**
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Server server = new Server();
		loadServerConfig();

		// SSL settings
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(httpPort);

		// SSL Context Factory
		SslContextFactory sslContextFactory = getSslContextFactory();

		// SSL HTTP Configuration
		HttpConfiguration https_config = new HttpConfiguration(getHttpsConfiguration());
		https_config.addCustomizer(new SecureRequestCustomizer());

		ServerConnector sslConnector = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
				new HttpConnectionFactory(https_config));
		sslConnector.setPort(httpsPort);
		server.setConnectors(new Connector[]{connector, sslConnector});

		//---
		System.setProperty("org.apache.jasper.compiler.disablejsr199", "false");

		WebAppContext webapp = new WebAppContext();
		/*
		 * All these configurations allow us to use things like Annotations 
		 * JSP 3.1 (@Servlet not CDI, you need weld for that) and JNDI.
		 */
		webapp.setConfigurations(new Configuration[]{
			//new AnnotationConfiguration(),
			new WebInfConfiguration(),
			new WebXmlConfiguration(),
			new MetaInfConfiguration(),
			//new FragmentConfiguration(),
			new EnvConfiguration(),
			new PlusConfiguration(),
			new JettyWebXmlConfiguration()
		});

		// Resources
		webapp.setResourceBase("web");
		webapp.setContextPath("/");

		webapp.setAttribute(
				"org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
				".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$");

		// Configure the application to support the compilation of JSP files.
		webapp.setAttribute("org.eclipse.jetty.containerInitializers", jspInitializers());
		webapp.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
		webapp.addBean(new ServletContainerInitializersStarter(webapp), true);
		webapp.setClassLoader(new URLClassLoader(new URL[0],
				ServerStarter.class.getClassLoader()));

		webapp.setParentLoaderPriority(true);

		// Filter mapping
		webapp.addFilter(AuthenticationFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		webapp.addFilter(MenuFilter.class, "/WEB-INF/menu.jsp", EnumSet.allOf(DispatcherType.class));

		// Servlet mapping:
		ServletHolder sh;
		sh = webapp.addServlet(ControllerServlet.class, CONTROLLER_PREFIX + "/*");
		sh.setInitOrder(1);
		sh = webapp.addServlet(SmartHandler.class, "/servlet/smart");
		sh.setInitOrder(1);

		loadModules(webapp);

		server.setHandler(webapp);

		SmartHandler.initContainers();

		TraverserConnector traverserConnector = new TraverserConnector();
		SmartHandler.traverserConnector = traverserConnector;
		Thread t = new Thread(traverserConnector);
		t.start();

		
		server.start();
		//server.dumpStdErr();
		server.join();

	}

	/**
	 * Find all jar files in specified directory.
	 *
	 * @param dir directory to check for
	 * @return Null if directory is empty or doesn't exist
	 */
	private static File[] loadFromDir(Path dir) {

		File[] files = null;
		if (dir != null && Files.exists(dir) && Files.isDirectory(dir)) {
			files = dir.toFile().listFiles((file) -> {
				String name = file.getName().toLowerCase();
				return name.endsWith(".jar");
			});
		}

		return files;
	}

	private static void loadModules(WebAppContext webapp) {

		List<URL> urls = new ArrayList<>();

		File[] modulesFiles = loadFromDir(Paths.get("modules"));
		File[] devicesFiles = loadFromDir(Paths.get("devices"));

		// Get urls
		if (modulesFiles != null) {
			for (File modulesFile : modulesFiles) {
				try {
					urls.add(modulesFile.toURI().toURL());
				} catch (MalformedURLException ex) {
					Logger.getLogger(ServerStarter.class.getName()).log(Level.SEVERE, "File not loaded" + modulesFile.getName());
				}
			}
		}

		if (devicesFiles != null) {
			for (File devicesFile : devicesFiles) {
				try {
					urls.add(devicesFile.toURI().toURL());
				} catch (MalformedURLException ex) {
					Logger.getLogger(ServerStarter.class.getName()).log(Level.SEVERE, "File not loaded" + devicesFile.getName());
				}
			}
		}

		// Class loader with all neccesary jars
		URLClassLoader cls = new URLClassLoader(urls.toArray(new URL[0]), ServerStarter.class.getClassLoader());
		webapp.setClassLoader(cls);

		if (modulesFiles != null) {
			for (File file : modulesFiles) {
				String prefix = file.getName().split("\\.", 2)[0].toLowerCase();
				ServiceLoader<SmartProcess> loader = ServiceLoader.load(SmartProcess.class, cls);
				Iterator<SmartProcess> iterator = loader.iterator();
				if (iterator.hasNext()) {
					SmartProcess process = iterator.next();
					process.setPrefix(prefix);
					SmartHandler.addProcess(prefix, process);

					//Class loader for current jar
					URLClassLoader webLoader;
					try {
						webLoader = new URLClassLoader(new URL[]{file.toURI().toURL()});

						// Load web.xml servlets and filters
						InputStream webXml = webLoader.getResourceAsStream("WEB-INF/web.xml");
						if (webXml != null) {
							WebXmlLoader xmlLoader = new WebXmlLoader(cls, prefix);
							xmlLoader.load(webapp, webXml);

							// Only add SmartProcess as main controller if there is no other solution
							if (!xmlLoader.hasCatcher()) {

								ServletHolder sh = webapp.addServlet(process.getClass(), "/" + prefix + "/*");
								sh.setInitOrder(1);
							}
						}
					} catch (MalformedURLException ex) {
						Logger.getLogger(ServerStarter.class.getName()).log(Level.SEVERE, "Modul loading failed.", ex);
					}
				}
			}
		}

		/**
		 * Run static inits before server start for all devices.
		 */
		if (modulesFiles != null || devicesFiles != null) {
			Lookup.Template<Device> template = new Template<>(Device.class);
			Lookup lu = Lookups.metaInfServices(cls);
			Lookup.Result<Device> result = lu.lookup(template);
			Set<Class<? extends Device>> c = result.allClasses();
			for (Class<?> device : c) {
				try {
					Class.forName(device.getCanonicalName(), true, cls);
				} catch (ClassNotFoundException ex) {
					Logger.getLogger(ServerStarter.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	private static void loadServerSettingsFile(File file) throws IOException {
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
						case VAR_HTTP:
							try {
								httpPort = Integer.parseInt(value);
							} catch (NumberFormatException ex) {
								httpPort = 8080;
								Logger.getLogger(SmartHandler.class
										.getName()).log(Level.INFO, "Invalid http_port input, using default " + httpPort);
							}
							break;
						case VAR_HTTPS:
							try {
								httpsPort = Integer.parseInt(value);
							} catch (NumberFormatException ex) {
								httpsPort = 8443;
								Logger.getLogger(SmartHandler.class
										.getName()).log(Level.INFO, "Invalid http_port input, using default " + httpsPort);
							}
							break;
						case VAR_KEYSTORE:
							keystore_pass = value;
							break;
						case VAR_KEYMANAGER:
							keymanager_pass = value;
							break;
						case VAR_TRUSTSTORE:
							truststore_pass = value;
							break;
					}
				}
			}
		} catch (IOException ex) {
			throw ex;
		}
	}

	/**
	 * Load database setting from file "server.INI".
	 */
	protected static void loadServerConfig() {

		File file = new File("server.INI");
		try {
			if (!file.createNewFile()) {
				// File already exists -> load it.
				loadServerSettingsFile(file);
			} else {
				// File doesnt exist and was just created.
				try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
					// Format: name=value
					pw.println(VAR_HTTP + " = " + httpPort);
					pw.println(VAR_HTTPS + " = " + httpsPort);
					pw.println(VAR_KEYSTORE + " = " + keystore_pass);
					pw.println(VAR_KEYMANAGER + " = " + keymanager_pass);
					pw.println(VAR_TRUSTSTORE + " = " + truststore_pass);

				} catch (IOException ex) {
					Logger.getLogger(SmartHandler.class
							.getName()).log(Level.SEVERE, "Saving to server.INI failed.");

				}
			}
		} catch (IOException ex) {
			Logger.getLogger(SmartHandler.class
					.getName()).log(Level.SEVERE, "File server.INI creation failed");
		}
	}

	private static SslContextFactory getSslContextFactory() {
		String path = "keystore.jks";
		File file = new File(path);

		if (!file.exists()) {
			path = ServerStarter.class.getResource("keystore.jks").toExternalForm();
		}

		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(path);
		sslContextFactory.setKeyStorePassword(keystore_pass);
		sslContextFactory.setKeyManagerPassword(keymanager_pass);
		sslContextFactory.setTrustStorePath(path);
		sslContextFactory.setTrustStorePassword(truststore_pass);
		sslContextFactory.setExcludeCipherSuites(
				"SSL_RSA_WITH_DES_CBC_SHA",
				"SSL_DHE_RSA_WITH_DES_CBC_SHA",
				"SSL_DHE_DSS_WITH_DES_CBC_SHA",
				"SSL_RSA_EXPORT_WITH_RC4_40_MD5",
				"SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
				"SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
				"SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");

		return sslContextFactory;
	}

	private static HttpConfiguration getHttpsConfiguration() {

		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(httpsPort);
		httpConfig.setOutputBufferSize(32768);
		httpConfig.setRequestHeaderSize(8192);
		httpConfig.setResponseHeaderSize(8192);
		httpConfig.setSendServerVersion(true);
		httpConfig.setSendDateHeader(false);

		return httpConfig;
	}

}
