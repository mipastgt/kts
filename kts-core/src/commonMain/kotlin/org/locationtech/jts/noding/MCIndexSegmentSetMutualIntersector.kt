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
package org.locationtech.jts.noding

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.SpatialIndex
import org.locationtech.jts.index.chain.MonotoneChain
import org.locationtech.jts.index.chain.MonotoneChainBuilder
import org.locationtech.jts.index.chain.MonotoneChainOverlapAction
import org.locationtech.jts.index.strtree.STRtree

/**
 * Intersects two sets of [SegmentString]s using a index based
 * on [MonotoneChain]s and a [SpatialIndex].
 *
 * Thread-safe and immutable.
 *
 * @version 1.7
 */
class MCIndexSegmentSetMutualIntersector : SegmentSetMutualIntersector {
  /**
   * The [SpatialIndex] used should be something that supports
   * envelope (range) queries efficiently.
   */
  private val index = STRtree()
  private var overlapTolerance = 0.0
  private var envelope: Envelope? = null

  /**
   * Constructs a new intersector for a given set of [SegmentString]s.
   *
   * @param baseSegStrings the base segment strings to intersect
   */
  constructor(baseSegStrings: Collection<*>?) {
    initBaseSegments(baseSegStrings)
  }

  constructor(baseSegStrings: Collection<*>?, env: Envelope?) {
    this.envelope = env
    initBaseSegments(baseSegStrings)
  }

  constructor(baseSegStrings: Collection<*>?, overlapTolerance: Double) {
    initBaseSegments(baseSegStrings)
    this.overlapTolerance = overlapTolerance
  }

  /**
   * Gets the index constructed over the base segment strings.
   *
   * @return the constructed index
   */
  fun getIndex(): SpatialIndex {
    return index
  }

  private fun initBaseSegments(segStrings: Collection<*>?) {
    for (obj in segStrings!!) {
      val ss = obj as SegmentString
      if (ss.size() == 0) continue
      addToIndex(ss)
    }
    // build index to ensure thread-safety
    index.build()
  }

  private fun addToIndex(segStr: SegmentString) {
    val segChains = MonotoneChainBuilder.getChains(segStr.getCoordinates(), segStr)
    val i = segChains.iterator()
    while (i.hasNext()) {
      val mc = i.next()
      if (envelope == null || envelope!!.intersects(mc.getEnvelope())) {
        index.insert(mc.getEnvelope(overlapTolerance), mc)
      }
    }
  }

  /**
   * Calls [SegmentIntersector.processIntersections]
   * for all <i>candidate</i> intersections between
   * the given collection of SegmentStrings and the set of indexed segments.
   *
   * @param segStrings set of segments to intersect
   * @param segInt segment intersector to use
   */
  override fun process(segStrings: Collection<*>?, segInt: SegmentIntersector) {
    val monoChains = ArrayList<MonotoneChain>()
    for (obj in segStrings!!) {
      addToMonoChains(obj as SegmentString, monoChains)
    }
    intersectChains(monoChains, segInt)
  }

  private fun addToMonoChains(segStr: SegmentString, monoChains: MutableList<MonotoneChain>) {
    if (segStr.size() == 0) return
    val segChains = MonotoneChainBuilder.getChains(segStr.getCoordinates(), segStr)
    val i = segChains.iterator()
    while (i.hasNext()) {
      val mc = i.next()
      if (envelope == null || envelope!!.intersects(mc.getEnvelope())) {
        monoChains.add(mc)
      }
    }
  }

  private fun intersectChains(monoChains: MutableList<MonotoneChain>, segInt: SegmentIntersector) {
    val overlapAction: MonotoneChainOverlapAction = SegmentOverlapAction(segInt)

    val i = monoChains.iterator()
    while (i.hasNext()) {
      val queryChain = i.next()
      val queryEnv = queryChain.getEnvelope(overlapTolerance)
      val overlapChains = index.query(queryEnv)
      val j = overlapChains.iterator()
      while (j.hasNext()) {
        val testChain = j.next() as MonotoneChain
        queryChain.computeOverlaps(testChain, overlapTolerance, overlapAction)
        if (segInt.isDone()) return
      }
    }
  }

  class SegmentOverlapAction(private val si: SegmentIntersector) : MonotoneChainOverlapAction() {
    override fun overlap(mc1: MonotoneChain, start1: Int, mc2: MonotoneChain, start2: Int) {
      val ss1 = mc1.getContext() as SegmentString
      val ss2 = mc2.getContext() as SegmentString
      si.processIntersections(ss1, start1, ss2, start2)
    }
  }
}
