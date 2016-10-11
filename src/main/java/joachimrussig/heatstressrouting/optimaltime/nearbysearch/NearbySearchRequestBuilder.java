package joachimrussig.heatstressrouting.optimaltime.nearbysearch;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Predicate;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;

import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.optimaltime.finder.OptimalTimeFinder;
import joachimrussig.heatstressrouting.optimaltime.finder.OptimalTimeFinderHeuristic;
import joachimrussig.heatstressrouting.optimaltime.finder.SimpleObjectiveFunction;
import joachimrussig.heatstressrouting.routing.RoutingHelper;
import joachimrussig.heatstressrouting.routing.weighting.WeightingType;

/**
 * A builder to create a new {@link NearbySearchRequest}.
 * 
 * @author Joachim Rußig
 *
 */
public class NearbySearchRequestBuilder {

	private RoutingHelper routingHelper;

	private GHPoint start;
	private LocalDateTime now;
	private Predicate<Entity> predicate = null;
	private int maxResults = 10;
	private double maxDistance = 1000.0;

	private WeightingType weightingType = WeightingType.HEAT_INDEX;

	private Duration timeBuffer = Duration.ofMinutes(15);
	private LocalDateTime earliestTime = null;
	private LocalDateTime latestTime = null;

	private OptimalTimeFinder finder = null;
	private ScoreFunction scoreFunction = null;

	/**
	 * Creates a new {@code NearbySearchRequestBuilder} with the required
	 * arguments to create a valid {@link {@code NearbySearchRequest}.
	 * 
	 * @param routingHelper
	 *            the {@link RoutingHelper} used to find the optimal routes
	 *            between start point and the places
	 * @param start
	 *            the start point
	 * @param now
	 *            the current time
	 */
	public NearbySearchRequestBuilder(RoutingHelper routingHelper,
			GHPoint start, LocalDateTime now) {
		this.routingHelper = routingHelper;
		this.start = start;
		this.now = now;
	}

	/**
	 * Creates the {@link NearbySearchRequest}.
	 * 
	 * <p>
	 * 
	 * If no {@link OptimalTimeFinder} {@code finder} is provided, the
	 * {@link OptimalTimeFinderHeuristic} is used.
	 * 
	 * <p>
	 * 
	 * If no {@link ScoreFunciton} {@code scoreFunction} is provided the
	 * {@link WeightedSumScoreFunction} is used if the {@link ObjectiveFunction}
	 * of the {@code finder} is a instance of {@link SimpleObjectiveFunction}
	 * and the {@link ThermalComfortScoreFunction()} otherwise.
	 * 
	 * @return the created {@code NearbySearchRequest}.
	 */
	public NearbySearchRequest build() {

		if (this.finder == null) {
			this.finder = new OptimalTimeFinderHeuristic(this.weightingType,
					routingHelper);
		}

		if (this.scoreFunction == null) {
			if (finder
					.getObjectiveFunction() instanceof SimpleObjectiveFunction) {
				this.scoreFunction = new WeightedSumScoreFunction();
			} else {
				this.scoreFunction = new ThermalComfortScoreFunction();
			}

		}

		if (timeBuffer != null)
			finder.setTimeBuffer(this.timeBuffer);
		finder.setEarliestTime(earliestTime);
		finder.setLatestTime(latestTime);

		return new NearbySearchRequest(this.start, this.now, this.predicate,
				this.maxResults, this.maxDistance, this.finder,
				this.scoreFunction);

	}

	/**
	 * 
	 * @param start
	 *            the start point to be set
	 * @return the updated builder
	 */
	public NearbySearchRequestBuilder setStart(GHPoint start) {
		this.start = start;
		return this;
	}

	/**
	 * 
	 * @param now
	 *            the point in time to be used
	 * @return the updated builder
	 */
	public NearbySearchRequestBuilder setNow(LocalDateTime now) {
		this.now = now;
		return this;
	}

	/**
	 * 
	 * @param predicate
	 *            the predicate to be used to filter the places; a place only
	 *            used if {@code predicate} returns {@code true}
	 * @return the updated builder
	 */
	public NearbySearchRequestBuilder setPredicate(
			Predicate<Entity> predicate) {
		this.predicate = predicate;
		return this;
	}

