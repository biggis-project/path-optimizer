package joachimrussig.heatstressrouting.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Optional;
import java.util.OptionalLong;

import org.apache.commons.lang3.tuple.Pair;

/**
 * The {@code TimeRange} class represent a time range between two
 * {@link Temporal}, e.g. {@link LocalDateTime} or {@link LocalTime}.
 * 
 * @author Joachim Rußig
 */
public class TimeRange<T extends Temporal> {

	private final T from;
	private final T to;

	/**
	 * Create a new time range.
	 * 
	 * @param from
	 *            lower bound
	 * @param to
	 *            upper bound
	 */
	public TimeRange(T from, T to) {
		this.from = from;
		this.to = to;
	}

	/**
	 * Create a new time range.
	 * 
	 * @param timeRange
	 *            pair of ({@code from}, {@code to})
	 */
	public TimeRange(Pair<T, T> timeRange) {
		this.from = timeRange.getLeft();
		this.to = timeRange.getRight();
	}

	/**
	 * Checks weather {@code time} is within range [{@code from}, {@code to}],
	 * lower and upper bound inclusive.
	 * 
	 * @param time
	 * @return true, if {@code time} is within range [{@code from}, {@code to}]
	 */
	public <U extends Comparable<? super T>> boolean containsInclusive(U time) {
		return time != null && time.compareTo(from) >= 0 && time.compareTo(to) <= 0;
	}

	/**
	 * Checks weather {@code time} is within range [{@code from}, {@code to}),
	 * only lower inclusive.
	 * 
	 * @param time
	 * @return true, if {@code time} is within range [{@code from}, {@code to})
	 */
	public <U extends Comparable<? super T>> boolean contains(U time) {
		return time != null && time.compareTo(from) >= 0 && time.compareTo(to) < 0;
	}

	/**
	 * {@code Duration} between {@code from} and {@code to}, if {@code T}
	 * supports {@code ChronoUnit.SECONDS} and {@code Optional.empty()}
	 * otherwise.
	 * 
	 * @return duration between {@code from} and {@code to} , if {@code T}
	 *         supports {@code ChronoUnit.SECONDS} and {@code Optional.empty()}
	 *         otherwise
	 */
	public Optional<Duration> getDuration() {
		if (from.isSupported(ChronoUnit.SECONDS))
			return Optional.of(Duration.between(from, to));
		else
			return Optional.empty();
	}

	/**
	 * Number of days between {@code from} and {@code to}, if {@code T} supports
	 * {@code ChronoUnit.DAYS} and {@code OptionalLong.empty()} otherwise.
	 * 
	 * @return duration in days
	 */
	public OptionalLong getDays() {
		if (from.isSupported(ChronoUnit.DAYS))
			return OptionalLong.of(ChronoUnit.DAYS.between(from, to));
		else
			return OptionalLong.empty();
	}

	/**
	 * Duration between {@code from} and {@code to} in seconds.
	 * 
	 * @return duration in seconds
	 */
	public long durationInSeconds() {
		return ChronoUnit.SECONDS.between(from, to);
	}

	/**
	 * Duration between {@code from} and {@code to} in milliseconds.
	 * 
	 * @return duration in milliseconds
	 */
	public long durationInMilliseconds() {
		return ChronoUnit.MILLIS.between(from, to);
	}

	/**
	 * 
	 * @return the lower bound of the time range
	 */
	public T getFrom() {
		return from;
	}

	/**
	 * 
	 * @return the upper bound of the time range
	 */
	public T getTo() {
		return to;
	}

	/**
	 * 
	 * @return pair of ({@code from}, {@code to})
	 */
	public Pair<T, T> toPair() {
		return Pair.of(from, to);
	}

	@Override
	public String toString() {
		return "TimeRange: " + from + " - " + to;
	}
}
