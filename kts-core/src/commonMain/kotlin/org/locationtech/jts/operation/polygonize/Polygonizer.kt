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
package org.locationtech.jts.operation.polygonize

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryComponentFilter
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon

/**
 * Polygonizes a set of [Geometry]s which contain linework that
 * represents the edges of a planar graph.
 *
 */
class Polygonizer
/**
 * Creates a polygonizer, specifying whether a valid polygonal geometry must be created.
 *
 * @param extractOnlyPolygonal true if a valid polygonal geometry should be extracted
 */
(private val extractOnlyPolygonal: Boolean) {

  /**
   * Adds every linear element in a [Geometry] into the polygonizer graph.
   */
  private class LineStringAdder(private val p: Polygonizer) : GeometryComponentFilter {
    override fun filter(g: Geometry) {
      if (g is LineString)
        p.add(g)
    }
  }

  // default factory
  private val lineStringAdder = LineStringAdder(this)

  protected var graph: PolygonizeGraph? = null
  // initialize with empty collections, in case nothing is computed
  private var dangles: MutableCollection<LineString> = ArrayList()
  private var cutEdges: MutableList<LineString> = ArrayList()
  private var invalidRingLines: MutableList<LineString> = ArrayList()

  protected var holeList: MutableList<EdgeRing>? = null
  protected var shellList: MutableList<EdgeRing>? = null
  protected var polyList: MutableList<Polygon>? = null

  private var isCheckingRingsValid = true

  private var geomFactory: GeometryFactory? = null

  /**
   * Creates a polygonizer that extracts all polygons.
   */
  constructor() : this(false)

  /**
   * Adds a collection of geometries to the edges to be polygonized.
   *
   * @param geomList a list of [Geometry]s with linework to be polygonized
   */
  fun add(geomList: Collection<*>) {
    val i = geomList.iterator()
    while (i.hasNext()) {
      val geometry = i.next() as Geometry
      add(geometry)
    }
  }

  /**
   * Add a [Geometry] to the edges to be polygonized.
   *
   * @param g a [Geometry] with linework to be polygonized
   */
  fun add(g: Geometry) {
    g.apply(lineStringAdder)
  }

  /**
   * Adds a linestring to the graph of polygon edges.
   *
   * @param line the [LineString] to add
   */
  private fun add(line: LineString) {
    // record the geometry factory for later use
    geomFactory = line.getFactory()
    // create a new graph using the factory from the input Geometry
    if (graph == null)
      graph = PolygonizeGraph(geomFactory!!)
    graph!!.addEdge(line)
  }

  /**
   * Allows disabling the valid ring checking,
   * to optimize situations where invalid rings are not expected.
   *
   * @param isCheckingRingsValid true if generated rings should be checked for validity
   */
  fun setCheckRingsValid(isCheckingRingsValid: Boolean) {
    this.isCheckingRingsValid = isCheckingRingsValid
  }

  /**
   * Gets the list of polygons formed by the polygonization.
   * @return a collection of [Polygon]s
   */
  fun getPolygons(): MutableCollection<Polygon>? {
    polygonize()
    return polyList
  }

  /**
   * Gets a geometry representing the polygons formed by the polygonization.
   *
   * @return a geometry containing the polygons
   */
  fun getGeometry(): Geometry {
    if (geomFactory == null) geomFactory = GeometryFactory()
    polygonize()
    if (extractOnlyPolygonal) {
      return geomFactory!!.buildGeometry(polyList!!)
    }
    // result may not be valid Polygonal, so return as a GeometryCollection
    return geomFactory!!.createGeometryCollection(GeometryFactory.toGeometryArray(polyList))
  }

  /**
   * Gets the list of dangling lines found during polygonization.
   * @return a collection of the input [LineString]s which are dangles
   */
  fun getDangles(): MutableCollection<LineString> {
    polygonize()
    return dangles
  }

  /**
   * Gets the list of cut edges found during polygonization.
   * @return a collection of the input [LineString]s which are cut edges
   */
  fun getCutEdges(): MutableCollection<LineString> {
    polygonize()
    return cutEdges
  }

  /**
   * Gets the list of lines forming invalid rings found during polygonization.
   * @return a collection of the input [LineString]s which form invalid rings
   */
  fun getInvalidRingLines(): MutableCollection<LineString> {
    polygonize()
    return invalidRingLines
  }

  /**
   * Performs the polygonization, if it has not already been carried out.
   */
  private fun polygonize() {
    // check if already computed
    if (polyList != null) return
    polyList = ArrayList()

    // if no geometries were supplied it's possible that graph is null
    val graph = this.graph ?: return

    dangles = graph.deleteDangles()
    cutEdges = graph.deleteCutEdges()
    val edgeRingList = graph.getEdgeRings()

    var validEdgeRingList: MutableList<EdgeRing> = ArrayList()
    val invalidRings = ArrayList<EdgeRing>()
    if (isCheckingRingsValid) {
      findValidRings(edgeRingList, validEdgeRingList, invalidRings)
      invalidRingLines = extractInvalidLines(invalidRings)
    } else {
      validEdgeRingList = edgeRingList
    }

    findShellsAndHoles(validEdgeRingList)
    HoleAssigner.assignHolesToShells(holeList!!, shellList!!)

    // order the shells to make any subsequent processing deterministic
    shellList!!.sortWith(EdgeRing.EnvelopeComparator())

    var includeAll = true
    if (extractOnlyPolygonal) {
      findDisjointShells(shellList!!)
      includeAll = false
    }
    polyList = extractPolygons(shellList!!, includeAll)
  }

  private fun findValidRings(edgeRingList: List<EdgeRing>, validEdgeRingList: MutableList<EdgeRing>, invalidRingList: MutableList<EdgeRing>) {
    for (er in edgeRingList) {
      er.computeValid()
      if (er.isValid())
        validEdgeRingList.add(er)
      else
        invalidRingList.add(er)
    }
  }

  private fun findShellsAndHoles(edgeRingList: List<EdgeRing>) {
    val holeList = ArrayList<EdgeRing>()
    val shellList = ArrayList<EdgeRing>()
    this.holeList = holeList
    this.shellList = shellList
    for (er in edgeRingList) {
      er.computeHole()
      if (er.isHole())
        holeList.add(er)
      else
        shellList.add(er)
    }
  }

  /**
   * Extracts unique lines for invalid rings,
   * discarding rings which correspond to outer rings and hence contain
   * duplicate linework.
   *
   * @param invalidRings the invalid rings to extract lines from
   * @return the invalid ring lines
   */
  private fun extractInvalidLines(invalidRings: MutableList<EdgeRing>): MutableList<LineString> {
    /*
     * Sort rings by increasing envelope area.
     */
    invalidRings.sortWith(EdgeRing.EnvelopeAreaComparator())
    /**
     * Scan through rings.  Keep only rings which have an adjacent EdgeRing
     * which is either valid or marked as not processed.
     */
    val invalidLines = ArrayList<LineString>()
    for (er in invalidRings) {
      if (isIncludedInvalid(er)) {
        invalidLines.add(er.getLineString())
      }
      er.setProcessed(true)
    }
    return invalidLines
  }

  /**
   * Tests if a invalid ring should be included in
   * the list of reported invalid rings.
   *
   * @param invalidRing the ring to test
   * @return true if the ring should be included
   */
  private fun isIncludedInvalid(invalidRing: EdgeRing): Boolean {
    for (de in invalidRing.getEdges()) {
      val deAdj = de.getSym() as PolygonizeDirectedEdge
      val erAdj = deAdj.getRing()!!
      val isEdgeIncluded = erAdj.isValid() || erAdj.isProcessed()
      if (!isEdgeIncluded)
        return true
    }
    return false
  }

  companion object {
    private fun findDisjointShells(shellList: List<EdgeRing>) {
      findOuterShells(shellList)

      var isMoreToScan: Boolean
      do {
        isMoreToScan = false
        for (er in shellList) {
          if (er.isIncludedSet())
            continue
          er.updateIncluded()
          if (!er.isIncludedSet()) {
            isMoreToScan = true
          }
        }
      } while (isMoreToScan)
    }

    /**
     * For each outer hole finds and includes a single outer shell.
     *
     * @param shellList the list of shell EdgeRings
     */
    private fun findOuterShells(shellList: List<EdgeRing>) {
      for (er in shellList) {
        val outerHoleER = er.getOuterHole()
        if (outerHoleER != null && !outerHoleER.isProcessed()) {
          er.setIncluded(true)
          outerHoleER.setProcessed(true)
        }
      }
    }

    private fun extractPolygons(shellList: List<EdgeRing>, includeAll: Boolean): MutableList<Polygon> {
      val polyList = ArrayList<Polygon>()
      for (er in shellList) {
        if (includeAll || er.isIncluded()) {
          polyList.add(er.getPolygon())
        }
      }
      return polyList
    }
  }
}
