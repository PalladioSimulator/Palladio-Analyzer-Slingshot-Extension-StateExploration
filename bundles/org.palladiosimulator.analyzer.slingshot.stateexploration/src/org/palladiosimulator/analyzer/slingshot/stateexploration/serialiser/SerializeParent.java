package org.palladiosimulator.analyzer.slingshot.stateexploration.serialiser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 
 * @author Sophie Stieß
 *
 */
public interface SerializeParent<T>{	
	
	public void serialize(final T thing, final Path path);
	
	default void write(final String string, final File file) {
		try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(string);
			writer.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
