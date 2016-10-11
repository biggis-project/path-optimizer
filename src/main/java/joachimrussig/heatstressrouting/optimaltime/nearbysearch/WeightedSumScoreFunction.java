package joachimrussig.heatstressrouting.optimaltime.nearbysearch;

import joachimrussig.heatstressrouting.util.Utils;

/**
 * A weighted sum score function. The {@code distance} and
 * {@code thermalComfort} values are normalized on [0,1] and weighted with
 * {@code weightDistance} respectively {@code weightThermalComfort} before there
 * are added.
 * 
 * @author Joachim Rußig
 */
public class WeightedSumScoreFunction implements ScoreFunction {

	private double weightDistance;
	private double weightThermalComfort;

	public WeightedSumScoreFunction() {
		this.weightDistance = 0.5;
		this.weightThermalComfort = 0.5;
	}

	@Override
	public double score(double distance, double thermalComfort,
			double minDistance, double maxDistance, double minThermalComfot,
			double maxThermalComfort) {
		double dist = Utils.normalize(distance, minDistance, maxDistance);
		double tc = Utils.normalize(thermalComfort, minThermalComfot,
				maxThermalComfort);
		return weightDistance * dist + weightThermalComfort * tc;
	}

	public double getWeightDistance() {
		return weightDistance;
	}

	public WeightedSumScoreFunction setWeightDistance(double weightDistance) {
		this.weightDistance = weightDistance;
		return this;
	}

	public double getWeightThermalComfort() {
		return weightThermalComfort;
	}

	public WeightedSumScoreFunction setWeightThermalComfort(
			double weightThermalComfort) {
		this.weightThermalComfort = weightThermalComfort;
		return this;
	}

	public WeightedSumScoreFunction setWeights(double weightDistance,
			double weightThermalComfort) {
		this.weightDistance = weightDistance;
		this.weightThermalComfort = weightThermalComfort;
		return this;
	}
}
