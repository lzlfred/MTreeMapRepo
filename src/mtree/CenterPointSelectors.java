package mtree;


import java.util.List;
import java.util.Random;


/**
 * This class provides access to two implementations of a CenterPointSelector. At this time it
 * appears that the CenterPointSelector returned by "maxOfRandomSamples()" is best because it
 * generates relatively few spheres when used (i.e. spheres aren't wasted). It also requires
 * relatively few distance computations.
 *
 * @author Jon Parker (jon.i.parker@gmail.com)
 */
class CenterPointSelectors {

	//implementation 1
	static CenterPointSelector singleRandomSample() {
		return new RandomCenterSelector();
	}


	//implementation 2
	static CenterPointSelector maxOfRandomSamples() {
		return new RandomizedMaxDistanceSelector();
	}

	/** This Selector picks 2 random Keys from a List of Entries provided. */
	private static class RandomCenterSelector<K> implements CenterPointSelector<K> {

		Random rng = new Random(17L);


		/**
		 * @param keys - A List of Keys that needs to be split
		 * @param metric - The distance metric that measures distance between 2 keys
		 *
		 * @return - Two keys that will be used as the centerPoints for a two new Spheres. These
		 * keys are randomly selected from the list of entries.
		 */
		@Override
		public Pair<K> selectNewCenterPoints(List<K> keys, DistanceMetric<K> metric) {

			//pick 2 unique random index values..
			int size = keys.size();
			int index1 = rng.nextInt(size);
			int index2 = rng.nextInt(size);
			while (index1 == index2) {
				index2 = rng.nextInt(size);
			}

			//whose number indicate a promoted center..		
			return new Pair(keys.get(index1), keys.get(index2));
		}
	}

	/**
	 * This Selector picks multiple random Keys Pairs from a List of Keys provided and returns
	 * the pair with the largest distance between them. This pair of Keys should generate 2 child
	 * spheres whose volumes overlap as little as possible.
	 */
	private static class RandomizedMaxDistanceSelector<K> implements CenterPointSelector<K> {

		Random rng = new Random(17L);


		/**
		 * @param keys - A List of Keys that needs to be split
		 * @param metric - The distance metric that measures distance between 2 keys
		 *
		 * @return - Two keys that will be used as the centerPoints for a two new Spheres. These
		 * keys are randomly selected from the list of entries.
		 */
		@Override
		public Pair<K> selectNewCenterPoints(List<K> keys, DistanceMetric<K> metric) {

			int numPairsToDraw = (int) Math.sqrt(keys.size()); //sqrt strikes a good balance

			Pair<K> bestPair = selectRandomPairOfKeys(keys);
			numPairsToDraw--;
			double biggestDistance = metric.distanceBtw(bestPair.first(), bestPair.second());

			while (numPairsToDraw > 0) {
				Pair<K> newPair = selectRandomPairOfKeys(keys);
				numPairsToDraw--;
				double newDistance = metric.distanceBtw(newPair.first(), newPair.second());

				if (newDistance > biggestDistance) {
					bestPair = newPair;
				}
			}

			return bestPair;
		}


		private Pair<K> selectRandomPairOfKeys(List<K> keys) {

			//pick 2 unique random index values..
			int size = keys.size();
			int index1 = rng.nextInt(size);
			int index2 = rng.nextInt(size);
			while (index1 == index2) {
				index2 = rng.nextInt(size);
			}

			//return the corresponding keys			
			return new Pair(keys.get(index1), keys.get(index2));
		}
	}
}
