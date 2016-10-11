package joachimrussig.heatstressrouting.routing.weighting;

import java.time.LocalDateTime;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;

import joachimrussig.heatstressrouting.routing.HeatStressGraphHopper;
import joachimrussig.heatstressrouting.thermalcomfort.ThermalComfortTemperature;
import joachimrussig.heatstressrouting.waysegments.WaySegment;
import joachimrussig.heatstressrouting.waysegments.WaySegments;

/**
 * An edge weighting that uses the air temperature as thermal comfort measure.
 * 
 * @author Joachim Rußig
 */
public class HeatStressWeightingTemperature extends HeatStressWeighting
		implements Weighting {

	public static final WeightingType WEIGHTING_TYPE = WeightingType.TEMPERATURE;
	public static final String NAME = WEIGHTING_TYPE.toString();

	public HeatStressWeightingTemperature(FlagEncoder encoder,
			HeatStressGraphHopper hopper, WaySegments segments, LocalDateTime time) {
		super(encoder, hopper, segments, time);
	}

	@Override
	public double getMinWeight(double distance) {
		return distance;
	}

	/**
	 * Computes the weight for the given way segment. The segment is weighted
	 * with {@link ThermalComfortTemperature.COMFORT_TEMPERATURE} if the air
	 * temperature value is below
	 * {@code ThermalComfortTemperature.COMFORT_TEMPERATURE}.
	 * 
	 * @param segment
	 *            the way segment to compute the weight for
	 * @param time
	 *            to compute the weight for
	 * @return the computed weight
	 */
	@Override
	protected double computeSegmentWeight(WaySegment segment,
			LocalDateTime time) {
		double[] dists = segment.getDistances();
		double[] temps = segment.getTemperatureDifferences();

		double meanTemp = getHopper().getWeatherData().getTemperature(time);

		assert dists.length == temps.length;

		double weight = 0;
		for (int i = 0; i < dists.length; i++) {
			double temp = meanTemp + temps[i];
			double hs = temp > ThermalComfortTemperature.COMFORT_TEMPERATURE
					? temp : ThermalComfortTemperature.COMFORT_TEMPERATURE;
			weight += dists[i] * hs;
		}

		return weight;
	}

	@Override
	public String getName() {
		return WEIGHTING_TYPE.toString();
	}

}
