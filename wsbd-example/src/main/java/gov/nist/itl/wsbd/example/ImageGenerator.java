package gov.nist.itl.wsbd.example;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.imageio.ImageIO;

/**
 * Represents: a class that generates images
 *
 * @author Jacob Glueck
 *
 */
public class ImageGenerator {

	/**
	 * The function f(time, x, y) = color
	 */
	private final Function<Long, BiFunction<Integer, Integer, Color>> colorFunction;
	
	/**
	 * Creates: a new image generator with the specified color function
	 *
	 * @param colorFunction
	 *            the color function: f(time, x, y) = color
	 */
	public ImageGenerator(Function<Long, BiFunction<Integer, Integer, Color>> colorFunction) {
		this.colorFunction = colorFunction;
	}

	/**
	 * Creates: a new image generator with the specified color function for each
	 * channel. Each function has a range of [0, 1].
	 *
	 * @param r
	 *            f(time, x, y)
	 * @param g
	 *            f(time, x, y)
	 * @param b
	 *            f(time, x, y)
	 */
	public ImageGenerator(Function<Long, BiFunction<Integer, Integer, Number>> r, Function<Long, BiFunction<Integer, Integer, Number>> g, Function<Long, BiFunction<Integer, Integer, Number>> b) {
		this(t -> (x, y) -> new Color(r.apply(t).apply(x, y).floatValue(), g.apply(t).apply(x, y).floatValue(), b.apply(t).apply(x, y).floatValue()));
	}

	/**
	 * Creates: a new image with the specified width and height
	 *
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 * @return the image
	 */
	public BufferedImage generate(int width, int height) {
		long time = System.currentTimeMillis();
		int[][] data = new int[height][width];
		BiFunction<Integer, Integer, Color> f = colorFunction.apply(time);
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				data[row][col] = f.apply(col, row).getRGB();
			}
		}
		return ImageGenerator.getImageFromArray(data);
	}
	
	/**
	 * Creates: a new image as a PNG with the specified width and height
	 *
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 * @return the image
	 */
	public byte[] generatePng(int width, int height) {
		try {
			BufferedImage i = generate(width, height);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(i, "png", out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Effect: creates an image from the specified array of pixels. The height
	 * is the number of rows ({@code pixels.length}) and the width is the number
	 * of columns ({@code pixels[0].length}. Each pixel is a color.
	 *
	 * @param pixels
	 *            the pixels
	 * @return the image
	 */
	private static BufferedImage getImageFromArray(int[][] pixels) {
		BufferedImage image = new BufferedImage(pixels[0].length, pixels.length, BufferedImage.TYPE_INT_ARGB);
		for (int row = 0; row < pixels.length; row++) {
			for (int col = 0; col < pixels[row].length; col++) {
				image.setRGB(col, row, pixels[row][col]);
			}
		}
		return image;
	}
}
