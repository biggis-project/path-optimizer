package joachimrussig.heatstressrouting.optimaltime.finder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.MultiStartUnivariateOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;
import org.jooq.lambda.Seq;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.PathWrapper;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.osmdata.OSMData;
import joachimrussig.heatstressrouting.osmdata.OSMUtils;
import joachimrussig.heatstressrouting.routing.HeatStressGraphHopper;
import joachimrussig.heatstressrouting.routing.RoutingHelper;
import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.util.TimeRange;
import joachimrussig.heatstressrouting.util.Utils;

/**
 * A class that finds the optimal point in time, i.e. the point in time with the
 * best value according to the specified {@link ObjectiveFunction}. To find the
 * optimal point in time the Brent method is used (as implemented by
 * {@link BrentOptimizer}). To prevent that the Brent method only finds a local
 * optimum the Brent optimizer is executed multiple times with {@code starts}
 * random start points.
 * 
 * @author Joachim Rußig
 */
public class OptimalTimeFinder {

	private static final Logger logger = LoggerFactory
			.getLogger(OptimalTimeFinder.class);

	public static final Duration DEFAULT_TIME_BUFFER = Duration.ofMinutes(15);

	protected static final double RELATIVE_THRESHOLD = 2 * Math.ulp(1d);

	protected static final double ABSOLUTE_THRESHOLD = 0.1;

	protected static final int MAX_EVAL = 100;

	protected static final GoalType GOAL_TYPE = GoalType.MINIMIZE;
	protected RoutingHelper routingHelper;
	protected OSMData osmData;
	protected GHPoint start;
	protected Node place;

	protected Duration timeBuffer = DEFAULT_TIME_BUFFER;
	protected LocalDateTime earliestTime;

	protected LocalDateTime latestTime;
	
	protected ArrayList<Throwable> lastWarnings;
	protected ArrayList<Throwable> lastErrors;
	
	protected ObjectiveFunction objectiveFunction;
	protected WeightingType defaultWeightingType = WeightingType.SHORTEST;
	
	protected int starts = 10;
	protected RandomGenerator rng = RandomGeneratorFactory
			.createRandomGenerator(new Random());

	protected ZoneId zoneId;
	/**
	 * 
	 * @param objectiveFunction
	 *            the objective function to optimize
	 * @param hopper
	 *            hopper used to find the routes
	 */
	public OptimalTimeFinder(ObjectiveFunction objectiveFunction,
			HeatStressGraphHopper hopper) {
		this(objectiveFunction, new RoutingHelper(hopper));
	}

	/**
	 * 
	 * @param objectiveFunction
	 *            the objective function to optimize
	 * @param routingHelper
	 *            routing helper used to find the routes
	 */
	public OptimalTimeFinder(ObjectiveFunction objectiveFunction,
			RoutingHelper routingHelper) {
		this.objectiveFunction = objectiveFunction;
		this.routingHelper = routingHelper;
		this.osmData = routingHelper.getHopper().getOsmData();
		this.zoneId = ZoneId.systemDefault();
		this.lastWarnings = new ArrayList<>();
		this.lastErrors = new ArrayList<>();
	}

