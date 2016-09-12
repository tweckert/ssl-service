package javax.net.ssl.pojo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:thomas.weckert@nexum.de">Thomas Weckert</a>
 */
public class RequestPojo {
	
	public static enum Method {
		GET, POST;
	}
	
	public static enum Protocol {
		HTTP, HTTPS;
	}
	
	public RequestPojo(Method method, Protocol protocol, String host, int port, String path, Map<String, String> parameters) {
		super();
		this.method = method;
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.path = path;
		this.parameters.putAll(parameters);
	}
	
	private Protocol protocol = Protocol.HTTP;
	private String host;
	private int port;
	private String path;
	private Map<String, String> parameters = new HashMap<String, String>();
	private Method method = Method.GET;
	
	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}	
	
	public void addParameter(String key, String value) {
		this.parameters.put(key, value);
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

}
