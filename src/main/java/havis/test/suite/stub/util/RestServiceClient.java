package havis.test.suite.stub.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/*TODO needed 
 havis.custom.harting.processviewer.rest.Authenticator;
 havis.custom.harting.processviewer.rest.Methods;
 */
public abstract class RestServiceClient {
	private static SSLContext trustAllContext;
	private static HostnameVerifier trustAllVerifier;
	private String baseUri;
	private String auth;
	private CookieManager cookieManager = new CookieManager();
	private boolean isHttps = false;
	private String localHostAddress = null;
	private int timeout = 30000;

	static {
		try {
			trustAllContext = SSLContext.getInstance("SSL");
			trustAllContext.init(null, new TrustManager[] { new X509TrustManager() {

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			} }, null);
			trustAllVerifier = new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
		} catch (Exception e) {
			// ignore
		}
	}

	private static final Logger log = Logger.getLogger(RestServiceClient.class.getName());

	public RestServiceClient(String baseUri, String user, String password) throws Exception {
		URI uri = new URI(baseUri);
		this.baseUri = baseUri;
		switch (uri.getScheme()) {
		case "http":
			isHttps = false;
			break;
		case "https":
			isHttps = true;
			break;
		default:
			throw new Exception("Unknown scheme '" + uri.getScheme() + "'");
		}

		if (uri.getHost() == null) {
			throw new Exception("No host specified for " + uri.toString());
		}

		auth = new Authenticator(user, password).getBasicAuthentication();

	}

	public void open() {
		log.info("Open REST service at " + baseUri);
	}
	
	public void close() {
		log.info("Close REST service at " + baseUri);
	}

	public void send(Methods method, String path) throws URISyntaxException, MalformedURLException, ProtocolException, IOException {
		final HttpURLConnection connection = prepare(method, path);
		try {
			postprocessing(connection);
		} finally {
			connection.disconnect();
		}
	}

	/**
	 * Example: send(Methods.POST, "rest/test",  object, new JacksonType<Object>(new TypeReference<Object>() {}));
	 * 
	 **/
	public <Type, WrappedType> void send(Methods method, String path, Type object, ObjectType<Type, WrappedType> type) throws URISyntaxException, MalformedURLException, ProtocolException,
			IOException {
		final HttpURLConnection connection = prepare(method, path);
		try {
			try (OutputStreamWriter stream = new OutputStreamWriter(connection.getOutputStream())) {
				marshal(stream, object, type);
			}
			postprocessing(connection);
		} finally {
			connection.disconnect();
		}
	}

	/**
	 * Example: send(Methods.GET, "rest/test", new JacksonType<Object>(new TypeReference<Object>() {}));
	 * 
	 **/	
	public <Type, WrappedType> Type sendReceive(Methods method, String path, ObjectType<Type, WrappedType> type) throws MalformedURLException, IOException, URISyntaxException, SecurityException {
		final HttpURLConnection connection = prepare(method, path);
		try {
			postprocessing(connection);
			return unmarshal(connection.getInputStream(), type);
		} finally {
			connection.disconnect();
		}
	}

	/**
	 * Example: send(Methods.GET, "rest/test", new JacksonType<Object>(new TypeReference<Object>() {}));
	 * 
	 **/	
	public String sendReceiveRaw(Methods method, String path) throws MalformedURLException, IOException, URISyntaxException, SecurityException {
		final HttpURLConnection connection = prepare(method, path);
		try {
			postprocessing(connection);
			try (java.util.Scanner s = new java.util.Scanner(connection.getInputStream())) {
				return s.useDelimiter("\\A").hasNext() ? s.useDelimiter("\\A").next() : "";
			}
		} finally {
			connection.disconnect();
		}
	}

	/**
	 * Example: send(Methods.POST, "rest/test", new JacksonType<Object>(new TypeReference<Object>() {}), object, new JacksonType<Object>(new TypeReference<Object>() {}));
	 * 
	 **/		
	public <Type, WrappedType, ReturnType, ReturnWrappedType> ReturnType sendReceive(Methods method, String path, ObjectType<Type, WrappedType> type, Object object, ObjectType<ReturnType, ReturnWrappedType> returnType) throws MalformedURLException, IOException, URISyntaxException,
			SecurityException {
		final HttpURLConnection connection = prepare(method, path);
		try {
			try (OutputStreamWriter stream = new OutputStreamWriter(connection.getOutputStream())) {
				marshal(stream, object, type);
			}

			postprocessing(connection);

			return unmarshal(connection.getInputStream(), returnType);
		} finally {
			connection.disconnect();
		}
	}

