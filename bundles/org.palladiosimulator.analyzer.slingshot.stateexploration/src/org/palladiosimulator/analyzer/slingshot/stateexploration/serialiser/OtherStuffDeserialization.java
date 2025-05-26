package org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.utils.PCMResourcePartitionHelper;
import org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser.data.OtherInitThings;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * 
 * TODO 
 * 
 * 
 * @author Sophie Stieß
 *
 */
public final class OtherStuffDeserialization implements DeserializeParent<OtherInitThings> {

	protected static final Logger LOGGER = Logger.getLogger(OtherStuffDeserialization.class);
	
	private final Gson gson;
	
	private final SPD spd;
	
	public OtherStuffDeserialization(final PCMResourceSetPartition partition) {
		super();
				
		this.spd = PCMResourcePartitionHelper.getSPD(partition);
		this.gson = createGson();
	}

	private Gson createGson() {
		
		return new GsonBuilder()
				.registerTypeAdapter(ScalingPolicy.class, new ScalingPolicyDeserializer())
				.create();
	}
	
	@Override
	public OtherInitThings deserialize(final Path path) {
		final String read = read(path.toFile());
		return gson.fromJson(read, OtherInitThings.class);
	}
	
	/**
	 * 
	 * @author Sophie Stieß
	 *
	 */
	private class ScalingPolicyDeserializer implements JsonDeserializer<ScalingPolicy> {
		@Override
		public ScalingPolicy deserialize(final JsonElement json, final Type typeOfT,
				final JsonDeserializationContext context) throws JsonParseException {

			if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
				final String id = json.getAsJsonPrimitive().getAsString();

				final List<ScalingPolicy> policies = spd.getScalingPolicies();
				final Optional<ScalingPolicy> matchingPolicy = policies.stream().filter(p -> p.getId().equals(id))
						.findFirst();

				if (matchingPolicy.isEmpty()) {
					throw new JsonParseException(String.format(
							"Cannot deserialise json \"%s\", no matching policy with id \"%s\" in SPD model \"%s\" [id = %s].",
							json.toString(), id, spd.getEntityName(), spd.getId()));
				}
				return matchingPolicy.get();
			}

			throw new JsonParseException(String
					.format("Cannot deserialise json \"%s\", expected policy id but found none.", json.toString()));
		}
	}
}
