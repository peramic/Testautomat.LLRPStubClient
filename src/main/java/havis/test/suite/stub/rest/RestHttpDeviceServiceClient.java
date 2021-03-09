package havis.test.suite.stub.rest;

import havis.device.rf.configuration.AntennaConfiguration;
import havis.device.rf.configuration.ConnectType;
import havis.test.suite.stub.util.JacksonRestServiceClient;
import havis.test.suite.stub.util.JacksonType;
import havis.test.suite.stub.util.Methods;
import havis.test.suite.stub.util.PlainTextRestServiceClient;
import havis.test.suite.stub.util.PlainTextType;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

public class RestHttpDeviceServiceClient extends JacksonRestServiceClient {
	protected static final Logger log = LoggerFactory.getLogger(RestHttpDeviceServiceClient.class);

	private PlainTextRestServiceClient plainTextClient;

	public RestHttpDeviceServiceClient(String baseUri, String user, String password) throws Exception {
		super(baseUri, user, password);
		plainTextClient = new PlainTextRestServiceClient(baseUri, user, password);
	}

	@Override
	public void open() {
		this.plainTextClient.open();
		super.open();
	}

	public void setRegion(String region) throws MalformedURLException, ProtocolException, URISyntaxException, IOException {
		log.info("Set region of device...");
		this.plainTextClient.send(Methods.PUT, "/rf/region", region, PlainTextType.DEFAULT);
	}

	public String getRegion() throws MalformedURLException, SecurityException, IOException, URISyntaxException {
		log.info("Get region device...");
		return this.plainTextClient.sendReceive(Methods.GET, "/rf/region", PlainTextType.DEFAULT);
	}

	public List<AntennaConfiguration> getAntennaConfig() throws MalformedURLException, SecurityException, IOException, URISyntaxException {
		log.info("Get config of antennas ...");
		return sendReceive(Methods.GET, "/rf/configuration/antennas", new JacksonType<List<AntennaConfiguration>>(
				new TypeReference<List<AntennaConfiguration>>() {
				}));
	}

	public void setAntennaConfiguration(int antennaId, AntennaConfiguration config) throws MalformedURLException, ProtocolException, URISyntaxException, IOException {
		log.info("Set config of antenna " + antennaId + " ...");
		send(Methods.PUT, "/rf/configuration/antennas/" + antennaId, config, new JacksonType<AntennaConfiguration>(
				new TypeReference<AntennaConfiguration>() {
				}));
	}

	public void configureDevice() throws Exception {
		RestHttpDeviceServiceClient client = new RestHttpDeviceServiceClient("https://10.10.10.10/Apps/rest", "admin", "admin");

		if (client.getRegion().equals("Unspecified")) {
			client.setRegion("EU");

			AntennaConfiguration antennaConfig = new AntennaConfiguration();
			antennaConfig.setConnect(ConnectType.TRUE);
			antennaConfig.setTransmitPower((short) 10);
			
			client.setAntennaConfiguration(1, antennaConfig);
			client.setAntennaConfiguration(2, antennaConfig);		}
	}

	@Override
	public void close() {
		super.close();
		this.plainTextClient.close();
	}
}
