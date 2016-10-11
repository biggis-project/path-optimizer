package joachimrussig.heatstressrouting.thermalcomfort;

/**
 * Utility class to compute an approximation of Steadman's heat index (Steadman,
 * 1979) using the formula published by Stull (2011, p. 77).
 * <p>
 * The heat index (or apparent temperature or temperature-humidity index) is a
 * measure of heat discomfort and heat-stress danger (Stull, 2011, pp. 76-78;
 * Steadman, 1979).
 *
 * <p>
 * Steadman, R. G. The Assessment of Sultriness. Part I: A Temperature-Humidity
 * Index Based on Human Physiology and Clothing. Science Journal of Applied
 * Meteorology, 1979, 18, 861-873, DOI: 10.1175/1520-0450(1979)018
 * <0861:TAOSPI>2.0.CO;2 <br>
 * Stull, R. Meteorology for Scientists and Engineers. Vancouver: University of
 * British Columbia, 2011, 3rd Ed. URL:
 * https://www.eoas.ubc.ca/books/Practical_Meteorology/mse3.html
 * 
 * @author Joachim Rußig
 */
public class HeatIndex {
	/**
	 * The minimum air temperature (in °C) the heat index is defined for.
	 */
	public static final double MIN_TEMPERATURE = 20;
	/**
	 * The maximum air temperature (in °C) the heat index is defined for.
	 */
	public static final double MAX_TEMPERATURE = 50;

	/**
	 * The minimum relative humidity (in %) the heat index is defined for.
	 */
	public static final double MIN_HUMIDITY = 0;
	/**
	 * The maximum relative humidity (in %) the heat index is defined for.
	 */
	public static final double MAX_HUMIDITY = 100;

	private HeatIndex() {
	}

	/**
	 * Computes an approximation of Steadman's heat index (Steadman, 1979) using
	 * the formula published by Stull (2011, p. 77).
	 * 
	 * @param temperature
	 *            in °C; the heat index is only defined for values between 20°C
	 *            and 50°C
	 * @param humidity
	 *            the relative humidity in % (a value between 0 and 100)
	 * @return approximation of the heat index
	 */
	public static double heatIndex(double temperature, double humidity) {
		if (!isValidTemperature(temperature)) {
			throw new IllegalArgumentException(
					"heat index is only defined for temperatures between 20°C and 50°C");
		}

		if (!isValidHumidity(humidity)) {
			throw new IllegalArgumentException(
					"humidity must be a value between 0 and 100");
		}

		double er = 1.6; // reference vapor pressure (in kPa)
		double tr = 0.8841 * temperature + 0.19;
		double p = 0.0196 * temperature + 0.9031;
		double es = 0.611 * Math
				.exp(5423 * ((1 / 273.15) - (1 / (temperature + 273.15))));
		return tr + (temperature - tr)
				* Math.pow((humidity * es) / (100 * er), p);
	}

	/**
	 * Checks if {@code temperature} is in range [{@code MIN_TEMPERATURE},
	 * {@code MAX_TEMPERATURE}].
	 * 
	 * @param temperature
	 *            the air temperature (in °C) to check
	 * @return true, if {@code temperature} is in range [{@code MIN_TEMPERATURE}
	 *         , {@code MAX_TEMPERATURE}]
	 */
	public static boolean isValidTemperature(double temperature) {
		return temperature >= MIN_TEMPERATURE && temperature <= MAX_TEMPERATURE;
	}

	/**
	 * Checks if {@code humidity} is in range [{@code MIN_HUMIDITY},
	 * {@code MAX_HUMIDITY}]
	 * 
	 * @param humidity
	 *            the relative humidity (in %) to check
	 * @return true, if {@code humidity} is in range [{@code MIN_HUMIDITY},
	 *         {@code MAX_HUMIDITY}]
	 */
	public static boolean isValidHumidity(double humidity) {
		return humidity >= MIN_HUMIDITY && humidity <= MAX_HUMIDITY;
	}

}
