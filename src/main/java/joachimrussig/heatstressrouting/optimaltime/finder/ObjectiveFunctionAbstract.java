package joachimrussig.heatstressrouting.optimaltime.finder;

import com.graphhopper.util.shapes.GHPoint;
import joachimrussig.heatstressrouting.thermalcomfort.ThermalComfort;
import joachimrussig.heatstressrouting.util.TimeRange;

import java.time.LocalDateTime;
import java.util.OptionalDouble;

/**
 * An abstract {@link ObjectiveFunction} that uses a {@link ThermalComfort}
 * object to compute the value.
 * 
 * @author Joachim Rußig
 */
public abstract class ObjectiveFunctionAbstract implements ObjectiveFunction {

	private ThermalComfort thermalComfort;

	ObjectiveFunctionAbstract(ThermalComfort thermalComfort) {
		this.thermalComfort = thermalComfort;
	}

	@Override
	public abstract OptionalDouble value(LocalDateTime time, GHPoint start,
			GHPoint place, TimeRange<LocalDateTime> limits,
			long minWalkingTime);

	public ThermalComfort getThermalComfort() {
		return thermalComfort;
	}

	public void setThermalComfort(ThermalComfort thermalComfort) {
		this.thermalComfort = thermalComfort;
	}
}
