package joachimrussig.heatstressrouting.evaluation.optimaltime;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import joachimrussig.heatstressrouting.evaluation.Evaluator;

/**
 * The {@code OptimalTimeEvaluationItem} represents an item used for evaluation.
 * The item contains information needed to perform an evaluation, e.g. the start
 * point, the current time, etc.
 * 
 * @author Joachim Rußig
 *
 */
public class OptimalTimeEvaluationItem {

	final Node start;
	private final List<Tag> targetTags;

	final LocalDateTime now;

	final Duration timeBuffer;

	private final LocalDateTime earliestTime;
	private final LocalDateTime latestTime;

	final Double maxDistance;
	final Integer maxResults;

	/**
	 * 
	 * @param start
	 *            the start point
	 * @param targetTags
	 *            the search criteria as a list of OSM tags
	 * @param now
	 *            the current point in time
	 * @param timeBuffer
	 *            time needed at the place maximum number of places to consider
	 */
	public OptimalTimeEvaluationItem(Node start, List<Tag> targetTags,
			LocalDateTime now, Duration timeBuffer) {
		this(start, targetTags, now, timeBuffer, null, null, null, null);
	}

	/**
	 * 
	 * @param start
	 *            the start point
	 * @param targetTags
	 *            the search criteria as a list of OSM tags
	 * @param now
	 *            the current point in time
	 * @param timeBuffer
	 *            time needed at the place
	 * @param earliestTime
	 *            the earliest time for the optimal point in time
	 * @param latestTime
	 *            the latest time for the optimal point in time
	 */
	public OptimalTimeEvaluationItem(Node start, List<Tag> targetTags,
			LocalDateTime now, Duration timeBuffer, LocalDateTime earliestTime,
			LocalDateTime latestTime) {
		this(start, targetTags, now, timeBuffer, earliestTime, latestTime, null,
				null);
	}

	/**
	 * 
	 * @param start
	 *            the start point
	 * @param targetTags
	 *            the search criteria as a list of OSM tags
	 * @param now
	 *            the current point in time
	 * @param timeBuffer
	 *            time needed at the place
	 * @param maxDistance
	 *            maximum direct distance ("as the crow flies") between the
	 *            start and the place
	 * @param maxResults
	 *            maximum number of places to consider
	 */
	public OptimalTimeEvaluationItem(Node start, List<Tag> targetTags,
			LocalDateTime now, Duration timeBuffer, Double maxDistance,
			Integer maxResults) {
		this(start, targetTags, now, timeBuffer, null, null, maxDistance,
				maxResults);
	}

	/**
	 * 
	 * @param start
	 *            the start point
	 * @param targetTags
	 *            the search criteria as a list of OSM tags
	 * @param now
	 *            the current point in time
	 * @param timeBuffer
	 *            time needed at the place
	 * @param earliestTime
	 *            the earliest time for the optimal point in time
	 * @param latestTime
	 *            the latest time for the optimal point in time
	 * @param maxDistance
	 *            maximum direct distance ("as the crow flies") between the
	 *            start and the place
	 * @param maxResults
	 *            maximum number of places to consider
	 */
	public OptimalTimeEvaluationItem(Node start, List<Tag> targetTags,
			LocalDateTime now, Duration timeBuffer, LocalDateTime earliestTime,
			LocalDateTime latestTime, Double maxDistance, Integer maxResults) {
		this.start = start;
		this.targetTags = targetTags;
		this.now = now;
		this.timeBuffer = timeBuffer;
		this.earliestTime = earliestTime;
		this.latestTime = latestTime;
		this.maxDistance = maxDistance;
		this.maxResults = maxResults;
	}

	public Node getStart() {
		return start;
	}

	public List<Tag> getTargetTags() {
		return targetTags;
	}

	public LocalDateTime getNow() {
		return now;
	}

	public Optional<LocalDateTime> getEarliestTime() {
		return Optional.ofNullable(earliestTime);
	}

	public Optional<LocalDateTime> getLatestTime() {
		return Optional.ofNullable(latestTime);
	}

	Optional<Duration> getTimeBuffer() {
		return Optional.ofNullable(timeBuffer);
	}

	public OptionalDouble getMaxDistance() {
		if (maxDistance != null)
			return OptionalDouble.of(maxDistance);
		else
			return OptionalDouble.empty();
	}

	public OptionalInt getMaxResults() {
		if (maxResults != null)
			return OptionalInt.of(maxResults);
		else
			return OptionalInt.empty();
	}

	public boolean hasEarliestTime() {
		return earliestTime != null;
	}

	public boolean hasLatestTime() {
		return latestTime != null;
	}

	public boolean hasTimeBuffer() {
		return timeBuffer != null;
	}

	public boolean hasMaxDistance() {
		return maxDistance != null;
	}

	public boolean hasMaxResults() {
		return maxResults != null;
	}

	public String toCsvRecord() {

		String tags = targetTags.stream()
				.map(t -> t.getKey() + "=" + t.getValue())
				.collect(Collectors.joining(Evaluator.DELIMITER_POINT_LIST));

		Object[] vals = new Object[] { start.getId(), start.getLatitude(),
				start.getLongitude(), now, tags, timeBuffer, earliestTime,
				latestTime, maxDistance, maxResults };

		return Arrays.stream(vals).map(String::valueOf)
				.collect(Collectors.joining(Evaluator.DELIMITER));

	}

	@Override
	public String toString() {
		String out = "Start = " + start + ", targetTags = " + targetTags
				+ ", now = " + now + ", timeBuffer = " + timeBuffer;
		if (hasEarliestTime())
			out += ", earliestTime" + earliestTime;
		if (hasLatestTime())
			out += ", latestTime" + latestTime;
		if (hasMaxDistance())
			out += ", maxDistance = " + maxDistance;
		if (hasMaxResults())
			out += ", maxResults = " + maxResults;
		return out;
	}
}