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
package org.locationtech.jts.operation.relate

import org.locationtech.jts.algorithm.BoundaryNodeRule
import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.PointLocator
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.IntersectionMatrix
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geomgraph.Edge
import org.locationtech.jts.geomgraph.EdgeEnd
import org.locationtech.jts.geomgraph.GeometryGraph
import org.locationtech.jts.geomgraph.Label
import org.locationtech.jts.geomgraph.Node
import org.locationtech.jts.geomgraph.NodeMap
import org.locationtech.jts.geomgraph.index.SegmentIntersector
import org.locationtech.jts.operation.BoundaryOp
import org.locationtech.jts.util.Assert

/**
 * Computes the topological relationship between two Geometries.
 *
 *
 * RelateComputer does not need to build a complete graph structure to compute
 * the IntersectionMatrix.  The relationship between the geometries can
 * be computed by simply examining the labelling of edges incident on each node.
 *
 *
 * RelateComputer does not currently support arbitrary GeometryCollections.
 * This is because GeometryCollections can contain overlapping Polygons.
 * In order to correct compute relate on overlapping Polygons, they
 * would first need to be noded and merged (if not explicitly, at least
 * implicitly).
 *
 */
internal class RelateComputer(private val arg: Array<GeometryGraph>) {

  private val li: LineIntersector = RobustLineIntersector()
  private val ptLocator = PointLocator()
  private val nodes = NodeMap(RelateNodeFactory())
  // this intersection matrix will hold the results compute for the relate
  private var im: IntersectionMatrix? = null
  private val isolatedEdges: MutableList<Edge> = ArrayList()

  // the intersection point found (if any)
  private var invalidPoint: Coordinate? = null

  fun computeIM(): IntersectionMatrix {
    val im = IntersectionMatrix()
    // since Geometries are finite and embedded in a 2-D space, the EE element must always be 2
    im.set(Location.EXTERIOR, Location.EXTERIOR, 2)

    // if the Geometries don't overlap there is nothing to do
    if (!arg[0].getGeometry()!!.getEnvelopeInternal().intersects(
        arg[1].getGeometry()!!.getEnvelopeInternal()
      )
    ) {
      computeDisjointIM(im, arg[0].getBoundaryNodeRule())
      return im
    }
    arg[0].computeSelfNodes(li, false)
    arg[1].computeSelfNodes(li, false)

    // compute intersections between edges of the two input geometries
    val intersector = arg[0].computeEdgeIntersections(arg[1], li, false)
//System.out.println("computeIM: # segment intersection tests: " + intersector.numTests);
    computeIntersectionNodes(0)
    computeIntersectionNodes(1)
    /**
     * Copy the labelling for the nodes in the parent Geometries.  These override
     * any labels determined by intersections between the geometries.
     */
    copyNodesAndLabels(0)
    copyNodesAndLabels(1)

    // complete the labelling for any nodes which only have a label for a single geometry
//Debug.addWatch(nodes.find(new Coordinate(110, 200)));
//Debug.printWatch();
    labelIsolatedNodes()
//Debug.printWatch();

    // If a proper intersection was found, we can set a lower bound on the IM.
    computeProperIntersectionIM(intersector, im)

    /**
     * Now process improper intersections
     * (eg where one or other of the geometries has a vertex at the intersection point)
     * We need to compute the edge graph at all nodes to determine the IM.
     */

    // build EdgeEnds for all intersections
    val eeBuilder = EdgeEndBuilder()
    val ee0 = eeBuilder.computeEdgeEnds(arg[0].getEdgeIterator())
    insertEdgeEnds(ee0)
    val ee1 = eeBuilder.computeEdgeEnds(arg[1].getEdgeIterator())
    insertEdgeEnds(ee1)

//Debug.println("==== NodeList ===");
//Debug.print(nodes);

    labelNodeEdges()

    /**
     * Compute the labeling for isolated components
     *
     * Isolated components are components that do not touch any other components in the graph.
     * They can be identified by the fact that they will
     * contain labels containing ONLY a single element, the one for their parent geometry.
     * We only need to check components contained in the input graphs, since
     * isolated components will not have been replaced by new components formed by intersections.
     */
//debugPrintln("Graph A isolated edges - ");
    labelIsolatedEdges(0, 1)
//debugPrintln("Graph B isolated edges - ");
    labelIsolatedEdges(1, 0)

    // update the IM from all components
    updateIM(im)
    return im
  }

  private fun insertEdgeEnds(ee: List<*>) {
    val i = ee.iterator()
    while (i.hasNext()) {
      val e = i.next() as EdgeEnd
      nodes.add(e)
    }
  }

