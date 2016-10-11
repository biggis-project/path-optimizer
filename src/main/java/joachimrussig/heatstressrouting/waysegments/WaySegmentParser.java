package joachimrussig.heatstressrouting.waysegments;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalTime;
import java.util.ArrayList;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import joachimrussig.heatstressrouting.waysegments.WaySegment;
import joachimrussig.heatstressrouting.waysegments.WaySegmentId;
import joachimrussig.heatstressrouting.waysegments.WaySegments;
import joachimrussig.heatstressrouting.util.TimeRange;

/**
 * The {@code WaySegmentParser} parses the way segment data given as comma
 * separated value (csv) file and returns the parsed data as {@link WaySegments}
 * object.
 * <p>
 * The columns of the csv file are separated by a pipe ('|'):
 * 
 * <ul>
 * <li>{@code way_id}: the OSM way id of the way segment</li>
 * <li>{@code from.osm.id}: the OSM node id of the first adjacent node</li>
 * <li>{@code to.osm.id}: the OSM node id of the second adjacent node</li>
 * <li>{@code dist}: length of an intersection with the way segment specified by
 * {@code way_id}, {@code from.osm.id} and {@code to.osm.id}</li>
 * <li>{@code delta_temp}: value of an intersected raster cell</li>
 * <li>{@code time_range}: time range the value is valied for; either the string
 * 'morgen' or 'abend'</li>
 * </ul>
 * 
 * A way segment can uniquely by identified by {@code way_id},
 * {@code from.osm.id} and {@code to.osm.id}. Each way segment can have multiple
 * ({@code dist}, {@code delta_temp}) pairs, where the pair are provided as
 * separate rows with the same {@code way_id}, {@code from.osm.id} and
 * {@code to.osm.id} ids.
 * <p>
 * 
 * @author Joachim Rußig
 */
public class WaySegmentParser {

	private static final String WAY_ID_COL = "way_id";
	private static final String FROM_COL = "from.osm.id";
	private static final String TO_COL = "to.osm.id";
	private static final String DIST_COL = "dist";
	private static final String TEMPERATURE_DIFFERENCE_COL = "delta_temp";
	private static final String TIME_COL = "time_range";
	private static final String TIME_MORNING = "morgen";
	private static final String TIME_EVENING = "abend";
	private static final char DELIMITER = '|';

	/**
	 * Creates a new {@code WaySegmentParser}.
	 */
	public WaySegmentParser() {
	}

	/**
	 * 
	 * Parses the csv file provided as {@code file} object.
	 * 
	 * @param file
	 *            the csv file to parse (see specification above)
	 * 
	 * @return the parsed way segments
	 * 
	 * 
	 * @throws IOException
	 *             if an error occurs while reading {@code file}
	 * @throws WaySegmentParserException
	 *             if an error occurred while parsing the record (e.g. a
	 *             required column is not set)
	 * @throws NumberFormatException
	 *             if a number cannot be parsed
	 * @throws UnsupportedTimeRangeException
	 *             if the {@code time_range} column cannot be parsed
	 * 
	 */
	public WaySegments parse(File file) throws IOException {
		CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(DELIMITER)
				.withHeader();

		CSVParser csvParser = CSVParser.parse(file, Charset.forName("UTF-8"),
				csvFormat);

		return this.parseCSVRecords(csvParser);
	}

	/**
	 * Parses the csv records provided as {@code CSVParser} object.
	 * 
	 * @param csvParser
	 *            the csv records to parse
	 * @return the parsed way segments
	 * 
	 * @throws WaySegmentParserException
	 *             if an error occurred while parsing the record (e.g. a
	 *             required column is not set)
	 * @throws NumberFormatException
	 *             if a number cannot be parsed
	 * @throws UnsupportedTimeRangeException
	 *             if the {@code time_range} column cannot be parsed
	 */
	public WaySegments parseCSVRecords(CSVParser csvParser) {

		ArrayListValuedHashMap<WaySegmentId, WaySegment> res = new ArrayListValuedHashMap<>();

		PeekingIterator<CSVRecord> iter = Iterators
				.peekingIterator(csvParser.iterator());

		while (iter.hasNext()) {
			ArrayList<Double> dists = new ArrayList<>();
			ArrayList<Double> tempDiffs = new ArrayList<>();

			CSVRecord current = iter.next();

			WaySegmentParser.ParsedRecord currentParsed = parseCsvRecord(
					current);

			dists.add(currentParsed.getDist());
			tempDiffs.add(currentParsed.getTempDiff());

			while (iter.hasNext()
					&& currentParsed.hasSameId(parseCsvRecord(iter.peek()))) {
				WaySegmentParser.ParsedRecord parsed = parseCsvRecord(
						iter.next());
				dists.add(parsed.getDist());
				tempDiffs.add(parsed.getTempDiff());
			}

			double[] distsArray = dists.stream()
					.mapToDouble(Double::doubleValue).toArray();
			double[] tempDiffsArray = tempDiffs.stream()
					.mapToDouble(Double::doubleValue).toArray();

			res.put(new WaySegmentId(currentParsed.getWayId(),
					currentParsed.getNodeIds()),
					new WaySegment(currentParsed.getWayId(),
							currentParsed.getNodeIds(), currentParsed.getTime(),
							distsArray, tempDiffsArray));

		}

		return new WaySegments(res);

	}

