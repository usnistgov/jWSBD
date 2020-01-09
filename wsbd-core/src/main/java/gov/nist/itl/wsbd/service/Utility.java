/*----------------------------------------------------------------------------------------------------+
|                             National Institute of Standards and Technology                          |
|                                        Biometric Clients Lab                                        |
+-----------------------------------------------------------------------------------------------------+
 File author(s):
      Kevin Mangold (kevin.mangold@nist.gov)
      Jaocb Glueck (jacob.glueck@nist.gov)

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;

import org.oasis_open.docs.bioserv.ns.wsbd_1.Dictionary;
import org.oasis_open.docs.bioserv.ns.wsbd_1.ObjectFactory;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Result;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Status;
import org.oasis_open.docs.bioserv.ns.wsbd_1.UuidArray;

/**
 * Contains: utility methods
 *
 * @author Kevin Mangold
 * @author Jacob Glueck
 *
 */
public class Utility {
	
	/**
	 * The object factory used to create things
	 */
	public static final ObjectFactory objectFactory = new ObjectFactory();
	/**
	 * The classes used for binding XML
	 */
	public static final String contextPath = "org.oasis_open.docs.bioserv.ns.wsbd_1";

	/**
	 * Prevent construction
	 */
	private Utility() {
	}
	
	/**
	 * Creates: a new result with the specified status and message
	 *
	 * @param status
	 *            the status
	 * @param message
	 *            the message
	 * @return the result
	 */
	public static Result result(Status status, String message) {

		Result result = new Result();
		result.setStatus(status);
		result.setMessage(Utility.objectFactory.createResultMessage(message));
		return result;
	}
	
	/**
	 * Creates: a result with the specified status and no message
	 *
	 * @param status
	 *            the status
	 * @return the result
	 */
	public static Result result(Status status) {
		
		return Utility.result(status, "");
	}
	
	/**
	 * Effect: sets the session ID of the result to the specified UUID.
	 *
	 * @param result
	 *            the result to modify
	 * @param id
	 *            the UUID
	 */
	public static void setResultSessionID(Result result, UUID id) {

		result.setSessionId(Utility.objectFactory.createResultSessionId(id.toString()));
	}

	/**
	 * Effect: sets the session ID of the result to the specified UUID.
	 *
	 * @param result
	 *            the result to modify
	 * @param in
	 *            the input stream to get the sensor data from
	 * @throws IOException
	 *             if there is a problem
	 */
	public static void setSensorData(Result result, InputStream in) throws IOException {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Utility.drain(in, out);
		result.setSensorData(Utility.objectFactory.createResultSensorData(out.toByteArray()));
	}
	
	/**
	 * Effect: deletes any sensor data
	 *
	 * @param result
	 *            the result to modify
	 */
	public static void removeSensorData(Result result) {
		result.setSensorData(null);
	}
	
	/**
	 * Effect: sets the capture IDs for a result
	 *
	 * @param result
	 *            the result
	 * @param captureIDs
	 *            the capture IDs
	 */
	public static void setCaptureIDs(Result result, List<UUID> captureIDs) {
		UuidArray ids = new UuidArray();
		for (UUID id : captureIDs) {
			ids.getElement().add(id.toString());
		}
		result.setCaptureIds(Utility.objectFactory.createResultCaptureIds(ids));
	}

	/**
	 * Effect: reads all bytes from the input stream into the output stream.
	 * Closes both streams when done.
	 *
	 * @param in
	 *            the input stream
	 * @param out
	 *            the output stream
	 * @throws IOException
	 *             if there is a problem
	 */
	public static void drain(InputStream in, OutputStream out) throws IOException {
		int read;
		byte[] buf = new byte[1024];
		while ((read = in.read(buf)) != -1) {
			out.write(buf, 0, read);
		}
		out.close();
		in.close();
	}

