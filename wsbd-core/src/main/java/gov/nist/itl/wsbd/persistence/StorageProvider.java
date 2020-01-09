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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import gov.nist.itl.wsbd.configuration.ServerConfiguration;

/**
 * Represents: a way to store data
 *
 * To store data, first call the {@link #reserve(long)} method to obtain a UUID
 * for the data. Then, write the data using {@link #store(UUID)} and read the
 * data using {@link #read(UUID)}. The storage provider provides a maximum limit
 * on the amount of data it can store, after which point it will remove the
 * least recently used file if configured to do so.
 *
 * This class is thread safe. Also, the streams produced by the methods
 * {@link #store(UUID)} and {@link #read(UUID)} may be read from or written to
 * at the same time as other methods in this class are being used (the streams
 * may be used on threads other than the thread this storage provider is used
 * on).
 *
 * @author Jacob Glueck
 *
 */
public abstract class StorageProvider {

	/**
	 * A lock used to guard the store map.
	 */
	private final Object lock;
	/**
	 * A map which stores information about the stored files
	 */
	private final Map<UUID, StoreEntry> store;
	/**
	 * The configuration
	 */
	private final StorageProviderConfiguration config;

	/**
	 * Creates: a new storage provider
	 *
	 * @param config
	 *            the configuration
	 */
	public StorageProvider(StorageProviderConfiguration config) {
		lock = new Object();
		store = new HashMap<>();
		this.config = config;
	}

	/**
	 * Effect: if at least <code>numBytes</code> of data are available, reserves
	 * space. If this method returns a UUID, it means that a call to store will
	 * not run out of space as long as the number of bytes written is less than
	 * or equal to <code>numBytes</code>. However, more bytes than
	 * <code>numBytes</code> may be written to the store, but the store might
	 * run out of space and the write method will throw a
	 * {@link StorageCapacityExceededException}.
	 *
	 * @param numBytes
	 *            the number of bytes to reserve.
	 * @return the UUID of the reserved space or <code>null</code> if there is
	 *         not enough space left.
	 */
	public UUID reserve(long numBytes) {

		synchronized (lock) {

			if (!freeSpace(numBytes)) {
				return null;
			}
			assert numBytes <= available();
			
			UUID id;
			do {
				id = UUID.randomUUID();
			} while (store.containsKey(id));
			store.put(id, new StoreEntry(numBytes));
			return id;
		}
	}
	
	/**
	 * Effect: attempts to delete the least recently used files until there is
	 * at least numBytes of space available. If <code>lruFileRemoved</code> is
	 * false, then this method does not remove anything.<br>
	 * Requires: the lock must be held (assert)
	 *
	 * @param numBytes
	 *            the number of bytes to try and free
	 * @return true if and only if after this method,
	 *         <code>numBytes <= available()</code>.
	 */
	private boolean freeSpace(long numBytes) {
		assert Thread.holdsLock(lock);
		// Try to drop sessions until space is available
		if (config.lruFileRemoved()) {
			while (numBytes > available()) {
				Iterator<Map.Entry<UUID, StoreEntry>> iter = store.entrySet().iterator();
				if (!iter.hasNext()) {
					return false;
				}
				Map.Entry<UUID, StoreEntry> lruSession = iter.next();
				while (iter.hasNext()) {
					Map.Entry<UUID, StoreEntry> next = iter.next();
					if (next.getValue().lastUsed.isBefore(lruSession.getValue().lastUsed)) {
						lruSession = next;
					}
				}
				try {
					if (delete(lruSession.getKey()) != 0) {
						return false;
					}
				} catch (IOException e) {
					return false;
				}
			}
		} else if (numBytes > available()) {
			// If space is still not available, return null
			return false;
		}
		
		// If we make it here, there is enough space
		assert numBytes <= available();
		return true;
	}

