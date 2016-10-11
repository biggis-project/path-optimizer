package joachimrussig.heatstressrouting.thermalcomfort;

import java.time.LocalDateTime;
import java.util.OptionalDouble;

import joachimrussig.heatstressrouting.weatherdata.WeatherData;

/**
 * The air temperature as measure of thermal comfort.
 * 
 * @author Joachim Rußig
 */
public class ThermalComfortTemperature extends ThermalComfortAbstract {

	/**
	 * Temperature which is consider as comfortable.
	 */
	public static final double COMFORT_TEMPERATURE = 20;

	public ThermalComfortTemperature(WeatherData weatherData) {
		super(weatherData);
	}

	/**
	 * Returns the temperature at {@code time} if it is above
	 * {@code COMFORT_TEMPERATURE} and {@code COMFORT_TEMPERATURE} otherwise,
	 * because temperature at and below {@code COMFORT_TEMPERATURE} consider as
	 * comfortable. Returns {@code OptionalDouble.emtpy()} if no data available
	 * at {@code time}.
	 * 
	 * @param time
	 */
	@Override
	public OptionalDouble value(LocalDateTime time) {
		if (!getWeatherData().inTimeRange(time))
			return OptionalDouble.empty();

		double temp = getWeatherData().getTemperature(time);
		if (temp > COMFORT_TEMPERATURE)
			return OptionalDouble.of(temp);
		else
			return OptionalDouble.of(COMFORT_TEMPERATURE);
	}
}
