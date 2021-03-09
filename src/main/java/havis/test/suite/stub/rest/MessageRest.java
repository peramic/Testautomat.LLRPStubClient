package havis.test.suite.stub.rest;

import havis.device.test.hardware.HardwareOperationType;
import havis.test.suite.api.NDIContext;
import havis.test.suite.api.dto.TestCaseInfo;
import havis.test.suite.common.PathResolver;
import havis.test.suite.common.helpers.FileHelper;
import havis.test.suite.common.messaging.XSD;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;

/**
 * The step property duration provides the duration in milliseconds to sleep.
 */
public class MessageRest implements havis.test.suite.api.Step {
	private static final Logger log = LoggerFactory.getLogger(MessageRest.class);
	private Map<String, Object> stepProperties;

	private DummyLLRPReaderServiceClientFactory serviceClientFactory;
	private RestHttpStubServiceClient stubServiceClient;
	private List<String> specURIs;
	private String readerName;
	private List<String> specContents = new ArrayList<>();
	private boolean isManual = false;
	private boolean forceSleep = false;
	private int offset = 0;
	private boolean addTags = false;
	private Unmarshaller unmarshall;

	/**
	 * See
	 * {@link Havis.RfidTestSuite.Interfaces.Step#prepare(NDIContext, String, TestCaseInfo, String, Map)}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> prepare(NDIContext context, String moduleHome, TestCaseInfo testCaseInfo, String stepId, Map<String, Object> stepProperties)
			throws Exception {
		// get Text
		if (!stepProperties.containsKey("text")) {
			throw new ConfigurationException("Step property 'text' is missed");
		}
		this.stepProperties = stepProperties;

		Object objectManual = (Object) context.getValue(NDIConstants.getStubcummunity(), NDIConstants.getIsmanual());
		
		if (objectManual != null) {
			isManual = (boolean) objectManual;
		}

		if (isManual) {
			return null;
		}

		// check step properties
		if (!stepProperties.containsKey("readerName")) {
			throw new ConfigurationException("Step property 'readerName' is required");
		}
		if (!stepProperties.containsKey("dataURI")) {
			throw new ConfigurationException("Step property 'dataURI' is required");
		}

		if (stepProperties.containsKey("forceSleep")) {
			forceSleep = Boolean.parseBoolean((String) stepProperties.get("forceSleep"));
		}

		if (stepProperties.containsKey("offset")) {
			offset = Integer.parseInt((String) stepProperties.get("offset"));
		}

		// get reader name
		readerName = (String) stepProperties.get("readerName");

		if (!(stepProperties.get("dataURI") instanceof List<?>)) {
			throw new ConfigurationException("Step property 'dataURI' must be a list");
		}

		if (stepProperties.containsKey("addTags")) {
			addTags = Boolean.parseBoolean((String) stepProperties.get("addTags"));
		}

		specURIs = (List<String>) stepProperties.get("dataURI");

		for (String specURI : specURIs) {
			// load dummy data from spec
			specURI = testCaseInfo.getHome() + "/" + specURI;
			String specContent = FileHelper.readFile(PathResolver.getResourceInputStream(specURI));
			// if spec parameters exist
			if (stepProperties.containsKey("accessReportSpecParameters")) {
				Map<String, Object> specParams = (Map<String, Object>) stepProperties.get("accessReportSpecParameters");
				if (specParams != null) {
					ST template = new ST(specContent, '$', '$');
					for (Entry<String, Object> specParam : specParams.entrySet()) {
						template.add(specParam.getKey(), specParam.getValue());
					}
					specContent = template.render();
				}
			}
			// validate spec if required
			boolean validate = true;
			if (stepProperties.containsKey("validate")) {
				validate = Boolean.parseBoolean((String) stepProperties.get("validate"));
			}
			if (validate) {
				String xsdPath = moduleHome + "/stub/XSD";
				List<InputStream> streams = new ArrayList<>();
				streams.add(PathResolver.getResourceInputStream(xsdPath, NDIConstants.getXsdhardwarebase()));
				streams.add(PathResolver.getResourceInputStream(xsdPath, NDIConstants.getXsdhardware()));
				streams.add(PathResolver.getResourceInputStream(xsdPath, NDIConstants.getXsdhardwareoperation()));
				XSD validator = new XSD(streams, NDIConstants.getXsdhardware());
				validator.validate(specContent);
			}
			
			specContents.add(specContent);
		}

		// get a service client for the reader
		serviceClientFactory = (DummyLLRPReaderServiceClientFactory) context.getValue(NDIConstants.getStubcummunity(), readerName);
		stubServiceClient = serviceClientFactory.getStubServiceClient();

		HashMap<String, Object> ret = new HashMap<>();
		ret.put("specs", specContents);
		return ret;
	}

	/**
	 * See {@link Havis.RfidTestSuite.Interfaces.Step#run()}
	 */
	@Override
	public String run() throws Exception {
		StringBuilder msg = new StringBuilder();
		Object value = stepProperties.get("text");
		if (value instanceof String) {
			msg.append(value);
		} else if (value instanceof List<?>) {
			for (Object v : (ArrayList<?>) value) {
				if (msg.length() > 0) {
					msg.append(System.getProperty("line.separator"));
				}
				msg.append(v);
			}
		}
		if (msg.length() > 0) {
			// add property for UI which calls this method
			stepProperties.put("UI.messageAfterExecute", msg.toString());
		}

		if (!isManual) {
			log.info("Loading dummy data for LR spec with name '" + readerName + "' from " + specURIs + " after " + offset + "s.");
			Thread.sleep(offset * 1000);

			unmarshall = JAXBContext.newInstance(HardwareOperationType.class).createUnmarshaller();
			List<HardwareOperationType> hardwareOperations = new ArrayList<HardwareOperationType>();
			for(String specContent : specContents){
				hardwareOperations.add((HardwareOperationType) unmarshall.unmarshal(new StringReader(specContent)));
			}
			
			if (addTags)
				stubServiceClient.addTags(hardwareOperations);
			else
				stubServiceClient.replaceConfig(hardwareOperations);
		}

		return null;
	}

	/**
	 * See {@link Havis.RfidTestSuite.Interfaces.Step#finish()}
	 */
	@Override
	public void finish() throws Exception {
		// add properties for UI module which calls this method
		if (stepProperties.containsKey("suspendAfterFinish")) {
			stepProperties.put("UI.suspendAfterFinish", Boolean.parseBoolean((String) stepProperties.get("suspendAfterFinish")));
			if (stepProperties.containsKey("suspendAfterFinishTimeout")) {
				int parseInt = Integer.parseInt((String) stepProperties.get("suspendAfterFinishTimeout")) - offset;
				if (parseInt < 0) {
					parseInt = 0;
				}
				stepProperties.put("UI.suspendAfterFinishTimeout", parseInt);
			}
		}

		if (!isManual) {
			serviceClientFactory.releaseClient(stubServiceClient);
			if (!isManual && !forceSleep) {
				stepProperties.put("UI.suspendAfterFinish", false);
			}
		}
	}
}
