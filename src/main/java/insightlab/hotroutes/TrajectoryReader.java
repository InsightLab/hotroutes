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

	public static Map<Long, List<GPXEntry>> readFromFile(String filePath) throws IOException {
		FileReader fileReader = new FileReader(new File(filePath));
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		Map<Long, List<GPXEntry>> trajectoriesMap = new TreeMap<Long, List<GPXEntry>>();

		bufferedReader.readLine();//header
		String line = bufferedReader.readLine();
		long previousTaxiId = -1;
		long taxiId = -1;
		long time;
		double latitude;
		double longitude;
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

			} else if (previousTaxiId != taxiId) {
				trajectoriesMap.put(previousTaxiId, pointsList);
				previousTaxiId = Long.parseLong(split[TAXI_ID_IDX]);
				pointsList = new ArrayList<GPXEntry>();

			}
			pointsList.add(gpxEntry);

			line = bufferedReader.readLine();
		}
		trajectoriesMap.put(previousTaxiId, pointsList);
		bufferedReader.close();
		return trajectoriesMap;

	}

	public static void main(String[] args) {
		try {
			Map<Long, List<GPXEntry>> map = readFromFile("/Users/liviaalmada/Documents/map_matching/taxi_reduzido.csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
