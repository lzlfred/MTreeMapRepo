MTreeMapRepo
============

This project contains a M-Tree based Map written in Java

A MTreeMap is a Map-like data-structure designed to efficiently support k-nearest-neighbor (kNN)
searches, range searches, as well as regular Map style put/get operations. To support kNN
searches a MTreeMap requires a Distance Metric object that defines the distance between any two
keys. The DistanceMetric should define a true Metric Space in which the following three
assumptions are true:

(1) d(x,y) >= 0
(2) d(x,y) = d(y,x)
(3) d(x,z) <= d(x,y) + d(y,z)
(4) d(x,y) = 0 if and only if x = y  (optional)

A MTreeMap is loosely based on the MTree introduced by Paolo Ciaccia, Marco Patella, and Pavel
Zezula in 1997 in the paper "M-tree An Efficient Access Method for Similarity Search in Metric
Spaces" from the Proceedings of the 23rd VLDB Conference.  The core differences are that this 
effort is a binary tree and that does not store previously computed distance computations.  
Ciaccia et al's MTree saves previously computed distances to more quickly find the appropriate 
branch of a multi-way tree to descend.  The code herein is far simplier because saving 
previously computed distance doesn't help (and thus isn't included) when searching a binary tree.

An MTreeMap is a binary search tree in which each node in the tree owns a sphere. That sphere's
radius is set to ensure that every Key beneath that node is located inside that node's sphere.
Therefore, the root node of an MTreeMap has a large radius because every Key contained in the
MTreeMap must fit inside its sphere. Sub-trees are associated with smaller spheres. Each sphere
has a centerpoint and radius whose values are used to route put/get requests, kNN searches, and
range searches.

When Keys are first added to an MTreeMap they are placed inside a SphereOfPoints. Eventually,
that Sphere will have too many entries and needs to be split. When this occurs the SphereOfPoints
becomes a SphereOfSpheres that owns 2 newly created SphereOfPoints. The 2 new SphereOfPoints
contain all the Key+Value pairs that the original "overfilled" SphereOfPoints contained. The
centerpoints of the 2 new SphereOfPoints are selected in order to reduce the overlapping volume
the 2 new spheres have with each other. Reducing this shared volume reduces the number of spheres
a search must visit.

When Key+Value pairs are removed from a MTreeMap they are removed, however, the fact that the key
was present in the MTreeMap may leave a permanent imprint on the MTreeMap. This occurs when the
Key was selected as the centerpoint for a new SphereOfPoints. In this case the Key is still used
to route get/put queries even though the Key is no longer associated with a Key+Value pair. Any
insertion of a Key can permanently reduce the query routing efficency of an MTreeMap.  This 
occurs when the key insertion forces a Sphere to increase its radius (which will not shrink upon
key removal).

Importantly, MTreeMaps have no automatic balancing mechanism. Therefore inserting Key+Value pairs
where the Keys vary in some predictable way is likely to produce a tree that is unbalanced. In
principal, tree balance could be maintained by rotating the Sphere nodes similar to how AVL and
Red-Black trees rotate nodes to maintain balance. The makeBalancedCopy() and rebalance() methods
are provided to combat tree imbalance, but these method are expensive and should be used
sparingly if possible.