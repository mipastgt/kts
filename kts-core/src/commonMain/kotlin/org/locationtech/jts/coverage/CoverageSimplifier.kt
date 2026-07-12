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

import org.locationtech.jts.geom.Geometry

/**
 * Simplifies the boundaries of the polygons in a polygonal coverage
 * while preserving the original coverage topology.
 *
 * @author Martin Davis
 */
class CoverageSimplifier(private val coverage: Array<Geometry>) {

  private var smoothWeight = CornerArea.DEFAULT_SMOOTH_WEIGHT
  private var removableSizeFactor = 1.0

  /**
   * Sets the factor applied to the area tolerance to determine
   * if small rings should be removed.
   *
   * @param removableSizeFactor the factor to determine ring size to remove
   */
  fun setRemovableRingSizeFactor(removableSizeFactor: Double) {
    var factor = removableSizeFactor
    if (factor < 0.0)
      factor = 0.0
    this.removableSizeFactor = factor
  }

  /**
   * Sets the weight influencing how smooth the simplification should be.
   * The weight must be between 0 and 1.
   *
   * @param smoothWeight a value between 0 and 1
   */
  fun setSmoothWeight(smoothWeight: Double) {
    if (smoothWeight < 0.0 || smoothWeight > 1.0)
      throw IllegalArgumentException("smoothWeight must be in range [0 - 1]")
    this.smoothWeight = smoothWeight
  }

  /**
   * Computes the simplified coverage using a single distance tolerance,
   * preserving the coverage topology.
   *
   * @param tolerance the simplification distance tolerance
   * @return the simplified coverage polygons
   */
  fun simplify(tolerance: Double): Array<Geometry?> {
    return simplifyEdges(tolerance, tolerance)
  }

  /**
   * Computes the simplified coverage using separate distance tolerances
   * for inner and outer edges.
   *
   * @param toleranceInner the distance tolerance for inner edges
   * @param toleranceOuter the distance tolerance for outer edges
   * @return the simplified coverage polygons
   */
  fun simplify(toleranceInner: Double, toleranceOuter: Double): Array<Geometry?> {
    return simplifyEdges(toleranceInner, toleranceOuter)
  }

  /**
   * Computes the simplified coverage using separate distance tolerances
   * for each coverage element.
   *
   * @param tolerances the distance tolerances for the coverage elements
   * @return the simplified coverage polygons
   */
  fun simplify(tolerances: DoubleArray): Array<Geometry?> {
    if (tolerances.size != coverage.size)
      throw IllegalArgumentException("number of tolerances does not match number of coverage elements")
    return simplifyEdges(tolerances)
  }

  private fun simplifyEdges(tolerances: DoubleArray): Array<Geometry?> {
    val covRings = CoverageRingEdges.create(coverage)
    val covEdges = covRings.getEdges()
    val edges = createEdges(covEdges, tolerances)
    return simplify(covRings, covEdges, edges)
  }

  private fun createEdges(covEdges: List<CoverageEdge>, tolerances: DoubleArray): Array<TPVWSimplifier.Edge> {
    return Array(covEdges.size) { i ->
      val covEdge = covEdges.get(i)
      val tol = computeTolerance(covEdge, tolerances)
      createEdge(covEdge, tol)
    }
  }

  private fun computeTolerance(covEdge: CoverageEdge, tolerances: DoubleArray): Double {
    val index0 = covEdge.getAdjacentIndex(0)
    // assert: index0 >= 0
    var tolerance = tolerances[index0]

    if (covEdge.hasAdjacentIndex(1)) {
      val index1 = covEdge.getAdjacentIndex(1)
      val tol1 = tolerances[index1]
      //-- use lowest tolerance for edge
      if (tol1 < tolerance)
        tolerance = tol1
    }
    return tolerance
  }

