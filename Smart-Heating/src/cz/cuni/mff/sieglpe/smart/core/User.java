/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.core;

/**
 * Information about user in the session.
 * @author Petr Siegl
 */
public class User {
    private String name;
    private int level;

	/**
	 * For initialization with setters.
	 */
	public User(){
        
    }
    
	/**
	 *
	 * @param name
	 * @param level
	 */
	public User(String name, int level){
        this.name = name;
        this.level = level;
    }
    
	/**
	 *
	 * @return
	 */
	public int getLevel() {
        return level;
    }

	/**
	 *
	 * @param id
	 */
	public void setLevel(int id) {
        this.level = id;
    }

	/**
	 *
	 * @return
	 */
	public String getName() {
        return name;
    }

	/**
	 *
	 * @param name
	 */
	public void setName(String name) {
        this.name = name;
    }
    
}
