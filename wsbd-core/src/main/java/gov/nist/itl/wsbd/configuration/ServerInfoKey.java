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

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.oasis_open.docs.bioserv.ns.wsbd_1.Status;

/**
 * Represents: the required elements of the service configuration
 *
 * @author Jacob Glueck
 *
 */
public enum ServerInfoKey {
	/**
	 * The minimum time this service will wait before removing a session due to
	 * inactivity.
	 */
	inactivityTimeout("nonNegativeInteger"),
	/**
	 * The maximum number of concurrent sessions this service can host.
	 */
	maximumConcurrentSessions("positiveInteger"),
	/**
	 * Indicates if the service will drop the least recently used session
	 */
	autoDropLRUSessions("boolean"),
	/**
	 * The timeout for sensor initialization
	 */
	initializationTimeout("positiveInteger"),
	/**
	 * The timeout for sensor uninitialization
	 */
	uninitializationTimeout("positiveInteger"),
	/**
	 * The timeout for setting sensor configuration
	 */
	getConfigurationTimeout("positiveInteger"),
	/**
	 * The timeout for setting the sensor configuration
	 */
	setConfigurationTimeout("positiveInteger"),
	/**
	 * The timeout for capturing or for ending a capture
	 */
	endCaptureTimeout("positiveInteger"),
	/**
	 * The timeout for starting an asynchronous capture
	 */
	beginCaptureTimeout("positiveInteger"),
	/**
	 * The timeout for a capture operation
	 */
	captureTimeout("positiveInteger"),
	/**
	 * The timeout for getting the capture status
	 */
	getCaptureStatusTimeout("positiveInteger"),
	/**
	 * True if after a lock steal, the new lock holder can end (and get the data
	 * from) the last client's async capture. False if trying to end someone
	 * else's capture returns {@link Status#SENSOR_BUSY}.
	 */
	transferrableAsyncCapture("boolean"),
	/**
	 * The timeout for canceling an asynchronous capture
	 */
	cancelAsyncCaptureTimeout("positiveInteger"),
	/**
	 * The amount of time it takes this sensor to process after capturing
	 */
	postAcquisitionProcessingTime("nonNegativeInteger"),
	/**
	 * The amount of time that must elapse after a client's last action before
	 * the client's lock can be stolen
	 */
	lockStealingPreventionPeriod("nonNegativeInteger"),
	/**
	 * The maximum storage capacity of this server for storing captured data
	 */
	maximumStorageCapacity("positiveInteger"),
	/**
	 * Indicates if the least recently used capture will be dropped to make room
	 * for a new capture
	 */
	lruCaptureDataAutomaticallyDropped("boolean");

	/**
	 * The XML data type of this parameter
	 */
	private final QName type;

	/**
	 * Creates: a new service information key with the specified XML type
	 *
	 * @param type
	 *            the type
	 */
	private ServerInfoKey(QName type) {
		this.type = type;
	}

	/**
	 * Creates: a new service information key with the specified type name
	 * interpreted in the {@link XMLConstants#W3C_XML_SCHEMA_NS_URI} name space
	 * with the prefix <code>xs</code>.
	 *
	 * @param typeName
	 *            the type name
	 */
	private ServerInfoKey(String typeName) {
		this(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, typeName, "xs"));
	}

	/**
	 * @return the XML type of this parameter
	 */
	public QName type() {

		return type;
	}
}