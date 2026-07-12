/*
 * Copyright (c) 2020 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.algorithm.construct

import kotlin.jvm.JvmStatic
import kotlin.math.ln
import kotlin.math.max

import org.locationtech.jts.util.PriorityQueue

import org.locationtech.jts.algorithm.Centroid
import org.locationtech.jts.algorithm.InteriorPoint
import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.operation.distance.IndexedFacetDistance

/**
 * Constructs the Maximum Inscribed Circle for a
 * polygonal [Geometry], up to a specified tolerance.
 * The Maximum Inscribed Circle is determined by a point in the interior of the area
 * which has the farthest distance from the area boundary,
 * along with a boundary point at that distance.
 *
 * The class supports polygons with holes and multipolygons.
 *
 * The implementation uses a successive-approximation technique
 * over a grid of square cells covering the area geometry.
 * The grid is refined using a branch-and-bound algorithm.
 *
 * @author Martin Davis
 * @see LargestEmptyCircle
 * @see InteriorPoint
 * @see Centroid
 */
class MaximumInscribedCircle {

  private val inputGeom: Geometry
  private val tolerance: Double
  private val factory: GeometryFactory
  private val ptLocater: IndexedPointInAreaLocator
  private val indexedDistance: IndexedFacetDistance

  private var centerCell: Cell? = null
  private var centerPt: Coordinate? = null
  private var radiusPt: Coordinate? = null
  private var centerPoint: Point? = null
  private var radiusPoint: Point? = null

  /**
   * Creates a new instance of a Maximum Inscribed Circle computation.
   *
   * @param polygonal an areal geometry
   * @param tolerance the distance tolerance for computing the centre point (must be positive)
   * @throws IllegalArgumentException if the tolerance is non-positive, or the input geometry is non-polygonal or empty.
   */
  constructor(polygonal: Geometry, tolerance: Double) {
    if (tolerance <= 0) {
      throw IllegalArgumentException("Tolerance must be positive")
    }
    if (!(polygonal is Polygon || polygonal is MultiPolygon)) {
      throw IllegalArgumentException("Input geometry must be a Polygon or MultiPolygon")
    }
    if (polygonal.isEmpty()) {
      throw IllegalArgumentException("Empty input geometry is not supported")
    }

    this.inputGeom = polygonal
    this.factory = polygonal.getFactory()
    this.tolerance = tolerance
    ptLocater = IndexedPointInAreaLocator(polygonal)
    indexedDistance = IndexedFacetDistance(polygonal.getBoundary())
  }

  /**
   * Gets the center point of the maximum inscribed circle
   * (up to the tolerance distance).
   *
   * @return the center point of the maximum inscribed circle
   */
  fun getCenter(): Point {
    compute()
    return centerPoint!!
  }

  /**
   * Gets a point defining the radius of the Maximum Inscribed Circle.
   *
   * @return a point defining the radius of the Maximum Inscribed Circle
   */
  fun getRadiusPoint(): Point {
    compute()
    return radiusPoint!!
  }

  /**
   * Gets a line representing a radius of the Largest Empty Circle.
   *
   * @return a line from the center of the circle to a point on the edge
   */
  fun getRadiusLine(): LineString {
    compute()
    val radiusLine = factory.createLineString(
      arrayOf(centerPt!!.copy(), radiusPt!!.copy()))
    return radiusLine
  }

  /**
   * Computes the signed distance from a point to the area boundary.
   * Points outside the polygon are assigned a negative distance.
   *
   * @param p the point to compute the distance for
   * @return the signed distance to the area boundary (negative indicates outside the area)
   */
  private fun distanceToBoundary(p: Point): Double {
    val dist = indexedDistance.distance(p)
    val isOutide = Location.EXTERIOR == ptLocater.locate(p.getCoordinate()!!)
    if (isOutide) return -dist
    return dist
  }

  private fun distanceToBoundary(x: Double, y: Double): Double {
    val coord = Coordinate(x, y)
    val pt = factory.createPoint(coord)
    return distanceToBoundary(pt)
  }

