package joachimrussig.heatstressrouting.evaluation.optimaltime;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.jooq.lambda.Seq;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import joachimrussig.heatstressrouting.osmdata.OSMData;
import joachimrussig.heatstressrouting.util.TimeRange;
import joachimrussig.heatstressrouting.util.Utils;
import joachimrussig.heatstressrouting.weatherdata.WeatherData;

/**
 * Factory to create random evaluation. The class proviedes different option to
 * control how the evaluation items are created.
 * 
 * @author joachimrussig
 *
 */
public class OptimalTimeEvaluationItemsFactory {

	private final Logger logger = LoggerFactory
			.getLogger(OptimalTimeEvaluationItemsFactory.class);

	private final Duration DEFAULT_TIME_BUFFER = Duration.ofMinutes(15);
	private static final int MAX_RETRIES = 20;

	private OSMData osmData;
	private WeatherData weatherData;

	private List<Tag> tags;
	private List<Node> starts;

	private int noStarts = 10;
	private int noTags = 1;

	private Random rnd = new Random();

	private TimeRange<LocalDateTime> timeRange;
	private Duration minRangeBetweenLimits = Duration.ZERO;
	private List<Duration> timeBuffers = null;
	private List<Double> maxDistances = null;
	private List<Integer> maxResults = null;

	private boolean timeLimits = false;

	private List<TimeRange<LocalTime>> nowTimeRanges = null;

	private Double minTemperature = null;

	/**
	 * 
	 * @param osmData
	 *            the OSM data
	 * @param weatherData
	 *            the weather data
	 * @param timeRange
	 *            maximum time range for the current time {@code now}
	 */
	public OptimalTimeEvaluationItemsFactory(OSMData osmData,
			WeatherData weatherData, TimeRange<LocalDateTime> timeRange) {
		this.osmData = osmData;
		this.weatherData = weatherData;
		this.timeRange = timeRange;
	}

	/**
	 * 
	 * @param seed
	 *            the seed for the random number generator
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setSeed(long seed) {
		this.rnd.setSeed(seed);
		return this;
	}

	/**
	 * 
	 * @param rnd
	 *            the random number generator used
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setRnd(Random rnd) {
		this.rnd = rnd;
		return this;
	}

	/**
	 * 
	 * @param noStarts
	 *            number of random start points
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setNoStarts(int noStarts) {
		this.noStarts = noStarts;
		return this;
	}

	/**
	 * 
	 * @param number
	 *            number of tags per each evluation item
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setNoTags(int number) {
		this.noTags = number;
		return this;
	}

	/**
	 * 
	 * @param timeLimits
	 *            indicates whether random earliest and latest points in time
	 *            should be generated
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setTimeLimits(boolean timeLimits) {
		this.timeLimits = timeLimits;
		return this;
	}

	/**
	 * Sets {@code timeLimits} to true and sets the minimum duration between
	 * earliest and latest to {@code duration}
	 * 
	 * @param minDuration
	 *            the minimum time range between earliest and latest
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setTimeLimits(
			Duration minDuration) {
		this.timeLimits = true;
		this.minRangeBetweenLimits = minDuration;
		return this;
	}

	/**
	 * 
	 * @param timeBuffers
	 *            that a list of possible time buffers; for each item one is
	 *            selected randomly
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setTimeBuffers(
			List<Duration> timeBuffers) {
		this.timeBuffers = timeBuffers;
		return this;
	}

	/**
	 * 
	 * @param maxDistances
	 *            maximum direct distances ("as the crow flies") between start
	 *            and place; for each itme one is selected randomly
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setMaxDistances(
			List<Double> maxDistances) {
		this.maxDistances = maxDistances;
		return this;
	}

	/**
	 * 
	 * @param maxDistance
	 *            maximum direct distances ("as the crow flies") between start
	 *            and place; used for all items
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setMaxDistance(
			Double maxDistance) {
		this.maxDistances = Lists.newArrayList(maxDistance);
		return this;
	}

	/**
	 * 
	 * @param maxResults
	 *            a list of maximum results to consider; for each item one is
	 *            selected randomly
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setMaxResults(
			List<Integer> maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	/**
	 * 
	 * @param maxResults
	 *            maximum number of results to consider; used for all items
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setMaxResults(Integer maxResults) {
		this.maxResults = Lists.newArrayList(maxResults);
		return this;
	}

	/**
	 * 
	 * @param starts
	 *            sets the number of random start points
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setStarts(List<Node> starts) {
		this.starts = starts;
		return this;
	}

	/**
	 * 
	 * @param tags
	 *            list of possible tags; {@code noTags} tags are selected for
	 *            each item randomly
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setTags(List<Tag> tags) {
		this.tags = tags;
		return this;
	}

	/**
	 * 
	 * @param nowTimeRange
	 *            a time range in which the time {@code now} is selected
	 *            randomly
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public OptimalTimeEvaluationItemsFactory setNowTimeRange(
			TimeRange<LocalTime> nowTimeRange) {
		this.nowTimeRanges = Lists.newArrayList(nowTimeRange);
		return this;
	}

	/**
	 * 
	 * @param nowTimeRanges
	 *            list of times range in which the time {@code now} is selected
	 *            randomly; for each item one of the time ranges is selected
	 *            randomly
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setNowTimeRanges(
			List<TimeRange<LocalTime>> nowTimeRanges) {
		this.nowTimeRanges = nowTimeRanges;
		return this;
	}

	/**
	 * 
	 * @param minTemperature
	 *            a minimum air temperature for all generated items
	 * @return
	 */
	public OptimalTimeEvaluationItemsFactory setMinTemperature(
			Double minTemperature) {
		this.minTemperature = minTemperature;
		return this;
	}

