package havis.test.suite.stub.util;

public enum Methods {
	GET("GET"), DELETE("DELETE"), PUT("PUT"), POST("POST");

	private final String method;

	private Methods(final String method) {
		this.method = method;
	}

	@Override
	public String toString() {
		return method;
	}

}
