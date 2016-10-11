package joachimrussig.heatstressrouting.evaluation.optimaltime;

/**
 * The type of the different optimal time finder varaints.
 * 
 * @author Joachim Rußig
 *
 */
public enum OptimalTimeResultType {

	REFERENCE, TEMPERATURE, HEATINDEX, ROUTING_TEMPERATURE, ROUTING_HEATINDEX;

	@Override
	public String toString() {
		return this.name().toLowerCase();
	}

}
