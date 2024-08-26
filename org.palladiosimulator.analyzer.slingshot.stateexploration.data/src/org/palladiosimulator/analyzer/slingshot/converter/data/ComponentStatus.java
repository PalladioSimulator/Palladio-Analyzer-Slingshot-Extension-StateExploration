package org.palladiosimulator.analyzer.slingshot.converter.data;

import java.util.List;

public record ComponentStatus(
		String id,
		String name,
		String assemblyContextId,
		List<ProcessingInfo> processingInfos
		) {
	public record ProcessingInfo(
			String id, 
			double MTTF,
			double MTTR,
			int numberOfReplicas,
			String scalingPolicyId,
			String scalingPolicyName
			) {
		
	}

}
