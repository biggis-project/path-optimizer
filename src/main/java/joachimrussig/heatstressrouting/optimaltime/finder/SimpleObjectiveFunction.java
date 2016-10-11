package joachimrussig.heatstressrouting.optimaltime.finder;

import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.thermalcomfort.ThermalComfort;
import joachimrussig.heatstressrouting.util.TimeRange;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * A simple objective function that only considers the thermal comfort value of
 * the specified {@link ThermalComfort} and not the distance between
 * {@code start} and {@code place}.
 * 
 * @author Joachim Rußig
 */
public class SimpleObjectiveFunction extends ObjectiveFunctionAbstract {

	public SimpleObjectiveFunction(ThermalComfort thermalComfort) {
		super(thermalComfort);
	}

	@Override
	public OptionalDouble value(LocalDateTime time, GHPoint start,
			GHPoint place, TimeRange<LocalDateTime> limits,
			long minWalkingTime) {
		return getThermalComfort().value(time);
	}

	@Override
	public Optional<WeightingType> getWeightingType() {
		return Optional.of(WeightingType.SHORTEST);
	}
}
