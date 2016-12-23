package joachimrussig.heatstressrouting.osmdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import com.graphhopper.util.shapes.GHPoint;

import net.osmand.util.OpeningHoursParser;

/**
 * A Class representing a OSM file and allows accessing entities, searching for
 * the nearest neighbor and defines other convenience functions.
 * <p>
 * 
 * @author Joachim Rußig
 */
public class OSMData {

	// A multi valued map is used, because the id is only unique within the
	// object type (i.e. node, way, or relation)
	private MultiValuedMap<Long, Entity> entities;
	private Bound boundingBox;

	/**
	 * Creates an new instance of {@code OSMData} of a {@link Set} of
	 * {@link Entity}s.
	 * 
	 * @param entities
	 *            set of {@code Entity}s to store
	 */
	public OSMData(Set<Entity> entities) {
		this.boundingBox = null;
		MultiValuedMap<Long, Entity> map = new ArrayListValuedHashMap<>();
		for (Entity e : entities) {
			if (e instanceof Bound)
				this.boundingBox = (Bound) e;
			map.put(e.getId(), e);
		}
		this.entities = map;
	}

	/**
	 * Creates an new instance of {@code OSMData} of a {@link MultiValuedMap} of
	 * {@link Entity}s.
	 * <p>
	 * <b>Note:</b> for each entry in {@code entities} the key and the id of the
	 * value must be equals
	 * 
	 * @param entities
	 *            set of {@code Entity}s to store
	 * @param boundingBox
	 *            the bounding box of the OSM data; can be {@code null}
	 */
	protected OSMData(MultiValuedMap<Long, Entity> entities,
			Bound boundingBox) {
		this.entities = entities;
		this.boundingBox = boundingBox;
	}

	/**
	 * Checks if any {@link Entity} in {@code entityList} has the OSM id
	 * {@code id}.
	 * 
	 * @param entityList
	 *            list {@code Entity}s to check
	 * @param id
	 *            OSM id to check
	 * @return true, if any {@link Entity} in {@code entityList} has the OSM id
	 *         {@code id}.
	 */
	public static <T extends Entity> boolean contains(List<T> entityList,
			long id) {
		return entityList.stream().anyMatch(n -> n.getId() == id);
	}

	/**
	 * Checks if the object class contains any {@link Entity} with id {@code id}
	 * .
	 * 
	 * @param id
	 *            the id to check
	 * @return {@code true}, if the object contains any {@code Entity} with id
	 *         {@code id}
	 * 
	 */
	public boolean contains(long id) {
		return entities.containsKey(id);
	}

	/**
	 * Checks if the object contains any {@link Entity} with the id {@code id}
	 * of type {@code type}.
	 * 
	 * @param id
	 *            the id to check
	 * @param type
	 *            the {@link EntityType} to check
	 * @return {@code true}, if the object contains any {@code Entity} with id
	 *         {@code id} and type {@code type}
	 */
	public boolean contains(long id, EntityType type) {
		return entities.containsKey(id) && entities.get(id).stream()
				.anyMatch(e -> e.getType().equals(type));
	}

	/**
	 * Checks if the object contains any {@link Node} with id {@code id}.
	 * 
	 * @param id
	 *            the id to check
	 * @return {@code true}, if the object contains any {@code Node} with id
	 *         {@code id}
	 */
	public boolean containsNode(long id) {
		return contains(id, EntityType.Node);
	}

	/**
	 * Checks if the object contains any {@link Way} with id {@code id}.
	 * 
	 * @param id
	 *            the id to check
	 * @return true, if the object contains any {@code Way} with id {@code id}
	 */
	public boolean containsWay(long id) {
		return contains(id, EntityType.Way);
	}

	/**
	 * Checks if the object contains any {@link Entity} of type {@code type}
	 * with id {@code id} that has a tag with key {@code key}.
	 * 
	 * @param id
	 *            the id to check
	 * @param key
	 *            the key to check
	 * @param entityType
	 *            the {@link EntityType}
	 * @return true, if the object contains any {@link Entity} of type
	 *         {@code type} with id {@code id} that has a tag with key
	 *         {@code key}
	 */
	public boolean containsTag(long id, String key, EntityType entityType) {
		return getEntityById(id, entityType)
				.map(e -> e.getTags().stream()
						.anyMatch(t -> t.getKey().equalsIgnoreCase(key)))
				.orElse(false);
	}

