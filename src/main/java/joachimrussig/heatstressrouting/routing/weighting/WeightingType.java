package joachimrussig.heatstressrouting.routing.weighting;

import java.util.Optional;

import joachimrussig.heatstressrouting.routing.HeatStressGraphHopper;

/**
 * The weighting types supported by {@link HeatStressGraphHopper}.
 * 
 * @author Joachim Rußig
 */
public enum WeightingType {

	SHORTEST, TEMPERATURE, HEAT_INDEX, HEAT_INDEX_WEIGHTED;

	@Override
	public String toString() {
		switch (this) {
		case SHORTEST:
			return "shortest";
		case TEMPERATURE:
			return "temperature";
		case HEAT_INDEX:
			return "heatindex";
		case HEAT_INDEX_WEIGHTED:
			return "heatindexweighted";
		default:
			throw new AssertionError("No such variant " + super.toString());
		}
	}

	/**
	 * Returns the corresponding {@code WeightingType} value if the string is
	 * equals to the string representation returned by {@code toString()} (the
	 * case is ignored), or {@code Optional.empty()} otherwise.
	 * 
	 * @param val
	 *            the value to get the weighting type for
	 * @return the corresponding {@code WeightingType} value if the string is
	 *         equals to the string representation returned by
	 *         {@code toString()} (the case is ignored), or
	 *         {@code Optional.empty()} otherwise
	 */
	public static Optional<WeightingType> from(String val) {
		val = val.trim();
		for (WeightingType value : WeightingType.values()) {
			if (value.equalsIgnoreCase(val))
				return Optional.of(value);
		}
		return Optional.empty();
	}

	/**
	 * Checks if {@code this} is equals to {@code s}.
	 * 
	 * @param s
	 * @return true, if {@code this} is equals to {@code s}
	 */
	public boolean equalsIgnoreCase(String s) {
		return this.toString().equalsIgnoreCase(s);
	}

}
