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
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Parameters;

public class TrajectoryMapMatching {

	private static TrajectoryMapMatching instance = null;

	public static TrajectoryMapMatching getInstance() {
		if (instance == null)
			instance = new TrajectoryMapMatching();
		return instance;
	}

	public Map<Long, MatchResult> runMapMatching(MapMatching mapMatching, Map<Long, List<GPXEntry>> map) {
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

	public void saveMapMatchingEdgesSpeed(Map<Long, MatchResult> mapMatchResult, String filePath, MyGraphHopper hopper)
			throws IOException {
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

	/**
	 * Save map-matching result in format of id;latitude;longitude;order
	 * 
	 * @param mapMatchResult
	 * @param filePath
	 * @param hopper
	 * @throws IOException
	 */
	public void saveMapMatchingEdges(Map<Long, MatchResult> mapMatchResult, String filePath, MyGraphHopper hopper)
			throws IOException {
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

	/**
	 * Save map-matching result in format of
	 * id;latitude;longitude;timestamp;speed;edge_id;osm_id
	 * 
	 * @param mapMatchResult
	 * @param filePath
	 * @param hopper
	 * @throws IOException
	 */
	public void saveMapMatchingPoints(Map<Long, MatchResult> mapMatchResult, String filePath, MyGraphHopper hopper)
			throws IOException {

		FileWriter writer = new FileWriter(new File(filePath));
		StringBuilder builder = new StringBuilder();
		
		try {
			builder.append("id;latitude;longitude;timestamp;edge_id;osm_id\n");

			for (Entry<Long, MatchResult> mapEntry : mapMatchResult.entrySet()) {
				GPXExtension first = null;

				for (EdgeMatch edgeMatch : mapEntry.getValue().getEdgeMatches()) {
					for (GPXExtension gpsExtension : edgeMatch.getGpxExtensions()) {
						if (first == null)
							first = gpsExtension;
						writeLine(hopper, builder, mapEntry, first, edgeMatch, gpsExtension);
						first = gpsExtension;
					}
				}
			}

			writer.write(builder.toString());
			writer.flush();
		} finally {
			writer.close();
		}

	}

	public void saveMapMatchingSegmentsWithoutEmptyEdges(Map<Long, MatchResult> mapMatchResult, String filePath,
			MyGraphHopper hopper) throws IOException {

		FileWriter writer = new FileWriter(new File(filePath));
		StringBuilder builder = new StringBuilder();
		EdgeMatch previousEdge = null;
		double speed;

		try {
			builder.append("trajId;ledgeId;timestamp;speed\n");

			// iterate over all trajectories
			for (Entry<Long, MatchResult> mapEntry : mapMatchResult.entrySet()) {
				GPXExtension previousGPS = null;
				Long trajId = mapEntry.getKey();// output traj_id
				
				// for each edge in trajectory map-matching
				for (EdgeMatch edgeMatch : mapEntry.getValue().getEdgeMatches()) {
					
					int edgeId = edgeMatch.getEdgeState().getEdge();// output edge_id
					
					if(edgeMatch.isEmpty()) {
						previousGPS = null;
					}
					// iterate over points on the edge
					for (GPXExtension gpsExtension : edgeMatch.getGpxExtensions()) {
						if (previousGPS == null) {
							previousGPS = gpsExtension;
						} else if (previousEdge.equals(edgeMatch) || previousEdge.getEdgeState().getAdjNode() == edgeMatch.getEdgeState().getBaseNode()) {
								speed = getSpeedMS(hopper, previousGPS, gpsExtension);
								writeSegmentOutput(builder, trajId, edgeId, gpsExtension.getEntry().getTime(), speed);								
						}
						previousGPS = gpsExtension;
						previousEdge = edgeMatch;
					}
				}
			}

			writer.write(builder.toString());
			writer.flush();
		} finally {
			writer.close();
		}

	}

	private void writeSegmentOutput(StringBuilder builder, Long trajId, int edgeId, long time, double speed) {
		builder.append(trajId);
		builder.append(";");

		builder.append(edgeId);
		builder.append(";");

		builder.append(time);
		builder.append(";");
		
		builder.append(speed);
		
		builder.append("\n");
	}

	//TODO refactoring
	public void saveMapMatchingSegmentsWithEmpyEdges(Map<Long, MatchResult> mapMatchResult, String filePath,
			MyGraphHopper hopper) throws IOException {
		FileWriter writer = new FileWriter(new File(filePath));
		StringBuilder builder = new StringBuilder();
		List<EdgeMatch> edgesWithoutPoints = new ArrayList<EdgeMatch>();
		EdgeMatch previousEdge = null;
		long timestamp, timestamp2;
		double speed;

		try {
			builder.append("id;latitude;longitude;timestamp;speed;edge_id;osm_id\n");

			// iterate over all trajectories
			for (Entry<Long, MatchResult> mapEntry : mapMatchResult.entrySet()) {
				GPXExtension previousGPS = null;
				Long trajId = mapEntry.getKey();// output traj_id
				mapEntry.getValue().getMatchLength();
				double totalDelta = 0;
				// for each edge in trajectory map-matching
				for (EdgeMatch edgeMatch : mapEntry.getValue().getEdgeMatches()) {
					int edgeId = edgeMatch.getEdgeState().getEdge();// output edge_id

					if (edgeMatch.getGpxExtensions().isEmpty())
						edgesWithoutPoints.add(edgeMatch);
					// iterate over points on the edge
					for (GPXExtension gpsExtension : edgeMatch.getGpxExtensions()) {
						if (previousGPS == null) {
							previousGPS = gpsExtension;
						} else {
							if (previousEdge == edgeMatch) {
								speed = getSpeedMS(hopper, previousGPS, gpsExtension);
								writeSegmentOutput(trajId, edgeId, previousGPS.getEntry().getTime(),
										gpsExtension.getEntry().getTime(), speed);

							} else {

								// line of previous point
								speed = getSpeedMS(hopper, previousGPS, gpsExtension);

								// first segment
								timestamp = previousGPS.getEntry().getTime();
								int node = previousEdge.getEdgeState().getAdjNode();
								hopper.getGraphHopperStorage().getNodeAccess().getLatitude(node);
								hopper.getGraphHopperStorage().getNodeAccess().getLongitude(node);

								long distance = (long) getPath(hopper, previousGPS.getEntry().getLat(),
										previousGPS.getEntry().getLon(),
										hopper.getGraphHopperStorage().getNodeAccess().getLatitude(node),
										hopper.getGraphHopperStorage().getNodeAccess().getLongitude(node)).getBest()
												.getDistance();
								double deltaTime = distance / speed;// time in milliseconds

								timestamp2 = timestamp + (long) deltaTime;
								writeSegmentOutput(trajId, edgeId, timestamp, timestamp2, speed);
								totalDelta += deltaTime;

								// empty edges
								if (!edgesWithoutPoints.isEmpty()) {
									for (EdgeMatch empty : edgesWithoutPoints) {
										distance = (long) getPath(hopper, previousGPS.getEntry().getLat(),
												previousGPS.getEntry().getLon(),
												hopper.getGraphHopperStorage().getNodeAccess()
														.getLatitude(empty.getEdgeState().getAdjNode()),
												hopper.getGraphHopperStorage().getNodeAccess()
														.getLongitude(empty.getEdgeState().getAdjNode())).getBest()
																.getDistance();
										deltaTime = distance / speed;// time in milliseconds

										writeSegmentOutput(trajId, edgeId, timestamp2, timestamp + deltaTime, speed);
										timestamp2 = timestamp + (long) deltaTime;
										totalDelta += deltaTime;
									}
								}

								// last segment
								node = edgeMatch.getEdgeState().getBaseNode();
								hopper.getGraphHopperStorage().getNodeAccess().getLatitude(node);
								hopper.getGraphHopperStorage().getNodeAccess().getLongitude(node);

								distance = (long) getPath(hopper,
										hopper.getGraphHopperStorage().getNodeAccess().getLatitude(node),
										hopper.getGraphHopperStorage().getNodeAccess().getLongitude(node),
										gpsExtension.getEntry().getLat(), gpsExtension.getEntry().getLon()).getBest()
												.getDistance();
								deltaTime = distance / speed;// time in milliseconds

								totalDelta += deltaTime;
								writeSegmentOutput(trajId, edgeId, timestamp2, timestamp + deltaTime, speed);

							}

							edgesWithoutPoints.clear();
						}

						previousGPS = gpsExtension;
						previousEdge = edgeMatch;
					}
				}
			}

			writer.write(builder.toString());
			writer.flush();
		} finally {
			writer.close();
		}
	}

	private void writeSegmentOutput(Long trajId, int edgeId, double timestamp1, double timestamp2, double speed) {
		// TODO Auto-generated method stub

	}

	private void writeLine(MyGraphHopper hopper, StringBuilder builder, Entry<Long, MatchResult> mapEntry,
			GPXExtension first, EdgeMatch edgeMatch, GPXExtension next) {
		builder.append(mapEntry.getKey());
		builder.append(";");

		builder.append(next.getQueryResult().getSnappedPoint().getLat());
		builder.append(";");

		builder.append(next.getQueryResult().getSnappedPoint().getLon());
		builder.append(";");

		builder.append(next.getEntry().getTime());
		builder.append(";");

		int edge = edgeMatch.getEdgeState().getEdge();
		builder.append(edge);
		builder.append(";");

		builder.append(hopper.getOSMWay(edge));
		builder.append("\n");
	}

	private double getSpeedMS(MyGraphHopper hopper, GPXExtension from, GPXExtension to) {
		GHResponse res = getPath(hopper, from, to);
		return res.getBest().getDistance() / (to.getEntry().getTime() - from.getEntry().getTime())*1000;
	}

	private GHResponse getPath(MyGraphHopper hopper, GPXExtension from, GPXExtension to) {
		GHRequest req = new GHRequest(from.getQueryResult().getSnappedPoint().getLat(),
				from.getQueryResult().getSnappedPoint().getLon(), to.getQueryResult().getSnappedPoint().getLat(),
				to.getQueryResult().getSnappedPoint().getLon());
		req.setVehicle("car").setAlgorithm("dijkstra");
		return hopper.route(req);
	}

	private GHResponse getPath(MyGraphHopper hopper, double latFrom, double lonFrom, double latTo, double lonTo) {
		GHRequest req = new GHRequest(latFrom, lonFrom, latTo, lonTo);
		req.setVehicle("car").setAlgorithm("dijkstra");
		return hopper.route(req);
	}

	public static void main(String[] args) {
		MyGraphHopper hopper = new MyGraphHopper();
		hopper.setDataReaderFile("/Users/liviaalmada/Documents/map_matching/osm-fortaleza.osm");
		hopper.setGraphHopperLocation("/Users/liviaalmada/Documents/map_matching/");
		CarFlagEncoder encoder = new CarFlagEncoder();
		hopper.setEncodingManager(new EncodingManager(encoder));
		hopper.getCHFactoryDecorator().setEnabled(false);
		hopper.importOrLoad();

		// create MapMatching object, can and should be shared accross threads
		String algorithm = Parameters.Algorithms.DIJKSTRA_BI;
		Weighting weighting = new FastestWeighting(encoder);
		AlgorithmOptions algoOptions = new AlgorithmOptions(algorithm, weighting);
		MapMatching mapMatching = new MapMatching(hopper, algoOptions);
		try {

			Map<Long, List<GPXEntry>> mapIdToTrajectories = TrajectoryReader
					.readFromFile("/Users/liviaalmada/Documents/map_matching/taxi_hot_10_5min_ordered.csv");
			Map<Long, MatchResult> mapIdToMatchResult = TrajectoryMapMatching.getInstance().runMapMatching(mapMatching,
					mapIdToTrajectories);
			TrajectoryMapMatching.getInstance().saveMapMatchingSegmentsWithoutEmptyEdges(mapIdToMatchResult, "teste5.csv",
					hopper);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