	/**
	 * 
	 * @return a list of randomly generated evaluation items
	 */
	public List<OptimalTimeEvaluationItem> createEvaluationItems() {

		if (this.tags == null) {
			List<Tag> tags = new ArrayList<>(
					OptimalTimeEvaluator.SHOP_TAGS.length
							+ OptimalTimeEvaluator.AMENITY_TAGS.length);
			tags.addAll(
					OptimalTimeEvaluator.createTags(OptimalTimeEvaluator.SHOP,
							Arrays.asList(OptimalTimeEvaluator.SHOP_TAGS)));
			tags.addAll(OptimalTimeEvaluator.createTags(
					OptimalTimeEvaluator.AMENITY,
					Arrays.asList(OptimalTimeEvaluator.AMENITY_TAGS)));
			this.tags = tags;
		}

		List<Node> starts;
		if (this.starts != null) {
			starts = this.starts;
		} else {
			starts = Seq.seq(osmData.getNodes().stream()).shuffle(rnd)
					.limit(noStarts).collect(Collectors.toList());
		}

		List<OptimalTimeEvaluationItem> res = new ArrayList<>(starts.size());

		for (Node start : starts) {
			List<Tag> tags = Seq.seq(this.tags).shuffle(rnd).limit(this.noTags)
					.collect(Collectors.toList());

			LocalDateTime now;
			if (nowTimeRanges != null) {
				TimeRange<LocalDate> dateRange = new TimeRange<>(
						timeRange.getFrom().toLocalDate(),
						timeRange.getTo().toLocalDate());
				LocalDate date = randomDateInRange(dateRange,
						this.minTemperature);
				int idx = rnd.nextInt(nowTimeRanges.size());
				LocalTime time = randomTimeInRange(nowTimeRanges.get(idx));
				now = LocalDateTime.of(date, time);
				OptimalTimeEvaluator.logger.debug("now = " + now
						+ ", nowTimeRange = " + nowTimeRanges.get(idx));
			} else {
				now = randomDateTimeInRange(timeRange, this.minTemperature);
			}

			Duration timeBuffer;
			if (timeBuffers != null && timeBuffers.size() > 0) {
				timeBuffer = timeBuffers.get(rnd.nextInt(timeBuffers.size()));
			} else {
				timeBuffer = DEFAULT_TIME_BUFFER;
			}

			Double maxDistance;
			if (maxDistances != null && maxDistances.size() > 0) {
				maxDistance = maxDistances
						.get(rnd.nextInt(maxDistances.size()));
			} else {
				maxDistance = null;
			}

			Integer maxResult;
			if (maxResults != null && maxResults.size() > 0) {
				maxResult = maxResults.get(rnd.nextInt(maxResults.size()));
			} else {
				maxResult = null;
			}

			LocalDateTime earliestTime = null;
			LocalDateTime latestTime = null;

			if (timeLimits) {
				TimeRange<LocalDateTime> range = new TimeRange<>(
						now.toLocalDate().atStartOfDay(),
						now.toLocalDate().atTime(LocalTime.MAX));
				Optional<TimeRange<LocalDateTime>> limits = randomTimeRange(
						range, minRangeBetweenLimits);
				if (limits.isPresent()) {
					earliestTime = limits.get().getFrom();
					latestTime = limits.get().getTo();
				}
			}
			res.add(new OptimalTimeEvaluationItem(start, tags, now, timeBuffer,
					earliestTime, latestTime, maxDistance, maxResult));

		}
		return res;

	}

