/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Every request should go through this filter. Checks if user is logged in and
 * dispatches the request to the front controller.
 *
 * @author Petr Siegl
 */
public class AuthenticationFilter implements Filter {

	/**
	 * No need.
	 * @param filterConfig
	 * @throws ServletException
	 */
	@Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // empty
    }

	/**
	 *
	 * @param request
	 * @param response
	 * @param filterChain
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        // Should be always true
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String path = httpRequest.getRequestURI();
            HttpSession session = httpRequest.getSession();

            if (path.startsWith("/resources")) {
                // If the request is for any resource let it through.
                filterChain.doFilter(request, response);
				
            } else if (path.startsWith("/logout")) {
                // Log user out
                logout(httpRequest, httpResponse);
				
            } else if (path.startsWith("/login.jsp")) {
                filterChain.doFilter(request, response);
				
            } else if (path.startsWith("/checklogin")) {
                // User trying to log in.
                try {
                    checkLogin(httpRequest, httpResponse, filterChain);
                } catch (SQLException ex) {
                    Logger.getLogger(AuthenticationFilter.class.getName()).log(Level.SEVERE, "Login loading failed.", ex);
                }
				
            } else if (session.getAttribute("user") == null) {
                // Not logged in redirect
                httpResponse.sendRedirect("/login.jsp");
				
            } else // logged in
            if (isPathToProcess(path)) {
				// Get prefix and "/" at start
				request.setAttribute("prefix", path.split("/",3)[1]);
                filterChain.doFilter(request, response);
				
            } else if (path.startsWith("/WEB-INF") ) {
                //Request already is from front controller,
                // no need to go there again.
                // fails if user uses /WEB-INF
                filterChain.doFilter(request, response);
				
            } else {
                // Request comes from some outside source,
                // first dispatch to the controller.
                request.getRequestDispatcher(ServerStarter.CONTROLLER_PREFIX + path).forward(request, response);
            }
        }
    }

	/**
	 * No need.
	 */
	@Override
    public void destroy() {
        // empty
    }

	/**
	 * Check if path stars with context of loaded process.
	 * @param path Path to check.
	 * @return true if it is a context.
	 */
	private static boolean isPathToProcess(String path){
		String[] tokens = path.split("/",3);
		if (tokens.length>1){
			return SmartHandler.isLoadedProcessPrefix(tokens[1]);
		}
		return false;
	}
	
    /**
     * Handles login request.
     *
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    private static void checkLogin(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException, SQLException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username != null && !username.isEmpty()) {

            Connection connection = SmartHandler.getDBConnection();
            PreparedStatement selectPS;
            ResultSet rs;
            String selectQuery;

            selectQuery = "SELECT * FROM USER WHERE USERNAME=?";
            selectPS = connection.prepareStatement(selectQuery);
            selectPS.setString(1, username);
            rs = selectPS.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("PASSWORD");
				int level = rs.getInt("LEVEL");
                User user = new User();
                user.setName(username);
				user.setLevel(level);
                if (verifyPassword(password, hashedPassword)) {
                    // Login successful
                    request.getSession().setAttribute("user", user);
                    response.sendRedirect("/index.jsp");
                } else {
                    //  fail - inccorect password
                    response.sendRedirect("/login.jsp?fail=true");
                }
            } else {
                // fail - user does not exist
                response.sendRedirect("/login.jsp?fail=true");
            }
        } else {
            // fail - no username parameter, or username is empty
            response.sendRedirect("/login.jsp?fail=true");
        }

    }

    /**
     * Log user out.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    public void logout(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        session.setAttribute("user", null);
        session.invalidate();
        response.sendRedirect("/index.jsp");
    }

    /**
     * Create hash fort the password.
     *
     * @param password Password for which to calculate hash.
     * @return
     */
    public static String createHash(String password) {
        String hash = null;
        try {
            hash = PasswordHasher.createHash(password);

        } catch (PasswordHasher.CannotPerformOperationException ex) {
            Logger.getLogger(AuthenticationFilter.class
                    .getName()).log(Level.SEVERE, "Hashing failed.", ex);
        }
        return hash;
    }

    /**
     * Verify if password has the hash value same as correctHash.
     *
     * @param password Password to hash.
     * @param correctHash Value to compare the calculated hash from password to.
     * @return True if the hashes match.
     */
    public static boolean verifyPassword(String password, String correctHash) {
        boolean verified = false;
        try {
            verified = PasswordHasher.verifyPassword(password.toCharArray(), correctHash);

        } catch (PasswordHasher.InvalidHashException| PasswordHasher.CannotPerformOperationException ex) {
            Logger.getLogger(AuthenticationFilter.class
                    .getName()).log(Level.SEVERE, "Password checking failed.", ex);

        }
        return verified;
    }

}
