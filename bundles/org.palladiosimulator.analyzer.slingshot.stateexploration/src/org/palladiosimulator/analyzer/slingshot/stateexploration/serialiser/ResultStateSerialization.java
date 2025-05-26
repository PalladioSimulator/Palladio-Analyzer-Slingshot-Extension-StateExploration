package org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser;

import java.nio.file.Path;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser.data.ResultState;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * TODO 
 * 
 * 
 * @author Sophie Stieß
 *
 */
public final class ResultStateSerialization implements SerializeParent<ResultState>{

	protected static final Logger LOGGER = Logger.getLogger(ResultStateSerialization.class);
	
	private final Gson gson;
	
	public ResultStateSerialization() {
		super();				
		this.gson = new GsonBuilder().create();
	}

	@Override
	public void serialize(final ResultState snapshot, final Path path) {
		final String json = gson.toJson(snapshot);
		write(json, path.toFile());
	}
}
