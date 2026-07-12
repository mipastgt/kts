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
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygonal
import org.locationtech.jts.operation.distance.IndexedFacetDistance

/**
 * Constructs the Largest Empty Circle for a set
 * of obstacle geometries, up to a given accuracy distance tolerance.
 * The obstacles may be any combination of point, linear and polygonal geometries.
 *
 * The implementation uses a successive-approximation technique
 * over a grid of square cells covering the obstacles and boundary.
 * The grid is refined using a branch-and-bound algorithm.
 *
 * @author Martin Davis
 *
 * @see MaximumInscribedCircle
 * @see InteriorPoint
 * @see Centroid
 */
class LargestEmptyCircle {

  private val obstacles: Geometry
  private val boundary: Geometry?
  private val tolerance: Double

  private val factory: GeometryFactory
  private val obstacleDistance: IndexedDistanceToPoint
  private var boundaryPtLocater: IndexedPointInAreaLocator? = null
  private var boundaryDistance: IndexedFacetDistance? = null
  private var gridEnv: Envelope? = null
  private var farthestCell: Cell? = null

  private var centerCell: Cell? = null
  private var centerPt: Coordinate? = null
  private var centerPoint: Point? = null
  private var radiusPt: Coordinate? = null
  private var radiusPoint: Point? = null
  private var bounds: Geometry? = null

  /**
   * Creates a new instance of a Largest Empty Circle construction,
   * interior-disjoint to a set of obstacle geometries
   * and having its center within a polygonal boundary.
   *
   * @param obstacles a non-empty geometry representing the obstacles
   * @param boundary a polygonal geometry (may be null or empty)
   * @param tolerance a distance tolerance for computing the circle center point (a positive value)
   */
  constructor(obstacles: Geometry?, boundary: Geometry?, tolerance: Double) {
    if (obstacles == null || obstacles.isEmpty()) {
      throw IllegalArgumentException("Obstacles geometry is empty or null")
    }
    if (boundary != null && boundary !is Polygonal) {
      throw IllegalArgumentException("Boundary must be polygonal")
    }
    if (tolerance <= 0) {
      throw IllegalArgumentException("Accuracy tolerance is non-positive: " + tolerance)
    }
    this.obstacles = obstacles
    this.boundary = boundary
    this.factory = obstacles.getFactory()
    this.tolerance = tolerance
    obstacleDistance = IndexedDistanceToPoint(obstacles)
  }

  /**
   * Gets the center point of the Largest Empty Circle
   * (up to the tolerance distance).
   *
   * @return the center point of the Largest Empty Circle
   */
  fun getCenter(): Point {
    compute()
    return centerPoint!!
  }

  /**
   * Gets a point defining the radius of the Largest Empty Circle.
   *
   * @return a point defining the radius of the Largest Empty Circle
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
   * Computes the signed distance from a point to the constraints
   * (obstacles and boundary).
   *
   * @param p the point to compute the distance for
   * @return the signed distance to the constraints (negative indicates outside the boundary)
   */
  private fun distanceToConstraints(p: Point): Double {
    val isOutide = Location.EXTERIOR == boundaryPtLocater!!.locate(p.getCoordinate()!!)
    if (isOutide) {
      val boundaryDist = boundaryDistance!!.distance(p)
      return -boundaryDist
    }
    val dist = obstacleDistance.distance(p)
    return dist
  }

  private fun distanceToConstraints(x: Double, y: Double): Double {
    val coord = Coordinate(x, y)
    val pt = factory.createPoint(coord)
    return distanceToConstraints(pt)
  }

  private fun initBoundary() {
    var bnds = this.boundary
    if (bnds == null || bnds.isEmpty()) {
      bnds = obstacles.convexHull()
    }
    bounds = bnds
    //-- the centre point must be in the extent of the boundary
    gridEnv = bnds.getEnvelopeInternal()
    // if bounds does not enclose an area cannot create a ptLocater
    if (bnds.getDimension() >= 2) {
      boundaryPtLocater = IndexedPointInAreaLocator(bnds)
      boundaryDistance = IndexedFacetDistance(bnds)
    }
  }

  private fun compute() {
    initBoundary()

    // check if already computed
    if (centerCell != null) return

    // if boundaryPtLocater is not present then result is degenerate (represented as zero-radius circle)
    if (boundaryPtLocater == null) {
      val pt = obstacles.getCoordinate()!!
      centerPt = pt.copy()
      centerPoint = factory.createPoint(pt)
      radiusPt = pt.copy()
      radiusPoint = factory.createPoint(pt)
      return
    }

    // Priority queue of cells, ordered by decreasing distance from constraints
    val cellQueue = PriorityQueue<Cell>()

    //-- grid covers extent of obstacles and boundary (if any)
    createInitialGrid(gridEnv!!, cellQueue)

    // use the area centroid as the initial candidate center point
    var farthest = createCentroidCell(obstacles)
    farthestCell = farthest

    /**
     * Carry out the branch-and-bound search
     * of the cell space
     */
    val maxIter = MaximumInscribedCircle.computeMaximumIterations(bounds!!, tolerance)
    var iter: Long = 0
    while (!cellQueue.isEmpty() && iter < maxIter) {
      iter++
      // pick the cell with greatest distance from the queue
      val cell = cellQueue.poll()!!

      // update the center cell if the candidate is further from the constraints
      if (cell.getDistance() > farthest.getDistance()) {
        farthest = cell
        farthestCell = farthest
      }

      /**
       * If this cell may contain a better approximation to the center
       * of the empty circle, then refine it (partition into subcells
       * which are added into the queue for further processing).
       */
      if (mayContainCircleCenter(cell)) {
        // split the cell into four sub-cells
        val h2 = cell.getHSide() / 2
        cellQueue.add(createCell(cell.getX() - h2, cell.getY() - h2, h2))
        cellQueue.add(createCell(cell.getX() + h2, cell.getY() - h2, h2))
        cellQueue.add(createCell(cell.getX() - h2, cell.getY() + h2, h2))
        cellQueue.add(createCell(cell.getX() + h2, cell.getY() + h2, h2))
      }
    }
    // the farthest cell is the best approximation to the LEC center
    centerCell = farthest
    // compute center point
    val cPt = Coordinate(farthest.getX(), farthest.getY())
    centerPt = cPt
    val cPoint = factory.createPoint(cPt)
    centerPoint = cPoint
    // compute radius point
    val nearestPts = obstacleDistance.nearestPoints(cPoint)
    val rPt = nearestPts[0].copy()
    radiusPt = rPt
    radiusPoint = factory.createPoint(rPt)
  }

