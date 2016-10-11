package joachimrussig.heatstressrouting.optimaltime.finder;

import java.time.LocalDateTime;
import java.util.OptionalDouble;

import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.Weighting;

/**
 * An objective function used by {@link OptimalTimeFinder} to find the point in
 * time with the lowest heat stress level.
 * 
 * @author Joachim Rußig
 */
@FunctionalInterface
public interface ObjectiveFunctionPath {

	/**
	 * Returns the objective function value at time {@code time} for the
	 * specified {@code path}.
	 * 
	 * @param time
	 * @param path
	 *            the path between the start and the place
	 * @param weighting
	 *            the weighting used to calculated the ObjectiveFunctionValue
	 * 
	 * @return the value if a solution is feasible and
	 *         {@code OptionalDouble.empty()} otherwise
	 */
	OptionalDouble value(LocalDateTime time, Path path, Weighting weighting);

}
