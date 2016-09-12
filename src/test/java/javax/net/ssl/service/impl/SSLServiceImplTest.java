package javax.net.ssl.service.impl;

import java.io.IOException;
import java.util.Collections;

import javax.net.ssl.pojo.RequestPojo;
import javax.net.ssl.pojo.ResponsePojo;
import javax.net.ssl.service.SSLService;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Thomas Weckert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/SSLServiceImplTestContext.xml" })
public class SSLServiceImplTest {

	@Autowired private SSLService sslService;
	
	@Test
	public void testExecutePostRequest() throws Exception {
		
		// GIVEN
		RequestPojo request = new RequestPojo(RequestPojo.Method.POST, RequestPojo.Protocol.HTTPS, "httpbin.org", 80, "/post", Collections.singletonMap("someParam", "someValue"));
		
		// WHEN
		ResponsePojo response = sslService.executeRequest(request);
		
		// THEN
		Assert.assertTrue(200 == response.getHttpStatus());
		
		JSONObject jsonResponse = new JSONObject(response.getData());
		JSONObject form = jsonResponse.getJSONObject("form");
		
		Assert.assertTrue("someValue".equals(form.getString("someParam")));
	}
	
	@Test
	public void testExecutePostRequestFails() throws Exception {
		
		// GIVEN
		RequestPojo request = new RequestPojo(RequestPojo.Method.POST, RequestPojo.Protocol.HTTPS, "www.bundesregierung.de", 80, "/post", Collections.singletonMap("someParam", "someValue"));
		
		// WHEN
		boolean hasSSLException = false;
		try {
			sslService.executeRequest(request);
		} catch (IOException e) {
			hasSSLException = javax.net.ssl.SSLHandshakeException.class.isAssignableFrom(e.getCause().getClass());
		}
		
		// THEN
		Assert.assertTrue(hasSSLException);
	}
	
	@Test
	public void testExecuteGetRequest() throws Exception {
		
		// GIVEN
		RequestPojo request = new RequestPojo(RequestPojo.Method.GET, RequestPojo.Protocol.HTTPS, "httpbin.org", 80, "/get", Collections.singletonMap("someParam", "someValue"));
		
		// WHEN
		ResponsePojo response = sslService.executeRequest(request);
		
		// THEN
		Assert.assertTrue(200 == response.getHttpStatus());
		
		JSONObject jsonResponse = new JSONObject(response.getData());
		JSONObject form = jsonResponse.getJSONObject("args");
		
		Assert.assertTrue("someValue".equals(form.getString("someParam")));
	}
	
}
