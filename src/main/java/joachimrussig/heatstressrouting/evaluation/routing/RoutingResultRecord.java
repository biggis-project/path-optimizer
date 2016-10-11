package joachimrussig.heatstressrouting.evaluation.routing;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.jooq.lambda.Seq;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;

import com.graphhopper.util.PointList;

import joachimrussig.heatstressrouting.evaluation.Evaluator;

/**
 * 
 * Represents a an evaluation result.
 * 
 * @author Joachim Rußig
 *
 */
public class RoutingResultRecord {

	/**
	 * ID of the result record
	 */
	public int id;
	/**
	 * ID of the start destination pair
	 */
	public int iteration;
	/**
	 * the point in time evaluated
	 */
	public LocalDateTime timePoint;
	/**
	 * the weighting method used
	 */
	public String method;
	/**
	 * the start node
	 */
	public Node from;
	/**
	 * the destination node
	 */
	public Node to;
	/**
	 * the distance of the found route
	 */
	public double dist;
	/**
	 * cost of the route according to the {@link HeatStressWeightingTemperature}
	 */
	public double costTemperature;
	/**
	 * cost of the route according to the {@link HeatStressWeightingHeatIndex}
	 */
	public double costHeatIndex;
	/**
	 * the duration needed to walk the found path
	 */
	public double duration;
	/**
	 * the path as a list of points
	 */
	public PointList path;

	public RoutingResultRecord(int id, int iteration, LocalDateTime timePoint,
			String method, Node from, Node to, double dist,
			double costTemperature, double costHeatIndex, double duration,
			PointList path) {
		this.id = id;
		this.iteration = iteration;
		this.timePoint = timePoint;
		this.method = method;
		this.from = from;
		this.to = to;
		this.dist = dist;
		this.costTemperature = costTemperature;
		this.costHeatIndex = costHeatIndex;
		this.duration = duration;
		this.path = path;
	}

	public String toCsvRecord() {

		String points = Seq.seq(this.path)
				.map(p -> p.getLat() + Evaluator.DELIMITER_POINT + p.getLon())
				.collect(Collectors.joining(Evaluator.DELIMITER_POINT_LIST));

		Object[] vals = new Object[] { id, iteration, timePoint, method,
				from.getId(), to.getId(), from.getLatitude(),
				from.getLongitude(), to.getLatitude(), to.getLongitude(), dist,
				costTemperature, costHeatIndex, duration, points };

		return Arrays.stream(vals).map(String::valueOf)
				.collect(Collectors.joining(Evaluator.DELIMITER));
	}

	@Override
	public String toString() {
		StringBuilder row = new StringBuilder();
		row.append(id);
		row.append(Evaluator.DELIMITER);
		row.append(this.iteration);
		row.append(Evaluator.DELIMITER);
		row.append(this.timePoint);
		row.append(Evaluator.DELIMITER);
		row.append(this.method);
		row.append(Evaluator.DELIMITER);
		row.append(this.from.getId());
		row.append(Evaluator.DELIMITER);
		row.append(this.to.getId());
		row.append(Evaluator.DELIMITER);
		row.append(this.from.getLatitude());
		row.append(Evaluator.DELIMITER);
		row.append(this.from.getLongitude());
		row.append(Evaluator.DELIMITER);
		row.append(this.to.getLatitude());
		row.append(Evaluator.DELIMITER);
		row.append(this.to.getLongitude());
		row.append(Evaluator.DELIMITER);
		row.append(this.dist);
		row.append(Evaluator.DELIMITER);
		row.append(this.costTemperature);
		row.append(Evaluator.DELIMITER);
		row.append(this.costHeatIndex);
		row.append(Evaluator.DELIMITER);
		row.append(this.duration);

		String points = Seq.seq(this.path)
				.map(p -> p.getLat() + Evaluator.DELIMITER_POINT + p.getLon())
				.collect(Collectors.joining(Evaluator.DELIMITER_POINT_LIST));

		row.append(Evaluator.DELIMITER);
		row.append(points);

		return row.toString();
	}
}