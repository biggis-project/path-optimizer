package joachimrussig.heatstressrouting.util;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import com.github.davidmoten.guavamini.Objects;

/**
 * The {@code Result} class represents either a value of type {@code T} or an
 * error of type {@code E}.
 * <p>
 * 
 * Inspired by Rust's Result type, see:
 * {@link https://doc.rust-lang.org/stable/std/result/enum.Result.html}.
 * 
 * @author Joachim Rußig
 */
public class Result<T, E> {

	private boolean isOkay;
	private final T okay;
	private final E error;

	/**
	 * Creates a new {@code Result}.
	 * 
	 * @param okay
	 *            value in case of success
	 * @param error
	 *            in case of an error
	 * @param isOkay
	 *            indicates if the created {@code Result} represents and okay or
	 *            an error case
	 * 
	 */
	private Result(T okay, E error, boolean isOkay) {
		this.isOkay = isOkay;
		this.okay = okay;
		this.error = error;
	}

	/**
	 * Creates a new {@code Result}, with an {@code okay} value.
	 * 
	 * @param okay
	 *            the value
	 * @return a new {@code Result} representing an okay case
	 */
	public static <T, E> Result<T, E> okayOf(T okay) {
		return new Result<>(okay, null, true);
	}

	/**
	 * Creates a new {@code Result}, with an {@code error} value.
	 * 
	 * @param error
	 *            the error
	 * @return a new {@code Result} representing an error case
	 */
	public static <T, E> Result<T, E> errorOf(E error) {
		return new Result<>(null, error, false);
	}

	
	/**
	 * Creates a new {@code Result}, with an {@code okay} value of the
	 * {@link Optional} if present or an {@code error} value with the provided error.
	 * 
	 * @param optional
	 *            the value to create a new result of
	 * @return a new {@code Result}, with an {@code okay} value of the
	 *         {@link Optional} if present or an {@code error} value of null
	 *         otherwise
	 */
	public static <T, E> Result<T, E> of(Optional<T> optional, E error) {
		if (optional.isPresent())
			return okayOf(optional.get());
		else
			return errorOf(error);
	}
	
	/**
	 * Creates a new {@code Result}, with an {@code okay} value of the
	 * {@link Optional} if present or an {@code error} value of {@code null} otherwise.
	 * 
	 * @param optional
	 *            the value to create a new result of
	 * @return a new {@code Result}, with an {@code okay} value of the
	 *         {@link Optional} if present or an {@code error} value of null
	 *         otherwise
	 */
	public static <T, E> Result<T, E> of(Optional<T> optional) {
		return of(optional, null);
	}

	/**
	 * Checks if it representing an okay case.
	 * 
	 * @return true, if {@code okay} is not {@code null}
	 */
	public boolean isOkay() {
		return isOkay;
	}

	/**
	 * Checks if it represent an error case.
	 * 
	 * @return true, if {@code isOkay} returns {@code false}
	 */
	public boolean isError() {
		return !isOkay();
	}

	/**
	 * Returns the {@code okay} value if present.
	 * 
	 * @return the {@code okay} value if present and not {@code null},
	 *         {@code Optional.empty()} otherwise
	 */
	public Optional<T> get() {
		return Optional.ofNullable(okay);
	}

	/**
	 * If the value is {@code okay} apply the provided mapping function to the
	 * contained value and otherwise returns the {@code error} value.
	 * 
	 * @param mapper
	 *            the mapping function to apply on the {@code okay} value
	 * @return the value returned by the {@code mapper} or the contained value
	 *         as a {@code Result}
	 */
	public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
		if (isOkay())
			return Result.okayOf(mapper.apply(okay));
		else
			return Result.errorOf(error);
	}

	/**
	 * If the contained value is an error, the provided mapping function is
	 * applied to the error value and otherwise the okay value is returned.
	 * 
	 * @param mapper
	 *            the mapping function to apply on the value
	 * @return the value returned by the {@code mapper} or the contained okay
	 *         value as a {@code Result}
	 */
	public <U> Result<T, U> mapError(Function<? super E, ? extends U> mapper) {
		if (!isOkay())
			return Result.errorOf(mapper.apply(error));
		else
			return Result.okayOf(okay);
	}

	/**
	 * If the contained value is an okay value, the operation {@code op} is
	 * applied on the contained value and the result is returned, otherwise the
	 * contained error is returned.
	 * 
	 * @param op
	 *            the operation to be applied on the contained okay value
	 * @return the result of {@code op} if the contained value is okay or the
	 *         contained error value
	 */
	public <U> Result<U, E> andThen(Function<? super T, Result<U, E>> op) {
		if (isOkay())
			return op.apply(okay);
		else
			return Result.errorOf(error);
	}

	/**
	 * 
	 * @param res
	 * @return {@code res} if the result is an error, or the contained value of
	 *         {@code this}
	 */
	public <F> Result<T, F> or(Result<T, F> res) {
		if (isOkay())
			return Result.okayOf(okay);
		else
			return res;
	}

	/**
	 * 
	 * @param op
	 * @return the {@code okay} value of {@code this}, if {@this} is okay, or
	 *         the result of {@code op} applied on the contained error value.
	 */
	public <F> Result<T, F> orElse(Function<? super E, Result<T, F>> op) {
		if (isOkay())
			return Result.okayOf(okay);
		else
			return op.apply(error);
	}

	/**
	 * Returns the {@code okay} value if present and throws an
	 * {@code NoSuchElementException} otherwise.
	 * 
	 * @throws NoSuchElementException
	 *             if {@code okay} is not present
	 * 
	 * @return the {@code okay} value if present
	 */
	public T unwrap() {
		if (isOkay())
			return okay;
		else
			throw new NoSuchElementException();
	}

	/**
	 * Returns the {@code okay} value if present or the {@code defaultValue}
	 * provided.
	 * 
	 * 
	 * @return the {@code okay} value if present or the default value
	 *         {@code defaultValue}
	 */
	public T unwrapOr(T defaultValue) {
		if (isOkay())
			return okay;
		else
			return defaultValue;
	}

	/**
	 * Returns the {@code okay} value if present or calls {@code op} on the
	 * error value.
	 * 
	 * @param op
	 *            function called with the {@code error} value, if the
	 *            {@code okay} value is not present; the error passed to
	 *            {@code op} might be {@code null}
	 * @return the {@code okay} value if present or the result of {@code op}
	 */
	public T unwrapOrElse(Function<E, T> op) {
		if (isOkay())
			return okay;
		else
			return op.apply(error);
	}

	/**
	 * Returns the {@code error} value if present.
	 * 
	 * @return the {@code error} value if present and not {@code null},
	 *         {@code Optional.empty()} otherwise
	 */
	public Optional<E> getError() {
		return Optional.ofNullable(error);
	}

	/**
	 * Returns the {@code error} value if present and throws an
	 * {@code NoSuchElementException} otherwise.
	 * 
	 * @throws NoSuchElementException
	 *             if {@code error} is not present
	 * 
	 * @return the {@code error} value if present
	 */
	public E unwrapError() {
		if (isError())
			return error;
		else
			throw new NoSuchElementException();
	}

	@Override
	public String toString() {
		if (isOkay()) {
			return "Okay[" + String.valueOf(okay) + "]";
		} else {
			return "Error[" + String.valueOf(error) + "]";
		}
	}

	@Override
	public int hashCode() {
		if (isOkay())
			return Objects.hashCode(okay);
		else
			return Objects.hashCode(error);
	}

}
