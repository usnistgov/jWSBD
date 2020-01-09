package gov.nist.itl.wsbd.example;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.oasis_open.docs.biometrics.ns.ws_bd_1.Dictionary;
import org.oasis_open.docs.biometrics.ns.ws_bd_1.Result;

import gov.nist.itl.wsbd.client.StatelessClient;
import gov.nist.itl.wsbd.configuration.SensorInformation;
import gov.nist.itl.wsbd.configuration.ServerConfiguration;
import gov.nist.itl.wsbd.configuration.ServiceConfiguration;
import gov.nist.itl.wsbd.dictionary.InvalidDictionaryException;
import gov.nist.itl.wsbd.server.WSBDServer;
import gov.nist.itl.wsbd.service.Utility;
import gov.nist.itl.wsbd.streaming.IllegalResourceException;
import gov.nist.itl.wsbd.streaming.StreamConfiguration;

/**
 * Represents: a server which runs the simulated sensor
 *
 * @author Jacob Glueck
 *
 */
public class SimulatedServer {

	/**
	 * Runs the server.
	 *
	 * @param args
	 *            ignored
	 * @throws IOException
	 *             bad
	 * @throws IllegalResourceException
	 *             UhOh
	 * @throws InvalidDictionaryException
	 *             probably not good
	 * @throws JAXBException
	 *             something might be wrong
	 */
	public static void main(String[] args) throws IOException, IllegalResourceException, InvalidDictionaryException, JAXBException {
		
		// Read the server configuration files in /src/main/resources
		ServerConfiguration serverConfiguration = new ServerConfiguration(Utility.unmarshal(SimulatedServer.class.getResourceAsStream("/serverConfiguration.xml"), Dictionary.class));
		SensorInformation sensorInformation = new SensorInformation(Utility.unmarshal(SimulatedServer.class.getResourceAsStream("/sensorInformation.xml"), Dictionary.class));
		StreamConfiguration streamConfiguration = new StreamConfiguration(Utility.unmarshal(SimulatedServer.class.getResourceAsStream("/streamConfiguration.xml"), Dictionary.class));
		ServiceConfiguration serviceConfiguraiton = new ServiceConfiguration(serverConfiguration, sensorInformation, streamConfiguration);

		// Create the simulated sensor service and initialize it. The initialize
		// method must be called before the service is used
		SimulatedSensorService service = new SimulatedSensorService(serviceConfiguraiton);
		service.initializeService();
		
		// Create a new server on port 7676
		WSBDServer server = new WSBDServer(service, "http://localhost", 7676);
		server.start();
		System.out.println(server.uri());
		
		// Create a client and capture some data
		StatelessClient client = new StatelessClient("http://localhost:7676/simulatedservice");
		Result r = client.register();
		UUID s = Utility.session(r);
		System.out.println("Registered client: " + s);
		client.lock(s);
		System.out.println("Locked client");
		UUID c = Utility.captures(client.capture(s)).get(0);
		System.out.println("Captured: " + c);
		System.out.println("To see the capture, go to: http://localhost:7676/simulatedservice/download/" + c + "/raw");
		System.out.println("To view a live locked stream, use: http://localhost:7676/simulatedservice/stream/locked/" + s);
		System.out.println("To view a live unlocked stream, use: http://localhost:7676/simulatedservice/stream/public");
		
		System.out.println("Server started. Enter to terminate.");
		try (Scanner scanner = new Scanner(System.in)) {
			scanner.nextLine();
		}
	}
}
