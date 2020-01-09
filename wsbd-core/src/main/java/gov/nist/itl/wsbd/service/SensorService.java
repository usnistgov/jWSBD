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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBElement;

import org.glassfish.grizzly.http.util.HttpStatus;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Dictionary;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Dictionary.Item;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Parameter;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Result;
import org.oasis_open.docs.bioserv.ns.wsbd_1.SensorStatus;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Status;

import gov.nist.itl.wsbd.configuration.ServerStateKey;
import gov.nist.itl.wsbd.configuration.ServiceConfiguration;
import gov.nist.itl.wsbd.dictionary.DictionaryWrapper;
import gov.nist.itl.wsbd.dictionary.InvalidDictionaryException;
import gov.nist.itl.wsbd.persistence.FileStorageProvider;
import gov.nist.itl.wsbd.persistence.StorageProvider;
import gov.nist.itl.wsbd.persistence.StorageProvider.StorageProviderConfiguration;
import gov.nist.itl.wsbd.service.SessionManager.SessionManagerConfiguration;
import gov.nist.itl.wsbd.streaming.IllegalResourceException;
import gov.nist.itl.wsbd.streaming.StreamHandle;

/**
 * Represents: a WS-BD sensor service. All sensors services should inherit from
 * this class. This class handles all the registration and locking requirements,
 * and allows subclasses to focus only on sensor operation. This class
 * guarantees that all the abstract methods except for
 * {@link #getStream(String)} will only be called from one thread. In order for
 * subclasses to use this class as a web service resource, subclasses must have
 * an <code>@Path("servicepath")</code> annotation.
 *
 * @author Kevin Mangold
 * @author Jacob Glueck
 *
 */
public abstract class SensorService implements ISensorService {
	
	/**
	 * The session manager used to manage the sessions
	 */
	private final SessionManager sessionManager;
	/**
	 * The lock used for access to the session manager
	 */
	private final Object lock;
	/**
	 * The executor service which executes all sensor jobs on one thread
	 */
	private final ExecutorService sensorJobExecutor;
	/**
	 * The timeout timer used to cancel long tasks
	 */
	private final ScheduledExecutorService timeoutTimer;
	/**
	 * The service configuration
	 */
	private final ServiceConfiguration configuration;
	/**
	 * The storage provider for storing captured data
	 */
	private final StorageProvider storage;

	/**
	 * True if and only if a sensor job is currently running
	 */
	private boolean sensorJobRunning;
	/**
	 * Not null if and only if an asynchronous sensor job is currently running.
	 * This is the session ID that started the asynchronous operation.
	 */
	private UUID asyncSensorJobOwner;
	/**
	 * True if a cancel has been requested
	 */
	private boolean cancelRequested;
	/**
	 * A test which return {@link Status#SUCCESS} if no asynchronous sensor job
	 * is currently running.
	 */
	private final Function<UUID, Result> runIfNoAsynSensorJob;
	/**
	 * A test which returns {@link Status#SUCCESS} if an asynchronous sensor job
	 * is running.
	 */
	private final Function<UUID, Result> runIfAsynSensorJob;
	/**
	 * Does nothing
	 */
	private final BiConsumer<UUID, Result> noAfterJob;
	/**
	 * Marks an asynchronous sensor job as running if the sensor job succeeded.
	 */
	private final BiConsumer<UUID, Result> startAsyncSensorJobIfSuccess;
	/**
	 * Marks an asynchronous sensor job as not running if the sensor job
	 * succeeded.
	 */
	private final BiConsumer<UUID, Result> stopAsyncSensorJobIfSuccessOrCancel;

	/**
	 * Set to true once {@link #initializeService()} has been called.
	 */
	private boolean serviceInitialized = false;
	/**
	 * An immutable map for stream names to stream handles
	 */
	private final Map<String, StreamHandle> streams;
	
