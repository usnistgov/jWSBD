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

package gov.nist.itl.wsbd.tests;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Test;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Dictionary;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Result;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Status;

import gov.nist.itl.wsbd.configuration.SensorInfoKey;
import gov.nist.itl.wsbd.configuration.ServerInfoKey;
import gov.nist.itl.wsbd.configuration.ServerStateKey;
import gov.nist.itl.wsbd.dictionary.DictionaryWrapper;
import gov.nist.itl.wsbd.dictionary.InvalidDictionaryException;
import gov.nist.itl.wsbd.service.Utility;
import gov.nist.itl.wsbd.streaming.StreamKey;

/**
 * Represents: tests that test configuring
 *
 * @author Kevin Mangold
 * @author Jacob Glueck
 *
 */
public class ConfigurationTests extends WSBDTest {

	@Test
	public void GetConfigurationIsSuccessful() throws InvalidDictionaryException, MalformedURLException, IOException, JAXBException {

		Result r = testClient.register();
		UUID s = Utility.session(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());
		Assert.assertNotNull(s);

		r = testClient.lock(s);
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());
		
		r = testClient.getConfiguration(s);
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());

		// Check that the metadata contains required fields
		DictionaryWrapper<Object> metadata = new DictionaryWrapper<>(r.getMetadata().getValue(), Object.class);
		List<String> keys = new LinkedList<>();
		keys.addAll(Arrays.asList(ServerInfoKey.values()).stream().map((thing) -> thing.toString()).collect(Collectors.toList()));
		keys.addAll(Arrays.asList(SensorInfoKey.values()).stream().map((thing) -> thing.toString()).collect(Collectors.toList()));
		keys.addAll(Arrays.asList(ServerStateKey.values()).stream().map((thing) -> thing.toString()).collect(Collectors.toList()));
		keys.addAll(Arrays.asList(StreamKey.values()).stream().map((thing) -> thing.toString()).collect(Collectors.toList()));
		metadata.keySet().retainAll(keys);
		Assert.assertEquals(keys.size(), metadata.size());

		r = testClient.unlock(s);
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());
	}
	
	@Test
	public void GetInformationIsSuccessful() throws InvalidDictionaryException, MalformedURLException, IOException, JAXBException {

		Result r = testClient.register();
		UUID s = Utility.session(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());
		Assert.assertNotNull(s);
		
		r = testClient.getServiceInformation();
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());

		// Check that the metadata contains required fields
		DictionaryWrapper<Object> metadata = new DictionaryWrapper<>(r.getMetadata().getValue(), Object.class);
		List<String> keys = new LinkedList<>();
		keys.addAll(Arrays.asList(ServerInfoKey.values()).stream().map((thing) -> thing.toString()).collect(Collectors.toList()));
		keys.addAll(Arrays.asList(SensorInfoKey.values()).stream().map((thing) -> thing.toString()).collect(Collectors.toList()));
		keys.addAll(Arrays.asList(ServerStateKey.values()).stream().map((thing) -> thing.toString()).collect(Collectors.toList()));
		keys.addAll(Arrays.asList(StreamKey.values()).stream().map((thing) -> thing.toString()).collect(Collectors.toList()));
		metadata.keySet().retainAll(keys);
		Assert.assertEquals(keys.size(), metadata.size());
	}

	@Test
	public void GetConfiguirationOfAnInvalidIdFails() throws MalformedURLException, IOException, JAXBException {
		Result r = testClient.getConfiguration(UUID.randomUUID());
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.INVALID_ID, r.getStatus());
	}

	@Test
	public void GetConfigurationRequiresLock() throws MalformedURLException, IOException, JAXBException {
		Result r = testClient.register();
		UUID s = Utility.session(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());
		Assert.assertNotNull(s);
		
		r = testClient.getConfiguration(s);
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.LOCK_NOT_HELD, r.getStatus());
	}
	
	@Test
	public void SetConfigurationIsSuccessful() throws InvalidDictionaryException, MalformedURLException, IOException, JAXBException {

		Result r = testClient.register();
		UUID s = Utility.session(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());
		Assert.assertNotNull(s);

		r = testClient.lock(s);
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());
		
		r = testClient.setConfiguration(s, new Dictionary());
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());

		r = testClient.unlock(s);
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());
	}
	
	@Test
	public void SetConfigurationOfAnInvalidIdFails() throws MalformedURLException, IOException, JAXBException {
		Result r = testClient.setConfiguration(UUID.randomUUID(), new Dictionary());
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.INVALID_ID, r.getStatus());
	}

	@Test
	public void SetConfigurationRequiresLock() throws MalformedURLException, IOException, JAXBException {
		Result r = testClient.register();
		UUID s = Utility.session(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());
		Assert.assertNotNull(s);
		
		r = testClient.setConfiguration(s, new Dictionary());
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.LOCK_NOT_HELD, r.getStatus());
	}
}