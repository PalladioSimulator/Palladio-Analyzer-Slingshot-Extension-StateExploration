package org.palladiosimulator.analyzer.slingshot.stateexploration.graphicalrepresentation;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.palladiosimulator.analyzer.slingshot.planner.data.Measurement;
import org.palladiosimulator.analyzer.slingshot.planner.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.planner.data.SLO;
import org.palladiosimulator.analyzer.slingshot.planner.data.State;
import org.palladiosimulator.analyzer.slingshot.planner.data.StateGraph;
import org.palladiosimulator.analyzer.slingshot.planner.data.Transition;
import org.palladiosimulator.analyzer.slingshot.planner.data.Measurement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GraphicalRepresentation {

	static public String getPictureString(StateGraph graph) {
		final String fileName = "/tmp/state_graph_graphical_representation.dot";
		final ArrayList<State> states = graph.getStates();

		try {
			FileWriter file = new FileWriter(fileName);

			// writing the head of the DOT file format for a directional graph
			file.write("digraph graphname {\n");
			file.write("rankdir=LR;\n");
			file.write("splines=false;\n");

			int noteCount = 0;

			for (State state : states) {
				ArrayList<Transition> transitions = state.getOutTransitions();

				String noteDescription = String.format("StartTime: %.2f\\nLength: %.2f\\nEndTime: %.2f\\n",
						state.getStartTime(), state.getDuration(), state.getEndTime());

				for (MeasurementSet ms : state.getMeasurements()) {
					noteDescription += String.format("%s (Size: %d): %.2f\\n", ms.getName(), ms.size(), ms.getMedian());
				}
				
				for (SLO slo : state.getSLOs()) {
					noteDescription += String.format("%s: %d to %d\\n", slo.getName(), slo.getLowerThreshold(), slo.getUpperThreshold());
				}
				
				noteDescription += String.format("Current Utility: %.2f\\n", state.getUtiltity());

				file.write("\"" + state.getId() + "\";\n");
				file.write("note" + noteCount + "[shape=box, label=\"" + noteDescription + "\", style=filled];\n");
				file.write("\"" + state.getId() + "\" -> note" + noteCount + " [style=dotted, arrowhead=none];\n");
				file.write("{rank=same; rankdir=TB; note" + noteCount + "; \"" + state.getId() + "\";}\n\n");

				noteCount++;

				// for every transition a path is written to the file
				for (Transition transition : transitions) {
					switch (transition.getReason()) {
						case EnviromentalChange: {
							file.write("\"" + transition.getSource().getId() + "\" -> \"" + transition.getTarget().getId()+ "\" [style=\"dotted\"];\n");
							break;
						}
						case IntervalChange: {
							file.write("\"" + transition.getSource().getId() + "\" -> \"" + transition.getTarget().getId()+ "\" [style=\"dashed\"];\n");
							break;
						}
						case ReactiveReconfigurationChange:
						case ReconfigurationChange: {
							file.write("\"" + transition.getSource().getId() + "\" -> \"" + transition.getTarget().getId()+ "\";\n");
							break;
						}
					}
				}
				file.write("\n");
			}

			// use sorted array of states to combine states with same start time in the same
			// rank
			ArrayList<State> sortedStates = (ArrayList<State>) states.stream().sorted(Comparator.comparing(State::getStartTime)).collect(Collectors.toList());
			sortedStates.remove(graph.getRoot());

			boolean nextRank = true;
			for (int i = 0; i < sortedStates.size(); i++) {
				if (nextRank) {
					file.write("{rank=same; ");
					nextRank = false;
				}

				file.write("\"" + sortedStates.get(i).getId() + "\"; ");

				if (((i + 1) >= sortedStates.size())
						|| sortedStates.get(i).getStartTime() != sortedStates.get(i + 1).getStartTime()) {
					nextRank = true;
					file.write("};\n");
				}
			}

			// writing the end of the DOT file format to the file
			file.write("}\n");
			file.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			// this.LOGGER.info(e);
			// return "Failed to write to file " + fileName;
		}
		
		// creating image from dot file using the linux program "dot" and opening it in
		// the Ubuntu default image viewer
		// TODO make program calls generic for all platforms
		try {
			Process p1 = Runtime.getRuntime().exec(
					"/usr/bin/dot -Tjpg /tmp/state_graph_graphical_representation.dot -o /tmp/state_graph_graphical_representation.jpg");
			p1.waitFor();
			//Process p2 = Runtime.getRuntime().exec("/usr/bin/eog /tmp/state_graph_graphical_representation.jpg");
			//p2.waitFor();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fileName;
	}
	
	public static String getJSONString(StateGraph graph) {
		JsonSerializer<MeasurementSet> serializerMeasurementSet = new JsonSerializer<MeasurementSet>() {  
		    @Override
		    public JsonElement serialize(MeasurementSet src, Type typeOfSrc, JsonSerializationContext context) {
		        JsonObject jsonMeasurementSet = new JsonObject();

		        jsonMeasurementSet.addProperty("name", src.getName());
		        jsonMeasurementSet.addProperty("measuringPointURI", src.getMeasuringPointURI());
		        jsonMeasurementSet.add("elements", context.serialize(src.toArray()));
		        
		        return jsonMeasurementSet;
		    }
		};
		
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		builder.registerTypeAdapter(MeasurementSet.class, serializerMeasurementSet);
		Gson gson = builder.create();
		
		return gson.toJson(graph);
	}
	
	public static StateGraph fromJSONFile(String fileName) {
		JsonDeserializer<MeasurementSet> deserializerMeasurementSet = new JsonDeserializer<MeasurementSet>() {
			@Override
			public MeasurementSet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				
				JsonObject obj = json.getAsJsonObject();
				
				MeasurementSet ms = new MeasurementSet();
				
				ms.setName(obj.get("name").getAsString());
				ms.setMeasuringPointURI(obj.get("measuringPointURI").getAsString());
				
				for (int i = 0; i < obj.get("elements").getAsJsonArray().size(); i++) {
					JsonObject el = obj.get("elements").getAsJsonArray().get(i).getAsJsonObject();
					
					ms.add(context.deserialize(el, Measurement.class));
				}
				
				return ms;
			}
		};
		
		
		
		try {
			GsonBuilder builder = new GsonBuilder();
			builder.serializeNulls();
			builder.registerTypeAdapter(MeasurementSet.class, deserializerMeasurementSet);
			Gson gson = builder.create();
		    
			Reader reader = Files.newBufferedReader(Paths.get(fileName));
		    StateGraph graph = gson.fromJson(reader, StateGraph.class);

		    for (State s : graph.getStates())
		    	for (Transition t : s.getOutTransitions())
		    		t.setSource(s);
		    
		    reader.close();
		    
		    return graph;

		} catch (Exception ex) {
		    ex.printStackTrace();
		}
		
		return null;
	}
}