	/**
	 * Creaets: a new sensor service
	 *
	 * @param configuration
	 *            the configuration
	 * @throws IOException
	 *             if there is a problem setting up the storage provider
	 * @throws IllegalResourceException
	 *             if there is a problem setting up the streams
	 */
	public SensorService(ServiceConfiguration configuration) throws IOException, IllegalResourceException {
		sessionManager = new SessionManager(new SessionManagerConfiguration(configuration.serverConfiguration()));
		lock = new Object();
		ThreadFactory daemonFactory = r -> {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			return thread;
		};
		sensorJobExecutor = Executors.newSingleThreadExecutor(daemonFactory);
		timeoutTimer = Executors.newSingleThreadScheduledExecutor(daemonFactory);
		this.configuration = configuration;
		storage = new FileStorageProvider(new StorageProviderConfiguration(configuration.serverConfiguration()));
		
		sensorJobRunning = false;
		asyncSensorJobOwner = null;
		cancelRequested = false;
		runIfNoAsynSensorJob = (id) -> {
			if (asyncSensorJobOwner == null) {
				return Utility.result(Status.SUCCESS);
			} else {
				return Utility.result(Status.SENSOR_BUSY, "Sensor running asynchronous capture");
			}
		};
		runIfAsynSensorJob = (id) -> {
			if (asyncSensorJobOwner != null) {
				if (configuration.serverConfiguration().transferrableAsyncCapture()) {
					// If the capture is transferable, then so long as an owner
					// exists, it is good enough.
					return Utility.result(Status.SUCCESS);
				} else {
					// With the code below, only the client who started the
					// asynchronous capture can get the data. With the code
					// above, anyone can finish the capture.
					if (asyncSensorJobOwner.equals(id)) {
						return Utility.result(Status.SUCCESS);
					} else {
						return Utility.result(Status.SENSOR_BUSY, "The currently running asynchronous sensor job is owned by another client. You must cancel the job.");
					}
				}
			} else {
				return Utility.result(Status.FAILURE, "No asynchronous capture running");
			}
		};
		noAfterJob = (id, result) -> {
		};
		startAsyncSensorJobIfSuccess = (id, result) -> {
			if (result.getStatus().equals(Status.SUCCESS)) {
				asyncSensorJobOwner = id;
			}
		};
		// If the job was canceled successfully, the sensor is now doing
		// nothing.
		stopAsyncSensorJobIfSuccessOrCancel = (id, result) -> {
			if (result.getStatus().equals(Status.SUCCESS) || result.getStatus().equals(Status.CANCELED) || result.getStatus().equals(Status.CANCELED_WITH_SENSOR_FAILURE)) {
				asyncSensorJobOwner = null;
			}
		};

		Set<StreamHandle> streamHandles = StreamHandle.handleFromStreamConfiguration(configuration.streamConfiguration());
		Map<String, StreamHandle> streamHandlesMap = new HashMap<>();
		for (StreamHandle handle : streamHandles) {
			streamHandlesMap.put(handle.name(), handle);
		}
		streams = Collections.unmodifiableMap(streamHandlesMap);

	}

	/**
	 * Effect: sets up this service. Must be called before any other methods
	 * (assert). May not be called more than once (assert).
	 */
	public void initializeService() {
		assert !serviceInitialized;
		setUpService();
		handleDefaultConfiguration(configuration.sensorConfiguration().dictionary());
		serviceInitialized = true;
	}
	
	@Override
	public JAXBElement<Result> IRegister() {
		assert serviceInitialized;
		synchronized (lock) {
			Result result = sessionManager.registerSession();
			return Utility.wrap(result);
		}
	}
	
	@Override
	public JAXBElement<Result> IUnregister(UUID sessionId) {
		assert serviceInitialized;
		
		synchronized (lock) {
			Result result = sessionManager.unregisterSession(sessionId);
			return Utility.wrap(result);
		}
	}
	
	@Override
	public JAXBElement<Result> ILock(UUID sessionId) {
		assert serviceInitialized;

		synchronized (lock) {
			Result result = sessionManager.lock(sessionId);
			return Utility.wrap(result);
		}
	}
	
	@Override
	public JAXBElement<Result> IStealLock(UUID sessionId) {
		assert serviceInitialized;
		
		synchronized (lock) {
			Result result = sessionManager.stealLock(sessionId);
			return Utility.wrap(result);
		}
	}
	
	@Override
	public JAXBElement<Result> IUnlock(UUID sessionId) {
		assert serviceInitialized;

		synchronized (lock) {
			Result result = sessionManager.unlock(sessionId);
			return Utility.wrap(result);
		}
	}