	/**
	 * Checks if the object contains any {@link Entity} of type {@code type}
	 * with id {@code id} that has the tag {@code tag}.
	 * 
	 * @param id
	 *            the id to check
	 * @param tag
	 *            the {@link Tag} to check
	 * @param entityType
	 *            the {@link EntityType}
	 * @return true, if the object contains any {@link Entity} of type
	 *         {@code type} with id {@code id} that has a tag with key
	 *         {@code key}
	 */
	public boolean containsTag(long id, Tag tag, EntityType entityType) {
		return getEntityById(id, entityType).map(e -> e.getTags().contains(tag))
				.orElse(false);
	}

	/**
	 * Returns all {@link Entity}s with the specified {@code id}. The OSM ID is
	 * only unique within a group (i.e. Node, Way, Relation).
	 * 
	 * @param id
	 *            the id to check
	 * @return all {@code Entity}s with the specified {@code id}
	 */
	public Collection<Entity> getEntitiesById(long id) {
		return entities.get(id);
	}

	/**
	 * Returns the {@link Entity} with the specified {@code id} and
	 * {@link EntityType} if present, or {@code Optional.empty()} otherwise.
	 * 
	 * @param id
	 *            the id to check
	 * @return the {@link Entity} with the specified {@code id} and
	 *         {@link EntityType} if present, or {@code Optional.empty()}
	 *         otherwise
	 */
	public Optional<Entity> getEntityById(long id, EntityType entityType) {
		return getEntitiesById(id).stream()
				.filter(e -> e.getType() == entityType).findAny();
	}

	/**
	 * Returns the {@link Node} with the id {@code id} if present and
	 * {@code Optional.empty()} otherwise.
	 * 
	 * @param id
	 *            the id to check
	 * @return the {@code Node} with the id {@code id} if present and
	 *         {@code Optional.empty()} otherwise
	 */
	public Optional<Node> getNodeById(long id) {
		return getEntityById(id, EntityType.Node).map(Node.class::cast);
	}

	/**
	 * Returns the {@link Way} with the id {@code id} if present and
	 * {@code Optional.empty()} otherwise.
	 * 
	 * @param id
	 *            the id to check
	 * @return the {@code Way} with the id {@code id} if present and
	 *         {@code Optional.empty()} otherwise
	 */
	public Optional<Way> getWayById(long id) {
		return getEntityById(id, EntityType.Way).map(Way.class::cast);
	}

	/**
	 * Returns the {@link Tag} with the key {@code key} of the {@link Entity}
	 * specified by {@code id} and {@code entityType}.
	 * 
	 * @param id
	 *            the OSM id of the {@code Entity}
	 * @param key
	 *            the key to check
	 * @param entityType
	 *            the {@link EntityType}
	 * @return the tag if present and {@code Optional.empty()} otherwise
	 */
	public Optional<Tag> getTagById(long id, String key,
			EntityType entityType) {
		return getEntityById(id, entityType).flatMap(e -> e.getTags().stream()
				.filter(t -> t.getKey().equalsIgnoreCase(key)).findFirst());
	}

	/**
	 * Checks, if the node specified by {@code id} has an 'opening_hours' tag.
	 * 
	 * @param id
	 *            OSM id of the node to check
	 * @return true, if the node specified by {@code id} has an 'opening_hours'
	 *         tag
	 */
	public boolean hasOpeningHours(long id) {
		return getTagById(id, OSMOpeningHours.OPENING_HOURS_KEY,
				EntityType.Node).isPresent();
	}

	/**
	 * Returns the opening hours of the node specified by {@code id} if present
	 * and {@code Optional.empty()} otherwise.
	 * 
	 * @param id
	 *            the id of the node
	 * @return the opening hours of the node as {@link OSMOpeningHours} object,
	 *         if present and parsed correctly, and {@code Optional.empty()}
	 *         otherwise.
	 */
	public Optional<OSMOpeningHours> getOpeningHours(long id) {

		Optional<String> ohStr = getTagById(id,
				OSMOpeningHours.OPENING_HOURS_KEY, EntityType.Node)
						.map(Tag::getValue);

		if (!ohStr.isPresent())
			return Optional.empty();

		return Optional.ofNullable(new OSMOpeningHours(
				OpeningHoursParser.parseOpenedHours(ohStr.get()).getRules()));
	}

	/**
	 * 
	 * @return the stored {@link Entity}s as a {@link Stream}
	 */
	public Stream<Entity> getEntityStream() {
		return entities.values().stream();
	}

	/**
	 * Returns the {@link Node}s which are part of the {@link Way} specified by
	 * {@code wayId}.
	 * 
	 * @param wayId
	 *            the id of the way
	 * @return the {@link Node}s which are part of the specified {@link Way}
	 */
	public List<Node> getWayNodes(long wayId) {
		Optional<Way> way = getWayById(wayId);
		if (way.isPresent()) {
			return way.get().getWayNodes().stream().map(WayNode::getNodeId)
					.map(this::getNodeById).filter(Optional::isPresent)
					.map(Optional::get).collect(Collectors.toList());
		} else {
			return new ArrayList<>();
		}
	}

