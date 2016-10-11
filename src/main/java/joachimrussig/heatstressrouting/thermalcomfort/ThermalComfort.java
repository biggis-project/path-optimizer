package joachimrussig.heatstressrouting.thermalcomfort;

import java.time.LocalDateTime;
import java.util.OptionalDouble;

/**
 * A interface the represents a thermal comfort function.
 * 
 * @author Joachim Rußig
 */
@FunctionalInterface
public interface ThermalComfort {

	/**
	 * The thermal comfort value at {@code time}.
	 * 
	 * @param time
	 * @return the thermal comfort value or {@code OptionalDouble.empty()} if no
	 *         valid value is present
	 */
	OptionalDouble value(LocalDateTime time);

}
