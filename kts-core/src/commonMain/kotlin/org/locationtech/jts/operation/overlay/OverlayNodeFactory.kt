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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geomgraph.DirectedEdgeStar
import org.locationtech.jts.geomgraph.Node
import org.locationtech.jts.geomgraph.NodeFactory
import org.locationtech.jts.geomgraph.PlanarGraph

/**
 * Creates nodes for use in the [PlanarGraph]s constructed during
 * overlay operations.
 *
 * @version 1.7
 */
class OverlayNodeFactory : NodeFactory() {
  override fun createNode(coord: Coordinate): Node {
    return Node(coord, DirectedEdgeStar())
  }
}
