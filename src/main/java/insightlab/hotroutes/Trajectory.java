package insightlab.hotroutes;

import java.util.List;

import com.graphhopper.util.GPXEntry;

public class Trajectory {
	
	private List<GPXEntry> points;
	private long trajId;
	private long taxiId;
	
	public Trajectory(long taxiId, long trajId, List<GPXEntry> points){
		this.trajId = trajId;
		this.taxiId = taxiId;
		this.points = points;
	}

	public List<GPXEntry> getPoints() {
		return points;
	}

	public void setPoints(List<GPXEntry> points) {
		this.points = points;
	}

	public long getTrajId() {
		return trajId;
	}

	public void setTrajId(long trajId) {
		this.trajId = trajId;
	}

	public long getTaxiId() {
		return taxiId;
	}

	public void setTaxiId(long taxiId) {
		this.taxiId = taxiId;
	}
}
