package joachimrussig.heatstressrouting.optimaltime.nearbysearch;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jooq.lambda.Seq;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.optimaltime.finder.OptimalTimeFinder;
import joachimrussig.heatstressrouting.optimaltime.finder.OptimalTimeFinderResult;
import joachimrussig.heatstressrouting.osmdata.OSMOpeningHours;

/**
 * The Class performs a nearby search, determines an optimal time for each found
 * palce using the specified {@link OptimalTimeFinder} and ranks them according
 * to the specified {@link ScoreFunction}
 * 
 * @author Joachim Rußig
 */
public class NearbySearch {

	private static final Logger logger = LoggerFactory
			.getLogger(NearbySearch.class);

	private OptimalTimeFinder finder;
	private ScoreFunction scoreFunction;

	private int maxResults = 20;
	private Double maxDistance = null;

	/**
	 * Creates a new {@code NearbySearch} object.
	 * 
	 * @param finder
	 *            the {@link OptimalTimeFinder} to use to find the optimal point
	 *            in time
	 * @param scoreFunction
	 *            the {@link ScoreFunction} to rank the found results
	 */
	public NearbySearch(OptimalTimeFinder finder, ScoreFunction scoreFunction) {
		this.finder = finder;
		this.scoreFunction = scoreFunction;
	}

	/**
	 * Performs a nearby search starting at {@code start} an time {@code now}
	 * for places that fulfill the {@code predicate} and returns only the best
	 * result according to {@code scoreFunction}.
	 * 
	 * @param start
	 *            the start point
	 * @param predicate
	 *            a predicate to filter desired places; a places is only
	 *            included, if predicate returns {@code true}
	 * @param now
	 *            the current point in time
	 * @param maxResults
	 *            maximale number of results to search for
	 * @return the {@code maxResults} nearest neighbors (according to the
	 *         haversine distance) that fulfill predicate and are opened at
	 *         {@code now}
	 */
	public Optional<NearbySearchResult> findBest(GHPoint start,
			Predicate<Entity> predicate, LocalDateTime now, int maxResutls) {
		return find(start, predicate, now, maxResutls).stream().findFirst();
	}

	/**
	 * Performs a nearby search starting at {@code start} an time {@code now}
	 * for places that fulfill the {@code predicate}. The results are ranked
	 * according to {@code scoreFunction}.
	 * 
	 * @param start
	 *            the start point
	 * @param predicate
	 *            a predicate to filter desired places; a places is only
	 *            included, if predicate returns {@code true}
	 * @param now
	 *            the current point in time
	 * @return the {@code this.maxResults} nearest neighbors (according to the
	 *         haversine distance) that fulfill predicate and are opened at
	 *         {@code now}
	 */
	public List<NearbySearchResult> find(GHPoint start,
			Predicate<Entity> predicate, LocalDateTime now) {
		return find(start, predicate, now, maxResults);
	}

	/**
	 * Performs a nearby search starting at {@code start} an time {@code now}
	 * for places that fulfill the {@code predicate}. The results are ranked
	 * according to {@code scoreFunction}.
	 * 
	 * @param start
	 *            the start point
	 * @param predicate
	 *            a predicate to filter desired places; a places is only
	 *            included, if predicate returns {@code true}
	 * @param now
	 *            the current point in time
	 * @param maxResults
	 *            maximale number of results to return
	 * @return the {@code maxResults} nearest neighbors (according to the
	 *         haversine distance) that fulfill predicate and are opened at
	 *         {@code now}
	 */
	public List<NearbySearchResult> find(GHPoint start,
			Predicate<Entity> predicate, LocalDateTime now, int maxResults) {
		return find(start, predicate, now, maxResults, this.maxDistance,
				this.finder, this.scoreFunction);

	}

