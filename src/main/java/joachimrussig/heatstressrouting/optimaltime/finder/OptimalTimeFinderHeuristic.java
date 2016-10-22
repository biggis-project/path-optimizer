package joachimrussig.heatstressrouting.optimaltime.finder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.MultiStartUnivariateOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.jooq.lambda.Seq;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.routing.Path;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.osmdata.OSMUtils;
import joachimrussig.heatstressrouting.routing.HeatStressGraphHopper;
import joachimrussig.heatstressrouting.routing.RoutingHelper;
import joachimrussig.heatstressrouting.routing.RoutingRequest;
import joachimrussig.heatstressrouting.routing.RoutingRequestBuilder;
import joachimrussig.heatstressrouting.routing.RoutingResponse;
import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.util.TimeRange;

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
public class OptimalTimeFinderHeuristic extends OptimalTimeFinder {

	private static final Logger logger = LoggerFactory
			.getLogger(OptimalTimeFinder.class);

	// TODO remove objectiveFunctionPath or find a better solution e.g. a
	// weighting function, that inherits from
	// com.graphhopper.routing.util.Weighting
	private final ObjectiveFunctionPath objectiveFunctionPath;
	private final WeightingType weightingType;

	/**
	 * 
	 * @param objectiveFunction
	 *            the objective function to optimize
	 * @param routingHelper
	 *            routing helper used to find the routes
	 */
	public OptimalTimeFinderHeuristic(ObjectiveFunctionPath objectiveFunction,
			WeightingType weightingType, RoutingHelper routingHelper) {
		super(null, routingHelper);
		this.objectiveFunctionPath = objectiveFunction;
		this.weightingType = weightingType;
	}

	/**
	 * 
	 * @param objectiveFunction
	 *            the objective function to optimize
	 * @param hopper
	 *            hopper used to find the routes
	 */
	public OptimalTimeFinderHeuristic(ObjectiveFunctionPath objectiveFunction,
			WeightingType weightingType, HeatStressGraphHopper hopper) {
		this(objectiveFunction, weightingType, new RoutingHelper(hopper));
	}

	/**
	 * 
	 * @param objectiveFunction
	 *            the objective function to optimize
	 * @param routingHelper
	 *            routing helper used to find the routes
	 */
	public OptimalTimeFinderHeuristic(WeightingType weightingType,
			RoutingHelper routingHelper) {
		this(new ObjectiveFunctionPathImpl(weightingType, routingHelper),
				weightingType, routingHelper);
	}

