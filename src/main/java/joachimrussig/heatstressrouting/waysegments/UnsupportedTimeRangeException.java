package joachimrussig.heatstressrouting.waysegments;

/**
 * Exception that indicates an unsupported time range.
 * 
 * @author Joachim Rußig
 */
public class UnsupportedTimeRangeException extends WaySegmentParserException {

	private static final long serialVersionUID = -7875040805480168906L;

	public UnsupportedTimeRangeException() {
	}

	public UnsupportedTimeRangeException(String message) {
		super(message);
	}
}
