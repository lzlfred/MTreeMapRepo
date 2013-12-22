package demo;


import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import mtree.DistanceMetric;
import mtree.MTreeMap;
import mtree.Result;


public class MTreeMapDemo {

	/** How many points should we put in the data-structure?. */
	private static final int NUM_POINTS = 300;

	/** This DistanceMetric computes the distance between to (x,y) points. */
	static class PointMetric implements DistanceMetric<Point2D> {

		@Override
		public double distanceBtw(Point2D item1, Point2D item2) {
			return item1.distance(item2);
		}
	}


	public static void main(String[] args) {

		MTreeMap<Point2D, Integer> mTree = new MTreeMap(new PointMetric());

		Collection<Point2D> points = makePoints();

		loadTree(points, mTree);

		System.out.println("Final Loaded Size :: " + mTree.size());
		
		examineEntries(mTree);

		reloadTree(points, mTree);

		performKNNsearches(mTree);
		performRangeSearches(mTree);

		unload(points, mTree);
		
		System.out.println("Final Unloaded Size :: " + mTree.size());
	}


	/**
	 * Load an MTreeMap with random points. The first point loaded is paired with the Integer value
	 * 0. The second point loaded is pair with the Integer value 1. And so on..
	 */
	private static void loadTree(Collection<Point2D> points, MTreeMap<Point2D, Integer> mTree) {
		System.out.println("Initial Load....");
		int counter = 0;
		for (Point2D point : points) {
			mTree.put(point, counter);
			System.out.println("  " + counter + " at " + point.toString());
			counter++;
		}
	}
	
	
	/**
	 * Look at the entries in the tree, confirm that they match the points that were just placed
	 * inside.
	 */
	private static void examineEntries(MTreeMap<Point2D, Integer> mTree) {
		
		System.out.println("Examining Entries...");
		
		for (Map.Entry<Point2D, Integer> entry : mTree.entrySet()) {
			System.out.println("  entry " + entry.getValue() + " at " + entry.getKey().toString());
		}
	}


	/**
	 * Reload the MTreeMap with the same random points. We want the "prior value" printed to go 0, 1,
	 * 2, ..." This indicates that the proper prior value is being evicted when the same Key is
	 * reinserted.
	 */
	private static void reloadTree(Collection<Point2D> points, MTreeMap<Point2D, Integer> mTree) {
		int counter = 0;
		for (Point2D point : points) {
			int priorValue = mTree.put(point, counter);
			counter++;
			System.out.println("prior value was :: " + priorValue);
		}
	}


	/** Perform some K-Nearest-Neighbor searches.  Print the Results. */
	private static void performKNNsearches(MTreeMap<Point2D, Integer> mTree) {

		Collection<Point2D> points = makePoints();

		int k = 3;
		for (Point2D point : points) {
			Collection<Result<Point2D, Integer>> results = mTree.getNClosest(point, k);

			int counter = 0;
			for (Result<Point2D, Integer> result : results) {
				System.out.println(
						"Result " + counter + " :: " + result.value()
						+ " at distance :: " + result.distance());
				counter++;
			}
			System.out.println("");
		}
	}


	/** Perform some Range Searches. Print the Results. */
	private static void performRangeSearches(MTreeMap<Point2D, Integer> mTree) {

		Collection<Point2D> points = makePoints();

		double RANGE = 0.05;
		for (Point2D point : points) {
			Collection<Result<Point2D, Integer>> results = mTree.getAllWithinRange(point, RANGE);

			int counter = 0;
			for (Result<Point2D, Integer> result : results) {
				System.out.println(
						"Result " + counter + " :: " + result.value()
						+ " at distance :: " + result.distance());
				counter++;
			}
			System.out.println("");
		}
	}


	private static void unload(Collection<Point2D> points, MTreeMap<Point2D, Integer> mTree) {

		int counter = 0;
		for (Point2D point2D : points) {
			int value = mTree.remove(point2D);
			System.out.println("removing point " + counter + " retrieved :: " + value);
			counter++;
		}
		System.out.println("");
	}


	private static Collection<Point2D> makePoints() {

		Random rng = new Random(17L);

		LinkedList<Point2D> list = new LinkedList<>();
		for (int i = 0; i < NUM_POINTS; i++) {
			list.add(new Point2D.Double(rng.nextDouble(), rng.nextDouble()));
		}

		return list;
	}
}