  private fun computeProperIntersectionIM(intersector: SegmentIntersector, im: IntersectionMatrix) {
    // If a proper intersection is found, we can set a lower bound on the IM.
    val dimA = arg[0].getGeometry()!!.getDimension()
    val dimB = arg[1].getGeometry()!!.getDimension()
    val hasProper = intersector.hasProperIntersection()
    val hasProperInterior = intersector.hasProperInteriorIntersection()

    // For Geometry's of dim 0 there can never be proper intersections.

    /**
     * If edge segments of Areas properly intersect, the areas must properly overlap.
     */
    if (dimA == 2 && dimB == 2) {
      if (hasProper) im.setAtLeast("212101212")
    } else if (dimA == 2 && dimB == 1) {
      if (hasProper) im.setAtLeast("FFF0FFFF2")
      if (hasProperInterior) im.setAtLeast("1FFFFF1FF")
    } else if (dimA == 1 && dimB == 2) {
      if (hasProper) im.setAtLeast("F0FFFFFF2")
      if (hasProperInterior) im.setAtLeast("1F1FFFFFF")
    } else if (dimA == 1 && dimB == 1) {
      if (hasProperInterior) im.setAtLeast("0FFFFFFFF")
    }
  }

  /**
   * Copy all nodes from an arg geometry into this graph.
   * The node label in the arg geometry overrides any previously computed
   * label for that argIndex.
   */
  private fun copyNodesAndLabels(argIndex: Int) {
    val i = arg[argIndex].getNodeIterator()
    while (i.hasNext()) {
      val graphNode = i.next()
      val newNode = nodes.addNode(graphNode.getCoordinate()!!)
      newNode.setLabel(argIndex, graphNode.getLabel()!!.getLocation(argIndex))
//node.print(System.out);
    }
  }

  /**
   * Insert nodes for all intersections on the edges of a Geometry.
   * Label the created nodes the same as the edge label if they do not already have a label.
   * This allows nodes created by either self-intersections or
   * mutual intersections to be labelled.
   * Endpoint nodes will already be labelled from when they were inserted.
   */
  private fun computeIntersectionNodes(argIndex: Int) {
    val i = arg[argIndex].getEdgeIterator()
    while (i.hasNext()) {
      val e = i.next() as Edge
      val eLoc = e.getLabel()!!.getLocation(argIndex)
      val eiIt = e.getEdgeIntersectionList().iterator()
      while (eiIt.hasNext()) {
        val ei = eiIt.next()
        val n = nodes.addNode(ei.coord) as RelateNode
        if (eLoc == Location.BOUNDARY)
          n.setLabelBoundary(argIndex)
        else {
          if (n.getLabel()!!.isNull(argIndex))
            n.setLabel(argIndex, Location.INTERIOR)
        }
//Debug.println(n);
      }
    }
  }

  /**
   * For all intersections on the edges of a Geometry,
   * label the corresponding node IF it doesn't already have a label.
   * This allows nodes created by either self-intersections or
   * mutual intersections to be labelled.
   * Endpoint nodes will already be labelled from when they were inserted.
   */
  private fun labelIntersectionNodes(argIndex: Int) {
    val i = arg[argIndex].getEdgeIterator()
    while (i.hasNext()) {
      val e = i.next() as Edge
      val eLoc = e.getLabel()!!.getLocation(argIndex)
      val eiIt = e.getEdgeIntersectionList().iterator()
      while (eiIt.hasNext()) {
        val ei = eiIt.next()
        val n = nodes.find(ei.coord) as RelateNode
        if (n.getLabel()!!.isNull(argIndex)) {
          if (eLoc == Location.BOUNDARY)
            n.setLabelBoundary(argIndex)
          else
            n.setLabel(argIndex, Location.INTERIOR)
        }
//n.print(System.out);
      }
    }
  }

  /**
   * If the Geometries are disjoint, we need to enter their dimension and
   * boundary dimension in the Ext rows in the IM
   *
   * @param boundaryNodeRule the Boundary Node Rule to use
   */
  private fun computeDisjointIM(im: IntersectionMatrix, boundaryNodeRule: BoundaryNodeRule) {
    val ga = arg[0].getGeometry()!!
    if (!ga.isEmpty()) {
      im.set(Location.INTERIOR, Location.EXTERIOR, ga.getDimension())
      im.set(Location.BOUNDARY, Location.EXTERIOR, getBoundaryDim(ga, boundaryNodeRule))
    }
    val gb = arg[1].getGeometry()!!
    if (!gb.isEmpty()) {
      im.set(Location.EXTERIOR, Location.INTERIOR, gb.getDimension())
      im.set(Location.EXTERIOR, Location.BOUNDARY, getBoundaryDim(gb, boundaryNodeRule))
    }
  }

