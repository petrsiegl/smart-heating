/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Gather all loaded processes for menu view.
 * @author Petr Siegl
 */
public class MenuFilter implements Filter {

	/**
	 * No need.
	 * @param filterConfig
	 * @throws ServletException
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		//empty
	}

	/**
	 * Get processes and filter.
	 * @param request
	 * @param response
	 * @param chain
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		request.setAttribute("processes", SmartHandler.processes);
		chain.doFilter(request, response);
	}

	/**
	 * No need.
	 */
	@Override
	public void destroy() {
		//empty
	}

}
