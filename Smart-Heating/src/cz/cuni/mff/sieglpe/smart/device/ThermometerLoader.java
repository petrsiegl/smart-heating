/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.device;

import cz.cuni.mff.sieglpe.smart.core.DeviceLoader;
import java.util.List;

/**
 * Loader of thermometers.
 * @author Petr Siegl
 */
public interface ThermometerLoader extends DeviceLoader{

	/**
	 * Load new thermometers to system.
	 * @return
	 */
	@Override
    public List<? extends Thermometer> load();
}
