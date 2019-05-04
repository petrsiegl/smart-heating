/*
 * This project is a Bachelor's thesis.
 * Charles University in Prague
 */
package cz.cuni.mff.sieglpe.smart.net;

import java.io.Serializable;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Used in XML format for communication between traverser and home system.
 * @author Petr Siegl
 */
@XmlRootElement
public class Message implements Serializable {
    public final long serialVersionUID = 1L;
    public String sessionId;
    public int requestId;
    public HttpKind kind;
    public String content;
    public Map<String,String[]> parameters;
    public String URI;
}