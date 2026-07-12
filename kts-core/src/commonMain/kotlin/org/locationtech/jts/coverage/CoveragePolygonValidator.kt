/*
 * Copyright (c) 2022 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.coverage

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.util.PolygonExtracter
import org.locationtech.jts.noding.MCIndexSegmentSetMutualIntersector

/**
 * Validates that a polygon forms a valid polygonal coverage
 * with the set of polygons adjacent to it.
 *
 * @see CoverageValidator
 *
 * @author Martin Davis
 */
class CoveragePolygonValidator(geom: Geometry, private val adjGeoms: Array<Geometry>) {

  private val targetGeom: Geometry = geom
  private var gapWidth = 0.0
  private val geomFactory: GeometryFactory = targetGeom.getFactory()
  private lateinit var adjCovPolygons: List<CoveragePolygon>

  /**
   * Sets the maximum gap width, if narrow gaps are to be detected.
   *
   * @param gapWidth the maximum width of gaps to detect
   */
  fun setGapWidth(gapWidth: Double) {
    this.gapWidth = gapWidth
  }

  /**
   * Validates the coverage polygon against the set of adjacent polygons
   * in the coverage.
   *
   * @return a linear geometry containing the segments causing invalidity (if any)
   */
  fun validate(): Geometry {
    val adjPolygons = extractPolygons(adjGeoms)
    adjCovPolygons = toCoveragePolygons(adjPolygons)

    val targetRings = CoverageRing.createRings(targetGeom)
    val adjRings = CoverageRing.createRings(adjPolygons)

    /**
     * Mark matching segments first.
     */
    val targetEnv = targetGeom.getEnvelopeInternal().copy()
    targetEnv.expandBy(gapWidth)

    checkTargetRings(targetRings, adjRings, targetEnv)

    return createInvalidLines(targetRings)
  }

  private fun checkTargetRings(targetRings: List<CoverageRing>, adjRings: List<CoverageRing>, targetEnv: Envelope) {
    markMatchedSegments(targetRings, adjRings, targetEnv)

    /**
     * Short-circuit if target is fully known (matched or invalid).
     */
    if (CoverageRing.isKnown(targetRings))
      return

    /**
     * Here target has at least one unmatched segment.
     */
    markInvalidInteractingSegments(targetRings, adjRings, gapWidth)
    markInvalidInteriorSegments(targetRings, adjCovPolygons)
  }

  private fun createEmptyResult(): Geometry {
    return geomFactory.createLineString()
  }

  /**
   * Marks matched segments.
   */
  private fun markMatchedSegments(targetRings: List<CoverageRing>,
                                  adjRngs: List<CoverageRing>, targetEnv: Envelope) {
    val segmentMap: MutableMap<CoverageRingSegment, CoverageRingSegment> = HashMap()
    markMatchedSegments(targetRings, targetEnv, segmentMap)
    markMatchedSegments(adjRngs, targetEnv, segmentMap)
  }

  /**
   * Adds ring segments to the segment map,
   * and detects if they match an existing segment.
   */
  private fun markMatchedSegments(rings: List<CoverageRing>, envLimit: Envelope,
                                  segmentMap: MutableMap<CoverageRingSegment, CoverageRingSegment>) {
    for (ring in rings) {
      for (i in 0 until ring.size() - 1) {
        val p0 = ring.getCoordinate(i)
        val p1 = ring.getCoordinate(i + 1)
        //-- skip segments which lie outside the limit envelope
        if (!envLimit.intersects(p0, p1)) {
          continue
        }
        //-- if segment keys match, mark them as matched (or invalid)
        val seg = CoverageRingSegment.create(ring, i)
        if (segmentMap.containsKey(seg)) {
          val segMatch = segmentMap.get(seg)!!
          seg.match(segMatch)
        } else {
          segmentMap.put(seg, seg)
        }
      }
    }
  }

  /**
   * Models a segment in a CoverageRing.
   */
  private class CoverageRingSegment private constructor(p0: Coordinate, p1: Coordinate, ring: CoverageRing, index: Int) : LineSegment(p0, p1) {
    private var ringForward: CoverageRing? = null
    private var indexForward = -1
    private var ringOpp: CoverageRing? = null
    private var indexOpp = -1

    init {
      if (p1.compareTo(p0) < 0) {
        reverse()
        ringOpp = ring
        indexOpp = index
      } else {
        ringForward = ring
        indexForward = index
      }
    }

    fun match(seg: CoverageRingSegment) {
      val isInvalid = checkInvalid(seg)
      if (isInvalid) {
        return
      }
      //-- record the match
      if (ringForward == null) {
        ringForward = seg.ringForward
        indexForward = seg.indexForward
      } else {
        ringOpp = seg.ringOpp
        indexOpp = seg.indexOpp
      }
      //-- mark ring segments as matched
      ringForward!!.markMatched(indexForward)
      ringOpp!!.markMatched(indexOpp)
    }

    private fun checkInvalid(seg: CoverageRingSegment): Boolean {
      if (ringForward != null && seg.ringForward != null) {
        ringForward!!.markInvalid(indexForward)
        seg.ringForward!!.markInvalid(seg.indexForward)
        return true
      }
      if (ringOpp != null && seg.ringOpp != null) {
        ringOpp!!.markInvalid(indexOpp)
        seg.ringOpp!!.markInvalid(seg.indexOpp)
        return true
      }
      return false
    }

