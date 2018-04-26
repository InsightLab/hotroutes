package insightlab.hotroutes;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;

public class ShortestPathFortaleza {

	public static void main(String[] args) {
		MyGraphHopper hopper = new MyGraphHopper();
		hopper.setCHEnabled(false);
		hopper.setDataReaderFile("/Users/regis/tmp/fortaleza.osm.pbf");
		hopper.setGraphHopperLocation("/Users/regis/graphhopper/fortaleza");
		hopper.setMinNetworkSize(200, 200);  // VERY, VERY IMPORTANT!!!
		CarFlagEncoder encoder = new CarFlagEncoder();
		hopper.setEncodingManager(new EncodingManager(encoder));
		hopper.importOrLoad();
		
		GHRequest req = new GHRequest(-3.760212,-38.528806,-3.760266,-38.52931);
		req.setVehicle("car").
	        setWeighting("fastest").
	        setAlgorithm("dijkstra");
		
		GHResponse res = hopper.route(req);
		System.out.println("errors: " + res.getErrors());
		System.out.println("list size: " + res.getAll().size());
		
		for (PathWrapper pw : res.getAll()) {
			try {
				System.out.println("distance:" + pw.getDistance());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
