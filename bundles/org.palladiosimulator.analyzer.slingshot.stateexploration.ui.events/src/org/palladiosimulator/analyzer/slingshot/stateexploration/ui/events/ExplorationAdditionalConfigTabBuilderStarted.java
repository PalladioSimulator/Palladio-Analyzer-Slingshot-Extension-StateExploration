package org.palladiosimulator.analyzer.slingshot.stateexploration.ui.events;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSystemEvent;

public class ExplorationAdditionalConfigTabBuilderStarted extends AbstractSystemEvent
implements Iterable<ExplorationAdditionalConfigTabBuilderStarted.Checkbox> {

	private final Queue<Checkbox> queue = new PriorityQueue<>();

	public Builder newFieldDefinition() {
		return new Builder();
	}

	@Override
	public Iterator<Checkbox> iterator() {
		return queue.iterator();
	}

	public final class Builder {
		private String label;
		private boolean defaultValue;

		public Builder label(final String label) {
			this.label = label;
			return this;
		}

		public Builder defaultValue(final boolean title) {
			this.defaultValue = title;
			return this;
		}

		public void build() {
			queue.add(new Checkbox(this));
		}
	}

	public static final class Checkbox implements Comparable<Checkbox> {

		private final String label;
		private final boolean defaultValue;

		private Checkbox(final Builder builder) {
			this.label = builder.label;
			this.defaultValue = builder.defaultValue;
		}

		public String getLabel() {
			return label;
		}

		public boolean getdefaultValue() {
			return defaultValue;
		}


		@Override
		public int compareTo(final Checkbox o) {
			return this.label.compareTo(o.label);
		}
	}
}
