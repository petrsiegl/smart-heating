/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

/**
 * Class helps with pairing of levels and integer values.
 * @author Petr Siegl
 */
public class UserLevel {

	/**
	 *
	 */
	public static final int USER = 1;

	/**
	 *
	 */
	public static final int MODERATOR = 50;

	/**
	 *
	 */
	public static final int ADMIN = 100;

	/**
	 * String name of level to int value.
	 * @param levelName
	 * @return int value of level
	 */
	public static int stringNameToInt(String levelName) {
		switch (levelName.toLowerCase()) {
			case "moderator":
				return MODERATOR;
			case "admin":
				return ADMIN;
			default:
				return USER;
		}
	}

	/**
	 * Int name of level to string value.
	 * @param level
	 * @return string name of level.
	 */
	public static String intToStringName(int level) {
		switch (level) {
			case 50:
				return "moderator";
			case 100:
				return "admin";
			default:
				return "user";
		}
	}
}
