package javax.net.ssl.pojo;

/**
 * @author Thomas Weckert
 */
public class ResponsePojo {
	
	private long elapsedTimeMillis;
	private int httpStatus;
	private String data;
	
	public ResponsePojo(int httpStatus, String data, long elapsedTimeMillis) {
		super();
		this.httpStatus = httpStatus;
		this.data = data;
		this.elapsedTimeMillis = elapsedTimeMillis;
	}
	
	public long getElapsedTimeMillis() {
		return elapsedTimeMillis;
	}
	
	public int getHttpStatus() {
		return httpStatus;
	}
	
	public String getData() {
		return data;
	}

}
