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

package gov.nist.itl.wsbd.server;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import gov.nist.itl.wsbd.service.ISensorService;

/**
 * Represents: a WSBD web service. This class makes it easy to start a WSBD
 * service.<br>
 * Example: <br>
 *
 * <pre>
 * <code>
 * 	ServerConfiguration serverConfiguration = new ServerConfiguration(Utility.unmarshal(WSBDTest.class.getResourceAsStream("/serverConfiguration.xml"),
 * 			Dictionary.class));
 * 	SensorInformation sensorInformation = new SensorInformation(Utility.unmarshal(WSBDTest.class.getResourceAsStream("/sensorInformation.xml"),
 * 			Dictionary.class));
 * 	serviceConfiguraiton = new ServiceConfiguration(serverConfiguration, sensorInformation);
 * 	testService = new TestService(serviceConfiguraiton);
 * 	testService.initializeService();
 * 	server = new WSBDServer(testService);
 * 	server.start();
 * </code>
 * </pre>
 *
 * @author Jacob Glueck
 *
 */
public class WSBDServer {
	
	/**
	 * The base URL of the service (no port, no service prefix)
	 */
	public static final String defaultBaseUrl = "http://localhost";
	/**
	 * The default port (0 means pick an open port)
	 */
	public static final int defaultPort = 0;
	
	/**
	 * The HTTP server
	 */
	private final HttpServer server;
	/**
	 * The base URL (no port)
	 */
	private final String baseUrl;
	/**
	 * The port
	 */
	private int port;
	/**
	 * Used to determine the port
	 */
	private final NetworkListener listener;

	/**
	 * Creates: a new WSBD service with the specified sensor service at the
	 * default port ({@link #defaultPort}) and default base URL
	 * ({@link #defaultBaseUrl}).
	 *
	 * @param service
	 *            the sensor service.
	 */
	public WSBDServer(ISensorService service) {
		this(service, WSBDServer.defaultBaseUrl, WSBDServer.defaultPort);
	}

	/**
	 * Creates: a new WSBD service with the specified service at the specified
	 * base URL and with the specified port.
	 *
	 * @param service
	 *            the service
	 * @param baseUrl
	 *            the base URL
	 * @param port
	 *            the port
	 */
	public WSBDServer(ISensorService service, String baseUrl, int port) {
		this.baseUrl = baseUrl;
		this.port = port;
		// Disable the annoying logs to stderr
		Logger.getLogger("org.glassfish.grizzly.http.server").setLevel(Level.OFF);
		Logger.getLogger("org.glassfish.jersey.server").setLevel(Level.OFF);
		ResourceConfig rc = new ResourceConfig();
		rc = rc.registerInstances(service);
		server = GrizzlyHttpServerFactory.createHttpServer(URI.create(this.baseUrl + ":" + this.port), rc);
		listener = new NetworkListener("bob", "0.0.0.0", 0);
		server.addListener(listener);
	}
	
	/**
	 * Effect: starts the server. If the port was 0 (which makes the server pick
	 * an open port), the port is assigned. To get it, use {@link #port()}.
	 *
	 * @throws IOException
	 *             if something bad happened.
	 */
	public void start() throws IOException {
		server.start();
		if (port == 0) {
			port = listener.getPort();
		}
	}
	
	/**
	 * Effect: stops the server now
	 */
	public void stop() {
		server.shutdownNow();
	}
	
	/**
	 * @return the URI of this server. If the server has not been started yet,
	 *         the port will be unassigned (0), unless one was specified in the
	 *         constructor.
	 */
	public URI uri() {
		return URI.create(baseUrl + ":" + port);
	}
	
	/**
	 * @return the port of this server. If the server has not been started yet,
	 *         the port will be unassigned (0), unless one was specified in the
	 *         constructor.
	 */
	public int port() {
		return port;
	}
	
	/**
	 * @return the base URL of this server.
	 */
	public String baseUrl() {
		return baseUrl;
	}
}
