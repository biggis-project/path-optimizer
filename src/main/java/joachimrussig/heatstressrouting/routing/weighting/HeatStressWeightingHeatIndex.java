package joachimrussig.heatstressrouting.routing.weighting;

import java.time.LocalDateTime;

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.Weighting;

import joachimrussig.heatstressrouting.routing.HeatStressGraphHopper;
import joachimrussig.heatstressrouting.thermalcomfort.HeatIndex;
import joachimrussig.heatstressrouting.thermalcomfort.ThermalComfortHeatIndex;
import joachimrussig.heatstressrouting.waysegments.WaySegment;
import joachimrussig.heatstressrouting.waysegments.WaySegments;

/**
 * A edge weighting that uses Steadman's heat index (see {@link HeatIndex}) as
 * thermal comfort measure.
 * 
 * @author Joachim Rußig
 */
public class HeatStressWeightingHeatIndex extends HeatStressWeighting
		implements Weighting {

	public static final WeightingType WEIGHTING_TYPE = WeightingType.HEAT_INDEX;
	public static final String NAME = WEIGHTING_TYPE.toString();

	public HeatStressWeightingHeatIndex(FlagEncoder encoder,
			HeatStressGraphHopper hopper, WaySegments segments, LocalDateTime time) {
		super(encoder, hopper, segments, time);
	}

	@Override
	public double getMinWeight(double distance) {
		return distance;
	}

	/**
	 * Computes the weight for the given way segment. The segment is weighted
	 * with {@link ThermalComfortHeatIndex.COMFORT_HEAT_INDEX} if the heat index
	 * value is below {@code ThermalComfortHeatIndex.COMFORT_HEAT_INDEX}.
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
		double relHumidity = getHopper().getWeatherData()
				.getRelativeHumidity(time);

		assert dists.length == temps.length;

		double weight = 0;
		for (int i = 0; i < dists.length; i++) {
			double temp = meanTemp + temps[i];
			weight += dists[i] * thermalComfort(temp, relHumidity);
		}

		return weight;
	}

	/**
	 * Computes the thermal comfort value (heat index) for the given
	 * {@code temperature} and {@code humidity}.
	 * 
	 * @param temperature
	 *            air temperature in °C
	 * @param humidity
	 *            relative humidity in %
	 * @return the computed heat index value or
	 *         {@link ThermalComfortHeatIndex.COMFORT_HEAT_INDEX} if the value
	 *         is below {@code ThermalComfortHeatIndex.COMFORT_HEAT_INDEX}
	 * 
	 */
	protected double thermalComfort(double temperature, double humidity) {
		// Fall back to air temperature if temperature is below 20°C or above
		// 50°C
		double hs = temperature < HeatIndex.MIN_TEMPERATURE
				|| temperature > HeatIndex.MAX_TEMPERATURE ? temperature
						: HeatIndex.heatIndex(temperature, humidity);
		// a heat index below COMFORT_HEAT_INDEX is considered as
		// comfortable
		if (hs <= ThermalComfortHeatIndex.COMFORT_HEAT_INDEX)
			hs = ThermalComfortHeatIndex.COMFORT_HEAT_INDEX;
		return hs;
	}

	@Override
	public String getName() {
		return WEIGHTING_TYPE.toString();
	}

}
