package mtree;


import java.io.Serializable;



public interface DistanceMetric<K> extends Serializable {

	/** 
	 * @return - The distance between the 2 objects in a Metric Space (this method must define a
	 * Metric Space).
	 */
	public double distanceBtw(K item1, K item2);
}