	/**
	 * Effect: runs a sensor job on the sensor job thread.
	 *
	 * @param requestor
	 *            the UUID of the requestor
	 * @param jobType
	 *            the sensor status to put in
	 *            <code>configuration.serverStateConfiguration()</code> while
	 *            the job is running. Once the job is over, if an asynchronous
	 *            capture is not running, sets the status to
	 *            {@link SensorStatus#READY}, otherwise does not change it back.
	 * @param timeout
	 *            the timeout
	 * @param shouldRun
	 *            a test which is run under the lock to determine if this job
	 *            should run. If this rest does not return
	 *            {@link Status#SUCCESS}, this method will return the result of
	 *            the test and will not do anything else.
	 * @param job
	 *            the job to run. This method returns the result of the job.
	 * @param afterJob
	 *            a cleanup task which is run under the lock and fed the result
	 *            of the sensor job.
	 * @return the result
	 */
	private Result runSensorJob(UUID requestor, SensorStatus jobType, Duration timeout, Function<UUID, Result> shouldRun, Callable<Result> job, BiConsumer<UUID, Result> afterJob) {
		assert serviceInitialized;

		synchronized (lock) {
			// Attempt to acquire the sensor
			Result result = sessionManager.acquireSensor(requestor);
			if (result.getStatus() == Status.SUCCESS) {
				if (sensorJobRunning) {
					result = Utility.result(Status.SENSOR_BUSY);
				} else {
					result = shouldRun.apply(requestor);
					if (result.getStatus() == Status.SUCCESS) {
						// Mark the sensor as doing something
						configuration.serverStateConfiguration().setSensorStatus(jobType);
						result = runJobOnSensorThread(timeout, job);
						afterJob.accept(requestor, result);
					}
				}
				if (asyncSensorJobOwner == null) {
					sessionManager.releaseSensor(requestor);
					// Mark the sensor as ready
					configuration.serverStateConfiguration().setSensorStatus(SensorStatus.READY);
				}
			}
			
			return result;
		}
	}
	
	/**
	 * Effect: runs a job on the sensor thread with a specified timeout. Makes
	 * all the needed state transitions for {@link #sensorJobRunning} and
	 * {@link #cancelRequested}. <br>
	 * Requires: the lock must be held (assert). This thread release the lock
	 * while running the job and reacquires it when done.
	 *
	 * @param timeout
	 *            the timeout
	 * @param job
	 *            the job
	 * @return the reuslt
	 */
	public Result runJobOnSensorThread(Duration timeout, Callable<Result> job) {
		assert serviceInitialized;
		assert Thread.holdsLock(lock);
		Result result = null;
		final AtomicReference<Result> jobResult = new AtomicReference<>(null);
		sensorJobRunning = true;
		final AtomicBoolean currentJobRunning = new AtomicBoolean(true);

		final Future<Void> future = sensorJobExecutor.submit(() -> {
			try {
				Result r = job.call();
				jobResult.set(r);
				return null;
			} finally {
				currentJobRunning.set(true);
				synchronized (lock) {
					lock.notifyAll();
				}
			}
		});

		ScheduledFuture<?> timeoutFuture = timeoutTimer.schedule(() -> {
			future.cancel(true);
		}, timeout.toMillis(), TimeUnit.MILLISECONDS);

		while (!future.isDone()) {
			try {
				lock.wait();
				if (cancelRequested) {
					future.cancel(true);
					cancelRequested = false;
				}
			} catch (InterruptedException e) {
			}
		}

		// Stop the cancellation thread
		timeoutFuture.cancel(false);

		try {
			future.get();
		} catch (InterruptedException e) {
			// This means the current thread was interrupted while waiting,
			// which should never happen. This will cause the server to
			// crash if asserts are enabled.
			assert false;
			result = Utility.result(Status.FAILURE, e.getMessage());
		} catch (ExecutionException e) {
			result = Utility.result(Status.FAILURE, e.getMessage());
		} catch (CancellationException e) {
			result = Utility.result(Status.CANCELED);
		}
		
		// If the result has something
		if (jobResult.get() != null) {
			result = jobResult.get();
		}
		// It should not be null by this point
		assert result != null;
		
		sensorJobRunning = false;
		lock.notifyAll();
		return result;
	}
	