	/**
	 * Parses the provided csv record and returns a {@code ParsedRecord} object.
	 * 
	 * @param record
	 *            the csv record to parse
	 * @return the parsed csv record
	 * 
	 * @throws WaySegmentParserException
	 *             if an error occurred while parsing the record (e.g. a
	 *             required column is not set)
	 * @throws NumberFormatException
	 *             if a number cannot be parsed
	 * @throws UnsupportedTimeRangeException
	 *             if the {@code time_range} column cannot be parsed
	 */
	private static ParsedRecord parseCsvRecord(CSVRecord record) {
		if (record.isSet(WAY_ID_COL) && record.isSet(FROM_COL)
				&& record.isSet(TO_COL) && record.isSet(DIST_COL)
				&& record.isSet(TEMPERATURE_DIFFERENCE_COL)
				&& record.isSet(TIME_COL)) {

			long wayId = Long.parseUnsignedLong(record.get(WAY_ID_COL));
			long from = Long.parseUnsignedLong(record.get(FROM_COL));
			long to = Long.parseUnsignedLong(record.get(TO_COL));
			Pair<Long, Long> id = Pair.of(from, to);
			double dist = Double.parseDouble(record.get(DIST_COL));

			double tempDiff = Double
					.parseDouble(record.get(TEMPERATURE_DIFFERENCE_COL));

			TimeRange<LocalTime> time = parseTimeString(record.get(TIME_COL));

			return new WaySegmentParser.ParsedRecord(wayId, id, dist, tempDiff,
					time);
		} else {
			throw new WaySegmentParserException(
					"could not parse csv record: " + record.toString());
		}
	}

	/**
	 * A helper class to represent a parsed way segment.
	 */
	private static class ParsedRecord {
		private final long wayId;
		private final Pair<Long, Long> nodeIds;
		private final double dist;
		private final double tempDiff;
		private final TimeRange<LocalTime> time;

		/**
		 * Creates a new {@code ParsedRecord} object.
		 * 
		 * @param wayId
		 *            the OSM way id of the record
		 * @param nodeIds
		 *            the OSM node ids of the record
		 * @param dist
		 *            the length of the represented intersection
		 * @param tempDiff
		 *            the value of the intersected raster cell
		 * @param time
		 *            time range the record is valid for
		 */
		protected ParsedRecord(long wayId, Pair<Long, Long> nodeIds,
				double dist, double tempDiff, TimeRange<LocalTime> time) {
			this.wayId = wayId;
			this.nodeIds = nodeIds;
			this.dist = dist;
			this.tempDiff = tempDiff;
			this.time = time;
		}

		/**
		 * Checks if the two {@code ParsedRecord}s have the same {@code wayId}
		 * and {@code nodeIds}.
		 * 
		 * @param other
		 *            a {@code ParsedRecord} to compare to
		 * @return true, iff both {@code wayId}s and both {@code nodeIds}s are
		 *         equals
		 */
		public boolean hasSameId(ParsedRecord other) {
			return this.wayId == other.wayId
					&& this.nodeIds.equals(other.nodeIds);
		}

		/**
		 * 
		 * @return the OSM way id
		 */
		public long getWayId() {
			return wayId;
		}

		/**
		 * 
		 * @return the OSM node ids
		 */
		public Pair<Long, Long> getNodeIds() {
			return nodeIds;
		}

		/**
		 * 
		 * @return the distance
		 */
		public double getDist() {
			return dist;
		}

		/**
		 * 
		 * @return the temperature difference
		 */
		public double getTempDiff() {
			return this.tempDiff;
		}

		/**
		 * 
		 * @return the time range the value is valid for
		 */
		public TimeRange<LocalTime> getTime() {
			return time;
		}

	}

	/**
	 * Parses the provided string {@code timeStr} to a {@link TimeRange}<{@link LocalTime}> object.
	 * 
	 * @param timeStr
	 *            the string to parse
	 * @return the string parsed to {@code TimeRange<LocalTime>}
	 * 
	 * @throws UnsupportedTimeRangeException
	 *             if the provided string does not represent a valid time range
	 */
	private static TimeRange<LocalTime> parseTimeString(String timeStr) {
		timeStr = timeStr.trim();
		if (timeStr.equalsIgnoreCase(TIME_MORNING)) {
			return new TimeRange<>(LocalTime.MIN, LocalTime.NOON);
		} else if (timeStr.equalsIgnoreCase(TIME_EVENING)) {
			return new TimeRange<>(LocalTime.NOON, LocalTime.MAX);
		} else {
			throw new UnsupportedTimeRangeException("only '" + TIME_MORNING
					+ "' and '" + TIME_EVENING + "' are supported");
		}

	}
}
