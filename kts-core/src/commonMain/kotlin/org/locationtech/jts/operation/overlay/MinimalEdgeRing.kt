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
package org.locationtech.jts.operation.overlay

import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geomgraph.DirectedEdge
import org.locationtech.jts.geomgraph.Edge
import org.locationtech.jts.geomgraph.EdgeRing

/**
 * A ring of [Edge]s with the property that no node
 * has degree greater than 2.  These are the form of rings required
 * to represent polygons under the OGC SFS spatial data model.
 *
 * @version 1.7
 * @see org.locationtech.jts.operation.overlay.MaximalEdgeRing
 */
class MinimalEdgeRing(start: DirectedEdge, geometryFactory: GeometryFactory) :
  EdgeRing(start, geometryFactory) {

  override fun getNext(de: DirectedEdge): DirectedEdge? {
    return de.getNextMin()
  }

  override fun setEdgeRing(de: DirectedEdge, er: EdgeRing) {
    de.setMinEdgeRing(er)
  }
}
