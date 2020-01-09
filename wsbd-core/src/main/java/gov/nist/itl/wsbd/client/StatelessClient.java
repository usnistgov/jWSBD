/*----------------------------------------------------------------------------------------------------+
|                             National Institute of Standards and Technology                          |
|                                        Biometric Clients Lab                                        |
+-----------------------------------------------------------------------------------------------------+
 File author(s):
      Kevin Mangold (kevin.mangold@nist.gov)
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

package gov.nist.itl.wsbd.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.oasis_open.docs.bioserv.ns.wsbd_1.Dictionary;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Result;
import org.oasis_open.docs.bioserv.ns.wsbd_1.SensorStatus;

import gov.nist.itl.wsbd.service.Utility;

/**
 * Represents: a test client which can call as the WSBD methods
 *
 * @author Jacob Glueck
 *
 */
public class StatelessClient {
	
	/**
	 * The base URL of the service. Does not contain a trailing slash
	 */
	private final String baseUrl;

	/**
	 * Creates: a new test client which tests the service at the specified URL.
	 * It does not matter if the base URL has or does not have a trailing slash.
	 *
	 * @param baseUrl
	 *            the base URL
	 */
	public StatelessClient(String baseUrl) {
		if (baseUrl.charAt(baseUrl.length() - 1) == '/') {
			this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		} else {
			this.baseUrl = baseUrl;
		}
	}

