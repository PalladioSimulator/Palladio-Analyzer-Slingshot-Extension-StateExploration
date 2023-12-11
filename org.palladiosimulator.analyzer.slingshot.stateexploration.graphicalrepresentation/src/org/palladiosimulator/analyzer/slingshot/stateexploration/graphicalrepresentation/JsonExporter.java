package org.palladiosimulator.analyzer.slingshot.stateexploration.graphicalrepresentation;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraph;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.planner.data.Transition;

import com.google.gson.Gson;

public class JsonExporter {
	public static String getJSONString(StateGraph graph) { 
		Gson gson = GsonProvider.getGson();
		
		return gson.toJson(graph);
	}
	
	public static StateGraph fromJSONFile(String fileName) {
		try {
			Gson gson = GsonProvider.getGson();
		    
			Reader reader = Files.newBufferedReader(Paths.get(fileName));
		    StateGraph graph = gson.fromJson(reader, StateGraph.class);

		    for (StateGraphNode s : graph.states()) {
		    	List<Transition> trans = new ArrayList<Transition>();
		    	trans.addAll(s.outTransitions());
		    	s.outTransitions().clear();
		    	
		    	for (Transition t : trans) {
		    		s.outTransitions().add(new Transition(s, t.target(), t.reason(), t.change()));
		    	}
		    }
		    
		    reader.close();
		    
		    return graph;

		} catch (Exception ex) {
		    ex.printStackTrace();
		}
		
		return null;
	}
}
