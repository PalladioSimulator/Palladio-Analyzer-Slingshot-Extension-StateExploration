package spielwiese.version2;

import java.util.ArrayList;
import java.util.List;

public class LoopThingParent extends Thing {
	
	private final List<LoopThingChild> children;

	public List<LoopThingChild> getChildren() {
		return children;
	}
	
	public void addLoopChild(final LoopThingChild child) {
		this.children.add(child);
	}
	
	public LoopThingParent(final String foo) {
		super(foo, null);
		this.children = new ArrayList<>();
	}
}
