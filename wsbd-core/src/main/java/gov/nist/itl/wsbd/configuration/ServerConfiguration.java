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

import java.math.BigInteger;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.oasis_open.docs.bioserv.ns.wsbd_1.Dictionary;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Status;

import gov.nist.itl.wsbd.dictionary.DictionaryWrapper;
import gov.nist.itl.wsbd.dictionary.InvalidDictionaryException;

/**
 * Represents: the configuration of the service. Contains values for the
 * parameters described in {@link ServerInformation}.
 *
 * @author Jacob Glueck
 *
 */
public class ServerConfiguration extends DictionaryWrapper<Object> {

	/**
	 * Default UID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates: a new service configuration with the specified values. Requires:
	 * the dictionary must contain values for all the keys in
	 * {@link ServerInfoKey}. Throws an {@link InvalidDictionaryException} if
	 * not.
	 *
	 * @param dictionary
	 *            the values
	 * @throws InvalidDictionaryException
	 *             If the dictionary does not contain values for all the keys in
	 *             {@link ServerInfoKey}.
	 */
	public ServerConfiguration(Dictionary dictionary) throws InvalidDictionaryException {
		super(dictionary, Object.class);
		// This map may only contain keys in ServiceInfoKey
		if (size() != ServerInfoKey.values().length) {
			throw new InvalidDictionaryException("This configuration contains the keys: " + keySet() + " but may only contain the keys: " + Arrays.toString(ServerInfoKey.values()));
		}
		for (ServerInfoKey key : ServerInfoKey.values()) {
			if (!containsKey(key.toString())) {
				throw new InvalidDictionaryException("Must contain value for key: " + key.toString());
			}
		}
	}

	/**
	 * XML type: xs:nonNegativeInteger
	 *
	 * @return the minimum time that must elapse before a session is terminated
	 *         due to inactivity, in milliseconds
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 * @throws ArithmeticException
	 *             if the timeout is too big
	 */
	public Duration inactivityTimeout() {
		
		return millisDuration(ServerInfoKey.inactivityTimeout);
	}

	/**
	 * XML type: xs:positiveInteger
	 *
	 * @return the maximum number of concurrent sessions this server can support
	 *         before either creating new sessions will fail or the least
	 *         recently used session will be terminated depending on the value
	 *         of {@link #autoDropLRUSessions()}.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 */
	public BigInteger maximumConcurrentSessions() {
		
		return (BigInteger) get(ServerInfoKey.maximumConcurrentSessions.toString());
	}

	/**
	 * XML type: xs:boolean
	 *
	 * @return true if when the server has the maximum number of registered
	 *         clients it will drop the least recently used client to add a new
	 *         one.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 */
	public boolean autoDropLRUSessions() {
		
		return (Boolean) get(ServerInfoKey.autoDropLRUSessions.toString());
	}
	
	/**
	 * XML type: xs:positiveInteger
	 *
	 * @return the amount of time before sensor initialization is canceled.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 * @throws ArithmeticException
	 *             if the timeout is too big
	 */
	public Duration initializationTimeout() {
		
		return millisDuration(ServerInfoKey.initializationTimeout);
	}

	/**
	 * XML type: xs:positiveInteger
	 *
	 * @return the amount of time before sensor uninitialization is canceled.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 * @throws ArithmeticException
	 *             if the timeout is too big
	 */
	public Duration uninitializationTimeout() {
		
		return millisDuration(ServerInfoKey.uninitializationTimeout);
	}

	/**
	 * XML type: xs:positiveInteger
	 *
	 * @return the amount of time before getting the sensor configuration is
	 *         canceled.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 * @throws ArithmeticException
	 *             if the timeout is too big
	 */
	public Duration getConfigurationTimeout() {
		
		return millisDuration(ServerInfoKey.getConfigurationTimeout);
	}

	/**
	 * XML type: xs:positiveInteger
	 *
	 * @return the amount of time before setting the sensor configuration is
	 *         canceled.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 * @throws ArithmeticException
	 *             if the timeout is too big
	 */
	public Duration setConfigurationTimeout() {
		
		return millisDuration(ServerInfoKey.setConfigurationTimeout);
	}
	
