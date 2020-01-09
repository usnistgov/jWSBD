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

package gov.nist.itl.wsbd.tests;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Test;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Result;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Status;

import gov.nist.itl.wsbd.service.Utility;

/**
 * Represents: tests that test the registration system
 *
 * @author Jacob Glueck
 *
 */
public class RegistrationTests extends WSBDTest {
	
	@Test
	public void RegistrationIsSuccessul() throws MalformedURLException, IOException, JAXBException {
		Result result = testClient.register();
		
		Assert.assertNotNull(result);
		Assert.assertEquals(Status.SUCCESS, result.getStatus());
		Assert.assertNotNull(result.getSessionId());
	}

	@Test
	public void UnregistrationIsSuccessful() throws MalformedURLException, IOException, JAXBException {
		Result result = testClient.register();
		UUID sessionId = Utility.session(result);
		Assert.assertNotNull(sessionId);
		result = testClient.unregister(sessionId);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(Status.SUCCESS, result.getStatus());
	}

	@Test
	public void UnregistrationWorksIfNotRegistered() throws MalformedURLException, IOException, JAXBException {
		Result result = testClient.unregister(UUID.randomUUID());
		
		Assert.assertNotNull(result);
		Assert.assertEquals(Status.SUCCESS, result.getStatus());
	}

	@Test
	public void UnregistrationFailsIfSensorBusy() throws MalformedURLException, IOException, JAXBException {
		Result result = testClient.register();
		UUID sessionId = Utility.session(result);
		Assert.assertNotNull(sessionId);
		Assert.assertEquals(Status.SUCCESS, result.getStatus());

		result = testClient.lock(sessionId);
		Assert.assertEquals(Status.SUCCESS, result.getStatus());
		
		result = testClient.beginCapture(sessionId);
		Assert.assertEquals(Status.SUCCESS, result.getStatus());
		
		result = testClient.unregister(sessionId);
		Assert.assertEquals(Status.SENSOR_BUSY, result.getStatus());
		
		result = testClient.endCapture(sessionId);
		Assert.assertEquals(Status.SUCCESS, result.getStatus());
		
		result = testClient.unregister(sessionId);
		Assert.assertEquals(Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void UnregistrationWorksIfLockedButNotUsingSensor() throws MalformedURLException, IOException, JAXBException {
		Result result = testClient.register();
		UUID sessionId = Utility.session(result);
		Assert.assertNotNull(sessionId);
		Assert.assertEquals(Status.SUCCESS, result.getStatus());

		result = testClient.lock(sessionId);
		Assert.assertEquals(Status.SUCCESS, result.getStatus());
		
		result = testClient.unregister(sessionId);
		Assert.assertEquals(Status.SUCCESS, result.getStatus());
	}
	
	@Test
	public void lruSessionIsUnregistered() throws MalformedURLException, IOException, JAXBException, InterruptedException {
		Assert.assertTrue(serviceConfiguraiton.serverConfiguration().autoDropLRUSessions());
		Result result = testClient.register();
		UUID sessionId = Utility.session(result);
		Assert.assertNotNull(sessionId);
		Assert.assertEquals(Status.SUCCESS, result.getStatus());

		// Register up to the maximum amount and make sure they all work
		for (int x = 0; x < serviceConfiguraiton.serverConfiguration().maximumConcurrentSessions().intValueExact() - 1; x++) {
			Result r = testClient.register();
			UUID s = Utility.session(r);
			Assert.assertEquals(Status.SUCCESS, r.getStatus());
			Assert.assertNotNull(s);
		}
		
		// Try to register the last one and make sure it fails (we should be
		// within the inactivity timeout)
		Result r = testClient.register();
		UUID s = Utility.session(r);
		Assert.assertEquals(Status.FAILURE, r.getStatus());
		Assert.assertNull(s);

		// Wait for the inactivity timeout and some fudge
		Thread.sleep(serviceConfiguraiton.serverConfiguration().inactivityTimeout().toMillis() + WSBDTest.FUDGE);

		r = testClient.register();
		s = Utility.session(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());
		Assert.assertNotNull(s);

		// Make sure the original is not registered by trying to lock it
		result = testClient.lock(sessionId);
		Assert.assertEquals(Status.INVALID_ID, result.getStatus());
		
	}
}