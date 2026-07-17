/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.noding.snapround

import org.locationtech.jts.util.Random

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.index.kdtree.KdNodeVisitor
import org.locationtech.jts.index.kdtree.KdTree

/**
 * An index which creates unique [HotPixel]s for provided points,
 * and performs range queries on them.
 * The points passed to the index do not needed to be
 * rounded to the specified scale factor; this is done internally
 * when creating the HotPixels for them.
 *
 * @author mdavis
 */
internal class HotPixelIndex(pm: PrecisionModel) {
  private val precModel: PrecisionModel = pm
  private val scaleFactor: Double = pm.getScale()

  /**
   * Use a kd-tree to index the pixel centers for optimum performance.
   * Since HotPixels have an extent, range queries to the
   * index must enlarge the query range by a suitable value
   * (using the pixel width is safest).
   */
  private val index = KdTree()

  /**
   * Utility class to shuffle an array of [Coordinate]s using
   * the Fisher-Yates shuffle algorithm
   */
  private class CoordinateShuffler
  /**
   * Creates an instance of this class
   * @param pts An array of [Coordinate]s.
   */
    (pts: Array<Coordinate>) : Iterator<Coordinate> {

    private val rnd = Random(13)
    private val coordinates: Array<Coordinate> = pts
    private val indices: IntArray = IntArray(pts.size)
    private var index: Int

    init {
      for (i in pts.indices)
        indices[i] = i
      index = pts.size - 1
    }

    override fun hasNext(): Boolean {
      return index >= 0
    }

    override fun next(): Coordinate {
      val j = rnd.nextInt(index + 1)
      val res = coordinates[indices[j]]
      indices[j] = indices[index--]
      return res
    }
  }

  /**
   * Adds a list of points as non-node pixels.
   *
   * @param pts the points to add
   */
  fun add(pts: Array<Coordinate>) {
    /**
     * Shuffle the points before adding.
     * This avoids having long monontic runs of points
     * causing an unbalanced KD-tree, which would create
     * performance and robustness issues.
     */
    val it = CoordinateShuffler(pts)
    while (it.hasNext()) {
      add(it.next())
    }
  }

  /**
   * Adds a list of points as node pixels.
   *
   * @param pts the points to add
   */
  fun addNodes(pts: List<Coordinate>) {
    /*
     * Node points are not shuffled, since they are
     * added after the vertex points, and hence the KD-tree should
     * be reasonably balanced already.
     */
    for (pt in pts) {
      val hp = add(pt)
      hp.setToNode()
    }
  }

  /**
   * Adds a point as a Hot Pixel.
   * If the point has been added already, it is marked as a node.
   *
   * @param p the point to add
   * @return the HotPixel for the point
   */
  fun add(p: Coordinate): HotPixel {
    // TODO: is there a faster way of doing this?
    val pRound = round(p)

    var hp = find(pRound)
    /*
     * Hot Pixels which are added more than once
     * must have more than one vertex in them
     * and thus must be nodes.
     */
    if (hp != null) {
      hp.setToNode()
      return hp
    }

    /*
     * A pixel containing the point was not found, so create a new one.
     * It is initially set to NOT be a node
     * (but may become one later on).
     */
    hp = HotPixel(pRound, scaleFactor)
    index.insert(hp.getCoordinate(), hp)
    return hp
  }

  private fun find(pixelPt: Coordinate): HotPixel? {
    val kdNode = index.query(pixelPt) ?: return null
    return kdNode.getData() as HotPixel
  }

  private fun round(pt: Coordinate): Coordinate {
    val p2 = pt.copy()
    precModel.makePrecise(p2)
    return p2
  }

  /**
   * Visits all the hot pixels which may intersect a segment (p0-p1).
   * The visitor must determine whether each hot pixel actually intersects
   * the segment.
   *
   * @param p0 the segment start point
   * @param p1 the segment end point
   * @param visitor the visitor to apply
   */
  fun query(p0: Coordinate, p1: Coordinate, visitor: KdNodeVisitor) {
    val queryEnv = Envelope(p0, p1)
    // expand query range to account for HotPixel extent
    // expand by full width of one pixel to be safe
    queryEnv.expandBy(1.0 / scaleFactor)
    index.query(queryEnv, visitor)
  }
}