	// /**
	// * Performs a nearby search starting at {@code start} at time {@code now}
	// * for places that fulfill the {@code predicate}. The results are ranked
	// * according to {@code scoreFunction}.
	// *
	// * @param start
	// * the start point
	// * @param predicate
	// * a predicate to filter desired places; a places is only
	// * included, if predicate returns {@code true}
	// * @param now
	// * the current point in time
	// * @param maxResults
	// * maximal number of results to return
	// * @param maxDistance
	// * the maximum distance between the start and the places
	// * @param finder
	// * the {@link OptimalTimeFinder} to use
	// * @param scoreFunction
	// * the score function used to rank the results
	// * @return the {@code maxResults} nearest neighbors (according to the
	// * haversine distance) that fulfill predicate and are opened at
	// * {@code now}
	// */
	// protected static List<NearbySearchResult> find(GHPoint start,
	// Predicate<Entity> predicate, LocalDateTime now, int maxResults,
	// double maxDistance, OptimalTimeFinder finder,
	// ScoreFunction scoreFunction) {
	//
	// logger.debug("Start = " + start + ", now = " + now + ", maxResults = "
	// + maxResults);
	//
	// // Find the candidate places in a radius of maxDistance around the
	// // start point that fulfill the predicate and have opening hours
	// // specified
	// List<Node> places = finder.getOsmData().kNearestNeighbor(start,
	// maxResults, maxDistance,
	// e -> predicate.test(e) && e.getTags().stream()
	// .anyMatch(t -> t.getKey().equalsIgnoreCase(
	// OSMOpeningHours.OPENING_HOURS_KEY)));
	//
	// logger.debug(places.size() + " place(s) found");
	//
	// // the current date
	// Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
	//
	// List<FinderResult> res = new ArrayList<>();
	//
	// double distanceMin = Double.MIN_VALUE;
	// double distanceMax = 0;
	//
	// double valueMin = Double.MIN_VALUE;
	// double valueMax = 0;
	//
	// // RoutingHelper helper = finder.getRoutingHelper();
	//
	// for (Node place : places) {
	// if (logger.isDebugEnabled()) {
	// logger.debug("\tPlace = " + place + "; tags = "
	// + place.getTags().stream().map(Tag::toString)
	// .collect(Collectors.joining(", ")));
	// logger.debug("start = " + start + ", date = " + date
	// + ", now = " + now);
	// }
	//
	// // find the optiaml point in time for the current place
	// Optional<OptimalTimeFinderResult> optTime = finder.find(start,
	// place, date, now);
	// logger.debug("optTime = " + optTime);
	// // if we found an optimal point in time find the optimal route from
	// // the start to the place
	// if (optTime.isPresent()) {
	// OptimalTimeFinderResult opt = optTime.get();
	// // GHPoint placePoint = OSMUtils.getGHPoint(place);
	//
	// // RoutingRequest reqShortest = RoutingRequestBuilder
	// // .shortestRoutingRequest(start, placePoint).build();
	// // Path pathShortest =
	// // helper.route(reqShortest).getPaths().get(0);
	//
	// distanceMin = Math.min(distanceMin, opt.getDistance());
	// distanceMax = Math.max(distanceMax, opt.getDistance());
	//
	// valueMin = Math.min(valueMin, opt.getOptimalValue());
	// valueMax = Math.max(valueMax, opt.getOptimalValue());
	//
	// // Path path = helper
	// // .routePath(start, placePoint, opt.getOptimalTime())
	// // .get().orElse(null);
	//
	// res.add(new FinderResult(place, opt));
	// }
	// }
	//
	// logger.debug("distanceMin = " + distanceMin + ", distanceMax = "
	// + distanceMax + ", valueMin = " + valueMin + ", valueMax = "
	// + valueMax);
	//
	// final double distMin = distanceMin;
	// final double distMax = distanceMax;
	//
	// final double valMin = valueMin;
	// final double valMax = valueMax;
	//
	// final boolean minimize = scoreFunction.minimize();
	//
	// // compute for each place the score with scoreFunction and rank the
	// // result accordingly
	// return Seq.seq(res)
	// .sorted((t1, t2) -> t1.optimalResult
	// .compareByOptimalValue(t2.optimalResult, minimize))
	// .zipWithIndex().map(t -> {
	// FinderResult r = t.v1();
	// int rank = ((int) t.v2().longValue()) + 1;
	//
	// double score = scoreFunction.score(
	// r.optimalResult.getDistance(),
	// r.optimalResult.getOptimalValue(), distMin, distMax,
	// valMin, valMax);
	//
	// return new NearbySearchResult(rank, score, r.place,
	// r.optimalResult, r.optimalResult.getOptimalPath(),
	// r.optimalResult.getShortestPath());
	// }).collect(Collectors.toList());
	// }

