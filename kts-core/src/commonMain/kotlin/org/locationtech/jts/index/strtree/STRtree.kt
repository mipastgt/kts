/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.index.strtree
import kotlin.math.ceil
import kotlin.math.sqrt

import org.locationtech.jts.util.PriorityQueue
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.ItemVisitor
import org.locationtech.jts.index.SpatialIndex
import org.locationtech.jts.util.Assert

/**
 * A query-only R-tree created using the Sort-Tile-Recursive (STR) algorithm.
 * For two-dimensional spatial data.
 *
 */
open class STRtree : AbstractSTRtree, SpatialIndex {

  class STRtreeNode(level: Int) : AbstractNode(level) {
    override fun computeBounds(): Any? {
      var bounds: Envelope? = null
      val i = getChildBoundables().iterator()
      while (i.hasNext()) {
        val childBoundable = i.next()
        if (bounds == null) {
          bounds = Envelope(childBoundable.getBounds() as Envelope)
        } else {
          bounds.expandToInclude(childBoundable.getBounds() as Envelope)
        }
      }
      return bounds
    }
  }

  /**
   * Constructs an STRtree with the default node capacity.
   */
  constructor() : this(DEFAULT_NODE_CAPACITY)

  /**
   * Constructs an STRtree with the given maximum number of child nodes that
   * a node may have.
   */
  constructor(nodeCapacity: Int) : super(nodeCapacity)

  /**
   * Constructs an STRtree with the given maximum number of child nodes that
   * a node may have, and the root that links to all other nodes
   */
  constructor(nodeCapacity: Int, root: STRtreeNode) : super(nodeCapacity, root)

  /**
   * Constructs an STRtree with the given maximum number of child nodes that
   * a node may have, and all leaf nodes in the tree
   */
  constructor(nodeCapacity: Int, itemBoundables: ArrayList<Boundable>) : super(nodeCapacity, itemBoundables)

  /**
   * Creates the parent level for the given child level. First, orders the items
   * by the x-values of the midpoints, and groups them into vertical slices.
   */
  override fun createParentBoundables(childBoundables: MutableList<Boundable>, newLevel: Int): MutableList<Boundable> {
    Assert.isTrue(!childBoundables.isEmpty())
    val minLeafCount = ceil(childBoundables.size / getNodeCapacity().toDouble()).toInt()
    val sortedChildBoundables = ArrayList(childBoundables)
    sortedChildBoundables.sortWith(xComparator)
    val verticalSlices = verticalSlices(sortedChildBoundables, ceil(sqrt(minLeafCount.toDouble())).toInt())
    return createParentBoundablesFromVerticalSlices(verticalSlices, newLevel)
  }

  private fun createParentBoundablesFromVerticalSlices(verticalSlices: Array<MutableList<Boundable>>, newLevel: Int): MutableList<Boundable> {
    Assert.isTrue(verticalSlices.isNotEmpty())
    val parentBoundables: MutableList<Boundable> = ArrayList()
    for (i in verticalSlices.indices) {
      parentBoundables.addAll(createParentBoundablesFromVerticalSlice(verticalSlices[i], newLevel))
    }
    return parentBoundables
  }

  protected open fun createParentBoundablesFromVerticalSlice(childBoundables: MutableList<Boundable>, newLevel: Int): MutableList<Boundable> {
    return super.createParentBoundables(childBoundables, newLevel)
  }

  /**
   * @param childBoundables Must be sorted by the x-value of the envelope midpoints
   */
  protected open fun verticalSlices(childBoundables: MutableList<Boundable>, sliceCount: Int): Array<MutableList<Boundable>> {
    val sliceCapacity = ceil(childBoundables.size / sliceCount.toDouble()).toInt()
    val slices = arrayOfNulls<MutableList<Boundable>>(sliceCount)
    val i = childBoundables.iterator()
    for (j in 0 until sliceCount) {
      val slice = ArrayList<Boundable>()
      slices[j] = slice
      var boundablesAddedToSlice = 0
      while (i.hasNext() && boundablesAddedToSlice < sliceCapacity) {
        val childBoundable = i.next()
        slice.add(childBoundable)
        boundablesAddedToSlice++
      }
    }
    @Suppress("UNCHECKED_CAST")
    return slices as Array<MutableList<Boundable>>
  }

  override fun createNode(level: Int): AbstractNode {
    return STRtreeNode(level)
  }

  override fun getIntersectsOp(): IntersectsOp {
    return intersectsOp
  }

  /**
   * Inserts an item having the given bounds into the tree.
   */
  override fun insert(itemEnv: Envelope, item: Any?) {
    if (itemEnv.isNull()) {
      return
    }
    super.insert(itemEnv, item)
  }

  /**
   * Returns items whose bounds intersect the given envelope.
   */
  override fun query(searchEnv: Envelope?): MutableList<*> {
    return super.query(searchEnv as Any?)
  }

  /**
   * Returns items whose bounds intersect the given envelope.
   */
  override fun query(searchEnv: Envelope?, visitor: ItemVisitor) {
    super.query(searchEnv as Any?, visitor)
  }

