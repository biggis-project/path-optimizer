package joachimrussig.heatstressrouting.osmdata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;
import joachimrussig.heatstressrouting.util.TimeRange;
import net.osmand.util.OpeningHoursParser;

/**
 * The {@code OSMOpeningHours} class extends the
 * {@link OpeningHoursParser.OpeningHours} which contains the entire
 * OpeningHours schema and offers some additional functions e.g. to receives the
 * opening hours.
 * 
 * @author Joachim Rußig
 */
public class OSMOpeningHours extends OpeningHoursParser.OpeningHours {

	private static final long serialVersionUID = 1L;

	public static final String OPENING_HOURS_KEY = "opening_hours";

	/**
	 * Creates a new {@class OSMOpeningHours} object.
	 * 
	 * @param rules
	 *            the {@link OpeningHoursParser.OpeningHoursRule}s
	 */
	public OSMOpeningHours(
			ArrayList<OpeningHoursParser.OpeningHoursRule> rules) {
		super(rules);
	}

	/**
	 * Returns the openingHours for the specified day.
	 * 
	 * @param day
	 *            the day for which the opening hours are requested
	 * @return a list of opening hour ranges for the given day
	 */
	public List<TimeRange<ZonedDateTime>> getOpeningHours(Date day) {
		Calendar cal = dateToCalendarAtStartOfDay(day);
		return getOpeningHours(cal);
	}

	/**
	 * Returns the openingHours for the specified date.
	 * 
	 * @param cal
	 *            the date for which the opening hours are requested
	 * @return a list of opening hour ranges for the given day
	 */
	public List<TimeRange<ZonedDateTime>> getOpeningHours(Calendar cal) {

		List<TimeRange<ZonedDateTime>> res = new ArrayList<>();
		for (OpeningHoursParser.OpeningHoursRule rule : getRules()) {
			if (rule.containsDay(cal) && rule.containsMonth(cal)
					&& rule instanceof OpeningHoursParser.BasicOpeningHourRule) {

				OpeningHoursParser.BasicOpeningHourRule baseRule = (OpeningHoursParser.BasicOpeningHourRule) rule;
				TIntArrayList startTimes = baseRule.getStartTimes();
				TIntArrayList endTimes = baseRule.getEndTimes();

				if (startTimes.size() != endTimes.size()) {
					throw new IllegalStateException(
							"different number of start and end times");
				}

				for (int i = 0; i < startTimes.size(); i++) {
					res.add(new TimeRange<>(
							toZonedDateTime(cal, startTimes.get(i)),
							toZonedDateTime(cal, endTimes.get(i))));
				}
			}
		}
		return res;
	}

	/**
	 * Converts {@code calendar} to a {@link ZonedDateTime} and adds
	 * {@code minutes} minutes to it.
	 * 
	 * @param calendar
	 *            the {@link Calendar} object to convert
	 * @param minutes
	 *            number of minutes to add to {@link calendar}
	 * @return {@code calendar} plus {@code minutes} as {@code ZonedDateTime}
	 * 
	 * @throws IllegalArgumentException
	 *             if {@code calendar} is not an instance of
	 *             {@link GregorianCalendar}
	 */
	private static ZonedDateTime toZonedDateTime(Calendar calendar,
			int minutes) {
		if (calendar instanceof GregorianCalendar) {
			ZonedDateTime dateTime = ((GregorianCalendar) calendar)
					.toZonedDateTime();
			dateTime = dateTime.plusMinutes(minutes);
			return dateTime;
		} else {
			throw new IllegalArgumentException(
					"calendar must be a instance of GregorianCalendar");
		}
	}

	/**
	 * Converts {@code date} to {@link Calendar} object at the start of the day.
	 * 
	 * @param date
	 *            {@code Date} object to concert
	 * @return {@code date} converted to {@code Calendar} at the start of the
	 *         day
	 */
	public static Calendar dateToCalendarAtStartOfDay(Date date) {
		Calendar calOld = dateToCalendar(date);
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0L);
		cal.set(calOld.get(Calendar.YEAR), calOld.get(Calendar.MONTH),
				calOld.get(Calendar.DAY_OF_MONTH), 0, 0);
		return cal;
	}

	/**
	 * Converts {@code date} to a {@link Calendar} object.
	 * 
	 * @param date
	 *            {@code Date} object to concert
	 * @return {@code date} converted to {@code Calendar}
	 */
	public static Calendar dateToCalendar(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;
	}

	/**
	 * Converts {@code date} to a {@link Calendar} object at the specified time
	 * zone.
	 * 
	 * @param date
	 *            the {@link Date} object to convert
	 * @param zoneId
	 *            the {@link ZoneId} of the time zone to use
	 * @return {@code date} converted to {@code Calendar}
	 */
	public static Calendar localDateToCalendar(LocalDate date, ZoneId zoneId) {
		Date d = Date.from(date.atStartOfDay(zoneId).toInstant());
		return dateToCalendar(d);
	}

	/**
	 * Checks if the feature is opened at {@code date}.
	 * 
	 * @param date
	 *            the {@code date} to check
	 * @return true, if feature is open
	 */
	public boolean isOpenedForTime(Date date) {
		return super.isOpenedForTime(dateToCalendar(date));
	}

	/**
	 * Checks if the feature is opened at {@code time}.
	 * 
	 * @param time
	 *            the {@code time} to check
	 * @return true, if feature is open
	 */
	public boolean isOpenedForTime(ZonedDateTime time) {
		Calendar cal = GregorianCalendar.from(time);
		return super.isOpenedForTime(cal);
	}

	/**
	 * Checks if the feature is opened at {@code time}.
	 * 
	 * @param time
	 *            the {@code time} to check
	 * @param zoneId
	 *            the {@link ZoneId} to use
	 * @return true, if feature is open
	 */
	public boolean isOpenedForTime(LocalDateTime time, ZoneId zoneId) {
		ZonedDateTime zonedDateTime = time.atZone(zoneId);
		return isOpenedForTime(zonedDateTime);
	}

}
