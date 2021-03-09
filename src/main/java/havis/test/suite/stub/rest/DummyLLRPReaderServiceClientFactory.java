package havis.test.suite.stub.rest;

import havis.test.suite.stub.util.RestServiceClient;

import java.util.LinkedList;
import java.util.Queue;

public class DummyLLRPReaderServiceClientFactory {
	private int cacheSize;
	private String uri;
	private final Object lock = new Object();
	private Queue<RestHttpDeviceServiceClient> deviceServiceClients = new LinkedList<RestHttpDeviceServiceClient>();
	private Queue<RestHttpStubServiceClient> stubServiceClients = new LinkedList<RestHttpStubServiceClient>();

	public DummyLLRPReaderServiceClientFactory(int cacheSize, String uri) {
		this.cacheSize = cacheSize;
		this.uri = uri;
	}

	public void open() {
	}

	public void close() {
		synchronized (lock) {
			for (RestHttpDeviceServiceClient client : deviceServiceClients) {
				client.close();
			}
			deviceServiceClients.clear();
			
			for (RestHttpStubServiceClient client : stubServiceClients) {
				client.close();
			}
			stubServiceClients.clear();
		}
	}

	public RestHttpDeviceServiceClient getDeviceServiceClient() throws Exception {
		String authentication = "admin";
		synchronized (lock) {
			if (deviceServiceClients.size() > 0) {
				return deviceServiceClients.poll();
			}
		}
		RestHttpDeviceServiceClient client = new RestHttpDeviceServiceClient(uri, authentication, authentication);
		client.open();
		return client;
	}
	
	public RestHttpStubServiceClient getStubServiceClient() throws Exception {
		String authentication = "admin";
		synchronized (lock) {
			if (stubServiceClients.size() > 0) {
				return stubServiceClients.poll();
			}
		}
		RestHttpStubServiceClient client = new RestHttpStubServiceClient(uri, authentication, authentication);
		client.open();
		return client;
	}

	public void releaseClient(RestServiceClient client) {
		synchronized (lock) {
			if(client instanceof RestHttpDeviceServiceClient){
				if (deviceServiceClients.size() < cacheSize) {
					deviceServiceClients.add((RestHttpDeviceServiceClient) client);
					return;
				}
			}else if(client instanceof RestHttpStubServiceClient){
				if (stubServiceClients.size() < cacheSize) {
					stubServiceClients.add((RestHttpStubServiceClient) client);
					return;
				}
			}
		}
		client.close();
	}

	

}
