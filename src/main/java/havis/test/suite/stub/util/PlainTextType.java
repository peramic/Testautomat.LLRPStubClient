package havis.test.suite.stub.util;

public class PlainTextType implements ObjectType<String, Class<String>> {

	public static final PlainTextType DEFAULT = new PlainTextType();

	private PlainTextType() {
	}

	@Override
	public Class<String> getType() {
		return String.class;
	}

}
