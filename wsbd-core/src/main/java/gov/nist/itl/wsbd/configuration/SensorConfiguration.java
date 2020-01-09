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

import gov.nist.itl.wsbd.dictionary.DictionaryWrapper;
import gov.nist.itl.wsbd.dictionary.InvalidDictionaryException;

/**
 * Represents: the configuration of the sensor. The sensor must define values
 * for every key in {@link SensorInfoKey}, but may have more values for sensor
 * specific configuration.
 *
 * @author Jacob Glueck
 *
 */
public class SensorConfiguration extends DictionaryWrapper<Object> {

	/**
	 * Default UID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates: a new sensor configuration from the specified dictionary
	 *
	 * @param dictionary
	 *            the sensor configuration
	 * @throws InvalidDictionaryException
	 *             if the set of keys in the dictionary is not a superset of the
	 *             required set of keys in {@link SensorInfoKey}.
	 */
	public SensorConfiguration(Dictionary dictionary) throws InvalidDictionaryException {
		super(dictionary, Object.class);
		// This map must contain all the keys in SensorInfoKey
		for (SensorInfoKey key : SensorInfoKey.values()) {
			if (!containsKey(key.toString())) {
				throw new InvalidDictionaryException("Must contain value for key: " + key.toString());
			}
		}
	}

	/**
	 * Creates: a new sensor configuration with the default values from the
	 * sensor information
	 *
	 * @param information
	 *            the sensor information
	 * @throws InvalidDictionaryException
	 *             if there is a problem
	 */
	public SensorConfiguration(SensorInformation information) throws InvalidDictionaryException {
		this(ServiceConfiguration.extractDefaults(information));

	}
}
