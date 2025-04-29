package spielwiese.version2;

import org.eclipse.emf.common.util.EList;
import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;

public class EListThing extends Thing{
	
	private final EList<AbstractUserAction> eList;
	

	public EListThing(final EList<AbstractUserAction> eList) {
		super("foo", null);
		this.eList = eList;
	}
}
