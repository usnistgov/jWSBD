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

package gov.nist.itl.wsbd.service;

import java.util.UUID;

import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.oasis_open.docs.bioserv.ns.wsbd_1.Dictionary;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Result;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Status;

/**
 * Represents: a Web Services for Biometric Devices (WS-BD) service
 *
 * For the requirements of all the methods, see the WS-BD specification.<br>
 * In order for subclasses to use this class as a web service resource,
 * subclasses must have an <code>@Path("servicepath")</code> annotation.
 *
 * @author Kevin Mangold
 * @author Jacob Glueck
 *
 */
@Singleton
public interface ISensorService {
	
	/**
	 * Effect: registers a new client
	 *
	 * @return the result
	 */
	@POST
	@Produces("text/xml")
	@Path("register")
	JAXBElement<Result> IRegister();
	
	/**
	 * Effect: unregisters a client
	 *
	 * @param sessionId
	 *            the client's session ID
	 * @return the result
	 */
	@DELETE
	@Produces("text/xml")
	@Path("register/{sessionId}")
	JAXBElement<Result> IUnregister(@PathParam("sessionId") UUID sessionId);
	
	/**
	 * Effect: tries to obtain a lock on the sensor for the specified client
	 *
	 * @param sessionId
	 *            the client's session ID
	 * @return the result
	 */
	@POST
	@Produces("text/xml")
	@Path("lock/{sessionId}")
	JAXBElement<Result> ILock(@PathParam("sessionId") UUID sessionId);
	
	/**
	 * Effect: tries to steal the lock from a client
	 *
	 * @param sessionId
	 *            the client's (the client trying to steal the lock) session ID
	 * @return the result
	 */
	@PUT
	@Produces("text/xml")
	@Path("lock/{sessionId}")
	JAXBElement<Result> IStealLock(@PathParam("sessionId") UUID sessionId);
	
	/**
	 * Effect: tries to release the lock on a sensor
	 *
	 * @param sessionId
	 *            the client's ID
	 * @return the result
	 */
	@DELETE
	@Produces("text/xml")
	@Path("lock/{sessionId}")
	JAXBElement<Result> IUnlock(@PathParam("sessionId") UUID sessionId);
	
	/**
	 * Gets the service information
	 *
	 * @return the service information
	 */
	@GET
	@Produces("text/xml")
	@Path("info")
	JAXBElement<Result> IGetServiceInformation();
	
	/**
	 * Effect: attempts to initialize the sensor
	 *
	 * @param sessionId
	 *            the client's ID
	 * @return the result
	 */
	@POST
	@Produces("text/xml")
	@Path("initialize/{sessionId}")
	JAXBElement<Result> IInitialize(@PathParam("sessionId") UUID sessionId);
	
	/**
	 * Effect: attempts to uninitialize the sensor
	 *
	 * @param sessionId
	 *            the client's ID
	 * @return the result
	 */
	@DELETE
	@Produces("text/xml")
	@Path("initialize/{sessionId}")
	JAXBElement<Result> IUninitialize(@PathParam("sessionId") UUID sessionId);
	
	/**
	 * Gets the sensor's configuration
	 *
	 * @param sessionId
	 *            the client'd ID
	 * @return the configuration
	 */
	@GET
	@Produces("text/xml")
	@Path("configure/{sessionId}")
	JAXBElement<Result> IGetConfiguration(@PathParam("sessionId") UUID sessionId);
	
	/**
	 * Effect: sets the sensor's configuration
	 *
	 * @param sessionId
	 *            the client's ID
	 * @param configuration
	 *            the configuration
	 * @return the result
	 */
	@POST
	@Produces("text/xml")
	@Path("configure/{sessionId}")
	JAXBElement<Result> ISetConfiguration(@PathParam("sessionId") UUID sessionId,
			Dictionary configuration);

	/**
	 * Effect: captures biometric data
	 *
	 * @param sessionId
	 *            the client's ID
	 * @return the result
	 */
	@POST
	@Produces("text/xml")
	@Path("capture/{sessionId}")
	JAXBElement<Result> ICapture(@PathParam("sessionId") UUID sessionId);

