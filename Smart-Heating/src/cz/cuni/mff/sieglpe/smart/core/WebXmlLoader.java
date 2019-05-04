/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.jetty.webapp.WebAppContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Petr Siegl
 */
public class WebXmlLoader {

	private final Map<String, String> servletsMap = new HashMap<>();
	private final Map<String, String> filtersMap = new HashMap<>();

	private final ClassLoader classLoader;
	private final String prefix;
	private boolean hasCatcher = false;

	/**
	 *
	 * @param cls
	 * @param prefix
	 */
	public WebXmlLoader(ClassLoader cls, String prefix) {
		classLoader = cls;
		this.prefix = "/" + prefix;
	}

	private void loadServlets(NodeList servletNodeList) {
		String className;
		String servletName;
		for (int i = 0; i < servletNodeList.getLength(); i++) {
			Node node = servletNodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {

				Element element = (Element) node;

				Node servletNode;
				if ((servletNode = element.getElementsByTagName("servlet-name").item(0)) != null) {
					servletName = servletNode.getTextContent();
					if ((servletNode = element.getElementsByTagName("servlet-class").item(0)) != null) {
						className = servletNode.getTextContent();
						servletsMap.put(servletName, className);
					}
				}

			}
		}
	}

	private void loadFilters(NodeList filtersNodeList) {
		String className;
		String filterName;
		for (int i = 0; i < filtersNodeList.getLength(); i++) {
			Node node = filtersNodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {

				Element element = (Element) node;

				Node filterNode;
				if ((filterNode = element.getElementsByTagName("filter-name").item(0)) != null) {
					filterName = filterNode.getTextContent();
					if ((filterNode = element.getElementsByTagName("filter-class").item(0)) != null) {
						className = filterNode.getTextContent();
						filtersMap.put(filterName, className);
					}
				}

			}
		}
	}

	private void loadServletMappings(WebAppContext webapp, NodeList mappingsNodeList) {
		String className;
		String servletName;
		String url;
		for (int i = 0; i < mappingsNodeList.getLength(); i++) {
			Node node = mappingsNodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {

				Element element = (Element) node;

				Node servletNode;
				if ((servletNode = element.getElementsByTagName("servlet-name").item(0)) != null) {
					servletName = servletNode.getTextContent();
					if ((servletNode = element.getElementsByTagName("url-pattern").item(0)) != null) {
						url = servletNode.getTextContent();
						className = servletsMap.get(servletName);
						if (className != null) {
							Class<?> clazz;
							try {
								clazz = classLoader.loadClass(className);

								if (Servlet.class.isAssignableFrom(clazz)) {
									if (url.equals("/*")) {
										hasCatcher = true;
									}
									if (!url.startsWith("/")) {
										url = "/" + url;
									}

									webapp.addServlet(clazz.asSubclass(Servlet.class), prefix + url);

								}
							} catch (ClassNotFoundException ex) {
								Logger.getLogger(WebXmlLoader.class.getName()).log(Level.INFO, "Class" + className + " not found in JAR");
							}
						}

					}
				}

			}
		}
	}

	private void loadFilterMappings(WebAppContext webapp, NodeList mappingsNodeList) {
		String className;
		String filterName;
		String url;
		for (int i = 0; i < mappingsNodeList.getLength(); i++) {
			Node node = mappingsNodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {

				Element element = (Element) node;

				Node filterNode;
				if ((filterNode = element.getElementsByTagName("filter-name").item(0)) != null) {
					filterName = filterNode.getTextContent();
					if ((filterNode = element.getElementsByTagName("url-pattern").item(0)) != null) {
						url = filterNode.getTextContent();
						className = filtersMap.get(filterName);
						if (className != null) {
							try {
								Class<?> clazz = classLoader.loadClass(className);
								if (Filter.class.isAssignableFrom(clazz)) {

									if (url.equals("/*")) {
										hasCatcher = true;
									}

									if (!url.startsWith("/")) {
										url = "/" + url;
									}
									webapp.addFilter(clazz.asSubclass(Filter.class), prefix + url, EnumSet.of(DispatcherType.REQUEST));

								}
							} catch (ClassNotFoundException ex) {
								Logger.getLogger(WebXmlLoader.class.getName()).log(Level.INFO, "Class" + className + " not found in JAR");
							}
						}
					}

				}
			}
		}
	}

	/**
	 *
	 * @return
	 */
	protected boolean hasCatcher() {
		return hasCatcher;
	}

	/**
	 * Loads settings from xml file using standart web.xml schema for servlets
	 * and filters. Elements loaded:
	 * servlet,filter,servlet-mapping,filter-mapping
	 *
	 * @param webapp context to which add servlet and filter mappings.
	 * @param xmlFile XML file from which to load
	 */
	public void load(WebAppContext webapp, InputStream xmlFile) {

		try {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();

			NodeList servlets = doc.getElementsByTagName("servlet");
			NodeList filters = doc.getElementsByTagName("filter");
			NodeList servletMaps = doc.getElementsByTagName("servlet-mapping");
			NodeList filterMaps = doc.getElementsByTagName("filter-mapping");

			loadServlets(servlets);
			loadFilters(filters);

			loadServletMappings(webapp, servletMaps);
			loadFilterMappings(webapp, filterMaps);

		} catch (SAXException ex) {
			Logger.getLogger(ServerStarter.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(ServerStarter.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ParserConfigurationException ex) {
			Logger.getLogger(ServerStarter.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