  /**
   * Removes a single item from the tree.
   *
   * @param itemEnv the Envelope of the item to remove
   * @param item the item to remove
   * @return `true` if the item was found
   */
  override fun remove(itemEnv: Envelope, item: Any?): Boolean {
    return super.remove(itemEnv as Any?, item)
  }

  /**
   * Returns the number of items in the tree.
   *
   * @return the number of items in the tree
   */
  public override fun size(): Int {
    return super.size()
  }

  /**
   * Returns the number of levels in the tree.
   *
   * @return the number of levels in the tree
   */
  public override fun depth(): Int {
    return super.depth()
  }

  override fun getComparator(): Comparator<Boundable> {
    return yComparator
  }

  /**
   * Finds the two nearest items in the tree,
   * using [ItemDistance] as the distance metric.
   *
   * @param itemDist a distance metric applicable to the items in this tree
   * @return the pair of the nearest items
   *    or `null` if the tree is empty
   */
  fun nearestNeighbour(itemDist: ItemDistance): Array<Any?>? {
    if (isEmpty()) return null

    // if tree has only one item this will return null
    val bp = BoundablePair(this.getRoot(), this.getRoot(), itemDist)
    return nearestNeighbour(bp)
  }

  /**
   * Finds the item in this tree which is nearest to the given item,
   * using [ItemDistance] as the distance metric.
   *
   * @return the nearest item in this tree
   *    or `null` if the tree is empty
   */
  fun nearestNeighbour(env: Envelope, item: Any?, itemDist: ItemDistance): Any? {
    if (isEmpty()) return null

    val bnd: Boundable = ItemBoundable(env, item)
    val bp = BoundablePair(this.getRoot(), bnd, itemDist)
    return nearestNeighbour(bp)!![0]
  }

  /**
   * Finds the two nearest items from this tree
   * and another tree,
   * using [ItemDistance] as the distance metric.
   *
   * @return the pair of the nearest items, one from each tree
   *    or `null` if no pair of distinct items can be found
   */
  fun nearestNeighbour(tree: STRtree, itemDist: ItemDistance): Array<Any?>? {
    if (isEmpty() || tree.isEmpty()) return null
    val bp = BoundablePair(this.getRoot(), tree.getRoot(), itemDist)
    return nearestNeighbour(bp)
  }

  private fun nearestNeighbour(initBndPair: BoundablePair): Array<Any?>? {
    var distanceLowerBound = Double.POSITIVE_INFINITY
    var minPair: BoundablePair? = null

    // initialize search queue
    val priQ = PriorityQueue<BoundablePair>()
    priQ.add(initBndPair)

    while (!priQ.isEmpty() && distanceLowerBound > 0.0) {
      // pop head of queue and expand one side of pair
      val bndPair = priQ.poll()!!
      val pairDistance = bndPair.getDistance()

      /*
       * If the distance for the first pair in the queue
       * is >= current minimum distance, other nodes
       * in the queue must also have a greater distance.
       */
      if (pairDistance >= distanceLowerBound) break

      /*
       * If the pair members are leaves
       * then their distance is the exact lower bound.
       */
      if (bndPair.isLeaves()) {
        // assert: currentDistance < minimumDistanceFound
        distanceLowerBound = pairDistance
        minPair = bndPair
      } else {
        /*
         * Otherwise, expand one side of the pair,
         * and insert the expanded pairs into the queue.
         */
        bndPair.expandToQueue(priQ, distanceLowerBound)
      }
    }
    if (minPair == null) return null
    // done - return items with min distance
    return arrayOf(
      (minPair.getBoundable(0) as ItemBoundable).getItem(),
      (minPair.getBoundable(1) as ItemBoundable).getItem()
    )
  }

  /**
   * Tests whether some two items from this tree and another tree
   * lie within a given distance.
   *
   * @return true if there are items within the distance
   */
  fun isWithinDistance(tree: STRtree, itemDist: ItemDistance, maxDistance: Double): Boolean {
    val bp = BoundablePair(this.getRoot(), tree.getRoot(), itemDist)
    return isWithinDistance(bp, maxDistance)
  }

  /**
   * Performs a withinDistance search on the tree node pairs.
   *
   * @param initBndPair the initial pair containing the tree root nodes
   * @param maxDistance the maximum distance to search for
   * @return true if two items lie within the given distance
   */
  private fun isWithinDistance(initBndPair: BoundablePair, maxDistance: Double): Boolean {
    var distanceUpperBound = Double.POSITIVE_INFINITY

    // initialize search queue
    val priQ = PriorityQueue<BoundablePair>()
    priQ.add(initBndPair)

    while (!priQ.isEmpty()) {
      // pop head of queue and expand one side of pair
      val bndPair = priQ.poll()!!
      val pairDistance = bndPair.getDistance()

      /*
       * If the distance for the first pair in the queue
       * is > maxDistance, all other pairs
       * in the queue must have a greater distance as well.
       */
      if (pairDistance > maxDistance) return false

      /*
       * If the maximum distance between the nodes
       * is less than the maxDistance,
       * than all items in the nodes must be
       * closer than the max distance.
       */
      if (bndPair.maximumDistance() <= maxDistance) return true
      /*
       * If the pair items are leaves
       * then their actual distance is an upper bound.
       */
      if (bndPair.isLeaves()) {
        // assert: currentDistance < minimumDistanceFound
        distanceUpperBound = pairDistance

        /*
         * If the items are closer than maxDistance
         * can terminate with result = true.
         */
        if (distanceUpperBound <= maxDistance) return true
      } else {
        /*
         * Otherwise, expand one side of the pair,
         * and insert the expanded pairs into the queue.
         */
        bndPair.expandToQueue(priQ, distanceUpperBound)
      }
    }
    return false
  }