	@Override
	public JAXBElement<Result> IGetServiceInformation() {
		assert serviceInitialized;
		
		Result result = Utility.result(Status.SUCCESS);
		// This is safe because configuration.information() copies all the items
		// into a new dictionary, and all the items are created new every time.
		Utility.setResultMetadata(result, configuration.information());
		return Utility.wrap(result);
	}
	
	@Override
	public JAXBElement<Result> IInitialize(UUID sessionId) {
		assert serviceInitialized;
		return Utility.wrap(runSensorJob(sessionId, SensorStatus.INITIALIZING, configuration.serverConfiguration().initializationTimeout(), runIfNoAsynSensorJob, this::initialize, noAfterJob));
	}
	
	@Override
	public JAXBElement<Result> IUninitialize(UUID sessionId) {
		assert serviceInitialized;

		return Utility.wrap(runSensorJob(sessionId, SensorStatus.UNINITIALIZING, configuration.serverConfiguration().uninitializationTimeout(), runIfNoAsynSensorJob, this::uninitialize, noAfterJob));
	}
	
	@Override
	public JAXBElement<Result> IGetConfiguration(UUID sessionId) {
		assert serviceInitialized;
		return Utility.wrap(runSensorJob(sessionId, SensorStatus.CONFIGURING, configuration.serverConfiguration().getConfigurationTimeout(), runIfNoAsynSensorJob, () -> {
			Result result = Utility.result(Status.SUCCESS);
			Utility.setResultMetadata(result, configuration.configuration());
			return result;
		}, noAfterJob));
	}
	
	@Override
	public JAXBElement<Result> ISetConfiguration(UUID sessionId, Dictionary newConfig) {
		assert serviceInitialized;
		return Utility.wrap(runSensorJob(sessionId, SensorStatus.CONFIGURING, configuration.serverConfiguration().setConfigurationTimeout(), runIfNoAsynSensorJob, () -> {
			for (Item i : newConfig.getItem()) {
				DictionaryWrapper<Parameter> info = null;
				if (configuration.serverInformation().containsKey(i.getKey())) {
					info = configuration.serverInformation();
				} else if (configuration.sensorInformation().containsKey(i.getKey())) {
					info = configuration.sensorInformation();
				}
				if (info == null) {
					return Utility.result(Status.NO_SUCH_PARAMTER, "The value " + i.getKey() + " is not a parameter");
				}
				if (info.get(i.getKey()).isReadOnly()) {
					return Utility.result(Status.UNSUPPORTED, "The parameter " + i.getKey() + " is read only");
				}
			}
			
			// At this point, all the values correspond to parameters that exist
			// and are writable, so start a sensor job to update them.
			Result result = setConfiguration(newConfig);
			// If the result succeeded, save the new configuration
			// The setConfiguration is atomic: if it returns success, all the
			// changes were made. If it returns anything else, no changes were
			// made.
			if (result.getStatus().equals(Status.SUCCESS)) {
				for (Item i : newConfig.getItem()) {
					DictionaryWrapper<Object> config = null;
					if (configuration.serverInformation().containsKey(i.getKey())) {
						config = configuration.serverConfiguration();
					} else if (configuration.sensorInformation().containsKey(i.getKey())) {
						config = configuration.sensorConfiguration();
					}
					config.put(i.getKey(), i.getValue());
				}

				// Update the time
				configuration.serverStateConfiguration().markUpdated();
			}
			return result;
		}, noAfterJob));
	}
	
	@Override
	public JAXBElement<Result> ICapture(UUID sessionId) {
		assert serviceInitialized;
		synchronized (lock) {
			Instant start = Instant.now();
			JAXBElement<Result> result = IBeginCapture(sessionId, configuration.serverConfiguration().captureTimeout());
			if (result.getValue().getStatus().equals(Status.SUCCESS)) {
				Duration elapsed = Duration.between(start, Instant.now());
				if (elapsed.compareTo(configuration.serverConfiguration().captureTimeout()) < 0) {
					result = IEndCapture(sessionId, configuration.serverConfiguration().captureTimeout().minus(elapsed));
				} else {
					if (asyncSensorJobOwner != null) {
						Result cancelAsync = runJobOnSensorThread(configuration.serverConfiguration().cancelAsyncCaptureTimeout(), this::cancelAsyncCapture);
						if (!cancelAsync.getStatus().equals(Status.SUCCESS)) {
							result = Utility.wrap(cancelAsync);
						}
					}
				}
			}
			return result;
		}
	}
	
