package joachimrussig.heatstressrouting.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Random;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import joachimrussig.heatstressrouting.weatherdata.WeatherData;

/**
 * Utility class containing convince functions used elsewhere.
 * 
 * @author Joachim Rußig
 */
public class Utils {

	private Utils() {
	}

	/**
	 * Checks if the string {@code value} can be parsed as an unsigned long.
	 * 
	 * @param val
	 *            the string to check
	 * @return true, if the string can be parsed as unsigned long value
	 */
	public static boolean isUnsignedLong(String val) {
		val = val.trim();
		try {
			Long.parseUnsignedLong(val);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Returns a formated string representation of the {@code Duration} object.
	 * Format: HH:MM:SS.
	 * 
	 * @see "http://stackoverflow.com/questions/266825/how-to-format-a-duration-
	 *      in-java-e-g-format-hmmss"
	 * @param duration
	 *            the {@code Duration} object to format
	 * @return a formated string representation of {@code duration}
	 */
	public static String formatDuration(Duration duration) {
		long seconds = duration.getSeconds();
		long absSeconds = Math.abs(seconds);
		String positive = String.format("%d:%02d:%02d", absSeconds / 3600,
				(absSeconds % 3600) / 60, absSeconds % 60);
		return seconds < 0 ? "-" + positive : positive;
	}

	/**
	 * Returns a formated string representation of {@code seconds}. Format:
	 * HH:MM:SS.
	 * 
	 * @param seconds
	 *            the duration to format in seconds
	 * @return a formated string representation of {@code seconds}
	 */
	public static String formatDuration(long seconds) {
		long absSeconds = Math.abs(seconds);
		String positive = String.format("%d:%02d:%02d", absSeconds / 3600,
				(absSeconds % 3600) / 60, absSeconds % 60);
		return seconds < 0 ? "-" + positive : positive;
	}

	/**
	 * Returns a formated string representation of {@code millis}. Format:
	 * HH:MM:SS.
	 * 
	 * @param millis
	 *            the duration to format in milliseconds
	 * @return a formated string representation of {@code millis}
	 */
	public static String formatDurationMills(long millis) {
		long seconds = millis / 1000;
		long absSeconds = Math.abs(seconds);
		String positive = String.format("%d:%02d:%02d.%03d", absSeconds / 3600,
				(absSeconds % 3600) / 60, absSeconds % 60, millis % 1000);
		return seconds < 0 ? "-" + positive : positive;
	}

	/**
	 * Returns a random long value between {@code lower} and {@code upper}
	 * (endpoints included).
	 * 
	 * @param rng
	 *            the random number generator to generate the random number
	 * @param lower
	 *            lower bound
	 * @param upper
	 *            upper bound
	 * 
	 * @return random long value between {@code lower} and {@code upper}
	 * 
	 * @throws IllegalStateException
	 *             if {@code lower} is not smaller than {@code upper}
	 * 
	 */
	public static long nextRandomLong(Random rng, long lower, long upper) {

		if (lower >= upper)
			throw new IllegalStateException("lower must be smaller then upper");

		RandomDataGenerator rndGenerator = new RandomDataGenerator(
				RandomGeneratorFactory.createRandomGenerator(rng));

		return rndGenerator.nextLong(lower, upper);
	}

	/**
	 * Returns a uniformly distributed random double value in the opened
	 * interval ({@code lower}, {@code upper}).
	 * 
	 * @param rng
	 *            the random number generator used to generate the value
	 * @param lower
	 *            lower bound
	 * @param upper
	 *            upper bound
	 * 
	 * @return random double value between {@code lower} and {@code upper}
	 * 
	 * @throws IllegalStateException
	 *             if {@code lower} is not smaller than {@code upper}
	 * 
	 */
	public static double nextRandomDouble(Random rng, double lower,
			double upper) {

		if (lower >= upper)
			throw new IllegalStateException("lower must be smaller then upper");

		RandomDataGenerator rndGenerator = new RandomDataGenerator(
				RandomGeneratorFactory.createRandomGenerator(rng));

		return rndGenerator.nextUniform(lower, upper);
	}

	/**
	 * Returns the value {@code x} normalized on [0,1]: (x - min) / (max - min).
	 * 
	 * @param x
	 *            the value to normalize
	 * @param min
	 *            min value used for normalization
	 * @param max
	 *            max value used for normalization
	 * @return {@code x} normalized on [0,1]
	 */
	public static double normalize(double x, double min, double max) {
		return (x - min) / (max - min);
	}

	/**
	 * Checks if all temperatures at {@code date} in {@code weatherData} are
	 * greater or equals to {@code minTemperature}.
	 * 
	 * @param weatherData
	 *            the weather data
	 * @param date
	 *            date to check
	 * @param minTemperature
	 *            the minimal temperature
	 * @return true, if all temperatures at {@code date} in {@code weatherData}
	 *         are greater or equals to {@code minTemperature}
	 */
	public static boolean allTempsGreaterEqMinTemp(WeatherData weatherData,
			LocalDate date, double minTemperature) {
		LocalDateTime fromDate = LocalDateTime.of(date, LocalTime.MIN);
		LocalDateTime toDate = LocalDateTime.of(date, LocalTime.MAX);
		return weatherData.getWeatherRecords()
				.subMap(fromDate, true, toDate, true).values().stream()
				.allMatch(w -> w.getTemperature() >= minTemperature);
	}
}
