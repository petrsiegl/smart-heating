/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.heating;

import cz.cuni.mff.sieglpe.smart.device.ThermometerLoader;
import cz.cuni.mff.sieglpe.smart.device.Thermometer;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
// Directory where the devices are normally stored.
import static cz.cuni.mff.sieglpe.smart.heating.DS18B20.DIR;

/**
 * Loader for D18B20 thermometers. Returns list of all D18B20 thermometers
 * currently connected.
 *
 * @author Petr Siegl 
 */
public class DS18B20Loader implements ThermometerLoader {

    /**
     * Load all currently connected D18B20 thermometers.
     * @return List of all D18B20 thermometers.
     */
    @Override
    public List<? extends DS18B20> load() {
        List<DS18B20> list = new ArrayList<>();
        Path baseDir = Paths.get(DIR);

        // In DIR there are directories for every found D18B20 device.
        // All the right directories start with 28- prefix.
        if (baseDir!=null && Files.exists(baseDir) && Files.isDirectory(baseDir)) {
            File[] files = baseDir.toFile().listFiles((file) -> {
                return file.getName().startsWith("28-");
            });
            for (File file : files) {
                list.add(new DS18B20(file.getName()));
            }
        }
        return list;
    }

}