	@Override
	public JAXBElement<Result> IBeginCapture(UUID sessionId) {
		assert serviceInitialized;
		try {
			return IBeginCapture(sessionId, configuration.serverConfiguration().beginCaptureTimeout());
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * Effect: begins a capture with a specified timeout
	 *
	 * @param sessionId
	 *            the session ID
	 * @param timeout
	 *            the timeout
	 * @return the result
	 */
	private JAXBElement<Result> IBeginCapture(UUID sessionId, Duration timeout) {
		assert serviceInitialized;
		return Utility.wrap(runSensorJob(sessionId, SensorStatus.CAPTURING, timeout, runIfNoAsynSensorJob, this::beginCapture, startAsyncSensorJobIfSuccess));
		
	}
	
	@Override
	public JAXBElement<Result> IEndCapture(UUID sessionId) {
		assert serviceInitialized;
		return IEndCapture(sessionId, configuration.serverConfiguration().endCaptureTimeout());
	}

	/**
	 * Effect: ends a capture with a specified timeout
	 *
	 * @param sessionId
	 *            the session ID
	 * @param timeout
	 *            the timeout
	 * @return the result
	 */
	private JAXBElement<Result> IEndCapture(UUID sessionId, Duration timeout) {
		assert serviceInitialized;
		return Utility.wrap(runSensorJob(sessionId, SensorStatus.CAPTURING, configuration.serverConfiguration().endCaptureTimeout(), runIfAsynSensorJob, () -> {
			List<CaptureData> data = new LinkedList<>();
			List<UUID> ids = new LinkedList<>();
			Result result = endCapture(data);
			// If the capture succeeded, we need to store the data
			if (result.getStatus().equals(Status.SUCCESS)) {
				for (CaptureData capture : data) {
					UUID storeID = storage.reserve(capture.size);
					if (storeID == null) {
						return Utility.result(Status.FAILURE, "Out of space!");
					}
					try {
						Result storeResult = Utility.result(Status.SUCCESS);
						Utility.setSensorData(storeResult, capture.data);

						// Add the extra metadata
						Dictionary metadata = configuration.configuration();
						Item captureDate = new Item();
						captureDate.setKey(CaptureMetadata.captureDate.toString());
						captureDate.setValue(Utility.fromInstant(Instant.now()));
						metadata.getItem().add(captureDate);
						Item contentType = new Item();
						contentType.setKey(CaptureMetadata.contentType.toString());
						contentType.setValue(capture.contentType);
						metadata.getItem().add(contentType);

						Utility.setResultMetadata(storeResult, metadata);
						Utility.marshal(Utility.objectFactory.createResult(storeResult), storage.store(storeID));
						storage.trim(storeID);
						ids.add(storeID);
					} catch (Exception e) {
						e.printStackTrace();
						String message = e.getMessage();
						if (message == null) {
							message = e.getCause() != null ? e.getCause().getMessage() : "Strange error.";
						}
						return Utility.result(Status.FAILURE, "Error while saving data: " + message);
					}
				}

				// If we make it here without returning, then all the captures
				// have been saved. So, record the capture IDs
				Utility.setCaptureIDs(result, ids);
			}
			return result;
		}, stopAsyncSensorJobIfSuccessOrCancel));
	}
	
	@Override
	public JAXBElement<Result> IGetSensorStatus() {
		assert serviceInitialized;

		Result result = Utility.result(Status.SUCCESS);
		// This is thread safe because the map is thread safe. We just need the
		// sensor status at this instant, so no locking is needed. (We do not
		// need to guarantee that the status cannot change for another
		// operation).
		SensorStatus status = configuration.serverStateConfiguration().sensorStatus();
		Dictionary dict = new Dictionary();
		Item item = new Item();
		item.setKey(ServerStateKey.sensorStatus.toString());
		item.setValue(status.value());
		dict.getItem().add(item);
		Utility.setResultMetadata(result, dict);
		return Utility.wrap(result);
	}
	
	@Override
	public JAXBElement<Result> IDownload(UUID captureId) {
		assert serviceInitialized;
		InputStream in = null;
		try {
			in = storage.read(captureId);
		} catch (IOException e) {
		}
		if (in == null) {
			return Utility.wrap(Utility.result(Status.INVALID_ID));
		} else {
			try {
				Result result = Utility.unmarshalResult(in);
				return Utility.wrap(result);
			} catch (Exception e) {
				e.printStackTrace();
				return Utility.wrap(Utility.result(Status.FAILURE, "Error reading data: " + e.getMessage()));
			}
		}
	}
	
	@Override
	public JAXBElement<Result> IGetDownloadInformation(UUID captureId) {
		assert serviceInitialized;
		JAXBElement<Result> result = IDownload(captureId);
		if (result.getValue().getStatus().equals(Status.SUCCESS)) {
			Utility.removeSensorData(result.getValue());
		}
		return result;
	}
	
	@Override
	public JAXBElement<Result> IThriftyDownload(UUID captureId, String maxSize) {
		assert serviceInitialized;
		JAXBElement<Result> result = IDownload(captureId);
		if (result.getValue().getStatus().equals(Status.SUCCESS)) {
			ByteArrayInputStream bigData = new ByteArrayInputStream(result.getValue().getSensorData().getValue());
			ByteArrayOutputStream smallData = new ByteArrayOutputStream();
			Result compressResult = compressData(bigData, smallData, maxSize);
			// If the compression failed, return it
			if (!compressResult.getStatus().equals(Status.SUCCESS)) {
				return Utility.wrap(compressResult);
			}
			try {
				Utility.setSensorData(result.getValue(), new ByteArrayInputStream(smallData.toByteArray()));
			} catch (IOException e) {
				// This should never happen
				assert false;
			}
			try {
				// Remove all the timeout and other junk to minimize the
				// metadata
				DictionaryWrapper<Object> metadata = new DictionaryWrapper<>(result.getValue().getMetadata().getValue(), Object.class);
				for (String key : configuration.serverConfiguration().keySet()) {
					metadata.remove(key);
				}
				for (String key : configuration.serverStateConfiguration().keySet()) {
					metadata.remove(key);
				}
				for (String key : configuration.streamConfiguration().keySet()) {
					metadata.remove(key);
				}
				minimizeMetadata(metadata);

				// Give the sensor a chance to get rid of more junk
				Utility.setResultMetadata(result.getValue(), metadata.dictionary());
			} catch (InvalidDictionaryException e) {
				// This should never happen
				assert false;
			}
		}
		return result;
	}
	
	@Override
	public Response IRawDownload(UUID captureId) {
		assert serviceInitialized;
		JAXBElement<Result> result = IDownload(captureId);
		InputStream stream;
		String contentType;
		if (result.getValue().getStatus().equals(Status.SUCCESS)) {
			stream = new ByteArrayInputStream(result.getValue().getSensorData().getValue());
			contentType = result.getValue().getMetadata().getValue().getItem().stream().filter((item) -> item.getKey().equals(CaptureMetadata.contentType.toString())).collect(Collectors.toList())
					.iterator().next().getValue().toString();
		} else {
			stream = new ByteArrayInputStream(String.format("Status: %s\nMessage: %s", result.getValue().getStatus().toString(), result.getValue().getMessage().getValue()).getBytes());
			contentType = MediaType.TEXT_PLAIN;
		}
		ResponseBuilder builder = Response.ok(stream, contentType);
		return builder.build();
	}
	
	@Override
	public JAXBElement<Result> ICancel(UUID sessionId) {
		assert serviceInitialized;
		synchronized (lock) {
			// Mark the sensor as canceling
			configuration.serverStateConfiguration().setSensorStatus(SensorStatus.CANCELING);

			if (sensorJobRunning) {
				cancelRequested = true;
				lock.notifyAll();
				while (sensorJobRunning) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
					}
				}
			}
			Result result = Utility.result(Status.SUCCESS);
			if (asyncSensorJobOwner != null) {
				Result cancelAsync = runJobOnSensorThread(configuration.serverConfiguration().cancelAsyncCaptureTimeout(), this::cancelAsyncCapture);
				if (!cancelAsync.getStatus().equals(Status.SUCCESS)) {
					result = cancelAsync;
				}
			}
			// No matter what, we must now mark the sensor as ready because
			// the job has finished. This cancel cannot fail because then
			// the sensor will be stuck.
			configuration.serverStateConfiguration().setSensorStatus(SensorStatus.READY);
			return Utility.wrap(result);
		}
	}

