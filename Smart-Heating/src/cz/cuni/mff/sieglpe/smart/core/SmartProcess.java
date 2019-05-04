/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

import javax.servlet.http.HttpServlet;

/**
 * All modules adding something new extends this class. 
 * Serves as main controller in default settings.
 * @author Petr Siegl 
 */
public abstract class SmartProcess  extends HttpServlet implements Runnable {
	
	/**
	 *
	 * @return
	 */
	public abstract String getName();
	
	/**
	 * Set  context for process.
	 * @param prefix
	 */
	public abstract void setPrefix(String prefix);
}
