package joachimrussig.heatstressrouting.waysegments;

/**
 * Exception thrown by the {@link WaySegmentParser} if an error occurred.
 * 
 * @author Joachim Rußig
 */
public class WaySegmentParserException extends RuntimeException {

	private static final long serialVersionUID = 4957358872792485630L;

	public WaySegmentParserException() {
		super();
	}

	public WaySegmentParserException(String message) {
		super(message);
	}
}