  private fun compute() {
    // check if already computed
    if (centerCell != null) return

    // Priority queue of cells, ordered by maximum distance from boundary
    val cellQueue = PriorityQueue<Cell>()

    createInitialGrid(inputGeom.getEnvelopeInternal(), cellQueue)

    // initial candidate center point
    var farthestCell = createInterorPointCell(inputGeom)

    /**
     * Carry out the branch-and-bound search
     * of the cell space
     */
    val maxIter = computeMaximumIterations(inputGeom, tolerance)
    var iter: Long = 0
    while (!cellQueue.isEmpty() && iter < maxIter) {
      iter++
      // pick the most promising cell from the queue
      val cell = cellQueue.poll()!!

      //-- if cell must be closer than furthest, terminate since all remaining cells in queue are even closer.
      if (cell.getMaxDistance() < farthestCell.getDistance())
        break

      // update the circle center cell if the candidate is further from the boundary
      if (cell.getDistance() > farthestCell.getDistance()) {
        farthestCell = cell
      }
      /**
       * Refine this cell if the potential distance improvement
       * is greater than the required tolerance.
       */
      val potentialIncrease = cell.getMaxDistance() - farthestCell.getDistance()
      if (potentialIncrease > tolerance) {
        // split the cell into four sub-cells
        val h2 = cell.getHSide() / 2
        cellQueue.add(createCell(cell.getX() - h2, cell.getY() - h2, h2))
        cellQueue.add(createCell(cell.getX() + h2, cell.getY() - h2, h2))
        cellQueue.add(createCell(cell.getX() - h2, cell.getY() + h2, h2))
        cellQueue.add(createCell(cell.getX() + h2, cell.getY() + h2, h2))
      }
    }
    // the farthest cell is the best approximation to the MIC center
    centerCell = farthestCell
    val cPt = Coordinate(farthestCell.getX(), farthestCell.getY())
    centerPt = cPt
    val cPoint = factory.createPoint(cPt)
    centerPoint = cPoint
    // compute radius point
    val nearestPts = indexedDistance.nearestPoints(cPoint)
    val rPt = nearestPts[0].copy()
    radiusPt = rPt
    radiusPoint = factory.createPoint(rPt)
  }

  /**
   * Initializes the queue with a cell covering
   * the extent of the area.
   *
   * @param env the area extent to cover
   * @param cellQueue the queue to initialize
   */
  private fun createInitialGrid(env: Envelope, cellQueue: PriorityQueue<Cell>) {
    val cellSize = max(env.getWidth(), env.getHeight())
    val hSide = cellSize / 2.0

    // Check for flat collapsed input and if so short-circuit
    // Result will just be centroid
    if (cellSize == 0.0) return

    val centre = env.centre()!!
    cellQueue.add(createCell(centre.x, centre.y, hSide))
  }

  private fun createCell(x: Double, y: Double, hSide: Double): Cell {
    return Cell(x, y, hSide, distanceToBoundary(x, y))
  }

  // create a cell at an interior point
  private fun createInterorPointCell(geom: Geometry): Cell {
    val p = geom.getInteriorPoint()
    return Cell(p.getX(), p.getY(), 0.0, distanceToBoundary(p))
  }

  companion object {
    /**
     * Computes the center point of the Maximum Inscribed Circle
     * of a polygonal geometry, up to a given tolerance distance.
     *
     * @param polygonal a polygonal geometry
     * @param tolerance the distance tolerance for computing the center point
     * @return the center point of the maximum inscribed circle
     */
    @JvmStatic
    fun getCenter(polygonal: Geometry, tolerance: Double): Point {
      val mic = MaximumInscribedCircle(polygonal, tolerance)
      return mic.getCenter()
    }

    /**
     * Computes a radius line of the Maximum Inscribed Circle
     * of a polygonal geometry, up to a given tolerance distance.
     *
     * @param polygonal a polygonal geometry
     * @param tolerance the distance tolerance for computing the center point
     * @return a line from the center to a point on the circle
     */
    @JvmStatic
    fun getRadiusLine(polygonal: Geometry, tolerance: Double): LineString {
      val mic = MaximumInscribedCircle(polygonal, tolerance)
      return mic.getRadiusLine()
    }

    /**
     * Computes the maximum number of iterations allowed.
     * Uses a heuristic based on the size of the input geometry
     * and the tolerance distance.
     *
     * @param geom the input geometry
     * @param toleranceDist the tolerance distance
     * @return the maximum number of iterations allowed
     */
    internal fun computeMaximumIterations(geom: Geometry, toleranceDist: Double): Long {
      val diam = geom.getEnvelopeInternal().getDiameter()
      val ncells = diam / toleranceDist
      //-- Using log of ncells allows control over number of iterations
      var factor = ln(ncells).toInt()
      if (factor < 1) factor = 1
      return (2000 + 2000 * factor).toLong()
    }
  }

  /**
   * A square grid cell centered on a given point,
   * with a given half-side size, and having a given distance
   * to the area boundary.
   */
  private class Cell(x: Double, y: Double, hSide: Double, distanceToBoundary: Double) : Comparable<Cell> {

    private val x = x // cell center x
    private val y = y // cell center y
    private val hSide = hSide // half the cell size

    // the distance from cell center to area boundary
    private val distance = distanceToBoundary

    // the maximum possible distance to area boundary for points in this cell
    private val maxDist = distance + hSide * SQRT2

    fun getEnvelope(): Envelope {
      return Envelope(x - hSide, x + hSide, y - hSide, y + hSide)
    }

    fun getMaxDistance(): Double = maxDist

    fun getDistance(): Double = distance

    fun getHSide(): Double = hSide

    fun getX(): Double = x

    fun getY(): Double = y

    /**
     * For maximum efficieny sort the PriorityQueue with largest maxDistance at front.
     * Since Java PQ sorts least-first, need to invert the comparison
     */
    override fun compareTo(o: Cell): Int {
      return -maxDist.compareTo(o.maxDist)
    }

    companion object {
      private const val SQRT2 = 1.4142135623730951
    }
  }
}
