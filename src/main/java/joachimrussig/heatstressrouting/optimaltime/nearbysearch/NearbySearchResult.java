package joachimrussig.heatstressrouting.optimaltime.nearbysearch;

import java.util.OptionalInt;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;

import com.graphhopper.routing.Path;

import joachimrussig.heatstressrouting.optimaltime.finder.OptimalTimeFinderResult;

/**
 * The {@code NearbySearchResult} class represents a single result of a nearby
 * search.
 * 
 * @author Joachim Rußig
 */
public class NearbySearchResult extends OptimalTimeFinderResult {

	private Integer rank;
	private double score;
	private Node place;

	public NearbySearchResult(int rank, double score, Node place,
			OptimalTimeFinderResult optimalTimeFinderResult, Path pathOptimal,
			Path pathShortest) {
		super(optimalTimeFinderResult.getOptimalTime(),
				optimalTimeFinderResult.getDistance(),
				optimalTimeFinderResult.getOptimalValue(),
				optimalTimeFinderResult.getDuration(), pathOptimal,
				pathShortest);
		this.rank = rank;
		this.score = score;
		this.place = place;
	}

	public NearbySearchResult(double score, Node place,
			OptimalTimeFinderResult optimalTimeFinderResult, Path pathOptimal,
			Path pathShortest) {
		super(optimalTimeFinderResult.getOptimalTime(),
				optimalTimeFinderResult.getDistance(),
				optimalTimeFinderResult.getOptimalValue(),
				optimalTimeFinderResult.getDuration(), pathOptimal, pathShortest);
		this.rank = null;
		this.score = score;
		this.place = place;
	}

	/**
	 * Compares the two {@code NearbySearchResult}s by the {@code score}
	 * 
	 * @param other
	 *            the {@code NearbySearchResult} to compare to
	 *
	 * @return -1 if {@code this.score < other.score}, 0 if
	 *         {@code this.score == other.score} and 1 if
	 *         {@code this.score > other.score}
	 */
	public <T extends NearbySearchResult> int compareByScore(T other) {
		if (this.score < other.getScore())
			return -1;
		else if (this.score > other.getScore())
			return 1;
		else
			return 0;
	}

	/**
	 * Compares the two {@code NearbySearchResult}s by the {@code score}
	 * 
	 * @see NearbySearchResult#compareByOptimalValue(OptimalTimeFinderResult)
	 * @param other
	 *            the {@code NearbySearchResult} to compare to
	 * @param minimize
	 *            indicates, if the score should be minimized or not
	 * @return result of
	 *         {@link NearbySearchResult#compareByOptimalValue(OptimalTimeFinderResult)}
	 *         if {@code minimize = true} and the inverse value otherwise
	 */
	public <T extends NearbySearchResult> int compareByScore(T other,
			boolean minimize) {
		int cmp = this.compareByScore(other);
		if (minimize)
			return cmp;
		else
			return -cmp;
	}

	public OptionalInt getRank() {
		return rank != null ? OptionalInt.of(rank) : OptionalInt.empty();
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public Node getPlace() {
		return place;
	}

	public void setPlace(Node place) {
		this.place = place;
	}


	@Override
	public String toString() {
		return "NearbySearchResult(rank = " + (rank != null ? rank : "None")
				+ ", score = " + score + ", place = " + place + ", "
				+ super.toString() + ")";
	}
}
