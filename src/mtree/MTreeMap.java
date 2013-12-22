package mtree;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A MTreeMap is a Map-like data-structure designed to efficiently support k-nearest-neighbor (kNN)
 * searches, range searches, as well as regular Map style put/get operations. To support kNN
 * searches a MTreeMap requires a Distance Metric object that defines the distance between any two
 * keys. The DistanceMetric should define a true Metric Space in which the following four
 * assumptions are true:
 *
 * (1) d(x,y) >= 0
 * (2) d(x,y) = d(y,x)
 * (3) d(x,z) <= d(x,y) + d(y,z)
 * (4) d(x,y) = 0 if and only if x = y (optional)
 *
 * A MTreeMap is loosely based on the MTree introduced by Paolo Ciaccia, Marco Patella, and Pavel
 * Zezula in 1997 in the paper "M-tree An Efficient Access Method for Similarity Search in Metric
 * Spaces" from the Proceedings of the 23rd VLDB Conference.
 *
 * An MTreeMap is a binary search tree in which each node in the tree owns a sphere. That sphere's
 * radius is set to ensure that every Key beneath that node is located inside that node's sphere.
 * Therefore, the root node of an MTreeMap has a large radius because every Key contained in the
 * MTreeMap must fit inside its sphere. Sub-trees are associated with smaller spheres. Each sphere
 * has a centerpoint and radius whose values are used to route put/get requests, kNN searches, and
 * range searches.
 *
 * When Keys are first added to an MTreeMap they are placed inside a SphereOfPoints. Eventually,
 * that Sphere will have too many entries and needs to be split. When this occurs the SphereOfPoints
 * becomes a SphereOfSpheres that own 2 newly created SphereOfPoints. The 2 new SphereOfPoints
 * contain all the Key+Value pairs that the original "overfilled" SphereOfPoints contained. The
 * centerpoints of the 2 new SphereOfPoints are selected in order to reduce the overlapping volume
 * the 2 new spheres have with each other. Reducing this shared volume reduces the number of spheres
 * a search must visit.
 *
 * When Key+Value pairs are removed from a MTreeMap they are removed, however, the fact that the key
 * was present in the MTreeMap may leave a permanent imprint on the MTreeMap. This occurs when the
 * Key was selected as the centerpoint for a new SphereOfPoints. In this case the Key is still used
 * to route get/put queries even though the Key is no longer associated with a Key+Value pair. Any
 * insertion of a Key can permanently reduce the query routing efficency of an MTreeMap. This
 * occurs when the key insertion forces a Sphere to increase its radius (which will not shrink upon
 * key removal).
 *
 * Importantly, MTreeMaps have no automatic balancing mechanism. Therefore inserting Key+Value pairs
 * where the Keys vary in some predictable way is likely to produce a tree that is unbalanced. In
 * principal, tree balance could be maintained by rotating the Sphere nodes similar to how AVL and
 * Red-Black trees rotate nodes to maintain balance. The makeBalancedCopy() and rebalance() methods
 * are provided to combat tree imbalance, but these method are expensive and should be used
 * sparingly if possible.
 *
 *
 * @author Jon Parker (jon.i.parker@gmail.com)
 *
 * @param <K> - The Keys, these keys are stored in HashMaps, so their hashcode() and equals()
 * methods must be defined correctly.
 * @param <V> - The Values
 */
public class MTreeMap<K, V> implements Serializable {

	/** This controls how SPHERE_OF_POINTS are split. */
	private final CenterPointSelector<K> centerPointSelector;

	/** The distance metric governing the space of Keys (K). */
	private final DistanceMetric<K> metric;

	/** The root of this tree. */
	private Sphere root;

	private int entryCount = 0;

	/** Used to judge the efficiency of different centerPointSelectors. */
	private int sphereCount = 0;


	public MTreeMap(DistanceMetric<K> metric) {
		this.metric = metric;
		this.centerPointSelector = CenterPointSelectors.maxOfRandomSamples(); //seems like the best method
	}


	/**
	 * Associates the specified value with the specified key in this map. If the map previously
	 * contained a mapping for the key, the old value is replaced.
	 *
	 * @param key - key with which the specified value is to be associated
	 * @param value - value to be associated with the specified key
	 *
	 * @return the previous value associated with key, or null if there was no prior mapping for the
	 * key. (A null return can also indicate that the map previously associated null with key.)
	 */
	public V put(K key, V value) {

		if (key == null) {
			throw new IllegalArgumentException("Null Keys are not permited because they cannot be "
					+ "placed in the metrice space");
		}

		//delay building root until now because we don't have a key for the centerPoint until 
		//the first use of put(K key)
		if (this.root == null) {
			this.root = new Sphere(key);
		}

		V prior = root.put(key, value);

		//if key was new, increase entry count
		if (prior == null) {
			entryCount++;
		}

		return prior;
	}