	/**
	 * Checks if the {@link Way} specified by {@code wayId} is cyclic. A way is
	 * cyclic if the first and the last node have the same id.
	 * 
	 * @param wayId
	 *            the if of the way to check
	 * @return {@code true}, if the way is cyclic
	 */
	public boolean isCyclicWay(long wayId) {
		Optional<Way> way = getWayById(wayId);
		if (way.isPresent()) {
			List<WayNode> wayNodes = way.get().getWayNodes();
			return wayNodes.get(0).getNodeId() == wayNodes
					.get(wayNodes.size() - 1).getNodeId();
		} else {
			return false;
		}
	}

	/**
	 * Computes the haversine distance between the nodes {@code n1} and
	 * {@code n2}.
	 * 
	 * @see OSMUtils#distance(Node, Node)
	 * @param n1
	 *            OSM node id of n1
	 * @param n2
	 *            OSM node id of n1
	 * @return the haversine distance between the two nodes, if both exits and
	 *         {@code OptionalDouble.empty()} otherwise
	 */
	public OptionalDouble distance(long n1, long n2) {
		Optional<Node> node1 = getNodeById(n1);
		Optional<Node> node2 = getNodeById(n1);

		if (node1.isPresent() && node2.isPresent()) {
			return OptionalDouble
					.of(OSMUtils.distance(node1.get(), node2.get()));
		} else {
			return OptionalDouble.empty();
		}
	}

	/**
	 * Returns the node of the {@link Way} specified by {@code id} which is
	 * nearest node to {@code point}, i.e. has the smallest haversine distance.
	 * 
	 * @param wayId
	 *            the way id
	 * @param point
	 *            the point to search the nearest node for
	 * @return the node in the way which is closest to {@code point} and
	 *         {@code Optional.empty()} if no such node is found
	 */
	public Optional<Node> nearestWayNode(long wayId, GHPoint point) {
		return getWayById(wayId)
				.flatMap(w -> Optional.of(getWayNodes(w.getId()).stream()))
				.map(nodes -> nodes
						.map(n -> Pair.of(OSMUtils.distance(point, n), n))
						.sorted().findFirst().orElse(null))
				.map(Pair::getRight);
	}

	/**
	 * Returns the node which is the nearest to {@code point} (haversine
	 * distance).
	 * 
	 * @param point
	 *            the point to search the nearest {@code Node} for
	 * @return the nearest node if any if found and {@code Optional.empty()}
	 *         otherwise
	 */
	public Optional<Node> nearestNeighbor(final GHPoint point) {
		return nearestNeighbor(point, null, e -> true);
	}

	/**
	 * Returns the node which is the nearest to {@code point} (haversine
	 * distance).
	 * 
	 * @param point
	 *            the point to search the nearest {@code Node} for
	 * @param predicate
	 *            only use nodes, that fulfill the predicate
	 * @return the nearest node if any if found and {@code Optional.empty()}
	 *         otherwise
	 */
	public Optional<Node> nearestNeighbor(final GHPoint point,
			Predicate<Entity> predicate) {
		return nearestNeighbor(point, null, predicate);
	}

	/**
	 * Returns the node which is the nearest to {@code point} (haversine
	 * distance).
	 * 
	 * @param point
	 *            the point to search the nearest {@code Node} for
	 * @param maxDistance
	 *            a maximal distance to consider in meter; {@code null} if no
	 *            maximal distance is desired
	 * @param predicate
	 *            only return nodes, that fulfill the predicate
	 * @return the nearest node if any if found and {@code Optional.empty()}
	 *         otherwise
	 */
	public Optional<Node> nearestNeighbor(final GHPoint point,
			Double maxDistance, Predicate<Entity> predicate) {
		return getEntityStream().filter(predicate.and(Node.class::isInstance))
				.map(Node.class::cast)
				.min((n1, n2) -> Double.compare(OSMUtils.distance(point, n1),
						OSMUtils.distance(point, n2)))
				.filter(n -> maxDistance == null
						|| OSMUtils.distance(point, n) <= maxDistance);
	}

	/**
	 * Returns the maximal {@code k} nearest neighboring nodes of {@code point}.
	 * 
	 * @param point
	 *            the point to search the nearest neighboring nodes for
	 * @param k
	 *            maximal number of neighbors to return
	 * @return the maximal {@code k} nearest neighboring nodes of {@code point}
	 */
	public List<Node> kNearestNeighbor(final GHPoint point, long k) {
		return kNearestNeighbor(point, k, null, e -> true);
	}