	/**
	 * Finds the optimal point in time for the given place {@code place} and
	 * current time {@code now}.
	 * 
	 * @param start
	 *            the start point
	 * @param place
	 *            the place to find the optimal time for
	 * @param date
	 *            the date to find the optimal time for
	 * @param now
	 *            the current time
	 * @return the optimal point in time or {@code Optional.empty()} if non
	 *         result is found
	 */
	public Optional<OptimalTimeFinderResult> find(GHPoint start,
			Node place, Date date, LocalDateTime now) {
		
		this.lastWarnings.clear();
		this.lastErrors.clear();

		GHPoint placePoint = OSMUtils.getGHPoint(place);

		List<TimeRange<ZonedDateTime>> openingHours = getOpeningHours(place,
				date);

		if (openingHours.size() == 0) 
//			result.addError(new OptimalTimeFinderException("no opening hours found"));
			return Optional.empty();
		

		// estimation of the minimum walking time
		long minWalkingTime = routingHelper.findShortestRoute(start, placePoint)
				.get().map(PathWrapper::getTime).orElse(0L);

		List<OptimalTimeFinderResult> res = new ArrayList<>();

		// if the place has multiple opening hours rules we searching the best
		// solution for each of them
		for (TimeRange<ZonedDateTime> rule : openingHours) {

			LocalDateTime timeOpen = rule.getFrom().toLocalDateTime();
			LocalDateTime timeClose = rule.getTo().toLocalDateTime();

			// the lower limit of the interval
			Optional<LocalDateTime> limitLower = Seq
					.of(timeOpen, now, getEarliestTime())
					.filter(Objects::nonNull).max();
			// the upper limit of the interval
			Optional<LocalDateTime> limitUpper = Seq
					.of(timeClose.minusSeconds(getTimeBuffer().getSeconds()
							+ (minWalkingTime / 1000)), getLatestTime())
					.filter(Objects::nonNull).min();

			logger.debug("limitLower = " + limitLower + ", limitUpper = "
					+ limitUpper + ", now = " + now);

			if (limitLower.isPresent() && limitUpper.isPresent()
					&& limitLower.get().isBefore(limitUpper.get())
					&& now.compareTo(limitUpper.get()) <= 0) {

				TimeRange<LocalDateTime> limits = new TimeRange<>(
						limitLower.get(), limitUpper.get());

				StopWatch sw = new StopWatch().start();

				Pair<LocalDateTime, Double> optimalTime = getOptimalTime(start,
						placePoint, limits, minWalkingTime, this.starts);

				logger.debug("computed optimal time in " + sw.stop() + ": "
						+ optimalTime);

				logger.debug("start = " + start + ", placePoint = " + placePoint
						+ ", optimalTime = " + optimalTime.getLeft()
						+ ", weightingType = "
						+ objectiveFunction.getWeightingType());

				Optional<PathWrapper> path = routingHelper
						.route(start, placePoint, optimalTime.getLeft(),
								objectiveFunction.getWeightingType()
										.orElse(defaultWeightingType))
						.get();

				if (path.isPresent()) {
					res.add(new OptimalTimeFinderResult(optimalTime.getLeft(),
							path.get().getDistance(), optimalTime.getRight(),
							path.get().getTime(), null, null));
				}
			}

		}
		return res.stream().min((r, o) -> Double
				.compare(r.getOptimalValue(), o.getOptimalValue()));
	}

	/**
	 * Finds the optimal point in time for the given place {@code place} and
	 * current time {@code now}.
	 * 
	 * @param start
	 *            the start point
	 * @param place
	 *            the place to find the optimal time for
	 * @param date
	 *            the date to find the optimal time for
	 * @param now
	 *            the current time
	 * @return the optimal point in time or {@code Optional.empty()} if non
	 *         result is found
	 */
	public Optional<OptimalTimeFinderResult> find(Node start,
			Node place, Date date, LocalDateTime now) {
		GHPoint s = new GHPoint(start.getLatitude(), start.getLongitude());
		return find(s, place, date, now);
	}

	public WeightingType getDefaultWeightingType() {
		return defaultWeightingType;
	}

	public LocalDateTime getEarliestTime() {
		return earliestTime;
	}

	public ArrayList<Throwable> getLastErrors() {
		return lastErrors;
	}

	public ArrayList<Throwable> getLastWarnings() {
		return lastWarnings;
	}

	public LocalDateTime getLatestTime() {
		return latestTime;
	}

	public ObjectiveFunction getObjectiveFunction() {
		return objectiveFunction;
	}

	protected List<TimeRange<ZonedDateTime>> getOpeningHours(Node place,
			Date date) {
		return osmData.getOpeningHours(place.getId())
				.map(o -> o.getOpeningHours(date)).orElse(new ArrayList<>());
	}