	/**
	 * Effect: saves the result as XML to the specified output stream. Closes
	 * the stream when done.
	 *
	 * @param result
	 *            the result
	 * @param out
	 *            the output stream
	 * @throws JAXBException
	 *             if there is a problem
	 * @throws IOException
	 *             if there is a problem
	 */
	public static void marshal(JAXBElement<?> result, OutputStream out) throws JAXBException, IOException {
		JAXBContext jaxbContext = JAXBContext.newInstance(Utility.contextPath);
		Marshaller marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.marshal(result, out);
		out.close();
	}
	
	/**
	 * Effect: unmarshals the result object stored in the specified input
	 * stream. Closes the input stream when done.
	 *
	 * @param in
	 *            the input stream
	 * @return the object
	 * @throws JAXBException
	 *             if there is a problem
	 * @throws IOException
	 *             if there is a problem
	 */
	public static Result unmarshalResult(InputStream in) throws JAXBException, IOException {
		return Utility.unmarshal(in, Result.class);
	}

	/**
	 * Effect: unmarshals the result object stored in the specified input
	 * stream. Closes the input stream when done.
	 *
	 * @param <T>
	 *            the type of thing to unmarshal
	 *
	 * @param in
	 *            the input stream
	 * @param type
	 *            the type of the output
	 * @return the object
	 * @throws JAXBException
	 *             if there is a problem
	 * @throws IOException
	 *             if there is a problem
	 */
	public static <T> T unmarshal(InputStream in, Class<T> type) throws JAXBException, IOException {
		JAXBContext jaxbContext = JAXBContext.newInstance(Utility.contextPath);
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		T thing = unmarshaller.unmarshal(new StreamSource(in), type).getValue();
		in.close();
		return thing;
	}
	
	/**
	 * Effect: creates a result metadata dictionary element
	 *
	 * @param d
	 *            the dictionary
	 * @return the dictionary element
	 */
	public static JAXBElement<Dictionary> createDictionaryElement(Dictionary d) {
		return Utility.objectFactory.createResultMetadata(d);
	}
	
	/**
	 * Effect: converts a result to an element
	 *
	 * @param result
	 *            the result
	 * @return the element
	 */
	public static JAXBElement<Result> wrap(Result result) {
		return Utility.objectFactory.createResult(result);
	}
	
	/**
	 * Effect: sets the metadata of the result to the specified metadata
	 *
	 * @param result
	 *            the result to modify
	 * @param metadata
	 *            the metadata
	 */
	public static void setResultMetadata(Result result, Dictionary metadata) {

		result.setMetadata(Utility.objectFactory.createResultMetadata(metadata));
	}
	
	/**
	 * Effect: converts a result to a nice (XML) string representation
	 *
	 * @param result
	 *            the result
	 * @return a nice string
	 */
	public static String elementToString(Result result) {
		return Utility.elementToString(Utility.wrap(result));
	}

	/**
	 * Effect: converts an XML element to a nice (XML) string representation
	 *
	 * @param thing
	 *            the result
	 * @return a nice string
	 */
	public static String elementToString(JAXBElement<?> thing) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try {
			Utility.marshal(thing, bytes);
		} catch (JAXBException | IOException e) {
			// This should never happen
			assert false;
			return null;
		}
		return new String(bytes.toByteArray());
	}
	
	/**
	 * Effect: prints the result to System.out
	 *
	 * @param result
	 *            the reuslt
	 */
	public static void printResult(Result result) {
		System.out.println(Utility.elementToString(result));
	}
	
	/**
	 * Effect: extracts the session ID from the result if there is one,
	 * otherwise returns null.
	 *
	 * @param result
	 *            the result
	 * @return the session ID.
	 */
	public static UUID session(Result result) {
		try {
			return UUID.fromString(result.getSessionId().getValue());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Effect: gets the capture IDs from the result if there are some, otherwise
	 * returns null.
	 *
	 * @param result
	 *            the result
	 * @return the capture IDs
	 */
	public static List<UUID> captures(Result result) {
		try {
			return result.getCaptureIds().getValue().getElement().stream().map((string) -> UUID.fromString(string)).collect(Collectors.toList());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Converts an instant to an XML date
	 *
	 * @param instant
	 *            the instant
	 * @return the XML date
	 */
	public static XMLGregorianCalendar fromInstant(Instant instant) {
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())));
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
}
