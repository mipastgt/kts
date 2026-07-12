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
package org.locationtech.jts.geom.impl

import kotlin.jvm.JvmStatic


import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFactory

/**
 * Creates [CoordinateSequence]s represented as an array of [Coordinate]s.
 *
 * @version 1.7
 */
class CoordinateArraySequenceFactory private constructor() : CoordinateSequenceFactory {

  private fun readResolve(): Any {
    // http://www.javaworld.com/javaworld/javatips/jw-javatip122.html
    return instance()
  }

  /**
   * Returns a [CoordinateArraySequence] based on the given array (the array is
   * not copied).
   *
   * @param coordinates
   *            the coordinates, which may not be null nor contain null
   *            elements
   */
  override fun create(coordinates: Array<Coordinate>?): CoordinateSequence {
    return CoordinateArraySequence(coordinates)
  }

  /**
   * @see org.locationtech.jts.geom.CoordinateSequenceFactory.create
   */
  override fun create(coordSeq: CoordinateSequence?): CoordinateSequence {
    return CoordinateArraySequence(coordSeq)
  }

  /**
   * The created sequence dimension is clamped to be &lt;= 3.
   *
   * @see org.locationtech.jts.geom.CoordinateSequenceFactory.create
   */
  override fun create(size: Int, dimension: Int): CoordinateSequence {
    var dimension = dimension
    if (dimension > 3)
      dimension = 3
    //throw new IllegalArgumentException("dimension must be <= 3");

    // handle bogus dimension
    if (dimension < 2)
      dimension = 2

    return CoordinateArraySequence(size, dimension)
  }

  override fun create(size: Int, dimension: Int, measures: Int): CoordinateSequence {
    var measures = measures
    var spatial = dimension - measures

    if (measures > 1) {
      measures = 1 // clip measures
      //throw new IllegalArgumentException("measures must be <= 1");
    }
    if (spatial > 3) {
      spatial = 3 // clip spatial dimension
      //throw new IllegalArgumentException("spatial dimension must be <= 3");
    }

    if (spatial < 2)
      spatial = 2 // handle bogus spatial dimension

    return CoordinateArraySequence(size, spatial + measures, measures)
  }

  companion object {
    private val instanceObject = CoordinateArraySequenceFactory()

    /**
     * Returns the singleton instance of [CoordinateArraySequenceFactory]
     */
    @JvmStatic
    fun instance(): CoordinateArraySequenceFactory {
      return instanceObject
    }
  }
}
