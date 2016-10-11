package joachimrussig.heatstressrouting.osmdata;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * A {@code CollectorSink} is a {@link Sink} that consumes the OSM data types
 * and stores them in a {@link MultiValueMap} with the OSM id as key and the OSM
 * entity as value.
 * 
 * @author Joachim Rußig
 */
public class CollectorSink implements Sink {

	/**
	 * internal representation of the collected entities.
	 */
	private MultiValuedMap<Long, Entity> entities;
	private Bound boundingBox = null;
	@SuppressWarnings("unused")
	private Map<String, Object> metaData;

	/**
	 * Creates a new {@code CollectorSink}.
	 */
	public CollectorSink() {
		this.entities = new ArrayListValuedHashMap<>();
	}

	@Override
	public void process(EntityContainer entityContainer) {
		Entity entity = entityContainer.getEntity();

		if (entity instanceof Bound)
			this.boundingBox = (Bound) entity;

		this.entities.put(entity.getId(), entity);
	}

	@Override
	public void initialize(Map<String, Object> metaData) {
		this.metaData = metaData;
	}

	@Override
	public void complete() {

	}

	@Override
	public void release() {

	}

	/**
	 * 
	 * @return the collected entities as {@link MultiValueMap} with the OSM id
	 *         as key and the OSM entity as value
	 */
	public MultiValuedMap<Long, Entity> getEntities() {
		return entities;
	}

	/**
	 * 
	 * @return returns the collected {@link Entity}s
	 */
	public Collection<Entity> getEntitiesSet() {
		return entities.values();
	}

	public void setEntities(MultiValuedMap<Long, Entity> entities) {
		this.entities = entities;
	}

	public Bound getBoundingBox() {
		return boundingBox;
	}

	public void setBoundingBox(Bound boundingBox) {
		this.boundingBox = boundingBox;
	}

}