	public int size() {
		return this.entryCount;
	}


	public boolean isEmpty() {
		return this.entryCount == 0;
	}


	public boolean containsKey(K key) {

		if (key == null) {
			throw new IllegalArgumentException("Null Keys are not permited.");
		}

		return root.contains(key);
	}


	/** Search for an exact key match. */
	public V get(K key) {

		if (key == null) {
			throw new IllegalArgumentException("Null Keys are not permited.");
		}

		return root.get(key);
	}


	public Result<K, V> getClosest(K key) {

		if (key == null) {
			throw new IllegalArgumentException("Null Keys are not permited.");
		}

		//nothing to retrieve...
		if (this.isEmpty()) {
			return null;
		}

		Query q = new Query(key, 1, metric);
		q.startQuery(root);

		Collection<Result<K, V>> results = q.results();
		ArrayList<Result<K, V>> list = new ArrayList<>(results);
		return list.get(0);
	}


	public Collection<Result<K, V>> getNClosest(K key, int n) {

		if (key == null) {
			throw new IllegalArgumentException("Null Keys are not permited.");
		}

		if (n < 1) {
			throw new IllegalArgumentException("n must be at least 1");
		}

		//nothing to retrieve...
		if (this.isEmpty()) {
			return null;
		}

		Query q = new Query(key, n, metric);
		q.startQuery(root);

		ArrayList<Result<K, V>> list = new ArrayList<>(q.results());
		Collections.sort(list);

		return list;
	}


	public Collection<Result<K, V>> getAllWithinRange(K key, double range) {

		if (key == null) {
			throw new IllegalArgumentException("Null Keys are not permited.");
		}

		if (range <= 0) {
			throw new IllegalArgumentException("The range must be strictly positive " + range);
		}

		//nothing to retrieve...
		if (this.isEmpty()) {
			return null;
		}

		Query q = new Query(key, metric, range);
		q.startQuery(root);

		ArrayList<Result<K, V>> list = new ArrayList<>(q.results());
		Collections.sort(list);

		return list;
	}


	public V remove(K key) {

		if (key == null) {
			throw new IllegalArgumentException("Null Keys are not permited.");
		}

		V removed = root.remove(key);

		//if successful, reduce entry count
		if (removed != null) {
			entryCount--;
		}
		return removed;
	}


	public void clear() {
		this.root = null;
		this.entryCount = 0;
		this.sphereCount = 0;
	}


	public Set<Map.Entry<K, V>> entrySet() {
		return root.entrySet();
	}


	/**
	 * @return - The number of different spheres used to contain this data. This number can be used
	 * to: (1) evaluate the quality of the CenterPointSelector being used and (2) detect when the
	 * tree is unbalanced.
	 */
	public int sphereCount() {
		return this.sphereCount;
	}


	/**
	 * Build an entirely new version of this MTreeMap. The newly built MTreeMap should be relatively
	 * well balanced because the Key+Value pairs from "this" MTreeMap are inserted into the "new"
	 * MTreeMap in random order.
	 *
	 * @return - A new version of this MTreeMap that should be well balanced
	 */
	public MTreeMap<K, V> makeBalancedCopy() {

		List<Map.Entry<K, V>> listOfEntries = Lists.newArrayList(this.entrySet());
		Collections.shuffle(listOfEntries);

		MTreeMap<K, V> newMap = new MTreeMap(metric);
		for (Map.Entry<K, V> entry : listOfEntries) {
			newMap.put(entry.getKey(), entry.getValue());
		}

		if (this.entryCount != newMap.entryCount) {
			throw new AssertionError("The rebalancing process changed the number of entries");
		}

		return newMap;
	}


	/** Rebuild this MTreeMap using makeBalancedCopy(). */
	public void rebalance() {

		MTreeMap<K, V> newMap = makeBalancedCopy();
		this.root = newMap.root;
		this.sphereCount = newMap.sphereCount;
		this.entryCount = newMap.entryCount;
	}

	/**
	 * A Sphere represents a Sphere in the Metric Space defined by the distance metric of this
	 * MTreeMap. The radius of a sphere is increased so that every Sphere contains every Key that
	 * can be found either directly inside it or inside one of its child Sphere.
	 */
	class Sphere implements Serializable {

		private static final int MAX_SIZE = 20;

		final K centerPoint;

		private double radius;

		private SphereType type;

		/** This Map is used when SphereType == SPHERE_OF_POINTS. */
		private Map<K, V> entries;

		/** This list is used when SphereType == SPHERE_OF_SPHERES. */
		private Pair<Sphere> childSpheres;


		/** Create a SphereOfPoints centered around this key. */
		Sphere(K key) {
			this.type = SphereType.SPHERE_OF_POINTS;
			this.centerPoint = key;
			this.entries = new HashMap<>();
			this.childSpheres = null;
			sphereCount++;
		}


