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
package org.locationtech.jts.operation.overlayng

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.noding.Noder

/**
 * Computes the geometric overlay of two [Geometry]s,
 * using an explicit precision model to allow robust computation.
 *
 * @author mdavis
 *
 * @see OverlayNGRobust
 */
class OverlayNG(
  geom0: Geometry,
  geom1: Geometry?,
  private val pm: PrecisionModel?,
  private val opCode: Int
) {

  private val inputGeom: InputGeometry = InputGeometry(geom0, geom1)
  private val geomFact: GeometryFactory = geom0.getFactory()
  private var noder: Noder? = null
  private var isStrictMode = STRICT_MODE_DEFAULT
  private var isOptimized = true
  private var isAreaResultOnly = false
  private var isOutputEdges = false
  private var isOutputResultEdges = false
  private var isOutputNodedEdges = false

  /**
   * Creates an overlay operation on the given geometries
   * using the precision model of the geometries.
   *
   * @param geom0 the A operand geometry
   * @param geom1 the B operand geometry (may be null)
   * @param opCode the overlay opcode
   */
  constructor(geom0: Geometry, geom1: Geometry?, opCode: Int) :
    this(geom0, geom1, geom0.getFactory().getPrecisionModel(), opCode)

  /**
   * Creates a union of a single geometry with a given precision model.
   *
   * @param geom the geometry
   * @param pm the precision model to use
   */
  constructor(geom: Geometry, pm: PrecisionModel?) : this(geom, null, pm, UNION)

  /**
   * Sets whether the overlay results are computed according to strict mode
   * semantics.
   *
   * @param isStrictMode true if strict mode is to be used
   */
  fun setStrictMode(isStrictMode: Boolean) {
    this.isStrictMode = isStrictMode
  }

  /**
   * Sets whether overlay processing optimizations are enabled.
   *
   * @param isOptimized whether to optimize processing
   */
  fun setOptimized(isOptimized: Boolean) {
    this.isOptimized = isOptimized
  }

  /**
   * Sets whether the result can contain only [Polygon] components.
   *
   * @param isAreaResultOnly true if the result should contain only area components
   */
  fun setAreaResultOnly(isAreaResultOnly: Boolean) {
    this.isAreaResultOnly = isAreaResultOnly
  }

  //------ Testing options -------

  fun setOutputEdges(isOutputEdges: Boolean) {
    this.isOutputEdges = isOutputEdges
  }

  fun setOutputNodedEdges(isOutputNodedEdges: Boolean) {
    this.isOutputEdges = true
    this.isOutputNodedEdges = isOutputNodedEdges
  }

  fun setOutputResultEdges(isOutputResultEdges: Boolean) {
    this.isOutputResultEdges = isOutputResultEdges
  }
  //---------------------------------

  fun setNoder(noder: Noder?) {
    this.noder = noder
  }

  /**
   * Gets the result of the overlay operation.
   *
   * @return the result of the overlay operation.
   *
   * @throws IllegalArgumentException if the input is not supported (e.g. a mixed-dimension geometry)
   * @throws TopologyException if a robustness error occurs
   */
  fun getResult(): Geometry {
    // handle empty inputs which determine result
    if (OverlayUtil.isEmptyResult(
        opCode,
        inputGeom.getGeometry(0),
        inputGeom.getGeometry(1),
        pm
      )
    ) {
      return createEmptyResult()
    }

    /**
     * The elevation model is only computed if the input geometries have Z values.
     */
    val elevModel = ElevationModel.create(inputGeom.getGeometry(0)!!, inputGeom.getGeometry(1))
    val result: Geometry
    if (inputGeom.isAllPoints()) {
      // handle Point-Point inputs
      result = OverlayPoints.overlay(opCode, inputGeom.getGeometry(0)!!, inputGeom.getGeometry(1)!!, pm)
    } else if (!inputGeom.isSingle() && inputGeom.hasPoints()) {
      // handle Point-nonPoint inputs
      result = OverlayMixedPoints.overlay(opCode, inputGeom.getGeometry(0)!!, inputGeom.getGeometry(1)!!, pm)
    } else {
      // handle case where both inputs are formed of edges (Lines and Polygons)
      result = computeEdgeOverlay()
    }
    /**
     * This is a no-op if the elevation model was not computed due to Z not present
     */
    elevModel.populateZ(result)
    return result
  }

  private fun computeEdgeOverlay(): Geometry {
    val edges = nodeEdges()

    val graph = buildGraph(edges)

    if (isOutputNodedEdges) {
      return OverlayUtil.toLines(graph, isOutputEdges, geomFact)
    }

    labelGraph(graph)

    if (isOutputEdges || isOutputResultEdges) {
      return OverlayUtil.toLines(graph, isOutputEdges, geomFact)
    }

    val result = extractResult(opCode, graph)

    /**
     * Heuristic check on result area.
     * Catches cases where noding causes vertex to move
     * and make topology graph area "invert".
     */
    if (OverlayUtil.isFloating(pm)) {
      val isAreaConsistent = OverlayUtil.isResultAreaConsistent(inputGeom.getGeometry(0), inputGeom.getGeometry(1), opCode, result)
      if (!isAreaConsistent)
        throw TopologyException("Result area inconsistent with overlay operation")
    }
    return result
  }

  private fun nodeEdges(): MutableList<Edge> {
    /**
     * Node the edges, using whatever noder is being used
     */
    val nodingBuilder = EdgeNodingBuilder(pm, noder)

    /**
     * Optimize Intersection and Difference by clipping to the
     * result extent, if enabled.
     */
    if (isOptimized) {
      val clipEnv = OverlayUtil.clippingEnvelope(opCode, inputGeom, pm)
      if (clipEnv != null)
        nodingBuilder.setClipEnvelope(clipEnv)
    }

    val mergedEdges = nodingBuilder.build(
      inputGeom.getGeometry(0),
      inputGeom.getGeometry(1)
    )

    /**
     * Record if an input geometry has collapsed.
     */
    inputGeom.setCollapsed(0, !nodingBuilder.hasEdgesFor(0))
    inputGeom.setCollapsed(1, !nodingBuilder.hasEdgesFor(1))

    return mergedEdges
  }

  private fun buildGraph(edges: Collection<Edge>): OverlayGraph {
    val graph = OverlayGraph()
    for (e in edges) {
      graph.addEdge(e.getCoordinates(), e.createLabel())
    }
    return graph
  }

  private fun labelGraph(graph: OverlayGraph) {
    val labeller = OverlayLabeller(graph, inputGeom)
    labeller.computeLabelling()
    labeller.markResultAreaEdges(opCode)
    labeller.unmarkDuplicateEdgesFromResultArea()
  }

  /**
   * Extracts the result geometry components from the fully labelled topology graph.
   *
   * @param opCode the overlay operation
   * @param graph the topology graph
   * @return the result geometry
   */
  private fun extractResult(opCode: Int, graph: OverlayGraph): Geometry {
    val isAllowMixedIntResult = !isStrictMode

    //--- Build polygons
    val resultAreaEdges = graph.getResultAreaEdges()
    val polyBuilder = PolygonBuilder(resultAreaEdges, geomFact)
    val resultPolyList = polyBuilder.getPolygons()
    val hasResultAreaComponents = resultPolyList.size > 0

    var resultLineList: List<LineString>? = null
    var resultPointList: List<Point>? = null

    if (!isAreaResultOnly) {
      //--- Build lines
      val allowResultLines = !hasResultAreaComponents ||
          isAllowMixedIntResult ||
          opCode == SYMDIFFERENCE ||
          opCode == UNION
      if (allowResultLines) {
        val lineBuilder = LineBuilder(inputGeom, graph, hasResultAreaComponents, opCode, geomFact)
        lineBuilder.setStrictMode(isStrictMode)
        resultLineList = lineBuilder.getLines()
      }
      /**
       * Operations with point inputs are handled elsewhere.
       * Only an Intersection op can produce point results
       * from non-point inputs.
       */
      val hasResultComponents = hasResultAreaComponents || resultLineList!!.size > 0
      val allowResultPoints = !hasResultComponents || isAllowMixedIntResult
      if (opCode == INTERSECTION && allowResultPoints) {
        val pointBuilder = IntersectionPointBuilder(graph, geomFact)
        pointBuilder.setStrictMode(isStrictMode)
        resultPointList = pointBuilder.getPoints()
      }
    }

    if (isEmpty(resultPolyList) &&
      isEmpty(resultLineList) &&
      isEmpty(resultPointList)
    )
      return createEmptyResult()

    val resultGeom = OverlayUtil.createResultGeometry(resultPolyList, resultLineList, resultPointList, geomFact)
    return resultGeom
  }

  private fun createEmptyResult(): Geometry {
    return OverlayUtil.createEmptyResult(
      OverlayUtil.resultDimension(
        opCode,
        inputGeom.getDimension(0),
        inputGeom.getDimension(1)
      ),
      geomFact
    )
  }

  companion object {
    /**
     * The code for the Intersection overlay operation.
     */
    const val INTERSECTION = 1 // OverlayOp.INTERSECTION

    /**
     * The code for the Union overlay operation.
     */
    const val UNION = 2 // OverlayOp.UNION

    /**
     *  The code for the Difference overlay operation.
     */
    const val DIFFERENCE = 3 // OverlayOp.DIFFERENCE

    /**
     *  The code for the Symmetric Difference overlay operation.
     */
    const val SYMDIFFERENCE = 4 // OverlayOp.SYMDIFFERENCE

    /**
     * The default setting for Strict Mode.
     */
    const val STRICT_MODE_DEFAULT = false

    /**
     * Tests whether a point with a given topological label
     * relative to two geometries is contained in
     * the result of overlaying the geometries using
     * a given overlay operation.
     *
     * @param label the topological label of the point
     * @param opCode the code for the overlay operation to test
     * @return true if the label locations correspond to the overlayOpCode
     */
    @JvmStatic
    fun isResultOfOpPoint(label: OverlayLabel, opCode: Int): Boolean {
      val loc0 = label.getLocation(0)
      val loc1 = label.getLocation(1)
      return isResultOfOp(opCode, loc0, loc1)
    }

    /**
     * Tests whether a point with given [Location]s
     * relative to two geometries would be contained in
     * the result of overlaying the geometries using
     * a given overlay operation.
     *
     * @param overlayOpCode the code for the overlay operation to test
     * @param loc0 the code for the location in the first geometry
     * @param loc1 the code for the location in the second geometry
     *
     * @return true if a point with given locations is in the result of the overlay operation
     */
    @JvmStatic
    fun isResultOfOp(overlayOpCode: Int, loc0: Int, loc1: Int): Boolean {
      var l0 = loc0
      var l1 = loc1
      if (l0 == Location.BOUNDARY) l0 = Location.INTERIOR
      if (l1 == Location.BOUNDARY) l1 = Location.INTERIOR
      when (overlayOpCode) {
        INTERSECTION ->
          return l0 == Location.INTERIOR && l1 == Location.INTERIOR
        UNION ->
          return l0 == Location.INTERIOR || l1 == Location.INTERIOR
        DIFFERENCE ->
          return l0 == Location.INTERIOR && l1 != Location.INTERIOR
        SYMDIFFERENCE ->
          return (l0 == Location.INTERIOR && l1 != Location.INTERIOR) ||
              (l0 != Location.INTERIOR && l1 == Location.INTERIOR)
      }
      return false
    }

    /**
     * Computes an overlay operation for
     * the given geometry operands, with the
     * noding strategy determined by the precision model.
     *
     * @param geom0 the first geometry argument
     * @param geom1 the second geometry argument
     * @param opCode the code for the desired overlay operation
     * @param pm the precision model to use
     * @return the result of the overlay operation
     */
    @JvmStatic
    fun overlay(geom0: Geometry, geom1: Geometry?, opCode: Int, pm: PrecisionModel?): Geometry {
      val ov = OverlayNG(geom0, geom1, pm, opCode)
      val geomOv = ov.getResult()
      return geomOv
    }

    /**
     * Computes an overlay operation on the given geometry operands,
     * using a supplied [Noder].
     *
     * @param pm the precision model to use (which may be null if the noder does not use one)
     * @param noder the noder to use
     * @return the result of the overlay operation
     */
    @JvmStatic
    fun overlay(geom0: Geometry, geom1: Geometry?, opCode: Int, pm: PrecisionModel?, noder: Noder?): Geometry {
      val ov = OverlayNG(geom0, geom1, pm, opCode)
      ov.setNoder(noder)
      val geomOv = ov.getResult()
      return geomOv
    }

    /**
     * Computes an overlay operation on the given geometry operands,
     * using a supplied [Noder].
     *
     * @param noder the noder to use
     * @return the result of the overlay operation
     */
    @JvmStatic
    fun overlay(geom0: Geometry, geom1: Geometry?, opCode: Int, noder: Noder?): Geometry {
      val ov = OverlayNG(geom0, geom1, null, opCode)
      ov.setNoder(noder)
      val geomOv = ov.getResult()
      return geomOv
    }

    /**
     * Computes an overlay operation on
     * the given geometry operands,
     * using the precision model of the geometry.
     *
     * @param geom0 the first argument geometry
     * @param geom1 the second argument geometry
     * @param opCode the code for the desired overlay operation
     * @return the result of the overlay operation
     */
    @JvmStatic
    fun overlay(geom0: Geometry, geom1: Geometry?, opCode: Int): Geometry {
      val ov = OverlayNG(geom0, geom1, opCode)
      return ov.getResult()
    }

    /**
     * Computes a union operation on
     * the given geometry, with the supplied precision model.
     *
     * @param geom the geometry
     * @param pm the precision model to use
     * @return the result of the union operation
     *
     * @see OverlayMixedPoints
     */
    @JvmStatic
    fun union(geom: Geometry, pm: PrecisionModel?): Geometry {
      val ov = OverlayNG(geom, pm)
      val geomOv = ov.getResult()
      return geomOv
    }

    /**
     * Computes a union of a single geometry using a custom noder.
     *
     * @param geom the geometry to union
     * @param pm the precision model to use (maybe be null)
     * @param noder the noder to use
     * @return the result geometry
     *
     * @see CoverageUnion
     */
    @JvmStatic
    fun union(geom: Geometry, pm: PrecisionModel?, noder: Noder?): Geometry {
      val ov = OverlayNG(geom, pm)
      ov.setNoder(noder)
      ov.setStrictMode(true)
      val geomOv = ov.getResult()
      return geomOv
    }

    private fun isEmpty(list: List<*>?): Boolean {
      return list == null || list.size == 0
    }
  }
}