	/**
	 * 
	 * @param range
	 * 
	 * @param minTemperature
	 *            a minimum air temperature; only those times are return where
	 *            all temperature at that date are greater or equals to
	 *            {@code minTemperature}
	 * @return a random {@link LocalDataTime} object in the specified range
	 *         {@code range}
	 */
	private LocalDateTime randomDateTimeInRange(TimeRange<LocalDateTime> range,
			Double minTemperature) {
		TimeRange<LocalDate> dateRange = new TimeRange<>(
				range.getFrom().toLocalDate(), range.getTo().toLocalDate());
		LocalDate date = randomDateInRange(dateRange, minTemperature);
		TimeRange<LocalTime> timeRange = new TimeRange<>(
				range.getFrom().toLocalTime(), range.getTo().toLocalTime());
		LocalTime time = randomTimeInRange(timeRange);
		if (date != null)
			return date.atTime(time);
		else
			return null;
	}

	/**
	 * 
	 * @param range
	 * @return a random {@link LocalTime} object in the specified range
	 *         {@code range}
	 */
	private LocalTime randomTimeInRange(TimeRange<LocalTime> range) {
		long offSet = Utils.nextRandomLong(rnd, 0L, range.durationInSeconds());
		return range.getFrom().plus(offSet, ChronoUnit.SECONDS);
	}

	/**
	 * 
	 * @param range
	 * @param minTemperature
	 *            a minimum air temperature; only those dates are returned,
	 *            where all temperature are greater or equals to
	 *            {@code minTemperature}
	 * @return a random {@link LocalDate} object in the specified range
	 *         {@code range}
	 */
	private LocalDate randomDateInRange(TimeRange<LocalDate> range,
			Double minTemperature) {
		long days = range.getDays().getAsLong();
		LocalDate date = null;
		int retries = 0;
		do {
			long offSet = Utils.nextRandomLong(rnd, 0L, days);
			date = range.getFrom().plusDays(offSet);
			retries++;
		} while (minTemperature != null && !Utils
				.allTempsGreaterEqMinTemp(weatherData, date, minTemperature)
				&& retries <= MAX_RETRIES);

		if (date == null)
			logger.error("date is null for time range " + range
					+ " and minTemperature = " + minTemperature);

		return date;
	}

	/**
	 * 
	 * @param range
	 * @param minRangeBetweenLimits
	 *            the minimum time between the earliest and latest point in time
	 * @return a random sub time range in {@code range}
	 */
	private Optional<TimeRange<LocalDateTime>> randomTimeRange(
			TimeRange<LocalDateTime> range, Duration minRangeBetweenLimits) {
		long rangeSecs = range.durationInSeconds();
		long minRange = minRangeBetweenLimits.getSeconds();

		if (minRange >= rangeSecs)
			return Optional.empty();

		long offSetLower = Utils.nextRandomLong(rnd, 0L,
				(rangeSecs - minRange));
		long offSetUpper = Utils.nextRandomLong(rnd, offSetLower, rangeSecs);

		LocalDateTime lower = range.getFrom().plusSeconds(offSetLower);
		LocalDateTime upper = range.getFrom().plusSeconds(offSetUpper);

		return Optional.of(new TimeRange<>(lower, upper));
	}

}