    companion object {
      fun create(ring: CoverageRing, index: Int): CoverageRingSegment {
        val p0 = ring.getCoordinate(index)
        val p1 = ring.getCoordinate(index + 1)
        //-- orient segment as if ring is in canonical orientation
        return if (ring.isInteriorOnRight()) {
          CoverageRingSegment(p0, p1, ring, index)
        } else {
          CoverageRingSegment(p1, p0, ring, index)
        }
      }
    }
  }

  /**
   * Marks invalid target segments which cross an adjacent ring segment,
   * lie partially in the interior of an adjacent ring,
   * or are nearly collinear with an adjacent ring segment up to the distance tolerance.
   */
  private fun markInvalidInteractingSegments(targetRings: List<CoverageRing>, adjRings: List<CoverageRing>,
                                             distanceTolerance: Double) {
    val detector = InvalidSegmentDetector(distanceTolerance)
    val segSetMutInt = MCIndexSegmentSetMutualIntersector(targetRings, distanceTolerance)
    segSetMutInt.process(adjRings, detector)
  }

  /**
   * Marks invalid target segments which are fully interior
   * to an adjacent polygon.
   */
  private fun markInvalidInteriorSegments(targetRings: List<CoverageRing>, adjCovPolygons: List<CoveragePolygon>) {
    for (ring in targetRings) {
      val stride = RING_SECTION_STRIDE
      var i = 0
      while (i < ring.size() - 1) {
        var iEnd = i + stride
        if (iEnd >= ring.size())
          iEnd = ring.size() - 1

        markInvalidInteriorSection(ring, i, iEnd, adjCovPolygons)
        i += stride
      }
    }
  }

  /**
   * Marks invalid target segments in a section which are interior
   * to an adjacent polygon.
   */
  private fun markInvalidInteriorSection(ring: CoverageRing, iStart: Int, iEnd: Int, adjPolygons: List<CoveragePolygon>) {
    val sectionEnv = ring.getEnvelope(iStart, iEnd)
    for (adjPoly in adjPolygons) {
      if (adjPoly.intersectsEnv(sectionEnv)) {
        //-- test vertices in section
        for (i in iStart until iEnd) {
          markInvalidInteriorSegment(ring, i, adjPoly)
        }
      }
    }
  }

  private fun markInvalidInteriorSegment(ring: CoverageRing, i: Int, adjPoly: CoveragePolygon) {
    //-- skip check for segments with known state.
    if (ring.isKnown(i))
      return

    /**
     * Check if vertex is in interior of an adjacent polygon.
     */
    val p = ring.getCoordinate(i)
    if (adjPoly.contains(p)) {
      ring.markInvalid(i)
      //-- previous segment may be interior (but may also be matched)
      val iPrev = if (i == 0) ring.size() - 2 else i - 1
      if (!ring.isKnown(iPrev))
        ring.markInvalid(iPrev)
    }
  }

  private fun createInvalidLines(rings: List<CoverageRing>): Geometry {
    val lines: MutableList<LineString> = ArrayList()
    for (ring in rings) {
      ring.createInvalidLines(geomFactory, lines)
    }

    if (lines.size == 0) {
      return createEmptyResult()
    } else if (lines.size == 1) {
      return lines.get(0)
    }
    return geomFactory.createMultiLineString(GeometryFactory.toLineStringArray(lines))
  }

  companion object {
    /**
     * Validates that a polygon is coverage-valid against the
     * surrounding polygons in a polygonal coverage.
     *
     * @param targetPolygon the polygon to validate
     * @param adjPolygons the adjacent polygons
     * @return a linear geometry containing the segments causing invalidity (if any)
     */
    @JvmStatic
    fun validate(targetPolygon: Geometry, adjPolygons: Array<Geometry>): Geometry {
      val v = CoveragePolygonValidator(targetPolygon, adjPolygons)
      return v.validate()
    }

    /**
     * Validates that a polygon is coverage-valid against the
     * surrounding polygons in a polygonal coverage,
     * and forms no gaps narrower than a specified width.
     *
     * @param targetPolygon the polygon to validate
     * @param adjPolygons a collection of the adjacent polygons
     * @param gapWidth the maximum width of invalid gaps
     * @return a linear geometry containing the segments causing invalidity (if any)
     */
    @JvmStatic
    fun validate(targetPolygon: Geometry, adjPolygons: Array<Geometry>, gapWidth: Double): Geometry {
      val v = CoveragePolygonValidator(targetPolygon, adjPolygons)
      v.setGapWidth(gapWidth)
      return v.validate()
    }

    private fun toCoveragePolygons(polygons: List<Polygon>): List<CoveragePolygon> {
      val covPolys: MutableList<CoveragePolygon> = ArrayList()
      for (poly in polygons) {
        covPolys.add(CoveragePolygon(poly))
      }
      return covPolys
    }

    private fun extractPolygons(geoms: Array<Geometry>): List<Polygon> {
      val polygons: MutableList<Polygon> = ArrayList()
      for (geom in geoms) {
        PolygonExtracter.getPolygons(geom, polygons)
      }
      return polygons
    }

    /**
     * Stride is chosen experimentally to provide good performance
     */
    private const val RING_SECTION_STRIDE = 1000
  }
}
