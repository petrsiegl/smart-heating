
package cz.cuni.mff.sieglpe.smart.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 
 * DeviceServlet uses this class for method identification.
 * @author Petr Siegl
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HtmlSetter {
    
    /** Name of the property being displayed by this method.
     * @return  */
    public String name();
}