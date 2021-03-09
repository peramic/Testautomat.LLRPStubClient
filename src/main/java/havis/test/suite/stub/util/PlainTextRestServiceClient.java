package havis.test.suite.stub.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

public class PlainTextRestServiceClient extends RestServiceClient {

	public PlainTextRestServiceClient(String baseUri, String user, String password) throws Exception {
		super(baseUri, user, password);
	}

	private static final String MIME_TYPE = "text/plain";

	@Override
	public String getRequestMimeType() {
		return MIME_TYPE;
	}

	@Override
	public String getResponseMimeType() {
		return MIME_TYPE;
	}

	@Override
	protected <Type, WrappedType> void marshal(Writer writer, Object object, ObjectType<Type, WrappedType> type) throws IOException {
		writer.write(Objects.toString(object));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <Type, WrappedType> Type unmarshal(InputStream stream, ObjectType<Type, WrappedType> type) throws IOException {
		StringBuilder stringBuilder = new StringBuilder();
		try (Reader reader = new BufferedReader(new InputStreamReader(stream))) {
			int c = 0;
			while ((c = reader.read()) != -1) {
				stringBuilder.append((char) c);
			}
		}
		return (Type) stringBuilder.toString();
	}
}