	/**
	 * Effect: tries to free space and adds the new space to the reservation and
	 * ID if possible <br>
	 * Requires: the lock must be held (assert)
	 *
	 * @param id
	 *            the ID
	 * @param numBytes
	 *            the number of bytes to try and free
	 * @return true if and only if the bytes were reserved
	 */
	private boolean freeSpaceAndReserve(UUID id, long numBytes) {
		assert Thread.holdsLock(lock);
		if (freeSpace(numBytes)) {
			store.get(id).reserved += numBytes;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Effect: attempts to increase the space reservation to allow
	 * <code>numExtraBytes</code> to be added. If the reservation is already big
	 * enough, this method does nothing. If the reservation is not big enough,
	 * this method increases it if there is space available, and tries to delete
	 * files if needed. <br>
	 * Requires: the lock must be held (assert)
	 *
	 * @param id
	 *            the ID
	 * @param numExtraBytes
	 *            the number of bytes that need to be written
	 * @throws StorageCapacityExceededException
	 *             if there is not space for the extra bytes
	 */
	public void ensureCapacity(UUID id, long numExtraBytes) throws StorageCapacityExceededException {
		assert Thread.holdsLock(lock);
		assert store.get(id).used <= store.get(id).reserved;

		long bytesNeeded = numExtraBytes + store.get(id).used - store.get(id).reserved;
		// If we need more space, try to reserve it
		if (bytesNeeded > 0) {
			if (!freeSpaceAndReserve(id, bytesNeeded)) {
				throw new StorageCapacityExceededException(id, store.get(id).reserved, store.get(id).used, available(), bytesNeeded);
			}
		}

		// We should have reserved enough space for the extra bytes
		assert store.get(id).used + numExtraBytes <= store.get(id).reserved;
	}
	
	/**
	 * Effect: trims the reservation down to the size of the used space, freeing
	 * up space for other files. Does nothing if the UUID does not exist.
	 *
	 * @param id
	 *            the ID to trim
	 */
	public void trim(UUID id) {
		synchronized (lock) {
			if (exists(id)) {
				assert store.get(id).reserved >= store.get(id).used;
				store.get(id).reserved = store.get(id).used;
			}
		}
	}
	
	/**
	 * @param id
	 *            the ID of the store
	 * @return true if and only if something is stored under the UUID or space
	 *         is reserved to store something under the UUID.
	 */
	public boolean exists(UUID id) {

		synchronized (lock) {
			return store.containsKey(id);
		}
	}
	
	/**
	 * Effect: opens an output stream to write data to the store under the
	 * specified ID. When the stream is closed, the store will be complete. If
	 * an entry for the specified UUID already exists, this method overwrites
	 * that entry.
	 *
	 * Reading and writing to the same store at the same time is undefined.
	 *
	 * @param id
	 *            the ID under which to store the data
	 * @return an output stream to write the data to or <code>null</code> if
	 *         there is no storage associated with the specified UUID.
	 * @throws IOException
	 *             if there is a problem
	 */
	public OutputStream store(UUID id) throws IOException {
		
		OutputStream result;
		synchronized (lock) {
			if (!exists(id)) {
				return null;
			}
			deleteData(id);
			store.get(id).used = 0;
			result = new OutputStreamWrapper(id, openStore(id));
		}
		return result;
	}
	
	/**
	 * Effect: opens an output stream to write data to the store under the
	 * specified ID. When the stream is closed, the store will be complete. If
	 * an entry for the specified UUID already exists, this method overwrites
	 * that entry.
	 *
	 * @param id
	 *            the ID under which to store the data
	 * @return an output stream to write the data to.
	 * @throws IOException
	 *             if there is a problem
	 */
	protected abstract OutputStream openStore(UUID id) throws IOException;

	/**
	 * Effect: opens a stream to read data.
	 *
	 * Reading and writing to the same store at the same time is undefined.
	 *
	 * @param id
	 *            the UUID of the data to read.
	 * @return the stream which reads the data.
	 * @throws IOException
	 *             If there is a problem
	 */
	public InputStream read(UUID id) throws IOException {
		
		InputStream result;
		synchronized (lock) {
			if (!exists(id)) {
				return null;
			}
			result = new InputStreamWrapper(id, openRead(id));
		}
		return result;
	}

	/**
	 * Effect: opens a stream to read data.
	 *
	 * @param id
	 *            the UUID of the data to read.
	 * @return the stream which reads the data.
	 * @throws IOException
	 *             If there is a problem
	 */
	protected abstract InputStream openRead(UUID id) throws IOException;
	
	/**
	 * Effect: deletes all data stored with this UUID. If this method does not
	 * throw an exception, then after this method is called, the UUID will be
	 * available again.
	 *
	 * @param id
	 *            the ID of the file to delete
	 * @return the number of open streams using this session. If 0, the data was
	 *         deleted. If negative, then there was no storage associated with
	 *         the specified UUID.Otherwise, the data was not deleted because it
	 *         is in use.
	 * @throws IOException
	 *             if there is a problem.
	 */
	public int delete(UUID id) throws IOException {

		int openCount;
		synchronized (lock) {
			if (!exists(id)) {
				return -1;
			}
			if ((openCount = store.get(id).openCount) == 0) {
				store.remove(id);
			}
		}
		if (openCount == 0) {
			deleteData(id);
		}
		return openCount;
	}
	
	/**
	 * Effect: deletes all data stored with this UUID. If this method does not
	 * throw an exception, then after this method is called, the UUID will be
	 * available again. Deleting data that does not exist must succeed.
	 *
	 * @param id
	 *            the ID of the file to delete
	 * @throws IOException
	 *             if there is a problem.
	 */
	protected abstract void deleteData(UUID id) throws IOException;
	
	/**
	 * @return the number of UUIDs for which data is stored or for which space
	 *         has been reserved.
	 */
	public int count() {

		return store.size();
	}
	
	/**
	 * The number of bytes of space currently used, including bytes reserved but
	 * not yet used.
	 *
	 * @return the number of bytes of space currently used
	 */
	public long used() {
		
		synchronized (lock) {
			long result = 0;
			for (StoreEntry entry : store.values()) {
				// Take the bigger value
				result += Math.max(entry.reserved, entry.used);
			}
			return result;
		}
	}

	/**
	 * @return the configuration
	 */
	public StorageProviderConfiguration configuration() {

		return config;
	}

	/**
	 * @return the number of bytes available.
	 */
	public long available() {

		return config.capacity - used();
	}
	
	/**
	 * Effect: sets the most recent use time of the specified UUID to now.
	 * Requires: the lock is held
	 *
	 * @param id
	 *            the ID
	 */
	private void logUse(UUID id) {

		assert Thread.holdsLock(lock);
		store.get(id).lastUsed = Instant.now();
	}

	/**
	 * Represents: the status of an entry in the store
	 *
	 * @author Jaocb Glueck
	 *
	 */
	private class StoreEntry {
		
		/**
		 * The amount of space, in bytes, reserved for this entry
		 */
		private long reserved;
		/**
		 * The amount of space, in bytes, used by this entry
		 */
		private long used;
		/**
		 * The time when this entry was last used
		 */
		private Instant lastUsed;
		/**
		 * The number of open streams on this entry
		 */
		private int openCount;
		
		/**
		 * Creates: a new entry with the specified amount of reserved space. The
		 * amount of used space is set to 0, and the time last used is set to
		 * now. The number of open streams is set to 0.
		 *
		 * @param reserved
		 *            the amount of reserved space in bytes
		 */
		public StoreEntry(long reserved) {
			this.reserved = reserved;
			used = 0;
			lastUsed = Instant.now();
			openCount = 0;
		}
	}

	/**
	 * Represents: an output stream which wraps another stream and keeps track
	 * of the bytes written and the time when the stream is used.
	 *
	 * @author Jacob Glueck
	 *
	 */
	private class OutputStreamWrapper extends OutputStream {

		/**
		 * The ID of the store this stream writes to
		 */
		private final UUID id;
		/**
		 * The underlying output stream
		 */
		private final OutputStream out;
		
		/**
		 * Creates: a new stream<br>
		 * Effect: increments the open stream count<br>
		 * Requires: the lock must be held (assert)
		 *
		 * @param id
		 *            the ID of the store this stream writes to
		 * @param out
		 *            the underlying output stream
		 */
		public OutputStreamWrapper(UUID id, OutputStream out) {
			assert Thread.holdsLock(lock);
			this.id = id;
			this.out = out;
			store.get(id).openCount++;
		}

		@Override
		public void write(int b) throws IOException {
			synchronized (lock) {
				ensureCapacity(id, 1);
			}
			out.write(b);
			synchronized (lock) {
				store.get(id).used++;
				logUse(id);
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			synchronized (lock) {
				ensureCapacity(id, len);
			}
			out.write(b, off, len);
			synchronized (lock) {
				store.get(id).used += len;
				logUse(id);
			}
		}

		@Override
		public void close() throws IOException {
			try {
				out.close();
				synchronized (lock) {
					logUse(id);
				}
			} finally {
				synchronized (lock) {
					store.get(id).openCount--;
				}
			}
		}
	}

	/**
	 * Represents: an input stream which wraps another stream and keeps track of
	 * the time when the stream is used.
	 *
	 * @author Jacob Glueck
	 *
	 */
	private class InputStreamWrapper extends InputStream {
		
		/**
		 * The UUID of the store which this stream reads
		 */
		private final UUID id;
		/**
		 * The underlying input stream
		 */
		private final InputStream in;
		
		/**
		 * Creates: a new stream<br>
		 * Effect: increments the open stream count<br>
		 * Requires: the lock must be held (assert)
		 *
		 * @param id
		 *            the ID of the store this stream reads from
		 * @param in
		 *            the underlying input stream
		 */
		public InputStreamWrapper(UUID id, InputStream in) {
			assert Thread.holdsLock(lock);
			this.id = id;
			this.in = in;
			store.get(id).openCount++;
		}
		
		@Override
		public int available() throws IOException {

			return in.available();
		}

		@Override
		public int read() throws IOException {
			
			int read = in.read();
			synchronized (lock) {
				logUse(id);
			}
			return read;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			
			int read = in.read(b, off, len);
			synchronized (lock) {
				logUse(id);
			}
			return read;
		}
		
		@Override
		public void mark(int readLimit) {

			in.mark(readLimit);
			synchronized (lock) {
				logUse(id);
			}
		}
		
		@Override
		public boolean markSupported() {
			
			synchronized (lock) {
				logUse(id);
			}
			return in.markSupported();
		}
		
		@Override
		public void reset() throws IOException {

			synchronized (lock) {
				logUse(id);
			}
			in.reset();
		}

		@Override
		public long skip(long n) throws IOException {
			
			long skip = in.skip(n);
			synchronized (lock) {
				logUse(id);
			}
			return skip;
		}
		
		@Override
		public void close() throws IOException {
			
			try {
				in.close();
				synchronized (lock) {
					logUse(id);
				}
			} finally {
				synchronized (lock) {
					store.get(id).openCount--;
				}
			}
		}
	}

	/**
	 * Represents: the configuration of a storage provider
	 *
	 * @author Jacob Glueck
	 *
	 */
	public static class StorageProviderConfiguration {
		
		/**
		 * The maximum number of bytes this storage provider can store
		 */
		private final long capacity;
		/**
		 * True if and only if this storage provider will remove try to remove
		 * the lease recently used file to free up space during a call to
		 * {@link #reserve(long)}.
		 */
		private final boolean lruFileRemoved;

		/**
		 * Creates: a new storage provider configuration with the specified
		 * information
		 *
		 * @param capacity
		 *            the capacity, in bytes
		 * @param lruFileRemoved
		 *            true if the LRU file should be dropped when more space is
		 *            necessary
		 */
		public StorageProviderConfiguration(long capacity, boolean lruFileRemoved) {
			this.capacity = capacity;
			this.lruFileRemoved = lruFileRemoved;
		}
		
		/**
		 * Creates: a new storage provider configuration with the specified
		 * information.
		 *
		 * @param config
		 *            the server configuration
		 * @throws ArithmeticException
		 *             if the values are out of range
		 */
		public StorageProviderConfiguration(ServerConfiguration config) {
			capacity = config.maximumStorageCapacity().longValueExact();
			lruFileRemoved = config.lruCaptureDataAutomaticallyDropped();
		}
		
		/**
		 * @return the capacity
		 */
		public long capacity() {
			return capacity;
		}
		
		/**
		 * @return the lruFileRemoved
		 */
		public boolean lruFileRemoved() {
			return lruFileRemoved;
		}
	}
}