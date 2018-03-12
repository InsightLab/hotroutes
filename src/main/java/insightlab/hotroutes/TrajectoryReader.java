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

	private static final int LATITUDE_IDX = 1;
	private static final int TAXI_ID_IDX = 0;
	private static final String SEPARATOR = ";";
	private static final int LONGITUDE_IDX = 2;
	private static final int TIME_IDX = 3;
	private static final int TIME_INTERVAL = 300000;// MILLISECONDS BETWEEN DISTINC TRIPS
	private static long traj_id = 1;

	public static Map<Long, List<GPXEntry>> readFromFile(String filePath) throws IOException {
		FileReader fileReader = new FileReader(new File(filePath));
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		Map<Long, List<GPXEntry>> trajectoriesMap = new TreeMap<Long, List<GPXEntry>>();

		bufferedReader.readLine();// header
		String line = bufferedReader.readLine();
		long previousTaxiId = -1;
		long taxiId = -1;

		long time, previousTime = -1;
		double latitude, prevLat = -1;
		double longitude, prevLon = -1;
		List<GPXEntry> pointsList = new ArrayList<GPXEntry>();

		while (line != null) {
			String[] split = line.split(SEPARATOR);

			taxiId = Long.parseLong(split[TAXI_ID_IDX]);
			latitude = Double.parseDouble(split[LATITUDE_IDX]);
			longitude = Double.parseDouble(split[LONGITUDE_IDX]);
			time = Long.parseLong(split[TIME_IDX]);
			GPXEntry gpxEntry = new GPXEntry(latitude, longitude, time);

			if (previousTaxiId == -1) {
				previousTaxiId = Long.parseLong(split[TAXI_ID_IDX]);
				previousTime = Long.parseLong(split[TIME_IDX]);

			} else if (previousTaxiId != taxiId || time - previousTime > TIME_INTERVAL) {
				//refine(pointsList);
				trajectoriesMap.put(traj_id, pointsList);
				previousTaxiId = -1;
				pointsList = new ArrayList<GPXEntry>();
				traj_id++;
			} else {
				previousTime = time;
			}
			pointsList.add(gpxEntry);

			line = bufferedReader.readLine();
		}
		trajectoriesMap.put(traj_id, pointsList);
		bufferedReader.close();
		return trajectoriesMap;

	}

	private static void refine(List<GPXEntry> pointsList) {
		for (int i = 0; i < pointsList.size(); i++) {
			int count = 0;
			GPXEntry gpxEntry = pointsList.get(i);
			for (int j = i + 1; j < pointsList.size(); j++) {
				count = testNext(pointsList, j, count, gpxEntry);
			}
			if (count >= 10) {
				pointsList.remove(i);
				i--;
			}
		}

	}

	private static int testNext(List<GPXEntry> pointsList, int i, int count, GPXEntry gpxEntry) {
		if (i + 1 < pointsList.size()) {
			GPXEntry gpxEntry2 = pointsList.get(i);
			// distances com o tempo próximo
			if (distance(gpxEntry.lat, gpxEntry.lon, gpxEntry2.lat, gpxEntry2.lon, "M") > 350
					&& Math.abs(gpxEntry.getTime() - gpxEntry2.getTime()) < 10000) {
				count++;
			}else if(distance(gpxEntry.lat, gpxEntry.lon, gpxEntry2.lat, gpxEntry2.lon, "M") < 1
					&& Math.abs(gpxEntry.getTime() - gpxEntry2.getTime()) > 60000) {
				count++;
			}
		}
		return count;
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
			Map<Long, List<GPXEntry>> map = readFromFile("/Users/liviaalmada/Documents/map_matching/taxi_reduzido.csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
