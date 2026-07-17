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
package org.locationtech.jts.util

import kotlin.jvm.JvmField
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow

import org.locationtech.jts.algorithm.Angle
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.util.AffineTransformation

/**
 * Computes various kinds of common geometric shapes.
 * Provides various ways of specifying the location and extent
 * and rotations of the generated shapes,
 * as well as number of line segments used to form them.
 * 
 * **Example of usage:**
 * ```
 *  val gsf = GeometricShapeFactory()
 *  gsf.setSize(100.0)
 *  gsf.setNumPoints(100)
 *  gsf.setBase(Coordinate(100.0, 100.0))
 *  gsf.setRotation(0.5)
 *  val rect = gsf.createRectangle()
 * ```
 *
 */
open class GeometricShapeFactory
/**
 * Create a shape factory which will create shapes using the given
 * [GeometryFactory].
 *
 * @param geomFact the factory to use
 */
(geomFact: GeometryFactory) {
  @JvmField
  protected var geomFact: GeometryFactory = geomFact
  @JvmField
  protected var precModel: PrecisionModel = geomFact.getPrecisionModel()
  @JvmField
  protected var dim: Dimensions = Dimensions()
  @JvmField
  protected var nPts: Int = 100

  /**
   * Default is no rotation.
   */
  @JvmField
  protected var rotationAngle: Double = 0.0

  /**
   * Create a shape factory which will create shapes using the default
   * [GeometryFactory].
   */
  constructor() : this(GeometryFactory())

  open fun setEnvelope(env: Envelope) {
    dim.setEnvelope(env)
  }

  /**
   * Sets the location of the shape by specifying the base coordinate
   * (which in most cases is the
   * lower left point of the envelope containing the shape).
   *
   * @param base the base coordinate of the shape
   */
  open fun setBase(base: Coordinate) {
    dim.setBase(base)
  }

  /**
   * Sets the location of the shape by specifying the centre of
   * the shape's bounding box
   *
   * @param centre the centre coordinate of the shape
   */
  open fun setCentre(centre: Coordinate) {
    dim.setCentre(centre)
  }

  /**
   * Sets the total number of points in the created [Geometry].
   * The created geometry will have no more than this number of points,
   * unless more are needed to create a valid geometry.
   */
  open fun setNumPoints(nPts: Int) {
    this.nPts = nPts
  }

  /**
   * Sets the size of the extent of the shape in both x and y directions.
   *
   * @param size the size of the shape's extent
   */
  open fun setSize(size: Double) {
    dim.setSize(size)
  }

  /**
   * Sets the width of the shape.
   *
   * @param width the width of the shape
   */
  open fun setWidth(width: Double) {
    dim.setWidth(width)
  }

  /**
   * Sets the height of the shape.
   *
   * @param height the height of the shape
   */
  open fun setHeight(height: Double) {
    dim.setHeight(height)
  }

  /**
   * Sets the rotation angle to use for the shape.
   * The rotation is applied relative to the centre of the shape.
   *
   * @param radians the rotation angle in radians.
   */
  open fun setRotation(radians: Double) {
    rotationAngle = radians
  }

  protected open fun rotate(geom: Geometry): Geometry {
    if (rotationAngle != 0.0) {
      val trans = AffineTransformation.rotationInstance(
        rotationAngle,
        dim.getCentre().x, dim.getCentre().y
      )
      geom.apply(trans)
    }
    return geom
  }

  /**
   * Creates a rectangular [Polygon].
   *
   * @return a rectangular Polygon
   *
   */
  open fun createRectangle(): Polygon {
    var i: Int
    var ipt = 0
    var nSide = nPts / 4
    if (nSide < 1) nSide = 1
    val XsegLen = dim.getEnvelope().getWidth() / nSide
    val YsegLen = dim.getEnvelope().getHeight() / nSide

    val pts = arrayOfNulls<Coordinate>(4 * nSide + 1)
    val env = dim.getEnvelope()

    //double maxx = env.getMinX() + nSide * XsegLen;
    //double maxy = env.getMinY() + nSide * XsegLen;

    i = 0
    while (i < nSide) {
      val x = env.getMinX() + i * XsegLen
      val y = env.getMinY()
      pts[ipt++] = coord(x, y)
      i++
    }
    i = 0
    while (i < nSide) {
      val x = env.getMaxX()
      val y = env.getMinY() + i * YsegLen
      pts[ipt++] = coord(x, y)
      i++
    }
    i = 0
    while (i < nSide) {
      val x = env.getMaxX() - i * XsegLen
      val y = env.getMaxY()
      pts[ipt++] = coord(x, y)
      i++
    }
    i = 0
    while (i < nSide) {
      val x = env.getMinX()
      val y = env.getMaxY() - i * YsegLen
      pts[ipt++] = coord(x, y)
      i++
    }
    pts[ipt++] = Coordinate(pts[0]!!)

    @Suppress("UNCHECKED_CAST")
    val ring = geomFact.createLinearRing(pts as Array<Coordinate>)
    val poly = geomFact.createPolygon(ring)
    return rotate(poly) as Polygon
  }

  //* @deprecated use [createEllipse] instead
  /**
   * Creates a circular or elliptical [Polygon].
   *
   * @return a circle or ellipse
   */
  open fun createCircle(): Polygon {
    return createEllipse()
  }

  /**
   * Creates an elliptical [Polygon].
   * If the supplied envelope is square the
   * result will be a circle.
   *
   * @return an ellipse or circle
   */
  open fun createEllipse(): Polygon {

    val env = dim.getEnvelope()
    val xRadius = env.getWidth() / 2.0
    val yRadius = env.getHeight() / 2.0

    val centreX = env.getMinX() + xRadius
    val centreY = env.getMinY() + yRadius

    val pts = arrayOfNulls<Coordinate>(nPts + 1)
    var iPt = 0
    for (i in 0 until nPts) {
      val ang = i * (2 * PI / nPts)
      val x = xRadius * Angle.cosSnap(ang) + centreX
      val y = yRadius * Angle.sinSnap(ang) + centreY
      pts[iPt++] = coord(x, y)
    }
    pts[iPt] = Coordinate(pts[0]!!)

    @Suppress("UNCHECKED_CAST")
    val ring = geomFact.createLinearRing(pts as Array<Coordinate>)
    val poly = geomFact.createPolygon(ring)
    return rotate(poly) as Polygon
  }

  /**
   * Creates a squircular [Polygon].
   *
   * @return a squircle
   */
  open fun createSquircle(): Polygon {
    return createSupercircle(4.0)
  }

  /**
   * Creates a supercircular [Polygon]
   * of a given positive power.
   *
   * @return a supercircle
   */
  open fun createSupercircle(power: Double): Polygon {
    val recipPow = 1.0 / power

    val radius = dim.getMinSize() / 2
    val centre = dim.getCentre()

    val r4 = (radius).pow(power)
    val y0 = radius

    val xyInt = (r4 / 2).pow(recipPow)

    val nSegsInOct = nPts / 8
    val totPts = nSegsInOct * 8 + 1
    val pts = arrayOfNulls<Coordinate>(totPts)
    val xInc = xyInt / nSegsInOct

    for (i in 0..nSegsInOct) {
      var x = 0.0
      var y = y0
      if (i != 0) {
        x = xInc * i
        val x4 = (x).pow(power)
        y = (r4 - x4).pow(recipPow)
      }
      pts[i] = coordTrans(x, y, centre)
      pts[2 * nSegsInOct - i] = coordTrans(y, x, centre)

      pts[2 * nSegsInOct + i] = coordTrans(y, -x, centre)
      pts[4 * nSegsInOct - i] = coordTrans(x, -y, centre)

      pts[4 * nSegsInOct + i] = coordTrans(-x, -y, centre)
      pts[6 * nSegsInOct - i] = coordTrans(-y, -x, centre)

      pts[6 * nSegsInOct + i] = coordTrans(-y, x, centre)
      pts[8 * nSegsInOct - i] = coordTrans(-x, y, centre)
    }
    pts[pts.size - 1] = Coordinate(pts[0]!!)

    @Suppress("UNCHECKED_CAST")
    val ring = geomFact.createLinearRing(pts as Array<Coordinate>)
    val poly = geomFact.createPolygon(ring)
    return rotate(poly) as Polygon
  }

  /**
   * Creates an elliptical arc, as a [LineString].
   * The arc is always created in a counter-clockwise direction.
   * This can easily be reversed if required by using
   * {#link LineString.reverse()}
   *
   * @param startAng start angle in radians
   * @param angExtent size of angle in radians
   * @return an elliptical arc
   */
  open fun createArc(
    startAng: Double,
    angExtent: Double
  ): LineString {
    val env = dim.getEnvelope()
    val xRadius = env.getWidth() / 2.0
    val yRadius = env.getHeight() / 2.0

    val centreX = env.getMinX() + xRadius
    val centreY = env.getMinY() + yRadius

    var angSize = angExtent
    if (angSize <= 0.0 || angSize > Angle.PI_TIMES_2)
      angSize = Angle.PI_TIMES_2
    val angInc = angSize / (nPts - 1)

    val pts = arrayOfNulls<Coordinate>(nPts)
    var iPt = 0
    for (i in 0 until nPts) {
      val ang = startAng + i * angInc
      val x = xRadius * Angle.cosSnap(ang) + centreX
      val y = yRadius * Angle.sinSnap(ang) + centreY
      pts[iPt++] = coord(x, y)
    }
    @Suppress("UNCHECKED_CAST")
    val line = geomFact.createLineString(pts as Array<Coordinate>)
    return rotate(line) as LineString
  }

  /**
   * Creates an elliptical arc polygon.
   * The polygon is formed from the specified arc of an ellipse
   * and the two radii connecting the endpoints to the centre of the ellipse.
   *
   * @param startAng start angle in radians
   * @param angExtent size of angle in radians
   * @return an elliptical arc polygon
   */
  open fun createArcPolygon(startAng: Double, angExtent: Double): Polygon {
    val env = dim.getEnvelope()
    val xRadius = env.getWidth() / 2.0
    val yRadius = env.getHeight() / 2.0

    val centreX = env.getMinX() + xRadius
    val centreY = env.getMinY() + yRadius

    var angSize = angExtent
    if (angSize <= 0.0 || angSize > Angle.PI_TIMES_2)
      angSize = Angle.PI_TIMES_2
    val angInc = angSize / (nPts - 1)
    // double check = angInc * nPts;
    // double checkEndAng = startAng + check;

    val pts = arrayOfNulls<Coordinate>(nPts + 2)

    var iPt = 0
    pts[iPt++] = coord(centreX, centreY)
    for (i in 0 until nPts) {
      val ang = startAng + angInc * i

      val x = xRadius * Angle.cosSnap(ang) + centreX
      val y = yRadius * Angle.sinSnap(ang) + centreY
      pts[iPt++] = coord(x, y)
    }
    pts[iPt++] = coord(centreX, centreY)
    @Suppress("UNCHECKED_CAST")
    val ring = geomFact.createLinearRing(pts as Array<Coordinate>)
    val poly = geomFact.createPolygon(ring)
    return rotate(poly) as Polygon
  }

  protected open fun coord(x: Double, y: Double): Coordinate {
    val pt = Coordinate(x, y)
    precModel.makePrecise(pt)
    return pt
  }

  protected open fun coordTrans(x: Double, y: Double, trans: Coordinate): Coordinate {
    return coord(x + trans.x, y + trans.y)
  }

  protected class Dimensions {
    @JvmField
    var base: Coordinate? = null
    @JvmField
    var centre: Coordinate? = null
    @JvmField
    var width: Double = 0.0
    @JvmField
    var height: Double = 0.0

    fun setBase(base: Coordinate?) {
      this.base = base
    }

    fun getBase(): Coordinate? {
      return base
    }

    fun setCentre(centre: Coordinate?) {
      this.centre = centre
    }

    fun getCentre(): Coordinate {
      if (centre == null) {
        centre = Coordinate(base!!.x + width / 2, base!!.y + height / 2)
      }
      return centre!!
    }

    fun setSize(size: Double) {
      height = size
      width = size
    }

    fun getMinSize(): Double {
      return min(width, height)
    }

    fun setWidth(width: Double) {
      this.width = width
    }

    fun getWidth(): Double {
      return width
    }

    fun getHeight(): Double {
      return height
    }

    fun setHeight(height: Double) {
      this.height = height
    }

    fun setEnvelope(env: Envelope) {
      this.width = env.getWidth()
      this.height = env.getHeight()
      this.base = Coordinate(env.getMinX(), env.getMinY())
      this.centre = Coordinate(env.centre()!!)
    }

    fun getEnvelope(): Envelope {
      val base = base
      if (base != null) {
        return Envelope(base.x, base.x + width, base.y, base.y + height)
      }
      val centre = centre
      if (centre != null) {
        return Envelope(
          centre.x - width / 2, centre.x + width / 2,
          centre.y - height / 2, centre.y + height / 2
        )
      }
      return Envelope(0.0, width, 0.0, height)
    }
  }
}
