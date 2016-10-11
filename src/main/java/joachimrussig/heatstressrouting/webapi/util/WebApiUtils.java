package joachimrussig.heatstressrouting.webapi.util;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.graphhopper.util.shapes.GHPoint;

import joachimrussig.heatstressrouting.routing.weighting.WeightingType;
import joachimrussig.heatstressrouting.util.Result;
import joachimrussig.heatstressrouting.util.Utils;

public class WebApiUtils {

	private WebApiUtils() {
	}

	/**
	 * Parses a geo coordinate string of the form 'lat,lon' (e.g.
	 * '49.0118083,8.4251357') where the decimal latitude and longitude values
	 * are separated by a comma (','). The latitude value must be in range [-90,
	 * 90] and the longitude value in range [-180, 180].
	 * 
	 * @param pointStr
	 *            the string to parse, e.g. '49.0118083,8.4251357'
	 * @return a {@link GHPoint} instance
	 * 
	 */
	public static Result<GHPoint, ParserException> parseGHPoint(
			String pointStr) {
		String[] components = pointStr.trim().split(",");

		if (components.length != 2)
			return Result
					.errorOf(new ParserException("wrong number of arguments"));

		double lat;
		double lon;
		try {
			lat = Double.parseDouble(components[0]);
			lon = Double.parseDouble(components[1]);
		} catch (NumberFormatException e) {
			return Result.errorOf(
					new ParserException("failed to parse coordinate", e));
		}

		if (lat < -90 || lat > 90)
			return Result.errorOf(new ParserException(
					"latitude value is not in range [-90, 90]"));

		if (lon < -180 || lon > 180)
			return Result.errorOf(new ParserException(
					"longitude value is not in range [-180, 180]"));

		return Result.okayOf(new GHPoint(lat, lon));
	}

	public static Result<LocalDateTime, Exception> parseLocalDateTime(
			String time) {
		Result<LocalDateTime, Exception> ret;

		if (time.equalsIgnoreCase("now")) {
			ret = Result.okayOf(LocalDateTime.now());
		} else if (Utils.isUnsignedLong(time)) {
			try {
				long timeLong = Long.parseUnsignedLong(time);
				LocalDateTime localDateTime = LocalDateTime.ofInstant(
						Instant.ofEpochMilli(timeLong), ZoneId.systemDefault());
				ret = Result.okayOf(localDateTime);
			} catch (NumberFormatException | DateTimeException e) {
				ret = Result.errorOf(e);
			}
		} else {
			try {
				ret = Result.okayOf(LocalDateTime.parse(time));
			} catch (DateTimeException e) {
				ret = Result.errorOf(e);
			}
		}

		return ret;
	}

	public static Result<Set<WeightingType>, List<String>> parseWeightingTypes(
			String weighting, WeightingType defaultType) {
		Set<WeightingType> weightingTypes = new LinkedHashSet<>();
		if (defaultType != null)
			weightingTypes.add(defaultType);

		List<String> errors = new ArrayList<>();

		for (String w : weighting.split(",")) {
			Optional<WeightingType> type = WeightingType.from(w);
			if (type.isPresent()) {
				weightingTypes.add(type.get());
			} else {
				errors.add(w);
			}
		}
		if (errors.isEmpty()) {
			return Result.okayOf(weightingTypes);
		} else {
			return Result.errorOf(errors);
		}
	}

	public static List<String> parseTagsList(String tags) {
		return Arrays.asList(tags.split(","));
	}
	
	public static Result<LocalDateTime, Exception> parseLocalDateTimeNullable(
			String time) {
		if (time.equalsIgnoreCase("null"))
			return Result.okayOf(null);

		return parseLocalDateTime(time);
	}

}