	@Override
	public Response IStream(String streamName) {
		StreamHandle handle = streams.get(streamName);
		if (handle == null) {
			return Response.status(HttpStatus.NOT_FOUND_404.getStatusCode()).entity("No such stream " + streamName).type(MediaType.TEXT_HTML).build();
		}
		if (handle.lockRequired()) {
			return Response.status(HttpStatus.FORBIDDEN_403.getStatusCode()).entity("The stream " + streamName + " requires a lock").type(MediaType.TEXT_HTML).build();
		}
		InputStream in = getStream(handle.name());
		if (in == null) {
			return Response.status(HttpStatus.INTERNAL_SERVER_ERROR_500.getStatusCode()).entity("While the stream " + streamName + " is specified in the service info, it has not been implemented")
					.type(MediaType.TEXT_HTML).build();
		}

		StreamingOutput stream = outputStream -> {
			byte[] buf = new byte[maxUnlockBytes()];
			int read;
			while ((read = in.read(buf)) != -1) {
				outputStream.write(buf, 0, read);
			}
		};
		return Response.ok(stream).type(handle.contentType()).build();
	}

	@Override
	public Response IStreamLocked(String streamName, UUID sessionId) {
		StreamHandle handle = streams.get(streamName);
		if (handle == null) {
			return Response.status(HttpStatus.NOT_FOUND_404.getStatusCode()).entity("No such stream " + streamName).type(MediaType.TEXT_HTML).build();
		}
		if (!handle.lockRequired()) {
			return Response.status(HttpStatus.NOT_FOUND_404.getStatusCode()).entity("The stream " + streamName + " does not require a lock").type(MediaType.TEXT_HTML).build();
		}
		InputStream in = getStream(handle.name());
		if (in == null) {
			return Response.status(HttpStatus.INTERNAL_SERVER_ERROR_500.getStatusCode()).entity("While the stream " + streamName + " is specified in the service info, it has not been implemented")
					.type(MediaType.TEXT_HTML).build();
		}

		StreamingOutput stream = outputStream -> {
			byte[] buf = new byte[maxUnlockBytes()];
			int read;
			while (holdsLock(sessionId) && (read = in.read(buf)) != -1) {
				outputStream.write(buf, 0, read);
			}
		};
		return Response.ok(stream).type(handle.contentType()).build();
	}
	