	@Override
	public Optional<OptimalTimeFinderResult> find(GHPoint start, Node place,
			Date date, LocalDateTime now) {

		super.lastWarnings.clear();
		super.lastErrors.clear();

		GHPoint placePoint = OSMUtils.getGHPoint(place);

		List<TimeRange<ZonedDateTime>> openingHours = getOpeningHours(place,
				date);

		if (openingHours.size() == 0)
			return Optional.empty();

		RoutingRequest reqShortest = RoutingRequestBuilder
				.shortestRoutingRequest(start, placePoint).build();
		RoutingResponse rspShortest = routingHelper.route(reqShortest);

		if (rspShortest.hasErrors()) {
			lastErrors.addAll(rspShortest.getErrors());
			return Optional.empty();
		}

		// estimation of the minimum walking time
		long walkingTimeShortest = rspShortest.getBest().getTime();

		List<OptimalTimeFinderResult> res = new ArrayList<>();

		// TODO can that be parallelized?
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
							+ (walkingTimeShortest / 1000)), getLatestTime())
					.filter(Objects::nonNull).min();

			logger.debug("Place = " + place.getId() + ", limitLower = "
					+ limitLower + ", limitUpper = " + limitUpper + ", now = "
					+ now);

			if (limitLower.isPresent() && limitUpper.isPresent()
					&& limitLower.get().isBefore(limitUpper.get())
					&& now.compareTo(limitUpper.get()) <= 0) {

				TimeRange<LocalDateTime> limits = new TimeRange<>(
						limitLower.get(), limitUpper.get());

				StopWatch sw = new StopWatch().start();

				Pair<LocalDateTime, Double> optimalTime = getOptimalTime(
						rspShortest.getPaths().get(0), limits, this.starts);

				logger.debug("computed optimal time in " + sw.stop() + ": "
						+ optimalTime);

				logger.debug("start = " + start + ", place = " + place.getId()
						+ " (" + placePoint + ")" + ", optimalTime = "
						+ optimalTime.getLeft() + ", weightingType = "
						+ this.weightingType);

				// compute the optimal route for the found optimal point in time
				RoutingRequest reqOptimal = new RoutingRequestBuilder(start,
						placePoint, this.weightingType, optimalTime.getLeft())
								.build();
				Optional<Path> path = routingHelper.route(reqOptimal).getPaths()
						.stream().findFirst();
				// Optional<Path> path = routingHelper.routePath(start,
				// placePoint,
				// optimalTime.getLeft(), this.weightingType).get();

				if (path.isPresent()) {

					// check if the optimal point in time violates the
					// constrains
					LocalDateTime timeOpt = optimalTime.getLeft();
					long walingTimeOpt = path.get().getTime();
					LocalDateTime t = timeOpt
							.plus(walingTimeOpt, ChronoUnit.MILLIS)
							.plus(timeBuffer);

					if (t.compareTo(timeOpen) >= 0
							&& t.compareTo(timeClose) <= 0) {
						// every thing is okay, we found a optimal solution
						res.add(new OptimalTimeFinderResult(timeOpt,
								path.get().getDistance(),
								optimalTime.getRight(), path.get().getTime(),
								path.get(), rspShortest.getPaths().get(0)));
					} else {
						// constrain violated, so we postpone the time by
						// the amount of time the opening hours are violated

						// amount of time the constrain is violated:
						// delta = (t_walk + t_buff) - (t_close - t_opt)
						Duration delta = Duration.ofMillis(walingTimeOpt)
								.plus(this.timeBuffer)
								.minus(Duration.between(timeOpt, timeClose));
						// the new start time
						LocalDateTime timeOptNew = timeOpt.minus(delta);
						// compute the optimal path for the new optimal time
						RoutingRequest reqOptimalNew = new RoutingRequestBuilder(
								start, placePoint, this.weightingType,
								timeOptNew).build();
						Optional<Path> pathNew = routingHelper
								.route(reqOptimalNew).getPaths().stream()
								.findFirst();
						// Optional<Path> pathNew =
						// routingHelper.routePath(start,
						// placePoint, timeOptNew, this.weightingType)
						// .get();
						long walingTimeOptNew = path.get().getTime();
						LocalDateTime tNew = timeOptNew
								.plus(walingTimeOptNew, ChronoUnit.MILLIS)
								.plus(timeBuffer);

						// because for the new optimal time a longer optimal
						// route could be found, we've to check the constrains
						// again
						if (tNew.compareTo(timeOpen) >= 0
								&& tNew.compareTo(timeClose) <= 0) {
							res.add(new OptimalTimeFinderResult(timeOptNew,
									pathNew.get().getDistance(),
									routingHelper.routeWeight(pathNew.get(),
											timeOptNew, this.weightingType),
									pathNew.get().getTime(), pathNew.get(),
									rspShortest.getPaths().get(0)));
						} else {
							// if the constrains are violated again we're using
							// the sub optimal route found in the previous step
							res.add(new OptimalTimeFinderResult(timeOptNew,
									path.get().getDistance(),
									routingHelper.routeWeight(path.get(),
											timeOptNew, this.weightingType),
									path.get().getTime(), path.get(),
									rspShortest.getPaths().get(0)));
						}

					}

				}
			}

		}
		return res.stream().min((r, o) -> Double.compare(r.getOptimalValue(),
				o.getOptimalValue()));
	}

	Pair<LocalDateTime, Double> getOptimalTime(Path path,
			TimeRange<LocalDateTime> timeRange, int starts) {

		// the objective function to be passed to the BrentOptimizer
		UnivariateFunction univariateFunction = (x) -> {
			LocalDateTime time = timeRange.getFrom().plusSeconds((long) x);

			Weighting weighting = routingHelper
					.createWeighting(this.weightingType, time);

			OptionalDouble value = objectiveFunctionPath.value(time, path,
					weighting);
			if (value.isPresent()) {
				return value.getAsDouble();
			} else {
				return Double.MAX_VALUE;
			}
		};

		// interval used for optimization is 0 and the duration between the
		// lower and upper bound in seconds
		double lower = 0;
		double upper = timeRange.durationInSeconds() + 1;

		logger.debug("lower = " + lower + ", upper = " + upper);

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

}
