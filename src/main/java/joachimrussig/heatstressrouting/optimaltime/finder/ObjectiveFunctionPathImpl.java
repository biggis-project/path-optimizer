package joachimrussig.heatstressrouting.optimaltime.finder;

import java.time.LocalDateTime;
import java.util.OptionalDouble;

import com.graphhopper.routing.Path;
import com.graphhopper.routing.weighting.Weighting;

import joachimrussig.heatstressrouting.routing.RoutingHelper;
import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.thermalcomfort.ThermalComfort;

/**
 * An abstract {@link ObjectiveFunction} that uses a {@link ThermalComfort}
 * object to compute the value.
 * 
 * @author Joachim Rußig
 */
public class ObjectiveFunctionPathImpl implements ObjectiveFunctionPath {

	private WeightingType weightingType;
	private RoutingHelper routingHelper;

	public ObjectiveFunctionPathImpl(WeightingType weightingType,
			RoutingHelper routingHelper) {
		this.weightingType = weightingType;
		this.routingHelper = routingHelper;
	}

	public OptionalDouble value(LocalDateTime time, Path path, Weighting weighting) {
		return OptionalDouble.of(routingHelper.routeWeight(path, time, weightingType));
	}

}
