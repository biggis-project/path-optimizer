package joachimrussig.heatstressrouting.evaluation.optimaltime;

import java.util.ArrayList;
import java.util.List;

/**
 * The results found for an {@link OptimalTimeEvaluationItem}.
 * 
 * @author Joachim Rußig
 *
 */
public final class OptimalTimeResultRecord {

	private final OptimalTimeEvaluationItem item;
	private List<OptimalTimeResultItem> results;

	public OptimalTimeResultRecord(OptimalTimeEvaluationItem item) {
		this(item, new ArrayList<>());
	}

	public OptimalTimeResultRecord(OptimalTimeEvaluationItem item,
			List<OptimalTimeResultItem> results) {
		this.item = item;
		this.results = results;
	}

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("OptimalTimeResultRecord(item: " + item.toString());
		for (OptimalTimeResultItem res : this.results) {
			out.append(",\n\t");
			out.append(res.getType() + ": " + res.toString());
		}
		if (results.isEmpty()) {
			out.append(")");
		} else {
			out.append("\n)");
		}

		return out.toString();
	}

	public List<OptimalTimeResultItem> getResults() {
		return results;
	}

	public void addResult(OptimalTimeResultItem result) {
		this.results.add(result);
	}

	public void addResults(List<OptimalTimeResultItem> results) {
		this.results.addAll(results);
	}

	public void setResults(List<OptimalTimeResultItem> results) {
		this.results = results;
	}

	public OptimalTimeEvaluationItem getItem() {
		return item;
	}

}