package havis.test.suite.stub.rest;

import havis.device.test.hardware.HardwareOperationType;
import havis.test.suite.stub.util.JaxbRestServiceClient;
import havis.test.suite.stub.util.JaxbType;
import havis.test.suite.stub.util.Methods;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestHttpStubServiceClient extends JaxbRestServiceClient {
	protected static final Logger log = LoggerFactory.getLogger(RestHttpStubServiceClient.class);
	private String baseUri;

	public RestHttpStubServiceClient(String baseUri, String user, String password) throws Exception {
		super(baseUri, user, password);
		this.baseUri = baseUri;
	}

	@Override
	public void close(){
		log.info("Close REST service at " + baseUri);
	}
	
	@Override
	public void open(){
		log.info("Open REST service at " + baseUri);
	}
	
	public void replaceConfig(List<HardwareOperationType> specContents) throws MalformedURLException, ProtocolException, URISyntaxException, IOException {
		clearStub();
		deleteTags();
		setConfig(specContents);
	}

	public void clearStub() throws MalformedURLException, ProtocolException, URISyntaxException, IOException {
		log.info("Update config on Hardware stub");
		send(Methods.PUT, "/test/default");
	}

	private void deleteTags() throws MalformedURLException, ProtocolException, URISyntaxException, IOException {
		log.info("Delete tags on " + baseUri + "/test/default/tags");
		send(Methods.DELETE, "/test/default/tags");
	}

	private void setConfig(List<HardwareOperationType> specContents) throws MalformedURLException, ProtocolException, URISyntaxException, IOException {
		log.info("Post new config on " + baseUri);
		for(HardwareOperationType specContent : specContents){
			send(Methods.POST, "/test", specContent, new JaxbType<HardwareOperationType>(HardwareOperationType.class));
		}
	}
	
	public void addTags(List<HardwareOperationType> specContents) throws MalformedURLException, ProtocolException, URISyntaxException, IOException{
		log.info("Adding tags to reader field");
		setConfig(specContents);
	}
}
