/*
 * Copyright (c) 2016 Vivid Solutions, and others.
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
import org.locationtech.jts.index.hprtree.HPRtree

/**
 * Nodes a set of [SegmentString]s using a index based
 * on [MonotoneChain]s and a [SpatialIndex].
 *
 */
class MCIndexNoder : SinglePassNoder {
  private val monoChains: MutableList<MonotoneChain> = ArrayList()
  private val index: SpatialIndex = HPRtree()
  private var idCounter = 0
  private var nodedSegStrings: Collection<*>? = null
  // statistics
  private var nOverlaps = 0
  private var overlapTolerance = 0.0

  constructor()

  constructor(si: SegmentIntersector) : super(si)

  /**
   * Creates a new noder with a given [SegmentIntersector]
   * and an overlap tolerance distance to expand intersection tests with.
   *
   * @param si the segment intersector
   * @param overlapTolerance the expansion distance for overlap tests
   */
  constructor(si: SegmentIntersector, overlapTolerance: Double) : super(si) {
    this.overlapTolerance = overlapTolerance
  }

  fun getMonotoneChains(): MutableList<MonotoneChain> {
    return monoChains
  }

  fun getIndex(): SpatialIndex {
    return index
  }

  override fun getNodedSubstrings(): MutableCollection<*> {
    return NodedSegmentString.getNodedSubstrings(nodedSegStrings)
  }

  override fun computeNodes(inputSegStrings: Collection<*>?) {
    this.nodedSegStrings = inputSegStrings
    val i = inputSegStrings!!.iterator()
    while (i.hasNext()) {
      add(i.next() as SegmentString)
    }
    intersectChains()
  }

  private fun intersectChains() {
    val overlapAction: MonotoneChainOverlapAction = SegmentOverlapAction(segInt!!)

    val i = monoChains.iterator()
    while (i.hasNext()) {
      val queryChain = i.next()
      val queryEnv = queryChain.getEnvelope(overlapTolerance)
      val overlapChains = index.query(queryEnv)
      val j = overlapChains.iterator()
      while (j.hasNext()) {
        val testChain = j.next() as MonotoneChain
        /*
         * following test makes sure we only compare each pair of chains once
         * and that we don't compare a chain to itself
         */
        if (testChain.getId() > queryChain.getId()) {
          queryChain.computeOverlaps(testChain, overlapTolerance, overlapAction)
          nOverlaps++
        }
        // short-circuit if possible
        if (segInt!!.isDone()) return
      }
    }
  }

  private fun add(segStr: SegmentString) {
    val segChains = MonotoneChainBuilder.getChains(segStr.getCoordinates(), segStr)
    val i = segChains.iterator()
    while (i.hasNext()) {
      val mc = i.next()
      mc.setId(idCounter++)
      index.insert(mc.getEnvelope(overlapTolerance), mc)
      monoChains.add(mc)
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