  private fun simplifyEdges(toleranceInner: Double, toleranceOuter: Double): Array<Geometry?> {
    val covRings = CoverageRingEdges.create(coverage)
    val covEdges = covRings.getEdges()
    val edges = createEdges(covEdges, toleranceInner, toleranceOuter)
    return simplify(covRings, covEdges, edges)
  }

  private fun simplify(covRings: CoverageRingEdges, covEdges: List<CoverageEdge>, edges: Array<TPVWSimplifier.Edge>): Array<Geometry?> {
    val cornerArea = CornerArea(smoothWeight)
    TPVWSimplifier.simplify(edges, cornerArea, removableSizeFactor)
    setCoordinates(covEdges, edges)
    val result = covRings.buildCoverage()
    return result
  }

  private fun setCoordinates(covEdges: List<CoverageEdge>, edges: Array<TPVWSimplifier.Edge>) {
    for (i in covEdges.indices) {
      val edge = edges[i]
      if (edge.getTolerance() > 0) {
        covEdges.get(i).setCoordinates(edges[i].getCoordinates())
      }
    }
  }

  companion object {
    /**
     * Simplifies the boundaries of a set of polygonal geometries forming a coverage,
     * preserving the coverage topology.
     *
     * @param coverage a set of polygonal geometries forming a coverage
     * @param tolerance the simplification tolerance
     * @return the simplified coverage polygons
     */
    @JvmStatic
    fun simplify(coverage: Array<Geometry>, tolerance: Double): Array<Geometry?> {
      val simplifier = CoverageSimplifier(coverage)
      return simplifier.simplify(tolerance)
    }

    /**
     * Simplifies the boundaries of a set of polygonal geometries forming a coverage,
     * preserving the coverage topology, using a separate tolerance
     * for each element of the coverage.
     *
     * @param coverage a set of polygonal geometries forming a coverage
     * @param tolerances the simplification tolerances (one per input element)
     * @return the simplified coverage polygons
     */
    @JvmStatic
    fun simplify(coverage: Array<Geometry>, tolerances: DoubleArray): Array<Geometry?> {
      val simplifier = CoverageSimplifier(coverage)
      return simplifier.simplify(tolerances)
    }

    /**
     * Simplifies the inner boundaries of a set of polygonal geometries forming a coverage,
     * preserving the coverage topology.
     *
     * @param coverage a set of polygonal geometries forming a coverage
     * @param tolerance the simplification tolerance
     * @return the simplified coverage polygons
     */
    @JvmStatic
    fun simplifyInner(coverage: Array<Geometry>, tolerance: Double): Array<Geometry?> {
      val simplifier = CoverageSimplifier(coverage)
      return simplifier.simplify(tolerance, 0.0)
    }

    /**
     * Simplifies the outer boundaries of a set of polygonal geometries forming a coverage,
     * preserving the coverage topology.
     *
     * @param coverage a set of polygonal geometries forming a coverage
     * @param tolerance the simplification tolerance
     * @return the simplified polygons
     */
    @JvmStatic
    fun simplifyOuter(coverage: Array<Geometry>, tolerance: Double): Array<Geometry?> {
      val simplifier = CoverageSimplifier(coverage)
      return simplifier.simplify(0.0, tolerance)
    }

    private fun createEdges(covEdges: List<CoverageEdge>, toleranceInner: Double, toleranceOuter: Double): Array<TPVWSimplifier.Edge> {
      return Array(covEdges.size) { i ->
        val covEdge = covEdges.get(i)
        val tol = computeTolerance(covEdge, toleranceInner, toleranceOuter)
        createEdge(covEdge, tol)
      }
    }

    private fun createEdge(covEdge: CoverageEdge, tol: Double): TPVWSimplifier.Edge {
      return TPVWSimplifier.Edge(covEdge.getCoordinates(), tol,
          covEdge.isFreeRing(), covEdge.isRemovableRing())
    }

    private fun computeTolerance(covEdge: CoverageEdge, toleranceInner: Double, toleranceOuter: Double): Double {
      return if (covEdge.isInner()) toleranceInner else toleranceOuter
    }
  }
}