  private fun labelNodeEdges() {
    val ni = nodes.iterator()
    while (ni.hasNext()) {
      val node = ni.next() as RelateNode
      node.getEdges()!!.computeLabelling(arg)
//Debug.print(node.getEdges());
//node.print(System.out);
    }
  }

  /**
   * update the IM with the sum of the IMs for each component
   */
  private fun updateIM(im: IntersectionMatrix) {
//Debug.println(im);
    for (e in isolatedEdges) {
      e.updateIM(im)
//Debug.println(im);
    }
    val ni = nodes.iterator()
    while (ni.hasNext()) {
      val node = ni.next() as RelateNode
      node.updateIM(im)
//Debug.println(im);
      node.updateIMFromEdges(im)
//Debug.println(im);
//node.print(System.out);
    }
  }

  /**
   * Processes isolated edges by computing their labelling and adding them
   * to the isolated edges list.
   * Isolated edges are guaranteed not to touch the boundary of the target (since if they
   * did, they would have caused an intersection to be computed and hence would
   * not be isolated)
   */
  private fun labelIsolatedEdges(thisIndex: Int, targetIndex: Int) {
    val ei = arg[thisIndex].getEdgeIterator()
    while (ei.hasNext()) {
      val e = ei.next() as Edge
      if (e.isIsolated()) {
        labelIsolatedEdge(e, targetIndex, arg[targetIndex].getGeometry()!!)
        isolatedEdges.add(e)
      }
    }
  }

  /**
   * Label an isolated edge of a graph with its relationship to the target geometry.
   * If the target has dim 2 or 1, the edge can either be in the interior or the exterior.
   * If the target has dim 0, the edge must be in the exterior
   */
  private fun labelIsolatedEdge(e: Edge, targetIndex: Int, target: Geometry) {
    // this won't work for GeometryCollections with both dim 2 and 1 geoms
    if (target.getDimension() > 0) {
      // since edge is not in boundary, may not need the full generality of PointLocator?
      // Possibly should use ptInArea locator instead?  We probably know here
      // that the edge does not touch the bdy of the target Geometry
      val loc = ptLocator.locate(e.getCoordinate()!!, target)
      e.getLabel()!!.setAllLocations(targetIndex, loc)
    } else {
      e.getLabel()!!.setAllLocations(targetIndex, Location.EXTERIOR)
    }
//System.out.println(e.getLabel());
  }

  /**
   * Isolated nodes are nodes whose labels are incomplete
   * (e.g. the location for one Geometry is null).
   * This is the case because nodes in one graph which don't intersect
   * nodes in the other are not completely labelled by the initial process
   * of adding nodes to the nodeList.
   * To complete the labelling we need to check for nodes that lie in the
   * interior of edges, and in the interior of areas.
   */
  private fun labelIsolatedNodes() {
    val ni = nodes.iterator()
    while (ni.hasNext()) {
      val n = ni.next()
      val label = n.getLabel()!!
      // isolated nodes should always have at least one geometry in their label
      Assert.isTrue(label.getGeometryCount() > 0, "node with empty label found")
      if (n.isIsolated()) {
        if (label.isNull(0))
          labelIsolatedNode(n, 0)
        else
          labelIsolatedNode(n, 1)
      }
    }
  }

  /**
   * Label an isolated node with its relationship to the target geometry.
   */
  private fun labelIsolatedNode(n: Node, targetIndex: Int) {
    val loc = ptLocator.locate(n.getCoordinate()!!, arg[targetIndex].getGeometry()!!)
    n.getLabel()!!.setAllLocations(targetIndex, loc)
//debugPrintln(n.getLabel());
  }

  companion object {
    /**
     * Compute the IM entry for the intersection of the boundary
     * of a geometry with the Exterior.
     * This is the nominal dimension of the boundary
     * unless the boundary is empty, in which case it is [Dimension.FALSE].
     * For linear geometries the Boundary Node Rule determines
     * whether the boundary is empty.
     *
     * @param geom the geometry providing the boundary
     * @param boundaryNodeRule  the Boundary Node Rule to use
     * @return the IM dimension entry
     */
    private fun getBoundaryDim(geom: Geometry, boundaryNodeRule: BoundaryNodeRule): Int {
      /**
       * If the geometry has a non-empty boundary
       * the intersection is the nominal dimension.
       */
      if (BoundaryOp.hasBoundary(geom, boundaryNodeRule)) {
        /**
         * special case for lines, since Geometry.getBoundaryDimension is not aware
         * of Boundary Node Rule.
         */
        if (geom.getDimension() == 1)
          return Dimension.P
        return geom.getBoundaryDimension()
      }
      /**
       * Otherwise intersection is F
       */
      return Dimension.FALSE
    }
  }
}