	/**
	 * Helper function to find the optimal point in time.
	 * 
	 * @param start
	 *            the start point
	 * @param place
	 *            the place to find the optimal time for
	 * @param timeRange
	 *            the lower and upper interval limits in which should be
	 *            searched for the optimal value
	 * @param minWalkingTime
	 *            minimum time required to walk from {@code start} to
	 *            {@code place}
	 * @param starts
	 *            number of start to be performed by the Brent optimizer
	 * @return optimal point in time and the optimal value of the objective
	 *         function
	 */
	Pair<LocalDateTime, Double> getOptimalTime(GHPoint start, GHPoint place,
			TimeRange<LocalDateTime> timeRange, long minWalkingTime,
			int starts) {

		// the objective function to be passed to the BrentOptimizer
		UnivariateFunction univariateFunction = (x) -> {
			LocalDateTime time = timeRange.getFrom().plusSeconds((long) x);
			OptionalDouble value = objectiveFunction.value(time, start, place,
					timeRange, minWalkingTime);
			if (value.isPresent()) {
				return value.getAsDouble();
			} else {
				// if no value is returned by the objectiveFunction than either
				// the constrain has be violated or there is no value available
				logger.debug("constrains violated! (time = " + time
						+ ", timeRange = " + timeRange + ", lastWalkingTime = "
						+ Utils.formatDurationMills(objectiveFunction
								.getLastWalkingTime().getAsLong())
						+ ", start = " + start + ", place = " + place + ")");
				return Double.MAX_VALUE;
			}
		};

		// interval used for optimization is 0 and the duration between the
		// lower and upper bound in seconds
		double lower = 0;
		double upper = timeRange.durationInSeconds() + 1;

		BrentOptimizer optimizer = new BrentOptimizer(RELATIVE_THRESHOLD,
				ABSOLUTE_THRESHOLD);
		MultiStartUnivariateOptimizer multiStartOptimizer = new MultiStartUnivariateOptimizer(
				optimizer, starts, rng);
		UnivariatePointValuePair res = multiStartOptimizer.optimize(
				new MaxEval(MAX_EVAL), GOAL_TYPE,
				new SearchInterval(lower, upper),
				new UnivariateObjectiveFunction(univariateFunction));

		return Pair.of(timeRange.getFrom().plusSeconds((long) res.getPoint()),
				res.getValue());
	}
	
	public boolean hasLastWarnings() {
		return lastWarnings != null && !lastWarnings.isEmpty();
	}
	
	public boolean hasLastErros() {
		return lastErrors != null && !lastErrors.isEmpty();
	}

	public OSMData getOsmData() {
		return osmData;
	}

	public Node getPlace() {
		return place;
	}

	public RoutingHelper getRoutingHelper() {
		return routingHelper;
	}

	public GHPoint getStart() {
		return start;
	}

	public int getStarts() {
		return starts;
	}

	public Duration getTimeBuffer() {
		return timeBuffer;
	}

	public ZoneId getZoneId() {
		return zoneId;
	}

	public void setDefaultWeightingType(WeightingType defaultWeightingType) {
		this.defaultWeightingType = defaultWeightingType;
	}

	public void setEarliestTime(LocalDateTime earliestTime) {
		this.earliestTime = earliestTime;
	}

	public void setLastErrors(ArrayList<Throwable> lastErrors) {
		this.lastErrors = lastErrors;
	}

	public void setLastWarnings(ArrayList<Throwable> lastWarnings) {
		this.lastWarnings = lastWarnings;
	}

	public void setLatestTime(LocalDateTime latestTime) {
		this.latestTime = latestTime;
	}

	
	public void setObjectiveFunction(ObjectiveFunction objectiveFunction) {
		this.objectiveFunction = objectiveFunction;
	}

	public void setOsmData(OSMData osmData) {
		this.osmData = osmData;
	}

	public void setPlace(Node place) {
		this.place = place;
	}

	public void setRoutingHelper(RoutingHelper routingHelper) {
		this.routingHelper = routingHelper;
	}

	public void setStart(GHPoint start) {
		this.start = start;
	}

	public void setStarts(int starts) {
		this.starts = starts;
	}

	public void setTimeBuffer(Duration timeBuffer) {
		this.timeBuffer = timeBuffer;
	}

	public void setZoneId(ZoneId zoneId) {
		this.zoneId = zoneId;
	}
}
