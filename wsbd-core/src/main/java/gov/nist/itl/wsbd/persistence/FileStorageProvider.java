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

package gov.nist.itl.wsbd.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Represents: a file data base
 *
 * @author Kevin Mangold
 * @author Jacob Glueck
 *
 */
public class FileStorageProvider extends StorageProvider {
	
	/**
	 * The parent directory for file storage
	 */
	private final File dir;
	
	/**
	 * Creates: a new file storage provider with a maximum capacity. The storage
	 * directory is a temporary directory which will be deleted when the program
	 * terminates.
	 *
	 * @param config
	 *            the configuration
	 * @throws IOException
	 *             if there is a problem
	 */
	public FileStorageProvider(StorageProviderConfiguration config) throws IOException {
		this(config, Files.createTempDirectory("WSBD-" + UUID.randomUUID()).toFile());
	}
	
	/**
	 * Creates: a new file storage provider which stores files in the specified
	 * directory.
	 *
	 * @param config
	 *            the configuration
	 * @param dir
	 *            the directory to store the files in
	 */
	private FileStorageProvider(StorageProviderConfiguration config, File dir) {
		super(config);
		this.dir = dir;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			FileStorageProvider.deleteFile(dir);
		}));
	}
	
	/**
	 * Effect: recursively deletes a file or directory
	 *
	 * @param f
	 *            the file
	 */
	private static void deleteFile(File f) {
		if (f.isDirectory()) {
			for (File child : f.listFiles()) {
				FileStorageProvider.deleteFile(child);
			}
		}
		f.delete();
	}
	
	/**
	 * @param id
	 *            the ID
	 * @return the file where data for the specified ID is stored
	 */
	private File file(UUID id) {
		return new File(dir, id.toString());
	}
	
	@Override
	protected OutputStream openStore(UUID id) throws IOException {
		return new FileOutputStream(file(id));
	}
	
	@Override
	protected InputStream openRead(UUID id) throws IOException {
		return new FileInputStream(file(id));
	}
	
	@Override
	protected void deleteData(UUID id) throws IOException {
		file(id).delete();
	}
}