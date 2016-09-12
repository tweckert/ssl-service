package javax.net.ssl.service;

import java.io.IOException;

import javax.net.ssl.pojo.RequestPojo;
import javax.net.ssl.pojo.ResponsePojo;

/**
 * @author Thomas Weckert
 */
public interface SSLService {

	ResponsePojo executeRequest(RequestPojo request) throws IOException;
	
}
