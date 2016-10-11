package joachimrussig.heatstressrouting.waysegments;

import joachimrussig.heatstressrouting.util.TimeRange;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The {@code WaySegment} class represents a part of an OSM way connected by tow
 * adjacent OSM nodes. Each segment can be identified by an {@link WaySegmentId}
 * that consist of the way id and the ids of the tow adjacent nodes. A set of
 * {@code distances} and a set of {@code temperatureDifferences} is associated
 * with each {@code WaySegement} representing the length and value of each
 * raster cell intersected by the {@code WaySegement}.
 * 
 * @author Joachim Rußig
 */
public class WaySegment {

	private final WaySegmentId id;
	private final double distance;
	private final TimeRange<LocalTime> timeRange;

	private final double[] distances;
	private final double[] temperatureDifferences;

	/**
	 * Creates a new {@code WaySegment}.
	 * 
	 * <p>
	 * 
	 * <b>Note:</b> {@code distances} and {@code temperatureDifferences} must
	 * have the same length.
	 * 
	 * @param wayId
	 *            the OSM way id
	 * @param nodeIds
	 *            the OSM node ids of the two adjacent nodes
	 * @param timeRange
	 *            a time range the value is valid for
	 * @param distances
	 *            lengths of the intersection with the intersected raster cells
	 * @param temperatureDifferences
	 *            values of the intersected raster cells
	 * 
	 * @throws IllegalArgumentException
	 *             if {@code distances} and {@code temperatureDifferences}
	 *             doesn't have the same length
	 */
	public WaySegment(long wayId, Pair<Long, Long> nodeIds,
			TimeRange<LocalTime> timeRange, double[] distances,
			double[] temperatureDifferences) {
		if (distances.length != temperatureDifferences.length)
			throw new IllegalArgumentException(
					"distances and temperatureDifferences must have the same length");

		this.id = new WaySegmentId(wayId, nodeIds);
		this.timeRange = timeRange;
		this.distances = distances;
		this.temperatureDifferences = temperatureDifferences;
		this.distance = Arrays.stream(distances).sum();
	}

	/**
	 * Creates a new {@code WaySegment}.
	 * 
	 * <p>
	 * 
	 * <b>Note:</b> {@code distances} and {@code temperatureDifferences} must
	 * have the same length.
	 * 
	 * @param wayId
	 *            the OSM way id
	 * @param nodeIds
	 *            the OSM node ids of the two adjacent nodes
	 * @param distances
	 *            lengths of the intersection with the intersected raster cells
	 * @param temperatureDifferences
	 *            values of the intersected raster cells
	 * 
	 * @throws IllegalArgumentException
	 *             if {@code distances} and {@code temperatureDifferences}
	 *             doesn't have the same length
	 */
	public WaySegment(long wayId, Pair<Long, Long> nodeIds, double[] distances,
			double[] temperatureDifferences) {
		this(wayId, nodeIds, null, distances, temperatureDifferences);
	}

	/**
	 * Checks if {@code this} and {@code other} have the same
	 * {@link WaySegmentId}
	 * 
	 * @param other
	 *            a {@code WaySegment}
	 * @return true, if {@code this} and {@code other} have the same
	 *         {@code WaySegmentId}
	 */
	public boolean hasSameId(WaySegment other) {
		return this.id.equals(other.id);
	}

	/**
	 * 
	 * @return the {@code id} of the {@code WaySegment}
	 */
	public WaySegmentId getId() {
		return this.id;
	}

	/**
	 * 
	 * @return the OSM way id
	 */
	public long getWayId() {
		return id.getWayId();
	}

	/**
	 * 
	 * @return the OSM ids of the adjacent nodes
	 */
	public Pair<Long, Long> getNodeIds() {
		return id.getNodeIds();
	}

	/**
	 * 
	 * @return the length of the {@code WaySegment}
	 */
	public double getDistance() {
		return distance;
	}

	/**
	 * 
	 * @return lengths of the intersection with the intersected raster cells
	 */
	public double[] getDistances() {
		return distances;
	}

	/**
	 * 
	 * @return values of the intersected raster cells
	 */
	public double[] getTemperatureDifferences() {
		return temperatureDifferences;
	}

	/**
	 * 
	 * @return time range the value is valid for
	 */
	public Optional<TimeRange<LocalTime>> getTimeRange() {
		return Optional.ofNullable(this.timeRange);
	}

	/**
	 * Checks weather a given time is within time range.
	 * 
	 * @param time
	 *            to be checked
	 * @return true, if start <= time < end or if timeRange is empty
	 */
	public boolean isWithinTimeRange(LocalTime time) {
		// http://stackoverflow.com/questions/22310329/jodatime-check-if-localtime-is-after-now-and-now-before-another-localtime

		if (timeRange != null)
			return timeRange.getFrom().compareTo(time) <= 0
					&& time.compareTo(timeRange.getTo()) < 0;
		else
			return true;
	}

	@Override
	public String toString() {
		return "EdgeSegment(wayId = " + getWayId() + ", nodeIds = "
				+ getNodeIds() + ", TemperatureDifferences = "
				+ Arrays.stream(temperatureDifferences)
						.mapToObj(String::valueOf)
						.collect(Collectors.joining(", "))
				+ ", distances = "
				+ Arrays.stream(distances).mapToObj(String::valueOf)
						.collect(Collectors.joining(", "))
				+ ")";
	}
}
