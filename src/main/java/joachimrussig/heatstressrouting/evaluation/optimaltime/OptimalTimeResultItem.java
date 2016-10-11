package joachimrussig.heatstressrouting.evaluation.optimaltime;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.jooq.lambda.Seq;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;

import com.graphhopper.util.PointList;

import joachimrussig.heatstressrouting.evaluation.Evaluator;
import joachimrussig.heatstressrouting.util.Utils;

/**
 * A single optimal time result for a specific place.
 * 
 * @author Joachim Rußig
 *
 */
public final class OptimalTimeResultItem {

	private final Node place;
	private final LocalDateTime time;
	private final OptimalTimeResultType type;
	private final int rank;
	private final double value;
	private final double distance;
	private final long duration;
	private final double temperature;
	private final double heatIndex;
	private final double costRouteTemperature;
	private final double costRouteHeatIndex;
	private final PointList path;

	/**
	 * 
	 * @param place
	 *            the place of the result item
	 * @param time
	 *            the optimal time
	 * @param type
	 *            the method used
	 * @param rank
	 *            the rank of the item, where 1 is the best
	 * @param value
	 *            the value of the item
	 * @param distance
	 *            distance from the start point to the place
	 * @param duration
	 *            time needed to walk from the start to the place
	 * @param temperature
	 *            the air temperature at time {@code time}
	 * @param heatIndex
	 *            the heat index value at time {@code time}
	 * @param costRouteTemperature
	 *            weight of the route from start to place according to
	 *            {@link joachimrussig.heatstressrouting.routing.weighting.HeatStressWeightingTemperature}
	 * @param costRouteHeatIndex
	 *            weight of the route from start to place according to
	 *            {@link joachimrussig.heatstressrouting.routing.weighting.HeatStressWeightingHeatIndex}
	 * @param path
	 *            the node coordinates of the slected path from start to place
	 */
	public OptimalTimeResultItem(Node place, LocalDateTime time,
			OptimalTimeResultType type, int rank, double value, double distance,
			long duration, double temperature, double heatIndex,
			double costRouteTemperature, double costRouteHeatIndex,
			PointList path) {
		this.place = place;
		this.time = time;
		this.type = type;
		this.rank = rank;
		this.value = value;
		this.distance = distance;
		this.duration = duration;
		this.temperature = temperature;
		this.heatIndex = heatIndex;
		this.costRouteTemperature = costRouteTemperature;
		this.costRouteHeatIndex = costRouteHeatIndex;
		this.path = path;
	}

	/**
	 * 
	 * 
	 * @param rank
	 * @return a copy of {@code this}, with rank set to {@code rank}
	 */
	public OptimalTimeResultItem setRank(int rank) {
		return new OptimalTimeResultItem(this.place, this.time, this.type, rank,
				this.value, this.distance, this.duration, this.temperature,
				this.heatIndex, this.costRouteTemperature,
				this.costRouteHeatIndex, this.path);
	}

	public double getDistance() {
		return distance;
	}

	public long getDuration() {
		return duration;
	}

	public double getHeatIndex() {
		return heatIndex;
	}

	public Node getPlace() {
		return place;
	}

	public int getRank() {
		return rank;
	}

	public double getTemperature() {
		return temperature;
	}

	public LocalDateTime getTime() {
		return time;
	}

	public OptimalTimeResultType getType() {
		return type;
	}

	public double getValue() {
		return value;
	}

	public double getCostRouteTemperature() {
		return costRouteTemperature;
	}

	public double getCostRouteHeatIndex() {
		return costRouteHeatIndex;
	}

	public PointList getPath() {
		return path;
	}

	@Override
	public String toString() {
		return "OptimalTimeResultItem(place = " + place + ", time = " + time
				+ ", type = " + type + ", rank = " + rank + ", value = " + value
				+ ", distance = " + distance + ", duration = "
				+ Utils.formatDurationMills(duration) + ", temperature = "
				+ temperature + ", heatIndex = " + heatIndex
				+ ", costRouteTemperature = " + costRouteTemperature
				+ ", costRouteHeatIndex = " + costRouteHeatIndex
				+ ", path.length = " + path.size() + ")";
	}

	public String toCsvRecord() {

		// collapse points into a single string
		String points = Seq.seq(this.path)
				.map(p -> p.getLat() + Evaluator.DELIMITER_POINT + p.getLon())
				.collect(Collectors.joining(Evaluator.DELIMITER_POINT_LIST));

		Object[] vals = new Object[] { place.getId(), place.getLatitude(),
				place.getLongitude(), time, type, rank, value, distance,
				duration, temperature, heatIndex, costRouteTemperature,
				costRouteHeatIndex, points };

		return Arrays.stream(vals).map(String::valueOf)
				.collect(Collectors.joining(Evaluator.DELIMITER));

	}

}