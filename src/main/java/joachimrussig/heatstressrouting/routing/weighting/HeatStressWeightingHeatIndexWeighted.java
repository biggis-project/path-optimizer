package joachimrussig.heatstressrouting.routing.weighting;

import java.time.LocalDateTime;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;

import joachimrussig.heatstressrouting.routing.HeatStressGraphHopper;
import joachimrussig.heatstressrouting.thermalcomfort.HeatIndex;
import joachimrussig.heatstressrouting.waysegments.WaySegment;
import joachimrussig.heatstressrouting.waysegments.WaySegments;

/**
 * A edge weighting that uses Steadman's heat index (see {@link HeatIndex}) as
 * thermal comfort measure. Distance and thermal comfort can be weighted using a
 * weighted product model.
 * 
 * @author Joachim Rußig
 */
public class HeatStressWeightingHeatIndexWeighted
		extends HeatStressWeightingHeatIndex implements Weighting {

	public static final WeightingType WEIGHTING_TYPE = WeightingType.HEAT_INDEX_WEIGHTED;
	public static final String NAME = WEIGHTING_TYPE.toString();

	private double weightDistance = 0.5;
	private double weightThermalComfort = 0.5;

	public HeatStressWeightingHeatIndexWeighted(FlagEncoder encoder,
			HeatStressGraphHopper hopper, WaySegments segments,
			LocalDateTime time) {
		super(encoder, hopper, segments, time);
	}

	@Override
	public double getMinWeight(double distance) {
		return distance;
	}

	@Override
	protected double computeSegmentWeight(WaySegment segment,
			LocalDateTime time) {
		double[] dists = segment.getDistances();
		double[] temps = segment.getTemperatureDifferences();

		double meanTemp = getHopper().getWeatherData().getTemperature(time);
		double relHumidity = getHopper().getWeatherData()
				.getRelativeHumidity(time);

		assert dists.length == temps.length;

		double weight = 0;
		for (int i = 0; i < dists.length; i++) {
			double temp = meanTemp + temps[i];
			double hs = thermalComfort(temp, relHumidity);
			weight += Math.pow(dists[i], weightDistance)
					* Math.pow(hs, weightThermalComfort);
		}

		return weight;
	}

	@Override
	public String getName() {
		return WEIGHTING_TYPE.toString();
	}

	public double getWeightDistance() {
		return weightDistance;
	}

	/**
	 * Sets the weights for the distance and the temperature.
	 * 
	 * @param weightDistance
	 *            a value between 0 and 1
	 * @param weightThermalComfort
	 *            a value between 0 and 1
	 * @throws IllegalArgumentException
	 *             if {@code weightDistance} or {@code weightThermalComfort} is
	 *             not between 0 and 1
	 */
	public void setWeights(double weightDistance, double weightThermalComfort) {
		if (weightDistance < 0 || weightDistance > 1)
			throw new IllegalArgumentException(
					"weightDistance must be a number between 0 and 1");

		if (weightThermalComfort < 0 || weightThermalComfort > 1)
			throw new IllegalArgumentException(
					"weightThermalComfort must be a number between 0 and 1");

		this.weightDistance = weightDistance;
		this.weightThermalComfort = weightThermalComfort;
	}

	public double getWeightThermalComfort() {
		return weightThermalComfort;
	}

}