  /**
   * Finds up to k items in this tree which are the nearest neighbors to the given `item`,
   * using `itemDist` as the distance metric.
   *
   * @return an array of the nearest items found (with length between 0 and K)
   */
  fun nearestNeighbour(env: Envelope, item: Any?, itemDist: ItemDistance, k: Int): Array<Any?> {
    if (isEmpty()) return arrayOfNulls(0)

    val bnd: Boundable = ItemBoundable(env, item)
    val bp = BoundablePair(this.getRoot(), bnd, itemDist)
    return nearestNeighbourK(bp, k)
  }

  private fun nearestNeighbourK(initBndPair: BoundablePair, k: Int): Array<Any?> {
    return nearestNeighbourK(initBndPair, Double.POSITIVE_INFINITY, k)
  }

  private fun nearestNeighbourK(initBndPair: BoundablePair, maxDistance: Double, k: Int): Array<Any?> {
    var distanceLowerBound = maxDistance

    // initialize internal structures
    val priQ = PriorityQueue<BoundablePair>()

    // initialize queue
    priQ.add(initBndPair)

    val kNearestNeighbors = PriorityQueue<BoundablePair>()

    while (!priQ.isEmpty() && distanceLowerBound >= 0.0) {
      // pop head of queue and expand one side of pair
      val bndPair = priQ.poll()!!
      val pairDistance = bndPair.getDistance()

      /*
       * If the distance for the first node in the queue
       * is >= the current maximum distance in the k queue , all other nodes
       * in the queue must also have a greater distance.
       */
      if (pairDistance >= distanceLowerBound) {
        break
      }
      /*
       * If the pair members are leaves
       * then their distance is the exact lower bound.
       */
      if (bndPair.isLeaves()) {
        // assert: currentDistance < minimumDistanceFound
        if (kNearestNeighbors.size() < k) {
          kNearestNeighbors.add(bndPair)
        } else {
          val bp1 = kNearestNeighbors.peek()!!
          if (bp1.getDistance() > pairDistance) {
            kNearestNeighbors.poll()
            kNearestNeighbors.add(bndPair)
          }
          /*
           * minDistance should be the farthest point in the K nearest neighbor queue.
           */
          val bp2 = kNearestNeighbors.peek()!!
          distanceLowerBound = bp2.getDistance()
        }
      } else {
        /*
         * Otherwise, expand one side of the pair,
         * and insert the new expanded pairs into the queue
         */
        bndPair.expandToQueue(priQ, distanceLowerBound)
      }
    }
    // done - return items with min distance

    return getItems(kNearestNeighbors)
  }

  companion object {

    private const val DEFAULT_NODE_CAPACITY = 10

    private val intersectsOp: IntersectsOp = object : IntersectsOp {
      override fun intersects(aBounds: Any?, bBounds: Any?): Boolean {
        return (aBounds as Envelope).intersects(bBounds as Envelope)
      }
    }

    private val xComparator: Comparator<Boundable> = Comparator { o1, o2 ->
      AbstractSTRtree.compareDoubles(
        centreX(o1.getBounds() as Envelope),
        centreX(o2.getBounds() as Envelope)
      )
    }

    private val yComparator: Comparator<Boundable> = Comparator { o1, o2 ->
      AbstractSTRtree.compareDoubles(
        centreY(o1.getBounds() as Envelope),
        centreY(o2.getBounds() as Envelope)
      )
    }

    private fun centreX(e: Envelope): Double {
      return avg(e.getMinX(), e.getMaxX())
    }

    private fun centreY(e: Envelope): Double {
      return avg(e.getMinY(), e.getMaxY())
    }

    private fun avg(a: Double, b: Double): Double {
      return (a + b) / 2.0
    }

    private fun getItems(kNearestNeighbors: PriorityQueue<BoundablePair>): Array<Any?> {
      /**
       * Iterate the K Nearest Neighbour Queue and retrieve the item from each BoundablePair
       * in this queue
       */
      val items = arrayOfNulls<Any?>(kNearestNeighbors.size())
      var count = 0
      while (!kNearestNeighbors.isEmpty()) {
        val bp = kNearestNeighbors.poll()!!
        items[count] = (bp.getBoundable(0) as ItemBoundable).getItem()
        count++
      }
      return items
    }
  }
}
