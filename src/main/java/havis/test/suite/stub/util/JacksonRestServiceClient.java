package havis.test.suite.stub.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

public class JacksonRestServiceClient extends RestServiceClient {

	private ObjectMapper mapper = new ObjectMapper();

	public JacksonRestServiceClient(String baseUri, String user, String password) throws Exception {
		super(baseUri, user, password);
		// supports jaxb annotations, e.g. XmlElement
		mapper.registerModule(new JaxbAnnotationModule());
	}

	private static final String MIME_TYPE = "application/json";

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
		mapper.writeValue(writer, object);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <Type, WrappedType> Type unmarshal(InputStream stream, ObjectType<Type, WrappedType> type) throws IOException {
		return mapper.readValue(stream, ((JacksonType<Type>) type).getType());
	}
}
