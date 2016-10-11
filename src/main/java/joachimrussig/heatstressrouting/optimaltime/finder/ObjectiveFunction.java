package joachimrussig.heatstressrouting.optimaltime.finder;

import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.util.TimeRange;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

/**
 * An objective function used by {@link OptimalTimeFinder} to find the point in
 * time with the lowest heat stress level.
 * 
 * @author Joachim Rußig
 */
@FunctionalInterface
public interface ObjectiveFunction {

	/**
	 * Returns the objective function value at time {@code time} between
	 * {@code start} and {@code place}.
	 * 
	 * @param time
	 *            the point in time to compute the value for
	 * @param start
	 *            coordinates of the start point
	 * @param place
	 *            coordinates for the place to compute the value for
	 * @param limits
	 *            the lower and upper interval limits in which the optimal
	 *            solution should be found
	 * @param minWalkingTime
	 *            the minimum time required to walk from {@code start} to
	 *            {@code place}
	 * @return the value if a solution is feasible and
	 *         {@code OptionalDouble.empty()} otherwise
	 */
	OptionalDouble value(LocalDateTime time, GHPoint start, GHPoint place,
			TimeRange<LocalDateTime> limits, long minWalkingTime);

	/**
	 * 
	 * @return the weighting type to uses with this objective function and
	 *         {@code Optional.empty()} in non is specified
	 */
	default Optional<WeightingType> getWeightingType() {
		return Optional.empty();
	}

	/**
	 * 
	 * @return time to walk required for the last computed value or
	 *         {@code OptionalLong.empty()} if non is present
	 */
	default OptionalLong getLastWalkingTime() {
		return OptionalLong.empty();
	}

}
