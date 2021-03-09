package havis.test.suite.stub.util;

public class JaxbType<T> implements ObjectType<T, Class<T>> {

	private Class<T> clazz;

	public JaxbType(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public Class<T> getType() {
		return this.clazz;
	}

}
