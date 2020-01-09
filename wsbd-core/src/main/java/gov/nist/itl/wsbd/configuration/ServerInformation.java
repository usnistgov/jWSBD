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

import org.oasis_open.docs.bioserv.ns.wsbd_1.Dictionary;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Parameter;

import gov.nist.itl.wsbd.dictionary.DictionaryWrapper;
import gov.nist.itl.wsbd.dictionary.InvalidDictionaryException;

/**
 * Represents: the service information, which describes the allowable values for
 * the service configuration. The service configuration consists of two parts:
 * the service information, which is specific to the server, and the sensor
 * information, which is specific to the sensor. This class represents the
 * possible configuration values of the service. The {@link SensorInformation}
 * class represents the possible configuration values of the sensor. All the
 * parameters in this class are read only.
 *
 * @author Jacob Glueck
 *
 */
public class ServerInformation extends DictionaryWrapper<Parameter> {

	/**
	 * Default UID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates: a new service information object representing the possible
	 * configuration values for a service. The service information object is
	 * given as an XML dictionary {@link Dictionary}. The WS-BD specification
	 * requires that the service information document contain certain keys.
	 * Those keys are enumerated in {@link ServerInfoKey}. This constructor
	 * generates the appropriate service information dictionary using the
	 * service configuration. It does so by making a parameter dictionary with
	 * every key in the service configuration. All the parameters are marked
	 * read only and do not support multiple values. Their types are set to
	 * those specified by the {@link ServerInfoKey}, and the default value is
	 * set the the value in the configuration.
	 *
	 * @param configuration
	 *            configuration of this service
	 * @throws InvalidDictionaryException
	 *             if a required value is <code>null</code>
	 */
	public ServerInformation(ServerConfiguration configuration) throws InvalidDictionaryException {
		super(new Dictionary(), Parameter.class);
		for (ServerInfoKey key : ServerInfoKey.values()) {
			Parameter param = new Parameter();
			param.setName(key.toString());
			param.setReadOnly(true);
			param.setSupportsMultiple(false);
			param.setType(key.type());
			Object value = configuration.get(key.toString());
			if (value == null) {
				throw new InvalidDictionaryException("Null value for key " + key.toString());
			}
			param.setDefaultValue(value);
			put(key.toString(), param);
		}
	}
}
