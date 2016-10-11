package joachimrussig.heatstressrouting.optimaltime.nearbysearch;

import java.time.LocalDateTime;
import java.util.function.Predicate;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;

import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.optimaltime.finder.OptimalTimeFinder;

/**
 * A class that represent a nearby search request that can be passed to
 * {@link NearbySearchHelper#find(NearbySearchRequest)} or
 * {@link NearbySearchHelper#findPar(NearbySearchRequest)} to execute a nearby
 * search.
 * 
 * <p>
 * 
 * Use the {@link NearbySearchRequestBuilder} to create a new
 * {@code NearbySearchRequest}.
 * 
 * @author Joachim Rußig
 *
 */
public class NearbySearchRequest {

	private final GHPoint start;
	private final LocalDateTime now;
	private final Predicate<Entity> predicate;
	private final int maxResults;
	private final double maxDistance;
	private final OptimalTimeFinder finder;
	private final ScoreFunction scoreFunction;

	/**
	 * Creates a new {@code NearbySearchRequest}.
	 * 
	 * <p>
	 * Use the {@link NearbySearchRequestBuilder} to create a new
	 * {@code NearbySearchRequest}.
	 *
	 * @param start
	 *            the start point
	 * @param now
	 *            the current time
	 * @param predicate
	 *            a predicate used to filter the places
	 * @param maxResults
	 *            the maximum number of results to consider
	 * @param maxDistance
	 *            the maximum direct distance (haversine distance) between the
	 *            start point and the place
	 * @param finder
	 *            the {@link OptimalTimeFinder} to use
	 * @param scoreFunction
	 *            the {@link ScoreFunction} used to rank the results
	 */
	protected NearbySearchRequest(GHPoint start, LocalDateTime now,
			Predicate<Entity> predicate, int maxResults, double maxDistance,
			OptimalTimeFinder finder, ScoreFunction scoreFunction) {
		this.start = start;
		this.now = now;
		this.predicate = predicate;
		this.maxResults = maxResults;
		this.maxDistance = maxDistance;
		this.finder = finder;
		this.scoreFunction = scoreFunction;
	}

	public GHPoint getStart() {
		return start;
	}

	public LocalDateTime getNow() {
		return now;
	}

	public Predicate<Entity> getPredicate() {
		return predicate;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public double getMaxDistance() {
		return maxDistance;
	}

	public OptimalTimeFinder getFinder() {
		return finder;
	}

	public ScoreFunction getScoreFunction() {
		return scoreFunction;
	}

	@Override
	public String toString() {
		return "NearbySearchRequest [start=" + start + ", now=" + now
				+ ", predicate=" + predicate + ", maxResults=" + maxResults
				+ ", maxDistance=" + maxDistance + ", finder=" + finder
				+ ", scoreFunction=" + scoreFunction + "]";
	}

}
