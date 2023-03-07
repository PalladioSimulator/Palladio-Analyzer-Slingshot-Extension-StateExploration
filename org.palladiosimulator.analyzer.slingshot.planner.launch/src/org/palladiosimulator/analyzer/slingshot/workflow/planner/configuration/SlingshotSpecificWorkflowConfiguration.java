package org.palladiosimulator.analyzer.slingshot.workflow.planner.configuration;

public final class SlingshotSpecificWorkflowConfiguration {
	
	private final String logFileName;
	private final String snapid;
	
	private final double snaptime;

	private final boolean startFromSnap;
	
	public SlingshotSpecificWorkflowConfiguration(final Builder builder) {
		this.logFileName = builder.logFileName;
		this.snaptime = builder.snaptime;
		this.startFromSnap = builder.startFromSnap;
		this.snapid = builder.snapid;
	}
	
	public String getLogFileName() {
		return this.logFileName;
	}
	
	public double getSnaptime() {
		return this.snaptime;
	}
	
	public String getSnapid() {
		return this.snapid;
	}
	
	
	public boolean isStartFromSnap() {
		return startFromSnap;
	}

	public static Builder builder() {
		return new Builder();
	}
	
	public static final class Builder {
		private String logFileName;
		private String snapid;
		private double snaptime;
		private boolean startFromSnap;

		private Builder() {}
		
		public Builder withLogFile(final String logFileName) {
			this.logFileName = logFileName;
			return this;
		}

		public Builder withSnapid(final String snapid) {
			this.snapid = snapid;
			return this;
		}

		public Builder withSnaptime(final String snaptime) {
			try {
				this.snaptime = Double.parseDouble(snaptime);
			} catch (Exception e) {
				this.snaptime = 0.0;
			}
			return this;
		}
		
		public Builder withStartFromSnap(final boolean startFromSnap) {
			this.startFromSnap = startFromSnap;
			return this;
		}
		
		public Builder withSnaptime(final double snaptime) {
			this.snaptime = snaptime;
			return this;
		}
		
		public SlingshotSpecificWorkflowConfiguration build() {
			return new SlingshotSpecificWorkflowConfiguration(this);
		}
	}
}
