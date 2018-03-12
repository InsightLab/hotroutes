package insightlab.hotroutes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Parameters;

public class MapMatchingFortaleza {
	public static void main(String[] args) {
		// import OpenStreetMap data
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

		// read trajectories file and convert each one to a List of GPXEntry
		try {

			Map<Long, List<GPXEntry>> mapIdToTrajectories = TrajectoryReader
					.readFromFile("/Users/liviaalmada/Documents/map_matching/taxi_hot_10_60min.csv");
			Map<Long, MatchResult> mapIdToMatchResult = TrajectoryMapMatching.getInstance().runMapMatching(mapMatching, mapIdToTrajectories);
		
			TrajectoryMapMatching.getInstance().saveMapMatchingPoints(mapIdToMatchResult,
					"/Users/liviaalmada/Documents/map_matching/taxi_hot_10_60min_trips.csv", hopper);
			
			
			TrajectoryMapMatching.getInstance().saveMapMatchingEdgesSpeed(mapIdToMatchResult,
					"/Users/liviaalmada/Documents/map_matching/taxi_hot_10_60min_trips_speed.csv", hopper);
			

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
