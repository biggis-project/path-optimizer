package joachimrussig.heatstressrouting.optimaltime.nearbysearch;

/**
 * Simple score function, that just returns the {@code thermalComfort} value.
 * 
 * @author joachimrussig
 *
 */
public class ThermalComfortScoreFunction implements ScoreFunction {

	@Override
	public double score(double distance, double thermalComfort,
			double minDistance, double maxDistance, double minThermalComfot,
			double maxThermalComfort) {
		return thermalComfort;
	}

}
