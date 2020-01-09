package gov.nist.itl.wsbd.example;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.ws.rs.Path;

import org.oasis_open.docs.biometrics.ns.ws_bd_1.Dictionary;
import org.oasis_open.docs.biometrics.ns.ws_bd_1.Result;
import org.oasis_open.docs.biometrics.ns.ws_bd_1.Status;

import gov.nist.itl.wsbd.configuration.ServiceConfiguration;
import gov.nist.itl.wsbd.dictionary.DictionaryWrapper;
import gov.nist.itl.wsbd.service.SensorService;
import gov.nist.itl.wsbd.service.Utility;
import gov.nist.itl.wsbd.streaming.IllegalResourceException;

/**
 * Represents: a simulated sensor that generates cool pictures
 *
 * @author Jacob Glueck
 *
 */
@Path("simulatedservice")
public class SimulatedSensorService extends SensorService {
	
	public static final int SIZE = 100;
	public static final double K = 2 * Math.PI / (SimulatedSensorService.SIZE / 1);
	public static final double RIPPLE_K = 2 * Math.PI / (SimulatedSensorService.SIZE / 5);
	public static final double OMEGA = 2 * Math.PI / 5000;
	public static final Function<Long, BiFunction<Integer, Integer, Double>> WAVE_FUNCTION = t -> (x, y) -> Math.sin(SimulatedSensorService.OMEGA * t) * Math.sin(SimulatedSensorService.K * x)
			* Math.sin(SimulatedSensorService.K * y);
	public static final BiFunction<Double, Double, Double> SHIFT = (a, b) -> a + b;
	public static final BiFunction<Double, Double, Double> SCALE = (a, b) -> a * b;
	public static final Function<Double, Double> NORM = a -> SimulatedSensorService.SCALE.apply(0.5, SimulatedSensorService.SHIFT.apply(1.0, a));
	public static final Function<Long, BiFunction<Integer, Integer, Double>> NORMED_WAVE_FUNCTION = t -> (x, y) -> SimulatedSensorService.NORM
			.apply(SimulatedSensorService.WAVE_FUNCTION.apply(t).apply(x, y));
	public static final Function<Long, BiFunction<Integer, Integer, Double>> RIPPLE_FUNCTION = t -> (x, y) -> Math
			.sin(Math.sqrt((x - SimulatedSensorService.SIZE / 2) * (x - SimulatedSensorService.SIZE / 2) + (y - SimulatedSensorService.SIZE / 2) * (y - SimulatedSensorService.SIZE / 2))
					* SimulatedSensorService.RIPPLE_K - SimulatedSensorService.OMEGA * t);
	public static final Function<Long, BiFunction<Integer, Integer, Double>> NORMED_RIPPLE_FUNCTION = t -> (x, y) -> SimulatedSensorService.NORM
			.apply(SimulatedSensorService.RIPPLE_FUNCTION.apply(t).apply(x, y));
	public static final Function<Double, BiFunction<Color, Color, Color>> MAP = v -> (i, f) -> new Color((int) (v * (f.getRed() - i.getRed()) + i.getRed()),
			(int) (v * (f.getGreen() - i.getGreen()) + i.getGreen()), (int) (v * (f.getBlue() - i.getBlue()) + i.getBlue()));
	
	private final ImageGenerator waveGenerator;
	private final ImageGenerator rippleGenerator;

	/**
	 * Creates: a new simulated sensor with the specified configuration
	 * 
	 * @param configuration
	 *            the configuration
	 * @throws IOException
	 *             if there is a problem
	 * @throws IllegalResourceException
	 *             if there is a problem
	 */
	public SimulatedSensorService(ServiceConfiguration configuration) throws IOException, IllegalResourceException {
		super(configuration);
		waveGenerator = new ImageGenerator(time -> (x, y) -> Color.getHSBColor(SimulatedSensorService.NORMED_WAVE_FUNCTION.apply(time).apply(x, y).floatValue(), 1.0f, 1.0f));
		rippleGenerator = new ImageGenerator(
				time -> (x, y) -> SimulatedSensorService.MAP.apply(SimulatedSensorService.NORMED_RIPPLE_FUNCTION.apply(time).apply(x, y)).apply(new Color(0x00CED1), new Color(0xAFEEEE)));
	}
	
	@Override
	protected Result initialize() {
		return Utility.result(Status.SUCCESS);
	}

	@Override
	protected Result uninitialize() {
		return Utility.result(Status.SUCCESS);
	}

	@Override
	protected Result setConfiguration(Dictionary configuration) {
		return Utility.result(Status.SUCCESS);
	}

	@Override
	protected void handleDefaultConfiguration(Dictionary configuration) {
	}

	@Override
	protected void setUpService() {
	}

	@Override
	protected Result endCapture(List<CaptureData> captureData) {
		CaptureData data = new CaptureData();
		data.contentType = "image/png";
		byte[] image = waveGenerator.generatePng(SimulatedSensorService.SIZE, SimulatedSensorService.SIZE);
		data.data = new ByteArrayInputStream(image);
		// We do not really know the size, so just forget about it
		data.size = image.length;
		captureData.add(data);
		return Utility.result(Status.SUCCESS);
		
	}

	@Override
	protected Result compressData(InputStream inData, OutputStream outData, String maxSize) {
		try {
			// Just copy it, we do not support compression
			Utility.drain(inData, outData);
			return Utility.result(Status.SUCCESS);
		} catch (IOException e) {
			return Utility.result(Status.FAILURE, e.getMessage());
		}
	}

	@Override
	protected void minimizeMetadata(DictionaryWrapper<Object> metadata) {
	}

	@Override
	protected InputStream getStream(String streamName) {
		ImageGenerator generator;
		if (streamName.equals("public")) {
			generator = waveGenerator;
		} else {
			generator = rippleGenerator;
		}

		try {
			PipedOutputStream out = new PipedOutputStream();
			PipedInputStream stream = new PipedInputStream(out);
			Thread streamThread = new Thread(() -> {
				try {
					while (true) {
						byte[] image = generator.generatePng(SimulatedSensorService.SIZE, SimulatedSensorService.SIZE);
						out.write(("--foo\r\nContent-Type: image/pngContent-Length:\r\n" + image.length + "\r\n\r\n").getBytes());
						out.write(image);
					}
				} catch (IOException e) {
					
				}
			});
			streamThread.setDaemon(true);
			streamThread.start();
			return stream;
		} catch (IOException e) {
			return null;
		}
	}
}
