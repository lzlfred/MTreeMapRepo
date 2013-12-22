package mtree;


import com.google.common.primitives.Doubles;


/**
 * Result objects are returned by the MTree when nearest neighbor or range searches are performed.
 *
 * @author Jon Parker (jon.i.parker@gmail.com)
 * @param <K>
 * @param <V>
 */
public class Result<K, V> implements Comparable<Result<K, V>> {

	final K key;

	final V value;

	final double distance;


	Result(K key, V value, double distance) {
		this.key = key;
		this.value = value;
		this.distance = distance;
	}


	public K key() {
		return this.key;
	}


	public V value() {
		return this.value;
	}


	public double distance() {
		return this.distance;
	}


	/**
	 * Results are sorted this way so the PriorityQueue used to collect the Results always has the
	 * Result with the k-th largest distance on top. This means the threshold for improving the
	 * k-nearest neighbor result is readily accessible.
	 */
	@Override
	public int compareTo(Result<K, V> other) {
		return Doubles.compare(other.distance, this.distance);
	}
}