		double radius() {
			return this.radius;
		}


		boolean isSphereOfPoints() {
			return this.type == SphereType.SPHERE_OF_POINTS;
		}


		Set<Map.Entry<K, V>> points() {
			return entries.entrySet();
		}


		boolean isSphereOfSpheres() {
			return this.type == SphereType.SPHERE_OF_SPHERES;
		}


		Pair<Sphere> spheres() {
			return childSpheres;
		}


		V put(K key, V value) {

			if (isFull()) {
				//split this node
				Pair<Sphere> newNodes = this.splitSphereOfPoints();

				//"promote" this SPHERE_OF_POINTS  to a SPHERE_OF_SPHERES
				//null out the list of entries
				//use the a pair of Spheres in its place
				this.type = SphereType.SPHERE_OF_SPHERES;
				this.entries = null;
				this.childSpheres = newNodes;

				//recall put because this node should no longer be full
				return this.put(key, value);
			}

			//update radius is necessary
			this.radius = Math.max(
					radius,
					metric.distanceBtw(this.centerPoint, key));

			if (isSphereOfPoints()) {
				return this.entries.put(key, value);
			}

			if (isSphereOfSpheres()) {
				Sphere child = findClosestChildSphere(key);
				return child.put(key, value);
			}

			throw new AssertionError("Should never get here, all SphereTypes covered");
		}


		V remove(K key) {
			if (this.isSphereOfPoints()) {
				return this.entries.remove(key);
			}

			if (this.isSphereOfSpheres()) {
				return findClosestChildSphere(key).remove(key);
			}

			throw new AssertionError("Should never get here, all SphereTypes covered");
		}


		boolean contains(K key) {
			if (this.isSphereOfPoints()) {
				return this.entries.containsKey(key);
			}

			if (this.isSphereOfSpheres()) {
				return findClosestChildSphere(key).contains(key);
			}

			throw new AssertionError("Should never get here, all SphereTypes covered");
		}


		V get(K key) {
			if (this.isSphereOfPoints()) {
				return this.entries.get(key);
			}

			if (this.isSphereOfSpheres()) {
				return findClosestChildSphere(key).get(key);
			}

			throw new AssertionError("Should never get here, all SphereTypes covered");
		}


		private boolean isFull() {
			return (this.type == SphereType.SPHERE_OF_POINTS) && (this.entries.size() >= MAX_SIZE);
		}


		/** @return - The child whose centerPoint is closest to the piece of data we are inserting. */
		private Sphere findClosestChildSphere(K key) {

			double firstDist = metric.distanceBtw(key, this.childSpheres.first().centerPoint);
			double secondDist = metric.distanceBtw(key, this.childSpheres.second().centerPoint);

			if (firstDist < secondDist) {
				return childSpheres.first();
			} else {
				return childSpheres.second();
			}
		}


		/** Split a SphereOfPoints into two SphereOfPoints. */
		private Pair<Sphere> splitSphereOfPoints() {

			if (this.type != SphereType.SPHERE_OF_POINTS) {
				throw new IllegalStateException("Only SPHERE_OF_POINTS should be split");
			}

			Pair<K> centers = pickCentersForNewSpheres();

			Sphere part1 = new Sphere(centers.first());
			Sphere part2 = new Sphere(centers.second());

			moveEntriesToParts(part1, part2);

			return new Pair(part1, part2);
		}


		private Pair<K> pickCentersForNewSpheres() {
			return centerPointSelector.selectNewCenterPoints(
					new ArrayList(entries.keySet()),
					metric);
		}


		/** Move the entries from this Sphere to the new Sphere. */
		private void moveEntriesToParts(Sphere part1, Sphere part2) {
			//push the contents of this.children to either part1 or part2
			for (Map.Entry<K, V> entry : entries.entrySet()) {
				addToBestOf(part1, part2, entry);
			}
		}


		private void addToBestOf(Sphere node1, Sphere node2, Map.Entry<K, V> entry) {

			double distanceTo1 = metric.distanceBtw(entry.getKey(), node1.centerPoint);
			double distanceTo2 = metric.distanceBtw(entry.getKey(), node2.centerPoint);

			if (distanceTo1 < distanceTo2) {
				node1.put(entry.getKey(), entry.getValue());
			} else {
				node2.put(entry.getKey(), entry.getValue());
			}
		}


		Set<Map.Entry<K, V>> entrySet() {

			if (this.isSphereOfPoints()) {
				return this.entries.entrySet();
			}

			if (this.isSphereOfSpheres()) {
				return Sets.union(
						this.childSpheres.first().entrySet(),
						this.childSpheres.second().entrySet());
			}

			throw new AssertionError("Should never get here, all SphereTypes covered");
		}
	}
}
