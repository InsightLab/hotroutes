package insightlab.hotroutes;

import java.util.ArrayList;
import java.util.List;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.matching.EdgeMatch;
import com.graphhopper.matching.GPXExtension;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.FastestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Parameters;

public class RunTrajectoryMapMatchingExample {
	public static void main(String[] args) {
		// import OpenStreetMap data
		GraphHopper hopper = new GraphHopperOSM();
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

		// do the actual matching, get the GPX entries from a file or via stream
		List<GPXEntry> inputGPXEntries = new ArrayList<GPXEntry>();
		inputGPXEntries.add(new GPXEntry(-3.7961837, -38.4980866, 1449914400000L));
		inputGPXEntries.add(new GPXEntry(-3.7962901, -38.4983201, 1449914405000L));
		inputGPXEntries.add(new GPXEntry(-3.796449, -38.498629, 1449914410000L));
		inputGPXEntries.add(new GPXEntry(-3.7966118, -38.4989031, 1449914415000L));
		inputGPXEntries.add(new GPXEntry(-3.7966631, -38.4989969, 1449914420000L));
		inputGPXEntries.add(new GPXEntry(-3.7966631, -38.4989969, 1449914425000L));
		inputGPXEntries.add(new GPXEntry(-3.7966631, -38.4989969, 1449914425000L));
		inputGPXEntries.add(new GPXEntry(-3.7966311, -38.499129, 1449914430000L));
		inputGPXEntries.add(new GPXEntry(-3.7966778, -38.4992487, 1449914433000L));

		MatchResult mr = mapMatching.doWork(inputGPXEntries);

		// return GraphHopper edges with all associated GPX entries
		List<EdgeMatch> matches = mr.getEdgeMatches();

		for (EdgeMatch edgeMatch : matches) {
			System.out.println(edgeMatch);
			for (GPXExtension gpsExtension : edgeMatch.getGpxExtensions()) {
				System.out.println(gpsExtension.getEntry().getTime());
			}
		}

		GHRequest req = new GHRequest(-3.7962371940234454, -38.49806240187512, -3.796617753154485, -38.49890036321081)
				.setVehicle("car").setAlgorithm("dijkstra");

		GHResponse res = hopper.route(req);
		System.out.println(res.getBest());

		// now do something with the edges like storing the edgeIds or doing
		// fetchWayGeometry etc
		matches.get(0).getEdgeState();
	}
}
