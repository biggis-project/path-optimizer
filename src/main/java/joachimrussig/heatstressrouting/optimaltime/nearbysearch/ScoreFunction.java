package joachimrussig.heatstressrouting.optimaltime.nearbysearch;

/**
 * A score function used to rank the results of a nearby search. Either lower
 * score (minimize, default) or a higher score (maximize) is better. Weather the
 * score should be minimized or maximized is indicated by the {@code minimize}
 * method.
 * 
 * @author Joachim Rußig
 */
@FunctionalInterface
public interface ScoreFunction {

	/**
	 * Computes the score of an item. The scores must be partial ordered.
	 * 
	 * @param distance
	 *            distance between the start point and the place
	 * @param thermalComfort
	 *            thermal comfort value
	 * @param minDistance
	 *            the minimum distance between the start point and a place
	 * @param maxDistance
	 *            the maximum distance between the start point and a place
	 * @param minThermalComfot
	 *            the minimum distance thermal comfort
	 * @param maxThermalComfort
	 *            the maximum distance thermal comfort
	 * @return the computed score
	 */
	double score(double distance, double thermalComfort, double minDistance,
			double maxDistance, double minThermalComfot,
			double maxThermalComfort);

	/**
	 * 
	 * @return indicates weather the score function should be minimized or not.
	 *         The default value is {@code true}.
	 */
	default boolean minimize() {
		return true;
	}

}
