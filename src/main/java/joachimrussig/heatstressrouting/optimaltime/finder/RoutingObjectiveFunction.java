package joachimrussig.heatstressrouting.optimaltime.finder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

import com.graphhopper.PathWrapper;
import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.routing.HeatStressGraphHopper;
import joachimrussig.heatstressrouting.routing.RoutingHelper;
import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.thermalcomfort.ThermalComfort;
import joachimrussig.heatstressrouting.util.TimeRange;

/**
 * A score function that use the optimal route according to the specified
 * {@link WeightingType} between {@code start} and {@code place} to compute the
 * heat stress value.
 * 
 * @author Joachim Rußig
 */
public class RoutingObjectiveFunction extends ObjectiveFunctionAbstract {

	private RoutingHelper routingHelper;
	private WeightingType weightingType;

	private Long lastWalkingTime = null;

	/**
	 * Creates a new {@code RoutingObjectiveFunction}.
	 * 
	 * @param thermalComfort
	 *            the {@link ThermalComfort} to use
	 * @param routingHelper
	 *            the {@link RoutingHelper} to use to find the optimal route
	 * @param weightingType
	 *            the edge {@link WeightingType} to use
	 */
	public RoutingObjectiveFunction(ThermalComfort thermalComfort,
			RoutingHelper routingHelper, WeightingType weightingType) {
		super(thermalComfort);
		this.routingHelper = routingHelper;
		this.weightingType = weightingType;
	}

	/**
	 * Creates a new {@code RoutingObjectiveFunction}.
	 * 
	 * @param thermalComfort
	 *            the {@link ThermalComfort} to use
	 * @param hopper
	 *            the {@link HeatStressGrpahHopper} to use to find the optimal
	 *            route
	 * @param weightingType
	 *            the edge {@link WeightingType} to use
	 */
	public RoutingObjectiveFunction(ThermalComfort thermalComfort,
			HeatStressGraphHopper hopper, WeightingType weightingType) {
		this(thermalComfort, new RoutingHelper(hopper), weightingType);
	}

	@Override
	public OptionalDouble value(LocalDateTime time, GHPoint start,
			GHPoint place, TimeRange<LocalDateTime> limits,
			long minWalkingTime) {
		Optional<PathWrapper> path = routingHelper
				.route(start, place, time, weightingType).get();

		if (path.isPresent()) {
			long timeWalk = Math.max(path.get().getTime() - minWalkingTime, 0);
			lastWalkingTime = timeWalk;
			if (time.plus(timeWalk, ChronoUnit.MILLIS)
					.compareTo(limits.getTo()) <= 0) {
				// return getThermalComfort().value(time);
				return OptionalDouble.of(path.get().getRouteWeight());
			}
		}

		return OptionalDouble.empty();
	}

	public RoutingHelper getRoutingHelper() {
		return routingHelper;
	}

	public void setRoutingHelper(RoutingHelper routingHelper) {
		this.routingHelper = routingHelper;
	}

	@Override
	public Optional<WeightingType> getWeightingType() {
		return Optional.ofNullable(weightingType);
	}

	@Override
	public OptionalLong getLastWalkingTime() {
		if (lastWalkingTime != null)
			return OptionalLong.of(lastWalkingTime);
		else
			return OptionalLong.empty();
	}

	public void setWeightingType(WeightingType weightingType) {
		this.weightingType = weightingType;
	}
}
