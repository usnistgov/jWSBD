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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Test;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Result;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Status;

import gov.nist.itl.wsbd.service.Utility;

/**
 * Represents: tests that test canceling
 *
 * @author Kevin Mangold
 * @author Jacob Glueck
 *
 */
public class StreamTests extends WSBDTest {

	private final static String unlockedStream = "cows";
	private final static String lockedStrema = "trees";
	private final static String bogusStream = "fish";
	
	@Test
	public void unlockedStreamWorks() throws MalformedURLException, IOException, JAXBException, InterruptedException {
		InputStream stream = testClient.stream(StreamTests.unlockedStream);
		byte[] buf = new byte[StreamTests.unlockedStream.length()];
		stream.read(buf);
		stream.close();
		Assert.assertEquals(StreamTests.unlockedStream, new String(buf));
	}
	
	@Test
	public void lockedStreamWorks() throws MalformedURLException, IOException, JAXBException, InterruptedException {
		Result r = testClient.register();
		UUID s = Utility.session(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());
		Assert.assertNotNull(s);

		r = testClient.lock(s);
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());

		InputStream stream = testClient.stream(StreamTests.lockedStrema, s);
		byte[] buf = new byte[StreamTests.lockedStrema.length()];
		stream.read(buf);
		stream.close();
		Assert.assertEquals(StreamTests.lockedStrema, new String(buf));
	}
	
	@Test
	public void lockedStreamRequiresLock() throws MalformedURLException, IOException, JAXBException, InterruptedException {
		InputStream stream = testClient.stream(StreamTests.lockedStrema, UUID.randomUUID());
		byte[] buf = new byte[StreamTests.lockedStrema.length()];
		stream.read(buf);
		stream.close();
		Assert.assertNotEquals(StreamTests.lockedStrema, new String(buf));
	}

	@Test
	public void lockedStreamStopsWithUnlock() throws MalformedURLException, IOException, JAXBException, InterruptedException {
		Result r = testClient.register();
		UUID s = Utility.session(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());
		Assert.assertNotNull(s);

		r = testClient.lock(s);
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());

		InputStream stream = testClient.stream(StreamTests.lockedStrema, s);
		byte[] buf = new byte[StreamTests.lockedStrema.length()];
		stream.read(buf);
		Assert.assertEquals(StreamTests.lockedStrema, new String(buf));

		r = testClient.unlock(s);
		Assert.assertNotNull(r);
		Assert.assertEquals(Status.SUCCESS, r.getStatus());

		// As long as this test terminates, it passes
		while (stream.read() != -1) {

		}
	}
	
	@Test(expected = FileNotFoundException.class)
	public void bogusStreamFails() throws MalformedURLException, IOException, JAXBException, InterruptedException {
		InputStream stream = testClient.stream(StreamTests.bogusStream);
	}
}
