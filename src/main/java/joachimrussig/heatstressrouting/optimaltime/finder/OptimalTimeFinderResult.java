package joachimrussig.heatstressrouting.optimaltime.finder;

import java.time.LocalDateTime;

import com.graphhopper.routing.Path;

import joachimrussig.heatstressrouting.util.Utils;

/**
 * The {@code OptimalTimeFinderResult} represent an result of the search of an
 * optimal point in time.
 * 
 * @author Joachim Rußig
 */
public class OptimalTimeFinderResult {

	private LocalDateTime optimalTime;
	private double distance;



	private double optimalValue;
	private long duration;
	private Path optimalPath;
	private Path shortestPath;
	/**
	 * Creates an new {@code OptimalTimeFinderResult}.
	 * 
	 * @param optimalTime
	 *            the optimal point in time found
	 * @param distance
	 *            the distance of the optimal route between start and place
	 * @param optimalValue
	 *            the optimal objective function value
	 * @param duration
	 *            time needed to walk the found route
	 */
	public OptimalTimeFinderResult(LocalDateTime optimalTime, double distance,
			double optimalValue, long duration, Path optimalPath, Path shortestPath) {
		this.optimalTime = optimalTime;
		this.distance = distance;
		this.optimalValue = optimalValue;
		this.duration = duration;
		this.optimalPath = optimalPath;
		this.shortestPath = shortestPath;
	}
	/**
	 * Compares the two {@code OptimalTimeFinderResult} according to the
	 * {@code optimalValue}.
	 * 
	 * @param other
	 * @throws IllegalStateException
	 *             if either {@code this} or {@code other} has errors
	 * @return -1 if {@code this.optimalValue < other.optimalValue}, 0 if
	 *         {@code this.optimalValue == other.optimalValue} and 1 if
	 *         {@code this.optimalValue > other.optimalValue}
	 */
	public <T extends OptimalTimeFinderResult> int compareByOptimalValue(
			T other) {
		if (this.optimalValue < other.getOptimalValue())
			return -1;
		else if (this.optimalValue > other.getOptimalValue())
			return 1;
		else
			return 0;
	}
	
	/**
	 * Compares the two {@code OptimalTimeFinderResult} according to the
	 * {@code optimalValue}.
	 * 
	 * @see OptimalTimeFinderResult#compareByOptimalValue(OptimalTimeFinderResult)
	 * @param other
	 * @param minimize
	 *            indicates whether the score should be minimized or not
	 * @throws IllegalStateException
	 *             if either {@code this} or {@code other} has errors
	 * @return the result of
	 *         {@link OptimalTimeFinderResult#compareByOptimalValue(OptimalTimeFinderResult)}
	 *         if {@code minimize = true} and the inverse value otherwise
	 */
	public <T extends OptimalTimeFinderResult> int compareByOptimalValue(
			T other, boolean minimize) {
		int cmp = this.compareByOptimalValue(other);
		if (minimize)
			return cmp;
		else
			return -cmp;
	}
	public double getDistance() {
		return distance;
	}


	public long getDuration() {
		return duration;
	}

	public Path getOptimalPath() {
		return optimalPath;
	}

	public LocalDateTime getOptimalTime() {
	return optimalTime;
}

	public double getOptimalValue() {
		return optimalValue;
	}

		public Path getShortestPath() {
			return shortestPath;
		}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public void setOptimalPath(Path optimalPath) {
		this.optimalPath = optimalPath;
	}

	public void setOptimalTime(LocalDateTime optimalTime) {
		this.optimalTime = optimalTime;
	}

	public void setOptimalValue(double optimalValue) {
		this.optimalValue = optimalValue;
	}

	/**
	 * Sets the results to the specified values
	 * 
	 * @param optimalTime
	 *            the optimal point in time found
	 * @param distance
	 *            the distance of the optimal route between start and place
	 * @param optimalValue
	 *            the optimal objective function value
	 * @param duration
	 *            time needed to walk the found route
	 */
	public void setResult(LocalDateTime optimalTime, double distance,
			double optimalValue, long duration) {
		this.optimalTime = optimalTime;
		this.distance = distance;
		this.optimalValue = optimalValue;
		this.duration = duration;
	}

	
	
	public void setShortestPath(Path shortestPath) {
		this.shortestPath = shortestPath;
	}
	
	

	@Override
	public String toString() {
		String res = "optimalTime = " + optimalTime + ", distance = " + distance
				+ ", optimalValue = " + optimalValue + ", duration = "
				+ Utils.formatDurationMills(duration);
		return res;
	}
}
