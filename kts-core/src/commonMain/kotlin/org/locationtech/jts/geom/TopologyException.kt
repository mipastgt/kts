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
 * Indicates an invalid or inconsistent topological situation encountered during processing
 *
 */
class TopologyException : RuntimeException {

  private var pt: Coordinate? = null

  constructor(msg: String) : super(msg)

  constructor(msg: String, pt: Coordinate) : super(msgWithCoord(msg, pt)) {
    this.pt = Coordinate(pt)
  }

  fun getCoordinate(): Coordinate? {
    return pt
  }

  companion object {
    private fun msgWithCoord(msg: String, pt: Coordinate?): String {
      if (pt != null)
        return msg + " [ " + pt + " ]"
      return msg
    }
  }
}
