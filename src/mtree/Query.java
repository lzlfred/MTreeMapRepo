package mtree;


import java.util.Collection;
import java.util.Map;
import java.util.PriorityQueue;
import mtree.MTreeMap.Sphere;


/**
 * Query objects collect Key+Values pairs that are close to the "searchKey". Queries can be used to
 * find the "k-nearest neighbors" and "all neighbors within range r".
 *
 * Note: It maybe be possible to speed up queries (slightly??) by adding a second PriorityQueue that
 * processes Spheres in order of which spheres are closest to the searchKey.
 *
 * @author Jon Parker (jon.i.parker@gmail.com)
 * @param <K>
 * @param <V>
 */
class Query<K, V> {

	private enum QueryType {

		K_NN_QUERY, RANGE_QUERY;

	}

	private final DistanceMetric<K> metric;

	private final QueryType type;

	private final K searchKey;

	private final int maxNumResults; //only used for kNN searches

	private final double fixedRadius; //only used for range searches

	private final PriorityQueue<Result<K, V>> queue;


	/**
	 * Create a kNN search query.
	 *
	 * @param searchKey - Search for this
	 * @param maxNumResults - The "k" in k-Nearest-Neighbors
	 * @param metric - The distance metric used to determine how far objects are
	 */
	Query(K searchKey, int maxNumResults, DistanceMetric<K> metric) {
		this.metric = metric;
		this.type = QueryType.K_NN_QUERY;
		this.searchKey = searchKey;
		this.maxNumResults = maxNumResults;
		this.fixedRadius = Double.POSITIVE_INFINITY;
		this.queue = new PriorityQueue<>();
	}


	/**
	 * Create a range query that returns all entries within range
	 *
	 * @param searchKey - Search for this
	 * @param metric - The distance metric used to determine how far objects are
	 * @param range - Include results within this distance
	 */
	Query(K searchKey, DistanceMetric<K> metric, double range) {
		this.metric = metric;
		this.type = QueryType.RANGE_QUERY;
		this.searchKey = searchKey;
		this.maxNumResults = Integer.MAX_VALUE;
		this.fixedRadius = range;
		this.queue = new PriorityQueue<>();
	}


	void startQuery(MTreeMap.Sphere root) {
		searchInside(root);
	}


	/**
	 * Attempt to improve the current query results by look within the submitted Sphere and its
	 * children.
	 */
	private void searchInside(MTreeMap.Sphere inputSphere) {

		if (!this.overlapsWith(inputSphere)) {
			//ignore submission (and all its sub-trees) because it cannot improve the current result
			return;
		}

		//The inputSphere intersects the "query sphere", 
		//Thus entries inside the inputSphere may improve the current query


		if (inputSphere.isSphereOfPoints()) {

			for (Object object : inputSphere.points()) {

				Map.Entry<K, V> entry = (Map.Entry<K, V>) object;

				Result r = new Result(entry.getKey(),
									  entry.getValue(),
									  metric.distanceBtw(searchKey, entry.getKey()));

				if (r.distance <= this.radius()) {

					this.queue.offer(r);

					//enforce the "k" in kNN search
					if (queue.size() > this.maxNumResults) {
						//if too big, remove the worst result
						queue.poll();
					}
				}
			}

			return; //done
		}


		if (inputSphere.isSphereOfSpheres()) {

			Pair<MTreeMap.Sphere> childSpheres = inputSphere.spheres();

			double firstDist = metric.distanceBtw(
					searchKey,
					(K) childSpheres.first().centerPoint);

			double secondDist = metric.distanceBtw(
					searchKey,
					(K) childSpheres.second().centerPoint);

			//submit the closest sphere first to reduce work (because we'll be more likley to skip it)
			if (firstDist < secondDist) {
				this.searchInside(childSpheres.first());
				this.searchInside(childSpheres.second());
			} else {
				this.searchInside(childSpheres.second());
				this.searchInside(childSpheres.first());
			}

			return; //done
		}

		throw new AssertionError("A Sphere should contain points or spheres");
	}


	/** @return - True when the "query sphere" and this sphere overlap. */
	private boolean overlapsWith(Sphere s) {

		double distance = metric.distanceBtw((K) s.centerPoint, this.searchKey);
		double overlap = s.radius() + this.radius() - distance;

		return (overlap >= 0);
	}


	/**
	 * @return - The "inclusion radius" based on the type of query being executed and the quality
	 * of the current results (so we can avoid processing sphere that cannot contain better results)
	 */
	private double radius() {

		if (type == QueryType.K_NN_QUERY) {
			if (queue.size() < maxNumResults) {
				//radius is still large because we haven't found "k" results yet
				return Double.POSITIVE_INFINITY;
			} else {
				return queue.peek().distance; //must beat this to improve
			}
		}

		if (type == QueryType.RANGE_QUERY) {
			return this.fixedRadius;  //includes everything within this radius
		}

		throw new AssertionError("Should never get here");
	}


	Collection<Result<K, V>> results() {
		return queue;
	}
}