	/**
	 * Checks to see if the current session currently holds the lock. This
	 * should not be used for anything other then
	 * {@link #IStreamLocked(String, UUID)} because all this method guarantees
	 * is that the lock was held when this method returned, not that the lock is
	 * still held when the return value is read. (Using this method improperly
	 * will create a race condition.)
	 *
	 * @param sessionId
	 *            the session ID
	 * @return true if locked
	 */
	private boolean holdsLock(UUID sessionId) {
		synchronized (lock) {
			return sessionManager.hasLock(sessionId);
		}
	}

	/**
	 * @return the maximum number of bytes of data a client on a locked live
	 *         stream will receive once he looses the lock. Must be a constant.
	 *         By default, 256.
	 */
	protected int maxUnlockBytes() {
		return 256;
	}
	
	/**
	 * Effect: initializes the sensor. As long as the sensor is initialized when
	 * this method returns, it must return a status of {@link Status#SUCCESS},
	 * no matter the original sensor state.
	 *
	 * @return the result
	 */
	protected abstract Result initialize();
	
	/**
	 * Effect: uninitializes the sensor. There is no gaurentee that the sensor
	 * is initialized when this method is called.
	 *
	 * @return the result
	 */
	protected abstract Result uninitialize();
	
	/**
	 * Effect: sets the sensor's configuration to the specified configuration.
	 *
	 * This method must be atomic: if it returns {@link Status#SUCCESS}, then it
	 * must have made all the changes to the configuration. If it returns any
	 * other status, then no changes must have been made.
	 *
	 * @param configuration
	 *            The new configuration. The sensor service guarantees that this
	 *            dictionary will only contain parameters appear in the service
	 *            information which are not marked as read only. It makes no
	 *            claim on the validity of the parameters.
	 * @return the result
	 */
	protected abstract Result setConfiguration(Dictionary configuration);

