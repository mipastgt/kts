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
package org.locationtech.jts.simplify

import org.locationtech.jts.geom.Envelope

internal class RingHullIndex {

  //TODO: use a proper spatial index
  val hulls: MutableList<RingHull> = ArrayList()

  fun add(ringHull: RingHull) {
    hulls.add(ringHull)
  }

  fun query(queryEnv: Envelope): MutableList<RingHull> {
    val result: MutableList<RingHull> = ArrayList()
    for (hull in hulls) {
      val envHull = hull.getEnvelope()
      if (queryEnv.intersects(envHull)) {
        result.add(hull)
      }
    }
    return result
  }
}