	/**
	 * Example: send(Methods.POST, "rest/test", new JacksonType<Object>(new TypeReference<Object>() {}), object, new JacksonType<Object>(new TypeReference<Object>() {}));
	 * 
	 **/		
	public <ReturnType, ReturnWrappedType> ReturnType sendReceiveRaw(Methods method, String path, String str, ObjectType<ReturnType, ReturnWrappedType> returnType) throws MalformedURLException, IOException, URISyntaxException,
			SecurityException {
		final HttpURLConnection connection = prepare(method, path);
		try {
			try (OutputStreamWriter stream = new OutputStreamWriter(connection.getOutputStream())) {
				stream.write(str);
			}

			postprocessing(connection);

			return unmarshal(connection.getInputStream(), returnType);
		} finally {
			connection.disconnect();
		}
	}	
	
	private void getLocalAddress(HttpURLConnection connection) {
		InetAddress address = null;
		try {
			Object networkClient;
			Class<?> networkClientClass;
			if (connection instanceof HttpsURLConnection) {
				Field delegateField = connection.getClass().getDeclaredField("delegate");
				delegateField.setAccessible(true);
				Object httpConnection = delegateField.get(connection);

				Field field = httpConnection.getClass().getSuperclass().getSuperclass().getDeclaredField("http");
				field.setAccessible(true);
				networkClient = field.get(httpConnection);
				networkClientClass = networkClient.getClass().getSuperclass().getSuperclass();
			} else {
				Field field = connection.getClass().getDeclaredField("http");
				field.setAccessible(true);
				networkClient = field.get(connection);
				networkClientClass = networkClient.getClass().getSuperclass();
			}

			Method method = networkClientClass.getDeclaredMethod("getLocalAddress");
			method.setAccessible(true);
			address = (InetAddress) method.invoke(networkClient);
		} catch (Throwable e) {
			e.printStackTrace();
			// ignore
		}
		localHostAddress = address.getHostAddress();
	}

	private HttpURLConnection prepare(Methods method, String path) throws URISyntaxException, IOException, MalformedURLException, ProtocolException {
		URI uri = new URI(baseUri + path);
		final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
		if (isHttps) {
			((HttpsURLConnection) connection).setSSLSocketFactory(trustAllContext.getSocketFactory());
			((HttpsURLConnection) connection).setHostnameVerifier(trustAllVerifier);
		}
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);
		switch (method) {
		case DELETE:
		case GET:
			connection.setDoOutput(false);
			break;
		case POST:
		case PUT:
			connection.setDoOutput(true);
		}
		connection.setRequestMethod(method.toString());
		connection.setRequestProperty("Content-type", getRequestMimeType());
		connection.setRequestProperty("Accept", getResponseMimeType());

		if (cookieManager.getCookieStore().getCookies().size() > 0) {
			// While joining the Cookies, use ',' or ';' as needed,
			// most servers are using ';'
			StringBuilder value = new StringBuilder();
			for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
				if (value.length() > 0)
					value.append(';');
				// always use simple format
				value.append(cookie.getName() + "=" + cookie.getValue());
			}
			connection.setRequestProperty("Cookie", value.toString());
		}

		connection.setRequestProperty("Authorization", auth);
		connection.connect();
		if (getLocalHostAddress() == null) {
			getLocalAddress(connection);
		}
		return connection;
	}

	private void postprocessing(final HttpURLConnection connection) throws IOException {
		int code = connection.getResponseCode();
		if (code < HttpURLConnection.HTTP_OK || code >= HttpURLConnection.HTTP_OK + 100) {
			try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
				byte[] buffer = new byte[1024];
				int length;
				if (connection.getErrorStream() != null) {
					while ((length = connection.getErrorStream().read(buffer)) != -1) {
						result.write(buffer, 0, length);
					}
				}
				throw new IOException("HTTP " + code + ": " + connection.getResponseMessage() + " cause:" + result.toString());
			}
		}
		for (Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
			if ("Set-Cookie".equalsIgnoreCase(entry.getKey()) && entry.getValue() != null) {
				for (String cookie : entry.getValue()) {
					cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
				}
			}
		}

	}

	public String getLocalHostAddress() {
		return localHostAddress;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public abstract String getRequestMimeType();

	public abstract String getResponseMimeType();

	protected abstract <Type, WrappedType> void marshal(Writer writer, Object object, ObjectType<Type, WrappedType> type) throws IOException;

	protected abstract <Type, WrappedType> Type unmarshal(InputStream stream, ObjectType<Type, WrappedType> type) throws IOException;
}