  /**
   * Tests whether a cell may contain the circle center,
   * and thus should be refined (split into subcells
   * to be investigated further.)
   *
   * @param cell the cell to test
   * @return true if the cell might contain the circle center
   */
  private fun mayContainCircleCenter(cell: Cell): Boolean {
    /**
     * Every point in the cell lies outside the boundary,
     * so they cannot be the center point
     */
    if (cell.isFullyOutside())
      return false

    /**
     * The cell is outside, but overlaps the boundary
     * so it may contain a point which should be checked.
     */
    if (cell.isOutside()) {
      val isOverlapSignificant = cell.getMaxDistance() > tolerance
      return isOverlapSignificant
    }

    /**
     * Cell is inside the boundary. It may contain the center
     * if the maximum possible distance is greater than the current distance
     * (up to tolerance).
     */
    val potentialIncrease = cell.getMaxDistance() - farthestCell!!.getDistance()
    return potentialIncrease > tolerance
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

  private fun createCell(x: Double, y: Double, h: Double): Cell {
    return Cell(x, y, h, distanceToConstraints(x, y))
  }

  // create a cell centered on area centroid
  private fun createCentroidCell(geom: Geometry): Cell {
    val p = geom.getCentroid()
    return Cell(p.getX(), p.getY(), 0.0, distanceToConstraints(p))
  }

  /**
   * A square grid cell centered on a given point
   * with a given side half-length,
   * and having a given distance from the center point to the constraints.
   */
  private class Cell(x: Double, y: Double, hSide: Double, distanceToConstraints: Double) : Comparable<Cell> {

    private val x = x // cell center x
    private val y = y // cell center y
    private val hSide = hSide // half the cell size

    // the distance from cell center to constraints
    private val distance = distanceToConstraints

    // the maximum possible distance to the constraints for points in this cell
    private val maxDist = distance + hSide * SQRT2

    fun isFullyOutside(): Boolean {
      return getMaxDistance() < 0
    }

    fun isOutside(): Boolean {
      return distance < 0
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

  companion object {
    /**
     * Computes the center point of the Largest Empty Circle
     * interior-disjoint to a set of obstacles,
     * with accuracy to a given tolerance distance.
     *
     * @param obstacles a geometry representing the obstacles
     * @param tolerance the distance tolerance for computing the center point
     * @return the center point of the Largest Empty Circle
     */
    @JvmStatic
    fun getCenter(obstacles: Geometry, tolerance: Double): Point {
      return getCenter(obstacles, null, tolerance)
    }

    /**
     * Computes the center point of the Largest Empty Circle
     * interior-disjoint to a set of obstacles and within a polygonal boundary,
     * with accuracy to a given tolerance distance.
     *
     * @param obstacles a geometry representing the obstacles
     * @param boundary a polygonal geometry to contain the LEC center
     * @param tolerance the distance tolerance for computing the center point
     * @return the center point of the Largest Empty Circle
     */
    @JvmStatic
    fun getCenter(obstacles: Geometry, boundary: Geometry?, tolerance: Double): Point {
      val lec = LargestEmptyCircle(obstacles, boundary, tolerance)
      return lec.getCenter()
    }

    /**
     * Computes a radius line of the Largest Empty Circle
     * interior-disjoint to a set of obstacles,
     * with accuracy to a given tolerance distance.
     *
     * @param obstacles a geometry representing the obstacles
     * @param tolerance the distance tolerance for computing the center point
     * @return a line from the center of the circle to a point on the edge
     */
    @JvmStatic
    fun getRadiusLine(obstacles: Geometry, tolerance: Double): LineString {
      return getRadiusLine(obstacles, null, tolerance)
    }

    /**
     * Computes a radius line of the Largest Empty Circle
     * interior-disjoint to a set of obstacles and within a polygonal boundary,
     * with accuracy to a given tolerance distance.
     *
     * @param obstacles a geometry representing the obstacles
     * @param boundary a polygonal geometry to contain the LEC center
     * @param tolerance the distance tolerance for computing the center point
     * @return a line from the center of the circle to a point on the edge
     */
    @JvmStatic
    fun getRadiusLine(obstacles: Geometry, boundary: Geometry?, tolerance: Double): LineString {
      val lec = LargestEmptyCircle(obstacles, boundary, tolerance)
      return lec.getRadiusLine()
    }
  }
}
