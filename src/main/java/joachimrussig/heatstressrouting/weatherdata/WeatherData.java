package joachimrussig.heatstressrouting.weatherdata;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import joachimrussig.heatstressrouting.thermalcomfort.HeatIndex;
import joachimrussig.heatstressrouting.util.TimeRange;

/**
 * The {@code WeatherData} class represents the weather data in a certain time
 * range.
 * 
 * @author Joachim Rußig
 */
public class WeatherData {

	private final TreeMap<LocalDateTime, WeatherRecord> weatherRecords;
	private ZoneId zoneId;

	/**
	 * Creates a new instance of the {@code WeatherData} class.
	 * <p>
	 * <b>Note:</b> the key of the tree map and time of the value must be equals
	 * 
	 * @param weatherRecords
	 *            the weather records to store
	 * @param zoneId
	 *            the time zone id of the stored values
	 */
	WeatherData(TreeMap<LocalDateTime, WeatherRecord> weatherRecords,
			ZoneId zoneId) {
		this.weatherRecords = weatherRecords;
		this.zoneId = zoneId;
	}

	/**
	 * Creates a new instance of the {@code WeatherData} class.
	 * 
	 * @param weatherRecords
	 *            the weather records to store
	 * @param zoneId
	 *            the time zone id of the stored values
	 */
	public WeatherData(Collection<WeatherRecord> weatherRecords,
			ZoneId zoneId) {
		this.weatherRecords = weatherRecords.stream()
				.map(r -> Pair.of(r.getTime(), r))
				.collect(Collectors.toMap(Pair::getKey, Pair::getValue,
						(a, b) -> a, TreeMap::new));
		this.zoneId = zoneId;
	}

	/**
	 * * Creates a new instance of the {@code WeatherData} class. The time zone
	 * id is set to {@code ZoneId.systemDefault()}.
	 * <p>
	 * <b>Note:</b> the key of the tree map and time of the value must be equals
	 * 
	 * @param weatherRecords
	 *            the weather records to store
	 */
	WeatherData(TreeMap<LocalDateTime, WeatherRecord> weatherRecords) {
		this(weatherRecords, ZoneId.systemDefault());
	}

	/**
	 * Returns the temperature at {@code time}, if it is present in
	 * {@code weatherRecords}, or a linear interpolation between the previous
	 * and the next value.
	 * 
	 * @param time
	 *            point in time the temperature is requested for
	 * @return the temperature or a linear interpolation (in °C)
	 * 
	 * @throws IllegalArgumentException
	 *             if {@code time} is not within time range of the stored
	 *             weather records
	 */
	public double getTemperature(LocalDateTime time) {
		return interpolate(time, WeatherRecord.ValueType.TEMPERATURE);
	}

	/**
	 * Returns the relative humidity at {@code time} if it is present in
	 * {@code weatherRecords} or an linear interpolation between the adjacent
	 * values.
	 * 
	 * @param time
	 *            point in time the relative humidity is requested for
	 * @return the relative humidity or a linear interpolation (in %)
	 * 
	 * @throws IllegalArgumentException
	 *             if {@code time} is not within time range of the stored
	 *             weather records
	 */
	public double getRelativeHumidity(LocalDateTime time) {
		return interpolate(time, WeatherRecord.ValueType.RELATIVE_HUMIDITY);
	}

	/**
	 * Returns the weather record at {@code time} if the values are present
	 * {@code weatherRecords} or a linear interpolation between the previous and
	 * next value.
	 * 
	 * @param time
	 *            point in time the weather record is requested for
	 * @return the weather record at {@code time} if the values are present
	 *         {@code weatherRecords} or a linear interpolation between the
	 *         adjacent
	 * 
	 * @throws IllegalArgumentException
	 *             if {@code time} is not within time range of the stored
	 *             weather records
	 */
	public WeatherRecord getWeatherRecord(LocalDateTime time) {
		double temp = getTemperature(time);
		double rh = getRelativeHumidity(time);
		return new WeatherRecord(time, temp, rh);
	}

	/**
	 * Returns an approximation of Steadman's heat index at {@code time}, or
	 * {@code OptionalDouble.empty()} if either temperature is not between 20°C
	 * and 50°C or relative humidity is not between 0 and 100.
	 * 
	 * @param time
	 *            point in time the heat index is requested for
	 * @return approximation of Steadman's heat index at {@code time}, or
	 *         {@code OptionalDouble.empty()} if either temperature or relative
	 *         humidity is not valid
	 * 
	 * @throws IllegalArgumentException
	 *             if {@code time} is not within time range of the stored
	 *             weather records
	 */
	public OptionalDouble getHeatIndex(LocalDateTime time) {
		if (!inTimeRange(time)) {
			throw new IllegalArgumentException("time " + time
					+ " is not within timeRange (" + weatherRecords.firstKey()
					+ " - " + weatherRecords.lastKey() + ")");
		}

		double temp = getTemperature(time);
		double rh = getRelativeHumidity(time);
		if (HeatIndex.isValidTemperature(temp) && HeatIndex.isValidHumidity(rh))
			return OptionalDouble.of(HeatIndex.heatIndex(temp, rh));
		else
			return OptionalDouble.empty();
	}