	/**
	 * Effect: sets the sensor's configuration to the specified
	 * configuration.<br>
	 *
	 * This method will be called only once when the service is initialized with
	 * the
	 *
	 * @param configuration
	 *            The new configuration. Unlike
	 *            {@link #setConfiguration(Dictionary)}, all the parameters
	 *            (even if they are read only) will be present so the sensor can
	 *            set or verify them.
	 */
	protected abstract void handleDefaultConfiguration(Dictionary configuration);
	
	/**
	 * Called during {@link #initializeService()} before
	 * {@link #handleDefaultConfiguration(Dictionary)}. Will only be called
	 * once.
	 */
	protected abstract void setUpService();
	
	/**
	 * Effect: starts an asynchronous capture. If this sensor does not support
	 * asynchronous captures, this method must do nothing and return
	 * {@link Status#SUCCESS} (as it does by default). There is no guarantee
	 * that initialize has been called before capture.
	 *
	 * @return the result
	 */
	protected Result beginCapture() {
		return Utility.result(Status.SUCCESS);
	}
	
	/**
	 * Effect: terminates the capture and records the data. The data should be
	 * added to the <code>captureData</code> list if this method returns
	 * {@link Status#SUCCESS}. If this method does not return
	 * {@link Status#SUCCESS}, the list is ignored and this method's result is
	 * returned. SensorService guarantees that this method will only be called
	 * if an asynchronous capture is currently running.
	 *
	 * @param captureData
	 *            the captured data
	 *
	 * @return the result
	 */
	protected abstract Result endCapture(List<CaptureData> captureData);

	/**
	 * Effect: if this sensor supports asynchronous captures, this method must
	 * cancel one if one is running. The sensor service class guarantees that
	 * this method will not be called unless an asynchronous capture is
	 * currently running. If this sensor does not support asynchronous captures,
	 * this method must return {@link Status#SUCCESS} and do nothing (as it does
	 * by default).
	 *
	 * @return the result
	 */
	protected Result cancelAsyncCapture() {
		return Utility.result(Status.SUCCESS);
	}
	
	/**
	 * Effect: compress the capture data in <code>inData</code> and writes it to
	 * <code>outData</code> in accordance with an implementation specific max
	 * size parameter.
	 *
	 * @param inData
	 *            the input data
	 * @param outData
	 *            the output data
	 * @param maxSize
	 *            the max size
	 * @return the result
	 */
	protected abstract Result compressData(InputStream inData, OutputStream outData, String maxSize);
	
	/**
	 * Effect: removes unnecessary information form the metadata for a thrifty
	 * download.
	 *
	 * @param metadata
	 *            the metadata
	 */
	protected abstract void minimizeMetadata(DictionaryWrapper<Object> metadata);

	/**
	 * Get the live stream data with the specified name.<br>
	 * Note: this method may be called from any thread at any time. Unlike all
	 * the other abstract methods, it must be implemented in a thread safe
	 * manner. For all the streams provided in the service configuration, this
	 * method must return a non null input stream. If it returns a null stream
	 * for a valid stream name, this server will return an HTTP status code 500
	 * for Internal Server Error. Each call to this method must return a unique
	 * stream.
	 *
	 * @param streamName
	 *            the stream name
	 * @return the stream data for that name
	 */
	protected abstract InputStream getStream(String streamName);
	
	/**
	 * Represents: a capture
	 *
	 * @author Jacob Glueck
	 *
	 */
	protected class CaptureData {

		/**
		 * Creates: a new empty capture data
		 */
		public CaptureData() {
		}
		
		/**
		 * The data
		 */
		public InputStream data;
		/**
		 * An estimate of the size of the data. Can be all 0, does not really
		 * matter, but will improve performance slightly if greater than or
		 * equal to the real file size.
		 */
		public long size;
		/**
		 * The content type as defined in the specification (section 4.3.1.4 and
		 * A.2).
		 */
		public String contentType;
	}
}