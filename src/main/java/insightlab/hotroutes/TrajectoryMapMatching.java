package insightlab.hotroutes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.GPXEntry;

public class TrajectoryMapMatching {

	public static Map<Long, MatchResult> runMapMatching(MapMatching mapMatching, Map<Long, List<GPXEntry>> map) {
		Map<Long, MatchResult> mapMatchResult = new TreeMap<Long, MatchResult>();
		for (Entry<Long, List<GPXEntry>> trajectory : map.entrySet()) {
			try {
				MatchResult matchResult = mapMatching.doWork(trajectory.getValue());
				mapMatchResult.put(trajectory.getKey(), matchResult);
			} catch (Exception e) {
				System.err.println("Não foi possível realizar o map-matching para trajetória:" + trajectory.getKey());
			}

		}

		return mapMatchResult;
	}

	public static void saveMapMatchingEdgesSpeed(Map<Long, MatchResult> mapMatchResult, String filePath,
			MyGraphHopper hopper) throws IOException {
		FileWriter writer = new FileWriter(new File(filePath));
		StringBuilder builder = new StringBuilder();
		try {
			builder.append("id;edge_id;start_time;edge_length;speed\n");
			List<EdgeMatch> edgesToFill = new ArrayList<EdgeMatch>();

			for (Entry<Long, MatchResult> mapEntry : mapMatchResult.entrySet()) {
				GPXExtension first = null;
				for (EdgeMatch edgeMatch : mapEntry.getValue().getEdgeMatches()) {
					int countPointsEdge = edgeMatch.getGpxExtensions().size();
					// More than one point on the edge
					if (countPointsEdge > 1 && first == null) {
						first = edgeMatch.getGpxExtensions().get(0);
						GPXExtension next = edgeMatch.getGpxExtensions().get(countPointsEdge - 1);
						// Teste velocidade na aresta
						System.out.println(edgeMatch.getEdgeState().getEdge());
						System.out.println(getSpeedMS(hopper, first, next));
						first = next;
					} else if (countPointsEdge == 0) {
						edgesToFill.add(edgeMatch);
					} else if (countPointsEdge == 1 && first != null) {
						first = edgeMatch.getGpxExtensions().get(0);
					}

				}
				edgesToFill.clear();
			}

			writer.write(builder.toString());
			writer.flush();
		} finally {
			writer.close();
		}
	}

	public static void saveMapMatchingEdges(Map<Long, MatchResult> mapMatchResult, String filePath,
			MyGraphHopper hopper) throws IOException {
		FileWriter writer = new FileWriter(new File(filePath));
		StringBuilder builder = new StringBuilder();
		GraphHopperStorage graph = hopper.getGraphHopperStorage();
		NodeAccess na = graph.getNodeAccess();
		int order;
		try {
			builder.append("id;latitude;longitude;order\n");

			for (Entry<Long, MatchResult> mapEntry : mapMatchResult.entrySet()) {
				order = 1;
				for (EdgeMatch edgeMatch : mapEntry.getValue().getEdgeMatches()) {
					int nodeId = edgeMatch.getEdgeState().getBaseNode();
										
					builder.append(mapEntry.getKey());
					builder.append(";");

					builder.append(na.getLatitude(nodeId));
					builder.append(";");

					builder.append(na.getLongitude(nodeId));
					builder.append(";");
					
					builder.append(order);
					builder.append("\n");
					order++;

				}
			}

			writer.write(builder.toString());
			writer.flush();
		} finally {
			writer.close();
		}
	}

	public static void saveMapMatchingPoints(Map<Long, MatchResult> mapMatchResult, String filePath,
			MyGraphHopper hopper) throws IOException {

		FileWriter writer = new FileWriter(new File(filePath));
		StringBuilder builder = new StringBuilder();
		try {
			builder.append("id;latitude;longitude;timestamp;speed;edge_id;osm_id\n");

			for (Entry<Long, MatchResult> mapEntry : mapMatchResult.entrySet()) {
				GPXExtension first = null;

				for (EdgeMatch edgeMatch : mapEntry.getValue().getEdgeMatches()) {
					for (GPXExtension gpsExtension : edgeMatch.getGpxExtensions()) {
						if (first == null)
							first = gpsExtension;

						writeLine(hopper, builder, mapEntry, first, edgeMatch, gpsExtension);
					}
				}
			}

			writer.write(builder.toString());
			writer.flush();
		} finally {
			writer.close();
		}

	}

	private static void writeLine(MyGraphHopper hopper, StringBuilder builder, Entry<Long, MatchResult> mapEntry,
			GPXExtension first, EdgeMatch edgeMatch, GPXExtension gpsExtension) {
		builder.append(mapEntry.getKey());
		builder.append(";");

		builder.append(gpsExtension.getQueryResult().getSnappedPoint().getLat());
		builder.append(";");

		builder.append(gpsExtension.getQueryResult().getSnappedPoint().getLon());
		builder.append(";");

		builder.append(gpsExtension.getEntry().getTime());
		builder.append(";");
		if (first != gpsExtension) {
			double speed = getSpeedMS(hopper, first, gpsExtension);
			builder.append(speed);
		}

		builder.append(";");

		int edge = edgeMatch.getEdgeState().getEdge();
		builder.append(edge);
		builder.append(";");

		builder.append(hopper.getOSMWay(edge));
		builder.append("\n");
	}

	private static double getSpeedMS(MyGraphHopper hopper, GPXExtension from, GPXExtension to) {
		GHResponse res = getPath(hopper, from, to);
		return res.getBest().getDistance() / ((double) (to.getEntry().getTime() - from.getEntry().getTime()) / 1000);
	}

	private static GHResponse getPath(MyGraphHopper hopper, GPXExtension from, GPXExtension to) {
		GHRequest req = new GHRequest(from.getQueryResult().getSnappedPoint().getLat(),
				from.getQueryResult().getSnappedPoint().getLon(), to.getQueryResult().getSnappedPoint().getLat(),
				to.getQueryResult().getSnappedPoint().getLon());
		req.setVehicle("car").setAlgorithm("dijkstra");
		return hopper.route(req);
	}

}
