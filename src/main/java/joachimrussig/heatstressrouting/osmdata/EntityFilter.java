package joachimrussig.heatstressrouting.osmdata;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A utility class, that contains same predefined entity filter.
 * 
 * @author Joachim Rußig
 */
public class EntityFilter {

	public static final HashMap<String, Tag> TAG_MAP;

	static {
		HashMap<String, Tag> map = new HashMap<>();
		map.put("supermarket", new Tag("shop", "supermarket"));
		map.put("bakery", new Tag("shop", "bakery"));
		map.put("kiosk", new Tag("shop", "kiosk"));
		map.put("chemist", new Tag("shop", "chemist"));

		map.put("cafe", new Tag("amenity", "cafe"));
		map.put("drinking_water", new Tag("amenity", "drinking_water"));
		map.put("fast_food", new Tag("amenity", "fast_food"));
		map.put("ice_cream", new Tag("amenity", "ice_cream"));
		map.put("taxi", new Tag("amenity", "taxi"));
		map.put("atm", new Tag("amenity", "atm"));
		map.put("bank", new Tag("amenity", "bank"));
		map.put("clinic", new Tag("amenity", "clinic"));
		map.put("dentist", new Tag("amenity", "dentist"));
		map.put("doctors", new Tag("amenity", "doctors"));
		map.put("hospital", new Tag("amenity", "hospital"));
		map.put("pharmacy", new Tag("amenity", "pharmacy"));
		map.put("police", new Tag("amenity", "police"));
		map.put("post_box", new Tag("amenity", "post_box"));
		map.put("post_office", new Tag("amenity", "post_office"));
		map.put("toilets", new Tag("amenity", "toilets"));

		TAG_MAP = map;
	}

	private EntityFilter() {
	}

	/**
	 * Factory function that wraps the passed predicate or function as a
	 * {@link Predicate}.
	 * 
	 * @param predicate
	 *            the predicate or function to wrap
	 * @return {@code predicate} as an instance of {@code Predicate}
	 */
	public static <T> Predicate<T> asPredicate(Predicate<T> predicate) {
		return v -> predicate.test(v);
	}

	/**
	 * Checks, if {@code entity} has a 'shop' tag.
	 * 
	 * @param entity
	 *            the entity to check
	 * @return true, if {@code entity} has a tag with a 'shop' key
	 */
	public static boolean isShop(Entity entity) {
		return entity.getTags().stream()
				.anyMatch(t -> t.getKey().equalsIgnoreCase("shop"));
	}

	/**
	 * Checks, if {@code entity} has a 'amenity' tag.
	 * 
	 * @param entity
	 *            the entity to check
	 * @return true, if {@code entity} has a tag with a 'amenity' key
	 */
	public static boolean isAmenity(Entity entity) {
		return entity.getTags().stream()
				.anyMatch(t -> t.getKey().equalsIgnoreCase("amenity"));
	}

	/**
	 * Checks, if {@code entity} is a supermarket.
	 * 
	 * @param entity
	 *            the entity to check
	 * @return true, if {@code entity} has a tag 'shop=supermarket'
	 */
	public static boolean isSupermarket(Entity entity) {
		return entity.getTags().stream()
				.anyMatch(t -> t.getKey().equalsIgnoreCase("shop")
						&& t.getValue().equalsIgnoreCase("supermarket"));
	}

	/**
	 * Creates a {@link Predicate} that checks, if an {@link Entity} contains
	 * any of the tags in specified {@code tags}.
	 * 
	 * @param tags
	 *            list of tags to check
	 * @return a {@code Predicate} that returns true, if an {@code Entity}
	 *         contains any of the tags specified in {@code tags}
	 */
	public static Predicate<Entity> containsAnyTag(List<Tag> tags) {
		return e -> e.getTags().stream().anyMatch(
				t -> tags.stream().anyMatch(t2 -> t.compareTo(t2) == 0));
	}

	/**
	 * Creates a {@link Predicate} that checks, if an {@link Entity} contains
	 * any of the tags in specified {@code tags}.
	 * 
	 * @param tags
	 *            list of tags to check
	 * @return a {@code Predicate} that returns true, if an {@code Entity}
	 *         contains any of the tags specified in {@code tags}
	 */
	public static Predicate<Entity> containsAnyTagString(List<String> tags) {
		List<Tag> tagList = tags.stream()
				.filter(TAG_MAP::containsKey)
				.map(TAG_MAP::get).collect(Collectors.toList());
		return e -> e.getTags().stream().anyMatch(
				t -> tagList.stream().anyMatch(t2 -> t.compareTo(t2) == 0));
	}

	public static Optional<Tag> getTag(String tag) {
		return Optional.ofNullable(TAG_MAP.get(tag));
	}
	
	public static boolean containsTagMap(String tag) {
		return TAG_MAP.containsKey(tag);
	}

	/**
	 * Checks, if {@code entity} has opening hours specified.
	 * 
	 * @param entity
	 *            the entity to check
	 * @return true, if {@code entity} has a tag with an 'opening_hours' key
	 */
	public static boolean hasOpeningHours(Entity entity) {
		return entity.getTags().stream().anyMatch(t -> t.getKey()
				.equalsIgnoreCase(OSMOpeningHours.OPENING_HOURS_KEY));
	}

}
