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

package gov.nist.itl.wsbd.streaming;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.oasis_open.docs.bioserv.ns.wsbd_1.Resource;
import org.oasis_open.docs.bioserv.ns.wsbd_1.ResourceArray;

/**
 * Represents: the configuration of a live sensor stream
 *
 * @author Jacob Glueck
 *
 */
public class StreamHandle {

	/**
	 * The placeholder in the URL if a lock is required. For example, the stream
	 * http://192.168.1.1/stream/uuid will require a lock because its third
	 * component is this string.
	 */
	public static final String LOCK_REQUIRED_PLACEHOLDER = "uuid";

	/**
	 * The name of the stream
	 */
	private final String name;
	/**
	 * The content type for this stream
	 */
	private final String contentType;
	/**
	 * True if this stream requires a lock
	 */
	private final boolean lockRequired;

	/**
	 * Creates: a new stream configuration with the specified information
	 *
	 * @param name
	 *            the name
	 * @param contentType
	 *            the content type
	 * @param lockRequired
	 *            true if a lock is required to view this stream
	 */
	public StreamHandle(String name, String contentType, boolean lockRequired) {
		this.name = name;
		this.contentType = contentType;
		this.lockRequired = lockRequired;
	}

	/*
	 * Eclipse generated
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (contentType == null ? 0 : contentType.hashCode());
		result = prime * result + (name == null ? 0 : name.hashCode());
		return result;
	}

	/*
	 * Eclipse generated
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		StreamHandle other = (StreamHandle) obj;
		if (contentType == null) {
			if (other.contentType != null) {
				return false;
			}
		} else if (!contentType.equals(other.contentType)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
	
	/**
	 * @return the name
	 */
	public String name() {
		return name;
	}
	
	/**
	 * @return the contentType
	 */
	public String contentType() {
		return contentType;
	}
	
	/**
	 * @return the lockRequired
	 */
	public boolean lockRequired() {
		return lockRequired;
	}
	
	@Override
	public String toString() {
		return "StreamHandle [name=" + name + ", contentType=" + contentType + ", lockRequired=" + lockRequired + "]";
	}

	/**
	 * Reads a resource array and creates stream handles. The URIs in the
	 * resource must be of the form
	 * <code>protocol://address/more/parts/of/the/address/ISensorService.STREAM_PREFIX/Stream-Name</code>
	 * or
	 * <code>protocol://address/more/parts/of/the/address/ISensorService.STREAM_PREFIX/Stream-Name/StreamHandle.LOCK_REQUIRED_PLACEHOLDER</code>
	 * if a lock is required to view this stream. Form example, the URI
	 * {@code http://192.168.1.1/service/stream/leftPinky} would be a public
	 * stream and the URI
	 * {@code http://192.168.1.1/service/stream/leftPinkyToe/uuid} would require
	 * a lock.
	 *
	 * @param array
	 *            the resource array
	 * @return the described stream handles
	 * @throws IllegalResourceException
	 *             if there is a problem parsing
	 */
	public static Set<StreamHandle> handlesFromResourceArray(ResourceArray array) throws IllegalResourceException {
		Set<StreamHandle> handles = new HashSet<>();
		for (Resource resource : array.getElement()) {
			String uri = resource.getUri();
			String[] split = uri.split(Pattern.quote("://"));
			if (split.length != 2) {
				throw new IllegalResourceException("String must only contain one \"://\".");
			}
			split = split[1].split("\\/");
			if (split.length < 3) {
				throw new IllegalResourceException("The URI must contain at least 3 parts (for this WSBD server implementation). Parts: " + Arrays.toString(split));
			}
			boolean lockRequired = false;
			String name = split[split.length - 1];
			if (split[split.length - 1].equals(StreamHandle.LOCK_REQUIRED_PLACEHOLDER)) {
				if (split.length == 3) {
					throw new IllegalResourceException("No stream name specified. Parts: " + Arrays.toString(split));
				}
				lockRequired = true;
				name = split[split.length - 2];
			}
			
			String contentType = resource.getContentType().getValue();
			StreamHandle handleToAdd = new StreamHandle(name, contentType, lockRequired);
			if (handles.contains(handleToAdd)) {
				throw new IllegalResourceException("Duplicate stream: " + handleToAdd.toString());
			}
			handles.add(handleToAdd);
		}
		return handles;
	}
	
	/**
	 * Sends the resource array to
	 * {@link #handlesFromResourceArray(ResourceArray)}
	 *
	 * @param streamConfiguration
	 *            the stream configuration
	 * @return the stream handles
	 * @throws IllegalResourceException
	 *             if there is a problem
	 */
	public static Set<StreamHandle> handleFromStreamConfiguration(StreamConfiguration streamConfiguration) throws IllegalResourceException {
		return StreamHandle.handlesFromResourceArray(streamConfiguration.get(StreamKey.livePreview.toString()));
	}

}
