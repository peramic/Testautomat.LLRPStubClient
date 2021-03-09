package havis.test.suite.stub.util;

import com.fasterxml.jackson.core.type.TypeReference;

public class JacksonType<T> implements ObjectType<T, TypeReference<T>> {

	public static final JacksonType<Object> IGNORE = new JacksonType<Object>(null);

	private TypeReference<T> type;

	public JacksonType(TypeReference<T> type) {
		this.type = type;
	}

	@Override
	public TypeReference<T> getType() {
		return this.type;
	}

}