	/**
	 * Effect: begins asynchronous capture of biometric data. This method must
	 * return as soon as capture as started. For a device which does not support
	 * asynchronous capture, this method must return a status of
	 * {@link Status#SUCCESS}.
	 *
	 * @param sessionId
	 *            the client's ID
	 * @return the result
	 */
	@POST
	@Produces("text/xml")
	@Path("capture/{sessionId}/async")
	JAXBElement<Result> IBeginCapture(@PathParam("sessionId") UUID sessionId);

	/**
	 * Effect: ends the asynchronous capture of biometric data. This method must
	 * block until the capture is completed. For a device which does not support
	 * asynchronous capture, this method must perform the entire capture
	 * sequence.
	 *
	 * @param sessionId
	 *            the client's ID
	 * @return the result
	 */
	@PUT
	@Produces("text/xml")
	@Path("capture/{sessionId}/async")
	JAXBElement<Result> IEndCapture(@PathParam("sessionId") UUID sessionId);
	
	/**
	 * Gets the current sensor status. The sensor status shows what the sensor
	 * is doing right now.
	 *
	 * @return a dictionary with one item with the key sensor status and with a
	 *         value of the status.
	 */
	@GET
	@Produces("text/xml")
	@Path("/status")
	JAXBElement<Result> IGetSensorStatus();
	
	/**
	 * Gets the captured biometric data
	 *
	 * @param captureId
	 *            the capture ID
	 * @return the data
	 */
	@GET
	@Produces("text/xml")
	@Path("download/{captureId}")
	JAXBElement<Result> IDownload(@PathParam("captureId") UUID captureId);
	
	/**
	 * Gets the download information for a specific capture
	 *
	 * @param captureId
	 *            the capture ID
	 * @return the result
	 */
	@GET
	@Produces("text/xml")
	@Path("download/{captureId}/info")
	JAXBElement<Result> IGetDownloadInformation(@PathParam("captureId") UUID captureId);
	
	/**
	 * Gets a reduced quality version of a specific capture
	 *
	 * @param captureId
	 *            the capture ID
	 * @param maxSize
	 *            the maximum size
	 * @return the result
	 */
	@GET
	@Produces("text/xml")
	@Path("download/{captureId}/{maxSize}")
	JAXBElement<Result> IThriftyDownload(@PathParam("captureId") UUID captureId, @PathParam("maxSize") String maxSize);

	/**
	 * Gets just the raw image data
	 *
	 * @param captureId
	 *            the capture ID
	 * @return the result
	 */
	@GET
	@Path("download/{captureId}/raw")
	Response IRawDownload(@PathParam("captureId") UUID captureId);
	
	/**
	 * Effect: attempts to cancel the currently running sensor operation
	 *
	 * @param sessionId
	 *            the client's ID
	 * @return the result
	 */
	@POST
	@Produces("text/xml")
	@Path("cancel/{sessionId}")
	JAXBElement<Result> ICancel(@PathParam("sessionId") UUID sessionId);

	/**
	 * The prefix for the stream
	 */
	public static String STREAM_PREFIX = "stream";
	
	/**
	 * Effect: gets the stream at the specified name. No lock is required. If no
	 * such stream exists, this method returns a 404. If a stream exists but a
	 * lock is required, this method returns 403 Forbidden.
	 *
	 * @param streamName
	 *            the stream name
	 * @return the stream
	 */
	@GET
	@Path(ISensorService.STREAM_PREFIX + "/{streamName}")
	Response IStream(@PathParam("streamName") String streamName);
	
	/**
	 * Effect: gets the stream at the specified name. A lock is required. If no
	 * such stream exists, this method returns a 404. If a stream exists but a
	 * lock is required, this method returns 403 Forbidden.
	 *
	 * @param streamName
	 *            the stream name
	 * @param sessionId
	 *            the session ID
	 * @return the stream
	 */
	@GET
	@Path(ISensorService.STREAM_PREFIX + "/{streamName}/{sessionId}")
	Response IStreamLocked(@PathParam("streamName") String streamName, @PathParam("sessionId") UUID sessionId);
}