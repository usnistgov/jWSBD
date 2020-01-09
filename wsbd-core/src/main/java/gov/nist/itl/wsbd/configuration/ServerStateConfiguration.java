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

import java.time.Instant;

import javax.xml.datatype.XMLGregorianCalendar;

import org.oasis_open.docs.bioserv.ns.wsbd_1.SensorStatus;

import gov.nist.itl.wsbd.dictionary.DictionaryWrapper;
import gov.nist.itl.wsbd.dictionary.InvalidDictionaryException;
import gov.nist.itl.wsbd.service.Utility;

/**
 * Represents: the server state configuration, which is the last updated time
 * and the sensor status (the values in {@link ServerStateKey}.
 *
 * @author Jacob Glueck
 *
 */
public class ServerStateConfiguration extends DictionaryWrapper<Object> {
	
	/**
	 * Default UID.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Creates: a new server state configuration with values from the default
	 * values in the information.
	 *
	 * @param information
	 *            the information
	 * @throws InvalidDictionaryException
	 *             if there is a problem
	 */
	public ServerStateConfiguration(ServerStateInformation information) throws InvalidDictionaryException {
		super(ServiceConfiguration.extractDefaults(information), Object.class);
	}
	
	/**
	 * XML type: xs:dateTime
	 *
	 * @return the time at which the service configuration (the entire
	 *         configuration consisting of both the {@link ServerConfiguration}
	 *         and the {@link SensorConfiguration}) was last updated.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 */
	public Instant lastUpdated() {
		
		XMLGregorianCalendar lastUpdated = (XMLGregorianCalendar) get(ServerStateKey.lastUpdated.toString());
		Instant result = Instant.ofEpochMilli(lastUpdated.toGregorianCalendar().getTimeInMillis());
		return result;
	}
	
	/**
	 * Effect: sets the last updated time to the specified instant.
	 *
	 * @param lastUpdated
	 *            the last updated time
	 */
	public void setLastUpdated(Instant lastUpdated) {
		put(ServerStateKey.lastUpdated.toString(), Utility.fromInstant(lastUpdated));
	}
	
	/**
	 * Effect: marks the configuration as last updated now
	 */
	public void markUpdated() {
		setLastUpdated(Instant.now());
	}
	
	/**
	 * @return the sensor status
	 */
	public SensorStatus sensorStatus() {
		return SensorStatus.fromValue((String) get(ServerStateKey.sensorStatus.toString()));
	}

	/**
	 * Effect: sets the sensor status
	 *
	 * @param status
	 *            the new status
	 */
	public void setSensorStatus(SensorStatus status) {
		put(ServerStateKey.sensorStatus.toString(), status.value());
	}
}
