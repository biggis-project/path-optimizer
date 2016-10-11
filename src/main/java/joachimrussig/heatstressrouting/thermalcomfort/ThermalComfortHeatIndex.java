package joachimrussig.heatstressrouting.thermalcomfort;

import java.time.LocalDateTime;
import java.util.OptionalDouble;

import joachimrussig.heatstressrouting.weatherdata.WeatherData;

/**
 * Steadman's heat index as thermal comfort.
 * 
 * @author Joachim Rußig
 */
public class ThermalComfortHeatIndex extends ThermalComfortAbstract {

	/**
	 * Heat index temperature consider as comfortable.
	 */
	public static final double COMFORT_HEAT_INDEX = 20;

	public ThermalComfortHeatIndex(WeatherData weatherData) {
		super(weatherData);
	}

	/**
	 * Returns the heat index at {@code time} if it is above
	 * {@code COMFORT_HEAT_INDEX} and {@code COMFORT_HEAT_INDEX} otherwise,
	 * because heat index values at and below {@code COMFORT_TEMPERATURE}
	 * consider as comfortable. The air temperature is returned, if the air
	 * temperature is below {@code HeatIndex.MIN_TEMPERATURE}. Returns
	 * {@code OptionalDouble.emtpy()} if no data available at {@code time}.
	 * 
	 * @param time
	 */
	@Override
	public OptionalDouble value(LocalDateTime time) {
		if (!getWeatherData().inTimeRange(time))
			return OptionalDouble.empty();

		double temperature = getWeatherData().getTemperature(time);
		double humidity = getWeatherData().getRelativeHumidity(time);

		double heatIndex;
		if (!HeatIndex.isValidTemperature(temperature)
				|| !HeatIndex.isValidHumidity(humidity))
			// If temperature is not between 20°C and 50°C or humidity is not
			// between 0 and 100, then fall back to air temperature
			heatIndex = temperature;
		else
			heatIndex = HeatIndex.heatIndex(temperature, humidity);

		if (heatIndex > COMFORT_HEAT_INDEX)
			return OptionalDouble.of(heatIndex);
		else
			return OptionalDouble.of(COMFORT_HEAT_INDEX);
	}
}
