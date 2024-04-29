package org.palladiosimulator.analyzer.slingshot.stateexploration.ui.events;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSystemEvent;

public class ExplorationConfigTabBuilderStarted extends AbstractSystemEvent implements Iterable<ExplorationConfigTabBuilderStarted.TextField> {

	private final Queue<TextField> queue = new PriorityQueue<>();

	public Builder newFieldDefinition() {
		return new Builder();
	}

	@Override
	public Iterator<TextField> iterator() {
		return queue.iterator();
	}

	public final class Builder {
		private String label;
		private String promptTitle;
		private boolean optional = false;

		public Builder label(final String label) {
			this.label = label;
			return this;
		}

		public Builder promptTitle(final String title) {
			this.promptTitle = title;
			return this;
		}

		public Builder optional(final boolean optional) {
			this.optional = optional;
			return this;
		}

		public void build() {
			if (this.promptTitle == null) {
				this.promptTitle = "Select " + this.label;
			}

			if (this.optional) {
				this.label = this.label + " (Optional)";
			}

			queue.add(new TextField(this));
		}
	}

	public static final class TextField implements Comparable<TextField> {

		private final String label;
		private final String promptTitle;
		private final boolean optional;

		private TextField(final Builder builder) {
			this.label = builder.label;
			this.promptTitle = builder.promptTitle;
			this.optional = builder.optional;
		}

		public String getLabel() {
			return label;
		}

		public String getPromptTitle() {
			return promptTitle;
		}

		public boolean isOptional() {
			return optional;
		}

		@Override
		public int compareTo(final TextField o) {
			if (this.optional == o.optional) {
				return this.label.compareTo(o.label);
			}
			return Boolean.compare(o.optional, optional);
		}

	}
}
