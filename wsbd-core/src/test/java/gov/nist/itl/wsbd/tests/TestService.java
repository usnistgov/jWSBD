package gov.nist.itl.wsbd.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.ws.rs.Path;

import org.oasis_open.docs.bioserv.ns.wsbd_1.Dictionary;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Result;
import org.oasis_open.docs.bioserv.ns.wsbd_1.Status;

import gov.nist.itl.wsbd.configuration.ServiceConfiguration;
import gov.nist.itl.wsbd.dictionary.DictionaryWrapper;
import gov.nist.itl.wsbd.service.SensorService;
import gov.nist.itl.wsbd.service.Utility;
import gov.nist.itl.wsbd.streaming.IllegalResourceException;

/**
 * Represents: a test sensor service which can simulate a sensor failure
 *
 * @author Jaocb Glueck
 *
 */
@Path("testservice")
public class TestService extends SensorService {

	/**
	 * The current sensor state
	 */
	private SensorState currentState;
	/**
	 * The sensor state lock
	 */
	private final Object lock = new Object();

	/**
	 * A quanta of time, in ms
	 */
	public static final int TIME_INCREMENT = 300;
	/**
	 * Half of the time quanta (ideal for a cancel so it will happen in the
	 * middle).
	 */
	public static final int TIME_OFFSET_1_2 = TestService.TIME_INCREMENT / 2;
	/**
	 * A third of the time quanta
	 */
	public static final int TIME_OFFSET_1_3 = TestService.TIME_INCREMENT / 3;
	/**
	 * Two thirds of the time quanta
	 */
	public static final int TIME_OFFSET_2_3 = 2 * TestService.TIME_INCREMENT / 3;
	
	/**
	 * The amount of time an initialization takes
	 */
	public volatile int initializationTime = TestService.TIME_INCREMENT;
	/**
	 * The amount of time an uninitialization takes
	 */
	public volatile int uninitializationTime = TestService.TIME_INCREMENT;
	/**
	 * The amount of time it takes to set the configuration
	 */
	public volatile int setConfigurationTime = TestService.TIME_INCREMENT;
	/**
	 * A handler that gets the configuration for set configuration
	 */
	public volatile Consumer<Dictionary> setConfigurationHandler = (thing) -> {
	};
	/**
	 * The amount of time it takes to end a capture
	 */
	public volatile int endCaptureTime = TestService.TIME_INCREMENT;
	/**
	 * The amount of time it takes to begin a capture
	 */
	public volatile int beginCaptureTime = TestService.TIME_INCREMENT;
	/**
	 * A function which generates capture data
	 */
	public volatile Supplier<String> captureDataSupplier = () -> {
		return "I am a super cool test capture.";
	};
	/**
	 * The amount of time it takes to get the capture status
	 */
	public volatile int getCaptureStatus = TestService.TIME_OFFSET_1_3;
	
	/**
	 * Creates: a new test sensor service with the specified configuration
	 *
	 * @param configuration
	 *            the configuration
	 * @throws IllegalResourceException
	 *             if there is a problem
	 * @throws IOException
	 *             if there is a problem
	 */
	public TestService(ServiceConfiguration configuration) throws IOException, IllegalResourceException {
		super(configuration);
	}

	/**
	 * Effect: if true, mark the sensor as failed. If false, and the sensor is
	 * currently failed, mark the sensor as uninitialized.
	 *
	 * @param failed
	 *            true it the sensor should fail
	 */
	public void setSensorFailed(boolean failed) {
		synchronized (lock) {
			if (failed) {
				currentState = SensorState.FAILED;
			} else if (currentState == SensorState.FAILED) {
				currentState = SensorState.UNINITIALIZED;
			}
		}
	}

	@Override
	protected Result initialize() {
		return doOp(initializationTime, () -> {
			currentState = SensorState.INITIALIZED;
			return Utility.result(Status.SUCCESS);
		});

	}
	
	@Override
	protected Result uninitialize() {
		return doOp(uninitializationTime, () -> {
			currentState = SensorState.UNINITIALIZED;
			return Utility.result(Status.SUCCESS);
		});
	}
	
	@Override
	protected Result setConfiguration(Dictionary configuration) {
		return doOp(setConfigurationTime, () -> {
			setConfigurationHandler.accept(configuration);
			return Utility.result(Status.SUCCESS);
		});
	}

	@Override
	protected Result beginCapture() {
		return doOp(beginCaptureTime, () -> super.beginCapture());
	}
	
	@Override
	protected Result endCapture(List<CaptureData> captureData) {
		return doOp(endCaptureTime, () -> {
			byte[] data = captureDataSupplier.get().getBytes();
			CaptureData result = new CaptureData();
			result.data = new ByteArrayInputStream(data);
			result.size = data.length;
			result.contentType = "text/html";
			captureData.add(result);
			return Utility.result(Status.SUCCESS);
		});
	}

	@Override
	protected Result compressData(InputStream inData, OutputStream outData, String maxSize) {
		try {
			Utility.drain(inData, outData);
			return Utility.result(Status.SUCCESS);
		} catch (IOException e) {
			return Utility.result(Status.FAILURE, e.getMessage());
		}
	}
	
	@Override
	protected void minimizeMetadata(DictionaryWrapper<Object> metadata) {
		
	}

	/**
	 * Effect: waits the amount of time. If the wait is interrupted, returns the
	 * appropriate type of cancel. If the sensor has failed, returns failed
	 * after the wait. Otherwise, returns the result of the operation.
	 *
	 * @param millis
	 *            the amount of time to wait
	 * @param op
	 *            the operation
	 * @return the result
	 */
	private Result doOp(int millis, Callable<Result> op) {
		synchronized (lock) {
			try {
				Thread.sleep(millis);
			} catch (InterruptedException e) {
				if (currentState == SensorState.FAILED) {
					return Utility.result(Status.CANCELED_WITH_SENSOR_FAILURE);
				} else {
					return Utility.result(Status.CANCELED);
				}
			}
			if (currentState == SensorState.FAILED) {
				return Utility.result(Status.SENSOR_FAILURE);
			}
			try {
				return op.call();
			} catch (Exception e) {
				return Utility.result(Status.FAILURE, e.getMessage());
			}
		}
	}

	/**
	 * Represents: the state of the sensor
	 *
	 * @author Jacob Glueck
	 */
	private enum SensorState {
		/**
		 * The sensor has not been initialized yet
		 */
		UNINITIALIZED,
		/**
		 * The sensor has been initialized
		 */
		INITIALIZED,
		/**
		 * The sensor has failed
		 */
		FAILED;
	}

	@Override
	protected void handleDefaultConfiguration(Dictionary configuration) {
		
	}

	@Override
	protected void setUpService() {
		
	}

	@Override
	protected InputStream getStream(String streamName) {
		byte[] data = streamName.getBytes();
		return new InputStream() {

			int index = 0;

			@Override
			public int read() throws IOException {
				int result = data[index];
				index = (index + 1) % data.length;
				return result;
			}
		};
	}
	
}