	/**
	 * Performs a nearby search starting at {@code start} at time {@code now}
	 * for places that fulfill the {@code predicate}. The results are ranked
	 * according to {@code scoreFunction}. Same as
	 * {@link NearbySearch#find(GHPoint, Predicate, LocalDateTime, int, double, OptimalTimeFinder, ScoreFunction, boolean)}
	 * but with {@code parallel} set to {@code false}.
	 * 
	 * @param start
	 *            the start point
	 * @param predicate
	 *            a predicate to filter desired places; a places is only
	 *            included, if predicate returns {@code true}
	 * @param now
	 *            the current point in time
	 * @param maxResults
	 *            maximal number of results to return
	 * @param maxDistance
	 *            the maximum distance between the start and the places
	 * @param finder
	 *            the {@link OptimalTimeFinder} to use
	 * @param scoreFunction
	 *            the score function used to rank the results
	 * @return the {@code maxResults} nearest neighbors (according to the
	 *         haversine distance) that fulfill predicate and are opened at
	 *         {@code now}
	 */
	protected static List<NearbySearchResult> find(final GHPoint start,
			final Predicate<Entity> predicate, final LocalDateTime now,
			final int maxResults, final double maxDistance,
			final OptimalTimeFinder finder, final ScoreFunction scoreFunction) {
		return find(start, predicate, now, maxResults, maxDistance, finder,
				scoreFunction, false);
	}

	/**
	 * Performs a nearby search starting at {@code start} at time {@code now}
	 * for places that fulfill the {@code predicate}. The results are ranked
	 * according to {@code scoreFunction}. Same as
	 * {@link NearbySearch#find(GHPoint, Predicate, LocalDateTime, int, double, OptimalTimeFinder, ScoreFunction, boolean)}
	 * but with {@code parallel} set to {@code true}.
	 * 
	 * @param start
	 *            the start point
	 * @param predicate
	 *            a predicate to filter desired places; a places is only
	 *            included, if predicate returns {@code true}
	 * @param now
	 *            the current point in time
	 * @param maxResults
	 *            maximal number of results to return
	 * @param maxDistance
	 *            the maximum distance between the start and the places
	 * @param finder
	 *            the {@link OptimalTimeFinder} to use
	 * @param scoreFunction
	 *            the score function used to rank the results
	 * @return the {@code maxResults} nearest neighbors (according to the
	 *         haversine distance) that fulfill predicate and are opened at
	 *         {@code now}
	 */
	protected static List<NearbySearchResult> findPar(final GHPoint start,
			final Predicate<Entity> predicate, final LocalDateTime now,
			final int maxResults, final double maxDistance,
			final OptimalTimeFinder finder, final ScoreFunction scoreFunction) {
		return find(start, predicate, now, maxResults, maxDistance, finder,
				scoreFunction, true);
	}

	/**
	 * Performs a nearby search starting at {@code start} at time {@code now}
	 * for places that fulfill the {@code predicate}. The results are ranked
	 * according to {@code scoreFunction}.
	 * 
	 * @param start
	 *            the start point
	 * @param predicate
	 *            a predicate to filter desired places; a places is only
	 *            included, if predicate returns {@code true}
	 * @param now
	 *            the current point in time
	 * @param maxResults
	 *            maximal number of results to return
	 * @param maxDistance
	 *            the maximum distance between the start and the places
	 * @param finder
	 *            the {@link OptimalTimeFinder} to use
	 * @param scoreFunction
	 *            the score function used to rank the results
	 * @param parallel
	 *            if {@code true}, the search is executed in parallel using a
	 *            parallel {@link java.util.stream.Stream}
	 * @return the {@code maxResults} nearest neighbors (according to the
	 *         haversine distance) that fulfill predicate and are opened at
	 *         {@code now}
	 */
	protected static List<NearbySearchResult> find(final GHPoint start,
			final Predicate<Entity> predicate, final LocalDateTime now,
			final int maxResults, final double maxDistance,
			final OptimalTimeFinder finder, final ScoreFunction scoreFunction,
			boolean parallel) {

		logger.debug("Start = " + start + ", now = " + now + ", maxResults = "
				+ maxResults);

		// Find the candidate places in a radius of maxDistance around the
		// start point that fulfill the predicate and have opening hours
		// specified
		List<Node> places = finder.getOsmData().kNearestNeighbor(start,
				maxResults, maxDistance,
				e -> predicate.test(e) && e.getTags().stream()
						.anyMatch(t -> t.getKey().equalsIgnoreCase(
								OSMOpeningHours.OPENING_HOURS_KEY)));

		logger.debug(places.size() + " place(s) found");

		// the current date
		Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());

