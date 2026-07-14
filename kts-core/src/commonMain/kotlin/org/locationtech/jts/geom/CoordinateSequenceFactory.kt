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
package org.locationtech.jts.geom

/**
 * A factory to create concrete instances of [CoordinateSequence]s.
 * Used to configure [GeometryFactory]s
 * to provide specific kinds of CoordinateSequences.
 *
 */
interface CoordinateSequenceFactory {

  /**
   * Returns a [CoordinateSequence] based on the given array.
   * Whether the array is copied or simply referenced
   * is implementation-dependent.
   * This method must handle null arguments by creating an empty sequence.
   *
   * @param coordinates the coordinates
   */
  fun create(coordinates: Array<Coordinate>?): CoordinateSequence

  /**
   * Creates a [CoordinateSequence] which is a copy
   * of the given [CoordinateSequence].
   * This method must handle null arguments by creating an empty sequence.
   *
   * @param coordSeq the coordinate sequence to copy
   */
  fun create(coordSeq: CoordinateSequence?): CoordinateSequence

  /**
   * Creates a [CoordinateSequence] of the specified size and dimension.
   * For this to be useful, the [CoordinateSequence] implementation must
   * be mutable.
   * 
   * If the requested dimension is larger than the CoordinateSequence implementation
   * can provide, then a sequence of maximum possible dimension should be created.
   * An error should not be thrown.
   *
   * @param size the number of coordinates in the sequence
   * @param dimension the dimension of the coordinates in the sequence (if user-specifiable,
   * otherwise ignored)
   */
  fun create(size: Int, dimension: Int): CoordinateSequence

  /**
   * Creates a [CoordinateSequence] of the specified size and dimension with measure support.
   * For this to be useful, the [CoordinateSequence] implementation must
   * be mutable.
   * 
   * If the requested dimension or measures are larger than the CoordinateSequence implementation
   * can provide, then a sequence of maximum possible dimension should be created.
   * An error should not be thrown.
   *
   * @param size the number of coordinates in the sequence
   * @param dimension the dimension of the coordinates in the sequence (if user-specifiable,
   * otherwise ignored)
   * @param measures the number of measures of the coordinates in the sequence (if user-specifiable,
   * otherwise ignored)
   */
  fun create(size: Int, dimension: Int, measures: Int): CoordinateSequence {
    return create(size, dimension)
  }
}
