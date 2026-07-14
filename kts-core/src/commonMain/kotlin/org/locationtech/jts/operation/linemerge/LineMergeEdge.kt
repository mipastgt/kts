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
package org.locationtech.jts.operation.linemerge

import org.locationtech.jts.geom.LineString
import org.locationtech.jts.planargraph.Edge

/**
 * An edge of a [LineMergeGraph]. The `marked` field indicates
 * whether this Edge has been logically deleted from the graph.
 *
 */
class LineMergeEdge
/**
 * Constructs a LineMergeEdge with vertices given by the specified LineString.
 */
(private val line: LineString) : Edge() {

  /**
   * Returns the LineString specifying the vertices of this edge.
   */
  fun getLine(): LineString {
    return line
  }
}