	/**
	 * Effect: calls the specified method at the specified URL with the
	 * specified HTTP method and no payload. Transforms the output into a
	 * {@link Result}.
	 *
	 * @param url
	 *            the URL
	 * @param method
	 *            the HTTP method
	 * @return the result
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result call(URL url, String method) throws IOException, JAXBException {
		return call(url, method, null);
	}

	/**
	 * Effect: calls the specified method at the specified URL with the
	 * specified HTTP method and an optional XML payload. Transforms the output
	 * into a {@link Result}.
	 *
	 * @param url
	 *            the URL
	 * @param method
	 *            the HTTP method
	 * @param payload
	 *            the XML payload. If null, no payload will be sent.
	 * @return the result
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result call(URL url, String method, JAXBElement<?> payload) throws IOException, JAXBException {
		return call(url, method, payload, (stream) -> {

			try {
				return Utility.unmarshalResult(stream);
			} catch (JAXBException e) {
				throw new IOException(e);
			}

		});
	}
	
	/**
	 * Effect: calls the specified method at the specified URL with the
	 * specified HTTP method and an optional XML payload. Transforms the result
	 * using the specified transformer.
	 *
	 * @param <R>
	 *            the type of result
	 * @param url
	 *            the URL
	 * @param method
	 *            the HTTP method
	 * @param payload
	 *            the XML payload. If null, no payload will be sent.
	 * @param transformer
	 *            the transformer to convert an input stream to the result type.
	 * @param close
	 *            true if the connection should be closed
	 * @return the result
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public <R> R call(URL url, String method, JAXBElement<?> payload, Transformer<R> transformer, boolean close) throws IOException, JAXBException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod(method);
		
		if (payload != null) {
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "text/xml");
			Utility.marshal(payload, connection.getOutputStream());
		}
		R result = transformer.transform(connection.getInputStream());
		if (close) {
			connection.disconnect();
		}
		return result;
	}
	
	/**
	 * Effect: calls the specified method at the specified URL with the
	 * specified HTTP method and an optional XML payload. Transforms the result
	 * using the specified transformer. Closes connection.
	 *
	 * @param <R>
	 *            the type of result
	 * @param url
	 *            the URL
	 * @param method
	 *            the HTTP method
	 * @param payload
	 *            the XML payload. If null, no payload will be sent.
	 * @param transformer
	 *            the transformer to convert an input stream to the result type.
	 * @return the result
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public <R> R call(URL url, String method, JAXBElement<?> payload, Transformer<R> transformer) throws IOException, JAXBException {
		return call(url, method, payload, transformer, true);
	}

	/**
	 * Effect: registers with the service
	 *
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result register() throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s", baseUrl, "register")), "POST");
	}

	/**
	 * Effect: unregisters
	 *
	 * @param sessionId
	 *            the session ID to unregister
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result unregister(UUID sessionId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s", baseUrl, "register", sessionId)), "DELETE");
	}
	
	/**
	 * Effect: locks
	 *
	 * @param sessionId
	 *            the session ID to lock
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result lock(UUID sessionId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s", baseUrl, "lock", sessionId)), "POST");
	}

	/**
	 * Effect: steals the lock
	 *
	 * @param sessionId
	 *            the session ID to steal the lock with
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result stealLock(UUID sessionId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s", baseUrl, "lock", sessionId)), "PUT");
	}

	/**
	 * Effect: unlocks
	 *
	 * @param sessionId
	 *            the session ID
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result unlock(UUID sessionId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s", baseUrl, "lock", sessionId)), "DELETE");
	}

	/**
	 * Effect: gets the service information
	 *
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result getServiceInformation() throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s", baseUrl, "info")), "GET");
	}

	/**
	 * Effect: initializes the sensor
	 *
	 * @param sessionId
	 *            the session ID
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result initialize(UUID sessionId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s", baseUrl, "initialize", sessionId)), "POST", null);
	}
	
	/**
	 * Effect: uninitializes the sensor
	 *
	 * @param sessionId
	 *            the session ID
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result uninitialize(UUID sessionId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s", baseUrl, "initialize", sessionId)), "DELETE", null);
	}

	/**
	 * Gets the configuration
	 *
	 * @param sessionId
	 *            the session ID
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result getConfiguration(UUID sessionId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s", baseUrl, "configure", sessionId)), "GET");
	}
	
	/**
	 * Effect: sets the configuration
	 *
	 * @param sessionId
	 *            the session ID
	 * @param configuration
	 *            the new configuration
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result setConfiguration(UUID sessionId, Dictionary configuration) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s", baseUrl, "configure", sessionId)), "POST", Utility.createDictionaryElement(configuration));
	}

	/**
	 * Effect: captures
	 *
	 * @param sessionId
	 *            the session ID
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result capture(UUID sessionId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s", baseUrl, "capture", sessionId)), "POST");
	}
	
	/**
	 * Effect: starts a capture
	 *
	 * @param sessionId
	 *            the session ID
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result beginCapture(UUID sessionId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s/%s", baseUrl, "capture", sessionId, "async")), "POST");
	}

	/**
	 * Effect: ends a capture
	 *
	 * @param sessionId
	 *            the session ID
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result endCapture(UUID sessionId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s/%s", baseUrl, "capture", sessionId, "async")), "PUT");
	}

	/**
	 * Effect: gets the capture status
	 *
	 * @param sessionId
	 *            the session ID
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public SensorStatus getStatus(UUID sessionId) throws MalformedURLException, IOException, JAXBException {
		Result result = call(new URL(String.format("%s/%s/", baseUrl, "status")), "GET");
		return SensorStatus.fromValue((String) result.getMetadata().getValue().getItem().get(0).getValue());
	}

	/**
	 * Effect: downloads a capture
	 *
	 * @param captureId
	 *            the capture ID
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result download(UUID captureId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s", baseUrl, "download", captureId)), "GET");
	}
	
	/**
	 * Effect: gets the download information
	 *
	 * @param captureId
	 *            the capture ID
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result getDownloadInformation(UUID captureId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s/info", baseUrl, "download", captureId)), "GET");
	}

	/**
	 * Effect: does a thrifty download
	 *
	 * @param captureId
	 *            the capture ID
	 * @param maxSize
	 *            the max size specification
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result thriftyDownload(UUID captureId, String maxSize) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s/%s", baseUrl, "download", captureId, maxSize)), "GET");
	}
	
	/**
	 *
	 * @param captureId
	 *            the capture ID
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public byte[] rawDownload(UUID captureId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s/%s", baseUrl, "download", captureId, "raw")), "GET", null, stream -> {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			Utility.drain(stream, bytes);
			return bytes.toByteArray();
		});
	}

	/**
	 *
	 * @param sessionId
	 *            the session ID
	 * @return the result
	 * @throws MalformedURLException
	 *             if the URL is malformed
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public Result cancel(UUID sessionId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s", baseUrl, "cancel", sessionId)), "POST");
	}

	/**
	 *
	 * @param streamName
	 *            the stream name
	 * @param captureId
	 *            the sessionId if the stream is locked
	 * @return the stream
	 * @throws MalformedURLException
	 *             if there is a problem
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public InputStream stream(String streamName, UUID captureId) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s/%s", baseUrl, "stream", streamName, captureId.toString())), "GET", null, stream -> stream, false);
	}
	
	/**
	 *
	 * @param streamName
	 *            the stream name
	 * @return the stream
	 * @throws MalformedURLException
	 *             if there is a problem
	 * @throws IOException
	 *             if there is a problem
	 * @throws JAXBException
	 *             if there is a problem
	 */
	public InputStream stream(String streamName) throws MalformedURLException, IOException, JAXBException {
		return call(new URL(String.format("%s/%s/%s", baseUrl, "stream", streamName)), "GET", null, stream -> stream, false);
	}
	
	/**
	 * Represents: a function the reads an input stream and produces a result.
	 *
	 * @author Jacob Glueck
	 *
	 * @param <R>
	 *            the type of result
	 */
	private interface Transformer<R> {
		
		/**
		 * Effect: transforms the data in the input stream into a result.
		 *
		 * @param in
		 *            the input stream
		 * @return the result
		 * @throws IOException
		 *             if there is a problem
		 */
		public R transform(InputStream in) throws IOException;
	}

}