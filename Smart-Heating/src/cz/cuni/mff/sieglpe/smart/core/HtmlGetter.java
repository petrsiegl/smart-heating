
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
public @interface HtmlGetter {
    
    /** Name of the property that will be displayed on web page.
     * @return  */
    public String name();

    /** Decides what kind of representation will be used.
     * @return  */
    public MethodKind kind();
	
	/**
	 * Possible vales for Radio type.
	 * @return
	 */
	public String[] radioValues() default {};
}
