package spielwiese.version2;

public class ClassThing<T> extends Thing{
	
	private final Class<T> clazz;
	

	public ClassThing(final Class<T> clazz) {
		super("foo", null);
		this.clazz = clazz;
	}
	
	

}
