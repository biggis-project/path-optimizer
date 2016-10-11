package joachimrussig.heatstressrouting.thermalcomfort;

import java.time.LocalDateTime;
import java.util.OptionalDouble;

import joachimrussig.heatstressrouting.weatherdata.WeatherData;

/**
 * An abstract thermal comfort class that uses {@link WeatherData} to store the
 * weather data.
 * 
 * @author Joachim Rußig
 */
public abstract class ThermalComfortAbstract implements ThermalComfort {
	private WeatherData weatherData;

	/**
	 * Creates a new {@code  ThermalComfortAbstract} object.
	 * 
	 * @param weatherData
	 *            the weather data to store
	 */
	public ThermalComfortAbstract(WeatherData weatherData) {
		this.weatherData = weatherData;
	}

	@Override
	public abstract OptionalDouble value(LocalDateTime time);

	/**
	 * 
	 * @return the stored weather data
	 */
	public WeatherData getWeatherData() {
		return weatherData;
	}

	/**
	 * sets the stored weather data
	 * 
	 * @param weatherData
	 */
	public void setWeatherData(WeatherData weatherData) {
		this.weatherData = weatherData;
	}
}
