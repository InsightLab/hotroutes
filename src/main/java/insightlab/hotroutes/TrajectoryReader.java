package insightlab.hotroutes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.graphhopper.util.GPXEntry;

public class TrajectoryReader {

	private static final int LATITUDE_IDX = 2;
	private static final int TAXI_ID_IDX = 0;
	private static final String SEPARATOR = ";";
	private static final int LONGITUDE_IDX = 3;
	private static final int TIME_IDX = 4;
	private static final int TRAJ_IDX = 1;

	public static Map<Long,Trajectory> readFromFile(String filePath) throws IOException {
		FileReader fileReader = new FileReader(new File(filePath));
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		Map<Long, Trajectory> trajectoriesMap = new TreeMap<Long, Trajectory>();

		bufferedReader.readLine();// header
		String line = bufferedReader.readLine();
		long previousTraj = -1;
		long taxiId = -1;
		long trajId = -1;

		long time = -1;
		double latitude = -1;
		double longitude = -1;
		List<GPXEntry> pointsList = new ArrayList<GPXEntry>();

		while (line != null) {
			String[] split = line.split(SEPARATOR);
			taxiId = Long.parseLong(split[TAXI_ID_IDX]);
			trajId = Long.parseLong(split[TRAJ_IDX]);
			latitude = Double.parseDouble(split[LATITUDE_IDX]);
			longitude = Double.parseDouble(split[LONGITUDE_IDX]);
			time = Long.parseLong(split[TIME_IDX]);
			GPXEntry gpxEntry = new GPXEntry(latitude, longitude, time);

			if (trajId == -1) {
				previousTraj = Long.parseLong(split[TAXI_ID_IDX]);
			} else if (previousTraj != trajId) {
				trajectoriesMap.put(trajId, new Trajectory(taxiId,trajId,pointsList));
				previousTraj = -1;
				pointsList = new ArrayList<GPXEntry>();
			} 
			pointsList.add(gpxEntry);
			line = bufferedReader.readLine();
		}
		trajectoriesMap.put(trajId, new Trajectory(taxiId,trajId,pointsList));
		bufferedReader.close();
		return trajectoriesMap;

	}


	private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
				+ Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515 * 1.609344;
		if (unit == "M") {
			dist = dist * 1000;
		}

		return (dist);
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts decimal degrees to radians : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts radians to decimal degrees : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private static double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}

	public static void main(String[] args) {
		try {
			Map<Long, Trajectory> map = readFromFile("/Users/liviaalmada/Documents/map_matching/taxi_reduzido.csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