		Stream<Node> placesStream;
		if (parallel)
			placesStream = places.parallelStream();
		else
			placesStream = places.stream();

		List<FinderResult> res = placesStream.map((Node place) -> {
			Optional<FinderResult> ret = Optional.empty();

			if (logger.isDebugEnabled()) {
				logger.debug("\tPlace = " + place + "; tags = "
						+ place.getTags().stream().map(Tag::toString)
								.collect(Collectors.joining(", ")));
				logger.debug("start = " + start + ", date = " + date
						+ ", now = " + now);
			}

			// find the optimal point in time for the current place
			Optional<OptimalTimeFinderResult> optTime = finder.find(start,
					place, date, now);
			logger.debug("optTime = " + optTime);
			// if we found an optimal point in time, find the optimal
			// route from the start to the place
			if (optTime.isPresent()) {
				OptimalTimeFinderResult opt = optTime.get();
				ret = Optional.of(new FinderResult(place, opt));
			}
			return ret;

		}).filter(Optional::isPresent).map(Optional::get)
				.collect(Collectors.toList());

		final double distanceMin = res.stream()
				.map(FinderResult::getOptimalResult)
				.mapToDouble(OptimalTimeFinderResult::getDistance).min()
				.orElse(Double.MIN_VALUE);
		final double distanceMax = res.stream()
				.map(FinderResult::getOptimalResult)
				.mapToDouble(OptimalTimeFinderResult::getDistance).max()
				.orElse(0.0);

		final double valueMin = res.stream().map(FinderResult::getOptimalResult)
				.mapToDouble(OptimalTimeFinderResult::getOptimalValue).min()
				.orElse(Double.MIN_VALUE);
		final double valueMax = res.stream().map(FinderResult::getOptimalResult)
				.mapToDouble(OptimalTimeFinderResult::getOptimalValue).max()
				.orElse(0.0);

		logger.debug("distanceMin = " + distanceMin + ", distanceMax = "
				+ distanceMax + ", valueMin =  " + valueMin + ", valueMax = "
				+ valueMax);

		final boolean minimize = scoreFunction.minimize();

		// compute for each place the score with scoreFunction and rank the
		// result accordingly
		return Seq.seq(res)
				.sorted((t1, t2) -> t1.optimalResult
						.compareByOptimalValue(t2.optimalResult, minimize))
				.zipWithIndex().map(t -> {
					FinderResult r = t.v1();
					int rank = ((int) t.v2().longValue()) + 1;

					double score = scoreFunction.score(
							r.optimalResult.getDistance(),
							r.optimalResult.getOptimalValue(), distanceMin,
							distanceMax, valueMin, valueMax);

					return new NearbySearchResult(rank, score, r.place,
							r.optimalResult, r.optimalResult.getOptimalPath(),
							r.optimalResult.getShortestPath());
				}).collect(Collectors.toList());
	}

	public OptimalTimeFinder getFinder() {
		return finder;
	}

	public void setFinder(OptimalTimeFinder finder) {
		this.finder = finder;
	}

	public ScoreFunction getScoreFunction() {
		return scoreFunction;
	}

	public void setScoreFunction(ScoreFunction scoreFunction) {
		this.scoreFunction = scoreFunction;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	public Double getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(Double maxDistance) {
		this.maxDistance = maxDistance;
	}

	private static class FinderResult {
		public Node place;
		public OptimalTimeFinderResult optimalResult;

		public FinderResult(Node place, OptimalTimeFinderResult optimalResult) {
			this.place = place;
			this.optimalResult = optimalResult;
		}

		public OptimalTimeFinderResult getOptimalResult() {
			return optimalResult;
		}

	}

}
