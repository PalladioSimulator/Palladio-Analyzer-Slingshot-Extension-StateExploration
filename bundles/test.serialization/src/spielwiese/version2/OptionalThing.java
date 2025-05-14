package spielwiese.version2;

import java.util.Optional;

public class OptionalThing<T> extends Thing{
	private final Optional<T> optional;
	
	public OptionalThing(final T thing) {
		super("foo", null);
		this.optional = thing == null ? Optional.empty() : Optional.of(thing);
	}
}
