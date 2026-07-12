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

package org.locationtech.jts.geom.util

import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmField
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.util.GeometricShapeFactory

/**
 * Creates geometries which are shaped like multi-armed stars
 * with each arm shaped like a sine wave.
 * These kinds of geometries are useful as a more complex
 * geometry for testing algorithms.
 *
 * @author Martin Davis
 *
 */
class SineStarFactory : GeometricShapeFactory {

  @JvmField
  protected var numArms = 8

  @JvmField
  protected var armLengthRatio = 0.5

  /**
   * Creates a factory which will create sine stars using the default
   * [GeometryFactory].
   */
  constructor() : super()

  /**
   * Creates a factory which will create sine stars using the given
   * [GeometryFactory].
   *
   * @param geomFact the factory to use
   */
  constructor(geomFact: GeometryFactory) : super(geomFact)

  /**
   * Sets the number of arms in the star
   *
   * @param numArms the number of arms to generate
   */
  fun setNumArms(numArms: Int) {
    this.numArms = numArms
  }

  /**
   * Sets the ratio of the length of each arm to the radius of the star.
   * A smaller number makes the arms shorter.
   * Value should be between 0.0 and 1.0
   *
   * @param armLengthRatio the ratio determining the length of them arms.
   */
  fun setArmLengthRatio(armLengthRatio: Double) {
    this.armLengthRatio = armLengthRatio
  }

  /**
   * Generates the geometry for the sine star
   *
   * @return the geometry representing the sine star
   */
  fun createSineStar(): Geometry {
    val env = dim.getEnvelope()
    val radius = env.getWidth() / 2.0

    var armRatio = armLengthRatio
    if (armRatio < 0.0)
      armRatio = 0.0
    if (armRatio > 1.0)
      armRatio = 1.0

    val armMaxLen = armRatio * radius
    val insideRadius = (1 - armRatio) * radius

    val centreX = env.getMinX() + radius
    val centreY = env.getMinY() + radius

    val pts = arrayOfNulls<Coordinate>(nPts + 1)
    var iPt = 0
    for (i in 0 until nPts) {
      // the fraction of the way through the current arm - in [0,1]
      val ptArcFrac = (i / nPts.toDouble()) * numArms
      val armAngFrac = ptArcFrac - floor(ptArcFrac)

      // the angle for the current arm - in [0,2Pi]
      // (each arm is a complete sine wave cycle)
      val armAng = 2 * PI * armAngFrac
      // the current length of the arm
      val armLenFrac = (cos(armAng) + 1.0) / 2.0

      // the current radius of the curve (core + arm)
      val curveRadius = insideRadius + armMaxLen * armLenFrac

      // the current angle of the curve
      val ang = i * (2 * PI / nPts)
      val x = curveRadius * cos(ang) + centreX
      val y = curveRadius * sin(ang) + centreY
      pts[iPt++] = coord(x, y)
    }
    pts[iPt] = Coordinate(pts[0]!!)

    @Suppress("UNCHECKED_CAST")
    val ring = geomFact.createLinearRing(pts as Array<Coordinate>)
    val poly = geomFact.createPolygon(ring)
    return poly
  }

  companion object {
    /**
     * Creates a sine star with the given parameters.
     *
     * @param origin the origin point
     * @param size the size of the star
     * @param nPts the number of points in the star
     * @param nArms the number of arms to generate
     * @param armLengthRatio the arm length ratio
     * @return a sine star shape
     */
    @JvmStatic
    fun create(origin: Coordinate, size: Double, nPts: Int, nArms: Int, armLengthRatio: Double): Geometry {
      val gsf = SineStarFactory()
      gsf.setCentre(origin)
      gsf.setSize(size)
      gsf.setNumPoints(nPts)
      gsf.setArmLengthRatio(armLengthRatio)
      gsf.setNumArms(nArms)
      val poly = gsf.createSineStar()
      return poly
    }
  }
}
