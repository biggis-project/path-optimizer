package joachimrussig.heatstressrouting.waysegments;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalDouble;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The {@code WaySegments} class represents a set of {@link WaySegment}s.
 * 
 * @author Joachim Rußig
 */
public class WaySegments {

	private MultiValuedMap<WaySegmentId, WaySegment> segments;

	/**
	 * Creates a new {@code WaySegments} object of an
	 * {@code ArrayListValuedHashMap}.
	 * <p>
	 * <b>Note: </b> for each entry the key and the {@link WaySegmentId} of the
	 * value must be equals.
	 * 
	 * @param segments
	 *            the {@code WaySegment}s
	 */
	WaySegments(ArrayListValuedHashMap<WaySegmentId, WaySegment> segments) {
		this.segments = segments;
	}

	/**
	 * Creates a new {@code WaySegments}.
	 * 
	 * @param edgeSegments
	 *            a set of {@code WaySegment}s
	 */
	public WaySegments(Collection<WaySegment> edgeSegments) {
		setSegments(edgeSegments);
	}

	/**
	 * Returns all {@link WaySegment}s with the specified {@code id}.
	 * 
	 * @param id
	 *            of the requested way segment
	 * @return all segments with the specified {@code id}
	 */
	public Collection<WaySegment> getSegments(WaySegmentId id) {
		return CollectionUtils.union(segments.get(id),
				segments.get(new WaySegmentId(id.getWayId(),
						Pair.of(id.getNodeIds().getRight(),
								id.getNodeIds().getLeft()))));
	}

	/**
	 * Returns the first segment if present, and {@code Optional.empty()}
	 * otherwise.
	 *
	 * @param id
	 *            of the requested way segment
	 * @param time
	 *            of the requested way segment
	 * @return the first segment matching {@code id} and {@code time}. If
	 *         {@code WaySegment.timeRange} is empty the first segment matching
	 *         the {@code id}.
	 */
	public Optional<WaySegment> getSegment(WaySegmentId id, LocalTime time) {
		Collection<WaySegment> vals = getSegments(id);
		if (vals.isEmpty())
			return Optional.empty();

		return vals.stream().filter(s -> s.isWithinTimeRange(time)).findFirst();
	}

	/**
	 * Returns all {@link WaySegment}s with the specified {@code wayId} and
	 * {@code nodeIds}.
	 * <p>
	 * <b>Note:</b> the order of the node ids is relevant.
	 * 
	 * @param wayId
	 *            the OSM way id of the requested segments
	 * @param nodesId
	 *            the OSM node ids of the requested segments
	 * @return all segments with the specified {@code wayId} and {@code nodeIds}
	 */
	public Collection<WaySegment> getSegments(long wayId,
			Pair<Long, Long> nodesId) {
		return getSegments(new WaySegmentId(wayId, nodesId));
	}

	/**
	 * 
	 * @return the {@code MultiValuedMap} used internally to store the segments
	 */
	public MultiValuedMap<WaySegmentId, WaySegment> getSegments() {
		return segments;
	}

	/**
	 * Sets the {@code MultiValuedMap} used internally to store the segments.
	 * 
	 * <b>Note: </b> for each entry the key and the {@code WaySegmentId} of the
	 * value must be the equals.
	 * 
	 * @param segments
	 *            the segments to set
	 */
	void setSegments(
			ArrayListValuedHashMap<WaySegmentId, WaySegment> segments) {
		this.segments = segments;
	}

	/**
	 * Sets the {@code WaySegment}s.
	 * 
	 * @param edgeSegments
	 *            a collection of way segments to store
	 */
	public void setSegments(Collection<WaySegment> edgeSegments) {
		MultiValuedMap<WaySegmentId, WaySegment> res = new ArrayListValuedHashMap<>();
		for (WaySegment segment : edgeSegments) {
			res.put(segment.getId(), segment);
		}
		this.segments = res;
	}

	/**
	 * 
	 * @return the minimal temperature difference of all segments, or
	 *         {@code OptionalDouble.empty()} if no segment is present
	 */
	public OptionalDouble getMinTemperatureDifference() {
		return this.segments.values().stream()
				.map(s -> Arrays.stream(s.getTemperatureDifferences()).min())
				.filter(OptionalDouble::isPresent)
				.mapToDouble(OptionalDouble::getAsDouble).min();
	}

}