	/**
	 * XML type: xs:positiveInteger
	 *
	 * @return the amount of time before a capture is canceled in milliseconds.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 * @throws ArithmeticException
	 *             if the timeout is too big
	 */
	public Duration endCaptureTimeout() {
		
		return millisDuration(ServerInfoKey.endCaptureTimeout);
	}
	
	/**
	 * XML type: xs:positiveInteger
	 *
	 * @return the amount of time before a capture is canceled in milliseconds.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 * @throws ArithmeticException
	 *             if the timeout is too big
	 */
	public Duration beginCaptureTimeout() {
		
		return millisDuration(ServerInfoKey.beginCaptureTimeout);
	}
	
	/**
	 * XML type: xs:positiveInteger
	 *
	 * @return the amount of time before a capture is canceled in milliseconds.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 * @throws ArithmeticException
	 *             if the timeout is too big
	 */
	public Duration captureTimeout() {
		
		return millisDuration(ServerInfoKey.captureTimeout);
	}
	
	/**
	 *
	 * @return True if after a lock steal, the new lock holder can end (and get
	 *         the data from) the last client's async capture. False if trying
	 *         to end someone else's capture returns {@link Status#SENSOR_BUSY}.
	 */
	public Boolean transferrableAsyncCapture() {
		return (Boolean) get(ServerInfoKey.transferrableAsyncCapture.toString());
	}
	
	/**
	 * XML type: xs:positiveInteger
	 *
	 * @return the amount of time before a capture is canceled in milliseconds.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 * @throws ArithmeticException
	 *             if the timeout is too big
	 */
	public Duration getCaptureStatusTimeout() {
		
		return millisDuration(ServerInfoKey.getCaptureStatusTimeout);
	}

	/**
	 * XML type: xs:positiveInteger
	 *
	 * @return the amount of time before canceling the asynchronous capture
	 *         times out.
	 */
	public Duration cancelAsyncCaptureTimeout() {
		return millisDuration(ServerInfoKey.cancelAsyncCaptureTimeout);
	}

	/**
	 * XML type: xs:nonNegativeInteger
	 *
	 * @return the amount of time to expect the server to require to process a
	 *         capture after the capture is completed in milliseconds.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 * @throws ArithmeticException
	 *             if the timeout is too big
	 */
	public Duration postAcquisitionProcessingTime() {
		
		return millisDuration(ServerInfoKey.postAcquisitionProcessingTime);
	}
	
	/**
	 * XML type: xs:nonNegativeInteger
	 *
	 * @return the minimum amount of time that must elapse from when a client
	 *         last is active to when another client can steal his lock, in
	 *         milliseconds.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 * @throws ArithmeticException
	 *             if the timeout is too big
	 */
	public Duration lockStealingPreventionPeriod() {
		
		return millisDuration(ServerInfoKey.lockStealingPreventionPeriod);
	}

	/**
	 * XML type: xs:positiveInteger
	 *
	 * @return the available storage capacity, in bytes.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 */
	public BigInteger maximumStorageCapacity() {
		
		return (BigInteger) get(ServerInfoKey.maximumStorageCapacity.toString());
	}
	
	/**
	 * XML type: xs:boolean
	 *
	 * @return true if the server will automatically delete the least recently
	 *         used capture to make room for a new capture when the service is
	 *         at its maximum storage capacity.
	 * @throws ClassCastException
	 *             if there is a type error in the server configuration
	 */
	public boolean lruCaptureDataAutomaticallyDropped() {
		
		return (Boolean) get(ServerInfoKey.lruCaptureDataAutomaticallyDropped.toString());
	}
	
	/**
	 * @param key
	 *            the key
	 * @return the duration, assuming the value in the map is in milliseconds.
	 * @throws ArithmeticException
	 *             if the value in the map is too big
	 */
	private Duration millisDuration(ServerInfoKey key) {

		return Duration.of(((BigInteger) get(key.toString())).longValueExact(), ChronoUnit.MILLIS);
	}
}
