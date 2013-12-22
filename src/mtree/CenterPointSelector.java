package mtree;


import java.io.Serializable;
import java.util.List;


/**
 * A CenterPointSelector selects two keys from a collection of keys. The keys selected become the
 * centerPoint for new Map.Sphere objects when a SPHERE_OF_POINTS is split in two.
 *  
 * @author Jon Parker (jon.i.parker@gmail.com)
 * @param <K>
 */
interface CenterPointSelector<K> extends Serializable {

	/**
	 * @param keys - A List of Keys that needs to be split
	 * @param metric - The distance metric that measures distance between 2 keys
	 *
	 * @return - Two keys that will be used as the centerPoints for a two new Spheres
	 */
	Pair<K> selectNewCenterPoints(List<K> keys, DistanceMetric<K> metric);
}