	/**
	 * 
	 * @param maxResults
	 *            the maximum number of results to consider
	 * @return the updated builder
	 */
	public NearbySearchRequestBuilder setMaxResults(int maxResults) {
		this.maxResults = maxResults;
		return this;
	}

	/**
	 * 
	 * @param maxDistance
	 *            the maximum direct distance (haversine distance) between the
	 *            start point and the place
	 * @return the updated builder
	 */
	public NearbySearchRequestBuilder setMaxDistance(double maxDistance) {
		this.maxDistance = maxDistance;
		return this;
	}

	/**
	 * 
	 * @param weightingType
	 *            the {@link WeightingType} to be used
	 * @return the updated builder
	 */
	public NearbySearchRequestBuilder setWeightingType(
			WeightingType weightingType) {
		this.weightingType = weightingType;
		return this;
	}

	/**
	 * 
	 * @param finder
	 *            the {@link OptimalTimeFinder} to be used.
	 * @return the updated builder
	 */
	public NearbySearchRequestBuilder setOptimalTimeFinder(
			OptimalTimeFinder finder) {
		this.finder = finder;
		return this;
	}

	/**
	 * 
	 * @param scoreFunction
	 *            the {@link ScoreFunction} to be used
	 * @return the updated builder
	 */
	public NearbySearchRequestBuilder setScoreFunction(
			ScoreFunction scoreFunction) {
		this.scoreFunction = scoreFunction;
		return this;
	}

	/**
	 * 
	 * @param routingHelper
	 *            {@link RoutingHelper} to be used
	 * @return the updated builder
	 */
	public NearbySearchRequestBuilder setRoutingHelper(
			RoutingHelper routingHelper) {
		this.routingHelper = routingHelper;
		return this;
	}

	/**
	 * 
	 * @param timeBuffer
	 *            the time buffer to be used
	 * @return the updated builder
	 */
	public NearbySearchRequestBuilder setTimeBuffer(Duration timeBuffer) {
		this.timeBuffer = timeBuffer;
		return this;
	}

	/**
	 * 
	 * @param earliestTime
	 *            the earliest point in time accepted
	 * @return the updated builder
	 * @throws IllegalArgumentException
	 *             if {@code earliestTime} and {@code latestTime} are both
	 *             specified and {@code earliestTime} is not before
	 *             {@code latestTime}
	 */
	public NearbySearchRequestBuilder setEarliestTime(
			LocalDateTime earliestTime) {
		if (earliestTime != null && this.latestTime != null
				&& !earliestTime.isBefore(this.latestTime))
			throw new IllegalArgumentException(
					"if arliestTime and latestTime are specified,"
							+ " then earliestTime must be before latest time");

		this.earliestTime = earliestTime;
		return this;
	}

	/**
	 * 
	 * @param latestTime
	 *            the latest point in time accepted
	 * @return he updated builder
	 * @throws IllegalArgumentException
	 *             if {@code earliestTime} and {@code latestTime} are both
	 *             specified and {@code earliestTime} is not before
	 *             {@code latestTime}
	 */
	public NearbySearchRequestBuilder setLatestTime(LocalDateTime latestTime) {
		if (latestTime != null && this.earliestTime != null
				&& !latestTime.isAfter(this.earliestTime))
			throw new IllegalArgumentException(
					"if arliestTime and latestTime are specified,"
							+ " then earliestTime must be before latest time");

		this.latestTime = latestTime;
		return this;
	}

	public RoutingHelper getRoutingHelper() {
		return routingHelper;
	}

	public GHPoint getStart() {
		return start;
	}

	public LocalDateTime getNow() {
		return now;
	}

	public Optional<Predicate<Entity>> getPredicate() {
		return Optional.ofNullable(predicate);
	}

	public int getMaxResults() {
		return maxResults;
	}

	public double getMaxDistance() {
		return maxDistance;
	}

	public WeightingType getWeightingType() {
		return weightingType;
	}

	public OptimalTimeFinder getFinder() {
		return finder;
	}

	public ScoreFunction getScoreFunction() {
		return scoreFunction;
	}

	public Duration getTimeBuffer() {
		return this.timeBuffer;
	}

	public Optional<LocalDateTime> getEarliestTime() {
		return Optional.ofNullable(this.earliestTime);
	}

	public Optional<LocalDateTime> getLatestTime() {
		return Optional.ofNullable(this.latestTime);
	}

}
