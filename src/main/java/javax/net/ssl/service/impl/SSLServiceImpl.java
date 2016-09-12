package javax.net.ssl.service.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.pojo.RequestPojo;
import javax.net.ssl.pojo.ResponsePojo;
import javax.net.ssl.service.SSLService;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

/**
 * @author Thomas Weckert
 */
@Scope(value = BeanDefinition.SCOPE_SINGLETON, proxyMode = ScopedProxyMode.INTERFACES)
@Service("SSLService")
public class SSLServiceImpl implements SSLService, InitializingBean {
	
	private static final Logger LOG = LoggerFactory.getLogger(SSLServiceImpl.class);
	
	@Value("${SSLService.pemCertResourcePath}") private String pemCertResourcePath;
	@Value("${SSLService.charEncoding}") private String charEncoding;
	@Value("${SSLService.connectTimeoutMillis}") private int connectTimeoutMillis;
	@Value("${SSLService.readTimeoutMillis}") private int readTimeoutMillis;
	private SSLContext sslContext;
	private HostnameVerifier hostnameVerifier;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		this.sslContext = initSSLContext(this.pemCertResourcePath);
		this.hostnameVerifier = initHostnameVerifier();		
	}
	
	@Override
	public ResponsePojo executeRequest(RequestPojo request) {
		
		ResponsePojo response = null;
		HttpURLConnection httpConnection = null;
		Writer writer = null;
		BufferedReader reader = null;
		try {
			
			// build the request URL
			String urlStr = buildUrl(request);
	        
	        // URL encode the request parameters
			List<UrlParameter> urlParameters = new ArrayList<UrlParameter>();
			for (Map.Entry<String, String> entry : request.getParameters().entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
					urlParameters.add(new UrlParameter(key, value, charEncoding));
				}
			}

			// create a string with all URL parameters
			StringBuffer urlParametersBuf = new StringBuffer();			
			Iterator<UrlParameter> i = urlParameters.listIterator();	
			while (i.hasNext()) {
				
				String nextUrlParameter = i.next().toString();
				if (StringUtils.isNotBlank(nextUrlParameter)) {
				
					urlParametersBuf.append(nextUrlParameter);
					
					if (i.hasNext()) {
						urlParametersBuf.append("&");
					}
				}
			}
			
			// start time
			long startTimeMillis = System.currentTimeMillis();
			
	        // create a HTTP request
			URL url = new URL(urlStr);
			if (RequestPojo.Method.POST.equals(request.getMethod())) {
				url = new URL(urlStr);
			} else {
				url = new URL(urlStr + "?" + urlParametersBuf.toString());
			}
			
			if (RequestPojo.Protocol.HTTPS.equals(request.getProtocol())) {
				
				HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();
				httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
				httpsConnection.setHostnameVerifier(hostnameVerifier);				
				
				httpConnection = (HttpURLConnection) httpsConnection;
			} else {
				
				httpConnection = (HttpURLConnection) url.openConnection();        		
			}

			if (RequestPojo.Method.POST.equals(request.getMethod())) {
				httpConnection.setRequestMethod("POST");
				// true indicates a POST request
				httpConnection.setDoOutput(true);
			} else {
				httpConnection.setRequestMethod("GET");
				// false indicates a GET request
				httpConnection.setDoOutput(false);
			}
			
			// the connection should not be considered as persistent (= HTTP 1.1 'keep-alive'), because it may be dead once it is reused
			httpConnection.setRequestProperty("Connection", "close");
			// caches should be ignored
			httpConnection.setUseCaches(false);
			httpConnection.setConnectTimeout(connectTimeoutMillis);
			httpConnection.setReadTimeout(readTimeoutMillis);
			// true indicates the server returns a response
			httpConnection.setDoInput(true);
			
			if (RequestPojo.Method.POST.equals(request.getMethod())) {
				// execute the POST request
				writer = new OutputStreamWriter(httpConnection.getOutputStream());
	            writer.write(urlParametersBuf.toString());
	            writer.flush();
			}
			
            // HTTP status code
			int httpStatus = httpConnection.getResponseCode();
            
			// fetch the response content
			reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
			
			StringBuffer result = new StringBuffer();
			String line = null;	 
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
					
			// elapsed time to execute the request
			long elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis;
			
			response = new ResponsePojo(httpStatus, result.toString(), elapsedTimeMillis);
		} catch (Exception e) {
			if (LOG.isErrorEnabled()) {
				LOG.error(StringUtils.join("Error executing HTTP request: ", e.getMessage()), e);
			}		
			response = null;
		} finally {
			
			IOUtils.closeQuietly(reader);
			IOUtils.closeQuietly(writer);
			
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
		}
		
		return response;
	}
	
	protected String buildUrl(RequestPojo request) {
		
		StringBuffer url = new StringBuffer();
		
		if (RequestPojo.Protocol.HTTPS.equals(request.getProtocol())) {
			url.append("https");
		} else {
			url.append("http");
		}
		
		url.append("://").append(request.getHost());
		
		if (request.getPort() != 80) {
			url.append(":").append(Integer.toString(request.getPort()));
		}
		
		if (StringUtils.isNotBlank(request.getPath())) {
			url.append(request.getPath());
		}
		
		return url.toString();
	}
	
	protected HostnameVerifier initHostnameVerifier() {
		
		return new HostnameVerifier() {
			
		    @Override
		    public boolean verify(String hostname, SSLSession session) {
		    
		    	// ensure that the certificate's host name matches the host name in the URL
		    	HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
		        return hostnameVerifier.verify("hostname", session);
		    }
		    
		};
	}
	
	protected SSLContext initSSLContext(String pemResourcePath) {
		
		InputStream pemInputStream = null;
		X509Certificate cert = null;
		SSLContext sslContext = null;			
		try {
			
			// load the SSL certificate exported as a PEM file from Firefox
			pemInputStream = this.getClass().getResourceAsStream(pemResourcePath);
			
			// convert the PEM file into an X509 certificate
			CertificateFactory certFactory = CertificateFactory.getInstance("X509");
	        cert = (X509Certificate) certFactory.generateCertificate(pemInputStream);
	        
	        // create an empty in-memory SSL keystore
	        KeyStore sslKeyStore = KeyStore.getInstance("JKS");
	        sslKeyStore.load(null);
	        
	        // add the SSL certificate to the keystore
	        String alias = StringUtils.join(cert.getSubjectX500Principal().getName(), "[", Integer.toString(cert.getSubjectX500Principal().getName().hashCode()), "]");
	        sslKeyStore.setCertificateEntry(alias, cert);
	        
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(sslKeyStore);

			// get an SSL context for TLS only
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
		} catch (Exception e) {
			if (LOG.isErrorEnabled()) {
				LOG.error(StringUtils.join("Error initializing SSL context with SSL certificate loaded from PEM file '", String.valueOf(pemResourcePath), "'"), e);
			}
		} finally {
			IOUtils.closeQuietly(pemInputStream);
		}
		
		return sslContext;
	}
	
	public static class UrlParameter {
		
		private String urlEncoded;
		
		public UrlParameter(String name, String value, String encoding) {
			
			super();
			
			if (StringUtils.isBlank(encoding)) {
				// use the system encoding
				encoding = System.getProperty("file.encoding");
			}
			
			if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(value)) {
				
				try {				
					this.urlEncoded = StringUtils.join(URLEncoder.encode(name, encoding), "=", URLEncoder.encode(value, encoding));
				} catch (UnsupportedEncodingException e) {
					// use the default encoding- better than nothing
					this.urlEncoded = StringUtils.join(URLEncoder.encode(name), "=", URLEncoder.encode(value));
				}
			} else {
				this.urlEncoded = StringUtils.EMPTY;
			}
		}
		
		@Override
		public String toString() {
			return String.valueOf(urlEncoded);
		}
		
	}

}
