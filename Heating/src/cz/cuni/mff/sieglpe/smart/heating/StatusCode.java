/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cuni.mff.sieglpe.smart.heating;

/**
 *
 * @author Petr Siegl
 */
public enum StatusCode {
	NONE(0, ""),
	MODE_CHANGE_FAIL(1, "Mode change failed."),
	TEMP_INP_FAIL(2, "Invalid temperature input."),
	UNIT_DEL_SUCC(3, "Unit deleted."),
	
	PREDEF_NUM_FAIL(4, "Invalid temperature input."),
	PREDEF_ADD_SUCC(5, "Predefined record added."),
	PREDEF_ADD_FAIL(6, "Adding of predefined record failed, try again. Check log for more info."),
	PREDEF_DAY_FAIL(7, "Invalid day input."),
	PREDEF_UNIT_FAIL(8, "Unit not found."),
	PREDEF_DEL_SUCC(9, "Record removed."),
	PREDEF_DEL_FAIL(10, "Record removing failed, try again."),
	
	CREATE_UNIT_SUCC(11, "Unit created."),
	CREATE_UNIT_EXISTS(13, "Unit with that ID already exists."),
	CREATE_NOT_FOUND(14, "Relay or Thermometer with that id not found."),
	CREATE_ID_FAIL(15, String.format("Please make sure your non-empty id contains only combination of letters, digits, hyphens or underscores and is form %s to %s characters long",
			SmartHeating.ID_MIN_LENGTH, SmartHeating.ID_MAX_LENGTH)),
	CREATE_UNIT_FAIL(16, "Creation failed, requested devices do not exist, try again."),
	CREATE_DB_FAIL(16, "Unit creation failed. Unit couldn't be saved to database. Database might be corrupted, try different ID or check database directly.");
	
	
	
	
	private final int code;
	private final String description;
	
	private StatusCode(int code, String description){
		this.code=code;
		this.description=description;
	}

	public int getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}
	
	@Override
	public String toString(){
		return code + ":" + description;
	}
	
}