	/**
	 * Returns the value of type {@code type} at time {@code time} if it is
	 * present, or a linear interpolation between the previous and next value.
	 * 
	 * @param time
	 *            point in time the value is requested for
	 * @param type
	 *            type of the requested value
	 * @return the value of type {@code type} at time {@code time} if it is
	 *         present, or a linear interpolation between the previous and next
	 *         value.
	 * 
	 * @throws IllegalArgumentException
	 *             if {@code time} is not within time range of the stored
	 *             weather records
	 * @throws IllegalArgumentException
	 *             if the {@code WeatherRecord.ValueType} passed is not
	 *             supported
	 */
	private double interpolate(LocalDateTime time,
			WeatherRecord.ValueType type) {
		if (!inTimeRange(time)) {
			throw new IllegalArgumentException("time " + time
					+ " is not within timeRange (" + weatherRecords.firstKey()
					+ " - " + weatherRecords.lastKey() + ")");
		}

		if (!type.isTemperature() && !type.isRelativeHumidity()) {
			throw new IllegalArgumentException(
					"unsupported weather record type " + type);
		}

		if (weatherRecords.containsKey(time)) {
			// a record for the requested time is present, so we just return
			// that
			return weatherRecords.get(time).getValue(time, type);
		} else {
			// we've got to interpolate the value
			double timeDouble = time.atZone(ZoneId.systemDefault()).toInstant()
					.toEpochMilli();

			// point in time of the previous respectively next weather record
			LocalDateTime lower = time.truncatedTo(ChronoUnit.HOURS);
			LocalDateTime upper = lower.plusHours(1);

			// LinearInterpolator can only deal with x values of type double so
			// we have to convert them
			double lowerDouble = (double) lower.atZone(zoneId).toInstant()
					.toEpochMilli();
			double upperDouble = (double) upper.atZone(zoneId).toInstant()
					.toEpochMilli();

			// the x and y coordinates of two points used for interpolation
			double[] x = new double[] { lowerDouble, upperDouble };
			double[] y = new double[] {
					weatherRecords.get(lower).getValue(time, type),
					weatherRecords.get(upper).getValue(time, type) };

			// TODO remove
			// PolynomialSplineFunction poly = new LinearInterpolator()
			// .interpolate(x, y);
			//
			// Function<Double, Double> fun = linearEquation(x, y);
			//
			// double ret = poly.value(timeDouble);

			double ret = ((y[1] - y[0]) / (x[1] - x[0])) * (timeDouble - x[0])
					+ y[0];

			return ret;
		}
	}

	/**
	 * Checks if {@code time} is within the open interval (
	 * {@code weatherRecords.firstKey()}, {@code weatherRecords.lastKey()}).
	 * 
	 * @param time
	 *            point in time to check
	 * @return true, if {@code time} is within the open interval (
	 *         {@code weatherRecords.firstKey()},
	 *         {@code weatherRecords.lastKey()})
	 */
	public boolean inTimeRange(LocalDateTime time) {
		return time != null && !this.weatherRecords.isEmpty()
				&& time.isAfter(this.weatherRecords.firstKey())
				&& time.isBefore(this.weatherRecords.lastKey());
	}

	/**
	 * 
	 * @return a map of time and temperature stored within this class
	 */
	public Map<LocalDateTime, Double> getTemperatureDistribution() {
		return weatherRecords.entrySet().stream()
				.map(e -> Pair.of(e.getKey(), e.getValue().getTemperature()))
				.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
	}

	/**
	 * 
	 * @return the map used to internally store the contained weather data
	 */
	public TreeMap<LocalDateTime, WeatherRecord> getWeatherRecords() {
		return weatherRecords;
	}

	/**
	 * Returns the weather records in the closed interval [{@code from},
	 * {@code to}] (i.e. lower and upper bound are inclusive).
	 * 
	 * @param from
	 *            the lower bound
	 * @param to
	 *            the upper bound
	 * @return the weather records in the closed interval [{@code from},
	 *         {@code to}]
	 */
	public NavigableMap<LocalDateTime, WeatherRecord> getWeatherRecords(
			LocalDateTime from, LocalDateTime to) {
		return weatherRecords.subMap(from, true, to, true);
	}

	/**
	 * 
	 * @return the time range for which the weather records are stored, i.e. the
	 *         time range of {@code weatherRecords.firstKey()} and
	 *         {@code weatherRecords.lastKey()}
	 */
	public TimeRange<LocalDateTime> getTimeRange() {
		return new TimeRange<>(weatherRecords.firstKey(),
				weatherRecords.lastKey());
	}

	/**
	 * 
	 * @return the time zone id
	 */
	public ZoneId getZoneId() {
		return zoneId;
	}

	/**
	 * 
	 * @param zoneId
	 *            the time zone id to set
	 */
	public void setZoneId(ZoneId zoneId) {
		this.zoneId = zoneId;
	}
}
