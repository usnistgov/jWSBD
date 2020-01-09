/*----------------------------------------------------------------------------------------------------+
|                             National Institute of Standards and Technology                          |
|                                        Biometric Clients Lab                                        |
+-----------------------------------------------------------------------------------------------------+
 File author(s):
      Jacob Glueck (jacob.glueck@nist.gov)

+-----------------------------------------------------------------------------------------------------+
| NOTICE & DISCLAIMER                                                                                 |
|                                                                                                     |
| The research software provided on this web site ("software") is provided by NIST as a public        |
| service. You may use, copy and distribute copies of the software in any medium, provided that you   |
| keep intact this entire notice. You may improve, modify and create derivative works of the software |
| or any portion of the software, and you may copy and distribute such modifications or works.        |
| Modified works should carry a notice stating that you changed the software and should note the date |
| and nature of any such change.  Please explicitly acknowledge the National Institute of Standards   |
| and Technology as the source of the software.                                                       |
|                                                                                                     |
| The software is expressly provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED,  |
| IN FACT OR ARISING BY OPERATION OF LAW, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF      |
| MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT AND DATA ACCURACY.  NIST        |
| NEITHER REPRESENTS NOR WARRANTS THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR         |
| ERROR-FREE, OR THAT ANY DEFECTS WILL BE CORRECTED.  NIST DOES NOT WARRANT OR MAKE ANY               |
| REPRESENTATIONS REGARDING THE USE OF THE SOFTWARE OR THE RESULTS THEREOF, INCLUDING BUT NOT LIMITED |
| TO THE CORRECTNESS, ACCURACY, RELIABILITY, OR USEFULNESS OF THE SOFTWARE.                           |
|                                                                                                     |
| You are solely responsible for determining the appropriateness of using and distributing the        |
| software and you assume all risks associated with its use, including but not limited to the risks   |
| and costs of program errors, compliance with applicable laws, damage to or loss of data, programs   |
| or equipment, and the unavailability or interruption of operation.  This software is not intended   |
| to be used in any situation where a failure could cause risk of injury or damage to property.  The  |
| software was developed by NIST employees.  NIST employee contributions are not subject to copyright |
| protection within the United States.                                                                |
|                                                                                                     |
| Specific hardware and software products identified in this open source project were used in order   |
| to perform technology transfer and collaboration. In no case does such identification imply         |
| recommendation or endorsement by the National Institute of Standards and Technology, nor            |
| does it imply that the products and equipment identified are necessarily the best available for the |
| purpose.                                                                                            |
+----------------------------------------------------------------------------------------------------*/

package gov.nist.itl.wsbd.tests;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.junit.After;
import org.junit.Before;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Dictionary;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Result;

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
 * Represents: a base class for running WSBD tests. This class sets up and
 * launches a server before every test.
 *
 * @author Jacob Glueck
 *
 */
public class WSBDTest {
	
	/**
	 * The service path
	 */
	public static final String servicePath = "testservice";
	/**
	 * The amount of extra time to wait after a timeout to ensure the timeout
	 * really times out
	 */
	public static final int FUDGE = 200;
	/**
	 * The server
	 */
	private WSBDServer server;
	/**
	 * The test service
	 */
	protected TestService testService;
	/**
	 * The test client
	 */
	protected StatelessClient testClient;
	/**
	 * The service configuration
	 */
	protected ServiceConfiguration serviceConfiguraiton;

	/**
	 * Effect: starts a new server for the next test. Also makes a new test
	 * client.
	 *
	 * @throws URISyntaxException
	 *             if there is a problem
	 * @throws InvalidDictionaryException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 * @throws IOException
	 *             if there is a problem
	 * @throws IllegalResourceException
	 *             if there is a problem
	 */
	@Before
	public void setUp() throws URISyntaxException, InvalidDictionaryException, JAXBException, IOException, IllegalResourceException {
		
		ServerConfiguration serverConfiguration = new ServerConfiguration(Utility.unmarshal(WSBDTest.class.getResourceAsStream("/serverConfiguration.xml"), Dictionary.class));
		SensorInformation sensorInformation = new SensorInformation(Utility.unmarshal(WSBDTest.class.getResourceAsStream("/sensorInformation.xml"), Dictionary.class));
		StreamConfiguration streamConfiguration = new StreamConfiguration(Utility.unmarshal(WSBDTest.class.getResourceAsStream("/streamConfiguration.xml"), Dictionary.class));
		serviceConfiguraiton = new ServiceConfiguration(serverConfiguration, sensorInformation, streamConfiguration);
		testService = new TestService(serviceConfiguraiton);
		testService.initializeService();
		server = new WSBDServer(testService);
		server.start();
		testClient = new StatelessClient(server.uri().toString() + "/" + WSBDTest.servicePath);
	}
	
	/**
	 * Effect: stops the last server
	 */
	@After
	public void tearDown() {
		server.stop();
	}

	/**
	 * Runs a small test
	 *
	 * @param args
	 *            Arg.. aye, aye, captain!
	 * @throws URISyntaxException
	 *             if there is a problem
	 * @throws InvalidDictionaryException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 * @throws IOException
	 *             if there is a problem
	 * @throws IllegalResourceException
	 *             if there is a problem
	 */
	public static void main(String[] args) throws URISyntaxException, InvalidDictionaryException, JAXBException, IOException, IllegalResourceException {
		WSBDTest test = new WSBDTest();
		test.setUp();
		System.out.println("Server running at: " + test.server.uri().toString());
		Result register = test.testClient.register();
		Utility.printResult(register);
		UUID sessionId = Utility.session(register);
		Utility.printResult(test.testClient.lock(sessionId));
		Result captureResult = test.testClient.capture(sessionId);
		Utility.printResult(captureResult);
		UUID captureId = Utility.captures(captureResult).get(0);
		Utility.printResult(test.testClient.download(captureId));
		new Scanner(System.in).nextLine();
		test.server.stop();
	}
}
