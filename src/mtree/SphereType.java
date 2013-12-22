package mtree;


/** @author Jon Parker (jon.i.parker@gmail.com) */
enum SphereType {

	/**
	 * SPHERE_OF_POINTS contain a HashMap of key, value pairs. When a SPHERE_OF_POINTS is split in
	 * two the original SPHERE_OF_POINTS is converted to a SPHERE_OF_SPHERES containing two new
	 * SPHERE_OF_POINTS. Together these new SPHERE_OF_POINTS contain the collection of key, value
	 * pairs the original SPHERE_OF_POINTS previously held.
	 */
	SPHERE_OF_POINTS,
	/**
	 * A SPHERE_OF_SPHERES contains 2 spheres. These objects are used to "route" put/get requests.
	 */
	SPHERE_OF_SPHERES;

}