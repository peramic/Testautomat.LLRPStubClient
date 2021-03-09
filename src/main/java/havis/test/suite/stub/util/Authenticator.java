package havis.test.suite.stub.util;

import java.io.UnsupportedEncodingException;

import javax.xml.bind.DatatypeConverter;

public class Authenticator {

	private final String user;
	private final String password;

	public Authenticator(String user, String password) {
		this.user = user;
		this.password = password;
	}

	public String getBasicAuthentication() {
		String token = this.user + ":" + this.password;
		try {
			return "Basic "
					+ DatatypeConverter.printBase64Binary(token
							.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException("Cannot encode with UTF-8", ex);
		}
	}

}
