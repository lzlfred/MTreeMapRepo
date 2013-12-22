package mtree;


import java.io.Serializable;


/**
 * Represent a pair of objects.
 *
 * @author Jon Parker (jon.i.parker@gmail.com)
 * @param <K>
 */
class Pair<K> implements Serializable {

	private final K item1;

	private final K item2;


	Pair(K item1, K item2) {
		this.item1 = item1;
		this.item2 = item2;
	}


	K first() {
		return item1;
	}


	K second() {
		return item2;
	}
}
