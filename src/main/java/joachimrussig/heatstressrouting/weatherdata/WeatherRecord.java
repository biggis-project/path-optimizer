package joachimrussig.heatstressrouting.weatherdata;

import java.time.LocalDateTime;

/**
 * The {@code WeatherRecord} class represents a single weather record. A
 * weather record consists of an air temperature value (in °C) and a relative humidity
 * (in %) value and a point in time at which the values were measured.
 * 
 * @author Jochim Rußig
 */
public class WeatherRecord {

	/**
	 * The types of the stored weather data.
	 * 
	 * @author Joachim Rußig
	 *
	 */
	public enum ValueType {
		TEMPERATURE, RELATIVE_HUMIDITY;

		boolean isTemperature() {
			return this == TEMPERATURE;
		}

		boolean isRelativeHumidity() {
			return this == RELATIVE_HUMIDITY;
		}
	}

	private final LocalDateTime time;
	private final double temperature;
	private final double relativeHumidity;

	/**
	 * Creates a new {@code WeatherRecord} with the point in time {@code time}
	 * and the values {@code temperature} and {@code humidity}.
	 * 
	 * @param time
	 *            point in at witch the values were measured
	 * @param temperature
	 *            the air temperature value in °C
	 * @param relativeHumidity
	 *            the relative humidity value in %
	 */
	public WeatherRecord(LocalDateTime time, double temperature,
			double relativeHumidity) {
		this.time = time;
		this.temperature = temperature;
		this.relativeHumidity = relativeHumidity;
	}

	/**
	 * Returns the value of type {@code type} at the point in time {@code time}.
	 * 
	 * @param time
	 *            requested point in time
	 * @param type
	 *            type of the requested value
	 * @return the value of type {@code type} at time {@code time} 
	 * 
	 * @throws IllegalStateException
	 *             if an unsupported {@code VauleType} is passed
	 */
	public double getValue(LocalDateTime time, ValueType type) {
		switch (type) {
		case TEMPERATURE:
			return getTemperature();
		case RELATIVE_HUMIDITY:
			return getRelativeHumidity();
		default:
			throw new IllegalStateException(
					"unsupported Type " + type.toString());
		}
	}

	/**
	 * 
	 * @return the contained air temperature value in °C
	 */
	public double getTemperature() {
		return temperature;
	}

	/**
	 * 
	 * @return the contained relative humidity value in %
	 */
	public double getRelativeHumidity() {
		return relativeHumidity;
	}

	/**
	 * 
	 * @return the point in time the values were measured
	 */
	public LocalDateTime getTime() {
		return time;
	}

	@Override
	public String toString() {
		return "WeatherRecord(time = " + time + ", temperature = " + temperature
				+ ", relativeHumidity = " + relativeHumidity + ")";
	}
}
