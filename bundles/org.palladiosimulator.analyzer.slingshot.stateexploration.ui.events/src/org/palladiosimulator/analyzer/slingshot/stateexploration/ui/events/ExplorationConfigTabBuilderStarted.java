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
		private String defaultValue;
		private boolean isFolderSelection = false;
		private boolean isOptional = false;

		public Builder label(final String label) {
			this.label = label;
			return this;
		}

		public Builder defaultValue(final String title) {
			this.defaultValue = title;
			return this;
		}

		public Builder setIsOptional() {
			this.isOptional = true;
			return this;
		}

		public Builder setIsFolderSelection() {
			this.isFolderSelection = true;
			return this;
		}

		public void build() {
			if (this.defaultValue == null) {
				this.defaultValue = "Select " + this.label;
			}

			queue.add(new TextField(this));
		}
	}

	public static final class TextField implements Comparable<TextField> {

		private final String label;
		private final String defaultValue;
		private final boolean isOptional;
		private final boolean isFolderSelection;

		private TextField(final Builder builder) {
			this.label = builder.label;
			this.defaultValue = builder.defaultValue;
			this.isOptional = builder.isOptional;
			this.isFolderSelection = builder.isFolderSelection;
		}

		public String getLabel() {
			return label;
		}

		public String getdefaultValue() {
			return defaultValue;
		}

		public boolean isOptional() {
			return this.isOptional;
		}

		public boolean isFolderSelection() {
			return this.isFolderSelection;
		}

		@Override
		public int compareTo(final TextField o) {
			if (this.isOptional == o.isOptional) {
				return this.label.compareTo(o.label);
			}
			return Boolean.compare(o.isOptional, isOptional);
		}

	}
}