	/**
	 * Returns the maximal {@code k} nearest neighboring nodes of {@code point}.
	 * 
	 * @param point
	 *            the point to search the nearest neighboring nodes for
	 * @param k
	 *            maximal number of neighbors to return
	 * @param maxDistance
	 *            maximal distance to consider in meter; {@code null} if no
	 *            maximal distance is desired
	 * @return the maximal {@code k} nearest neighboring nodes of {@code point}
	 */
	public List<Node> kNearestNeighbor(final GHPoint point, long k,
			Double maxDistance) {
		return kNearestNeighbor(point, k, maxDistance, e -> true);
	}

	/**
	 * Returns the maximal {@code k} nearest neighboring nodes of {@code point}.
	 * 
	 * @param point
	 *            the point to search the nearest neighboring nodes for
	 * @param k
	 *            maximal number of neighbors to return
	 * @param predicate
	 *            only return nodes that fulfill the predicate
	 * @return the maximal {@code k} nearest neighboring nodes of {@code point}
	 */
	public List<Node> kNearestNeighbor(final GHPoint point, long k,
			Predicate<Entity> predicate) {
		return kNearestNeighbor(point, k, null, predicate);
	}

	/**
	 * Returns the maximal {@code k} nearest neighboring nodes of {@code point}.
	 * 
	 * @param point
	 *            the point to search the nearest neighboring nodes for
	 * @param k
	 *            maximal number of neighbors to return
	 * @param maxDistance
	 *            maximal distance to consider in meter; {@code null} if no
	 *            maximal distance is desired
	 * @param predicate
	 *            only return nodes that fulfill the predicate
	 * @return the maximal {@code k} nearest neighboring nodes of {@code point}
	 */
	public List<Node> kNearestNeighbor(final GHPoint point, long k,
			Double maxDistance, Predicate<Entity> predicate) {
		return getEntityStream().filter(predicate.and(Node.class::isInstance))
				.map(Node.class::cast)
				.map(n -> Pair.of(OSMUtils.distance(point, n), n))
				.filter(pair -> maxDistance == null
						|| pair.getKey() <= maxDistance)
				.sorted().limit(k).map(Pair::getValue)
				.collect(Collectors.toList());
	}

	/**
	 * Returns the coordinates of the {@link Node} specified by {@code id} as
	 * {@link GHPoint}
	 * 
	 * @param id
	 *            the id of the node
	 * @return the coordinates of the {@link Node} specified by {@code id} as
	 *         {@link GHPoint} and {@code Optional.empty()} if there is no such
	 *         node
	 */
	public Optional<GHPoint> getGHPoint(long id) {
		return getNodeById(id)
				.map(n -> new GHPoint(n.getLatitude(), n.getLongitude()));
	}

	/**
	 * 
	 * @return the stored {@link Entity}s
	 */
	public Set<Entity> getEntities() {
		return getEntityStream().collect(Collectors.toSet());
	}

	/**
	 * Sets the entities
	 * 
	 * @param entities
	 */
	protected void setEntities(Set<Entity> entities) {
		MultiValuedMap<Long, Entity> map = new ArrayListValuedHashMap<>();
		for (Entity e : entities) {
			map.put(e.getId(), e);
		}
		this.entities = map;
	}

	/**
	 * 
	 * @return all stored {@link Node}s
	 */
	public Set<Node> getNodes() {
		return getEntityStream()
				.filter(e -> e.getType().equals(EntityType.Node))
				.map(Node.class::cast).collect(Collectors.toSet());
	}

	/**
	 * 
	 * @return @return all stored {@link Way}s
	 */
	public Set<Way> getWays() {
		return getEntityStream().filter(e -> e.getType().equals(EntityType.Way))
				.map(Way.class::cast).collect(Collectors.toSet());
	}

	/**
	 * Returns all {@link Way}s that contains the {@code Node} specified by
	 * {@code id}.
	 * 
	 * @param nodeId
	 * @return all {@link Way}s that contains the {@code Node} specified by
	 */
	public Set<Way> getWaysContainingNode(long nodeId) {
		return getEntityStream().filter(e -> e.getType().equals(EntityType.Way))
				.map(Way.class::cast)
				.filter(w -> w.getWayNodes().stream()
						.anyMatch(n -> n.getNodeId() == nodeId))
				.collect(Collectors.toSet());
	}

	public Optional<Bound> getBoundingBox() {
		return Optional.ofNullable(boundingBox);
	}

	public void setBoundingBox(Bound boundingBox) {
		this.boundingBox = boundingBox;
	}

}
