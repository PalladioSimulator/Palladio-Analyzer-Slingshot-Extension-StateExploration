package spielwiese.version2;

public class LoopThingChild extends Thing {
	
	private final LoopThingParent parent;

	
	public LoopThingChild(final String foo, final LoopThingParent parent) {
		super(foo, null);
		this.parent = parent;
	}
}
