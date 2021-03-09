package havis.test.suite.stub.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class JaxbRestServiceClient extends RestServiceClient {

	private static final String MIME_TYPE = "application/xml";

	private Lock marshallerLock = new ReentrantLock();
	private Marshaller marshaller = null;
	private Lock unmarshallerLock = new ReentrantLock();
	private Unmarshaller unmarshaller = null;

	public JaxbRestServiceClient(String baseUri, String user, String password) throws Exception {
		super(baseUri, user, password);
	}

	@Override
	public String getRequestMimeType() {
		return MIME_TYPE;
	}

	@Override
	public String getResponseMimeType() {
		return MIME_TYPE;
	}

	@SuppressWarnings("unchecked")
	private <Type, WrappedType> void initMarshaller(ObjectType<Type, WrappedType> type) throws JAXBException {
		if (this.marshaller == null) {
			this.marshallerLock.lock();
			try {
				if (this.marshaller == null) {
					this.marshaller = JAXBContext.newInstance(((JaxbType<Type>) type).getType()).createMarshaller();
				}
			} finally {
				this.marshallerLock.unlock();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <Type, WrappedType> void initUnmarshaller(ObjectType<Type, WrappedType> type) throws JAXBException {
		if (this.unmarshaller == null) {
			this.unmarshallerLock.lock();
			try {
				if (this.unmarshaller == null) {
					this.unmarshaller = JAXBContext.newInstance(((JaxbType<Type>) type).getType()).createUnmarshaller();
				}
			} finally {
				this.unmarshallerLock.unlock();
			}
		}
	}

	@Override
	protected <Type, WrappedType> void marshal(Writer writer, Object object, ObjectType<Type, WrappedType> type) throws IOException {
		try {
			initMarshaller(type);
			this.marshaller.marshal(object, writer);
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <Type, WrappedType> Type unmarshal(InputStream stream, ObjectType<Type, WrappedType> type) throws IOException {
		try {
			initUnmarshaller(type);
			return (Type) this.unmarshaller.unmarshal(stream);
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}

}
