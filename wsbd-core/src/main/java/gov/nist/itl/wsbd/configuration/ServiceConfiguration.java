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

package gov.nist.itl.wsbd.configuration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.oasis_open.docs.bioserv.ns.wsbd_1.Dictionary;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Parameter;

import gov.nist.itl.wsbd.dictionary.DictionaryWrapper;
import gov.nist.itl.wsbd.dictionary.InvalidDictionaryException;
import gov.nist.itl.wsbd.streaming.StreamConfiguration;
import gov.nist.itl.wsbd.streaming.StreamInformation;

/**
 * Represents: the configuration and information for the entire service (both
 * the server and the sensor).
 *
 * @author Jacob Glueck
 *
 */
public class ServiceConfiguration {
	
	/**
	 * The server information
	 */
	private final ServerInformation serverInformation;
	/**
	 * The server configuration
	 */
	private final ServerConfiguration serverConfiguration;
	/**
	 * The sensor information
	 */
	private final SensorInformation sensorInformation;
	/**
	 * The sensor configuration
	 */
	private final SensorConfiguration sensorConfiguration;
	/**
	 * The server state information
	 */
	private final ServerStateInformation serverStateInformation;
	/**
	 * The server configuration information
	 */
	private final ServerStateConfiguration serverStateConfiguration;
	/**
	 * The streaming information
	 */
	private final StreamInformation streamInformation;
	/**
	 * The stream configuration
	 */
	private final StreamConfiguration streamConfiguration;
	
	/**
	 * Creates: a new service configuration with no streams
	 *
	 * @param serverConfiguration
	 *            the server configuration
	 * @param sensorInformation
	 *            the sensor information
	 * @throws InvalidDictionaryException
	 *             if there is a problem creating the server information
	 */
	public ServiceConfiguration(ServerConfiguration serverConfiguration, SensorInformation sensorInformation) throws InvalidDictionaryException {
		this(serverConfiguration, sensorInformation, new StreamConfiguration());
	}

	/**
	 * Creates: a new service configuration
	 *
	 * @param serverConfiguration
	 *            the server configuration
	 * @param sensorInformation
	 *            the sensor information
	 * @param streamConfiguration
	 *            the stream configuration
	 * @throws InvalidDictionaryException
	 *             if there is a problem creating the server information
	 */
	public ServiceConfiguration(ServerConfiguration serverConfiguration, SensorInformation sensorInformation, StreamConfiguration streamConfiguration)
			throws InvalidDictionaryException {
		serverInformation = new ServerInformation(serverConfiguration);
		this.serverConfiguration = serverConfiguration;
		this.sensorInformation = sensorInformation;
		sensorConfiguration = new SensorConfiguration(sensorInformation);
		serverStateInformation = new ServerStateInformation();
		serverStateConfiguration = new ServerStateConfiguration(serverStateInformation);
		streamInformation = new StreamInformation(streamConfiguration);
		this.streamConfiguration = streamConfiguration;
	}

	/**
	 * Creates: a new dictionary which is the combination of dictionary
	 * wrappers. Requires: the intersection of the set of keys of the component
	 * dictionaries is the empty set (assert).
	 *
	 * @param dicts
	 *            the component dictionaries
	 * @return the union
	 */
	private static Dictionary combine(DictionaryWrapper<?>... dicts) {

		Dictionary result = new Dictionary();
		assert ServiceConfiguration.checkUniqueKeys(dicts);
		for (DictionaryWrapper<?> dict : dicts) {
			result.getItem().addAll(dict.dictionary().getItem());
		}
		return result;
	}

	/**
	 * @param dicts
	 *            the dictionaries to check
	 * @return true if and only if the intersection of the key sets of all the
	 *         dictionaries is the empty set.
	 */
	private static boolean checkUniqueKeys(DictionaryWrapper<?>... dicts) {

		if (dicts.length > 1) {
			Set<?> first = new HashSet<>(dicts[0].keySet());
			for (int x = 1; x < dicts.length; x++) {
				first.retainAll(new HashSet<>(dicts[x].keySet()));
			}
			return first.size() == 0;
		}
		return true;
	}

	/**
	 * @return the information for the entire service (both the server
	 *         information and the sensor information)
	 */
	public Dictionary information() {

		return ServiceConfiguration.combine(serverInformation, sensorInformation, serverStateInformation, streamInformation);
	}

	/**
	 * @return the configuration for the entire service (both the server
	 *         configuration and the sensor configuration).
	 */
	public Dictionary configuration() {
		
		return ServiceConfiguration.combine(serverConfiguration, sensorConfiguration, serverStateConfiguration, streamConfiguration);
	}

	/**
	 * @return the serverInformation
	 */
	public ServerInformation serverInformation() {
		
		return serverInformation;
	}

	/**
	 * @return the serverConfiguration
	 */
	public ServerConfiguration serverConfiguration() {
		
		return serverConfiguration;
	}

	/**
	 * @return the sensorInformation
	 */
	public SensorInformation sensorInformation() {
		
		return sensorInformation;
	}

	/**
	 * @return the sensorConfiguration
	 */
	public SensorConfiguration sensorConfiguration() {
		
		return sensorConfiguration;
	}

	/**
	 * @return the serverStateInformation
	 */
	public ServerStateInformation serverStateInformation() {
		
		return serverStateInformation;
	}

	/**
	 * @return the serverStateConfiguration
	 */
	public ServerStateConfiguration serverStateConfiguration() {
		
		return serverStateConfiguration;
	}

	/**
	 * @return the serverStateInformation
	 */
	public StreamInformation streamInformation() {
		
		return streamInformation;
	}

	/**
	 * @return the serverStateConfiguration
	 */
	public StreamConfiguration streamConfiguration() {
		
		return streamConfiguration;
	}
	
	/**
	 * Extracts the default values from the sensor information
	 *
	 * @param information
	 *            the sensor information
	 * @return the default values
	 * @throws InvalidDictionaryException
	 *             if there is a problem
	 */
	public static Dictionary extractDefaults(DictionaryWrapper<Parameter> information) throws InvalidDictionaryException {
		DictionaryWrapper<Object> dw = new DictionaryWrapper<>(new Dictionary(), Object.class);
		for (Map.Entry<String, Parameter> param : information.entrySet()) {
			dw.put(param.getKey(), param.getValue().getDefaultValue());
		}
		return dw.dictionary();
	}
}
