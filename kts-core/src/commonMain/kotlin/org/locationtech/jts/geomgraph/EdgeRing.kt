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
package org.locationtech.jts.geomgraph

import kotlin.jvm.JvmField

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.algorithm.PointLocation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.util.Assert

/**
 * @version 1.7
 */
abstract class EdgeRing(
  start: DirectedEdge,
  @JvmField protected val geometryFactory: GeometryFactory
) {

  @JvmField
  protected var startDe: DirectedEdge? = null // the directed edge which starts the list of edges for this EdgeRing
  private var maxNodeDegree = -1
  private val edges: MutableList<DirectedEdge> = ArrayList() // the DirectedEdges making up this EdgeRing
  private val pts: MutableList<Coordinate> = ArrayList()
  private val label = Label(Location.NONE) // label stores the locations of each geometry on the face surrounded by this ring
  private var ring: LinearRing? = null  // the ring created for this EdgeRing
  private var hole = false
  private var shell: EdgeRing? = null   // if non-null, the ring is a hole and this EdgeRing is its containing shell
  private val holes: MutableList<EdgeRing> = ArrayList() // a list of EdgeRings which are holes in this EdgeRing

  init {
    computePoints(start)
    computeRing()
  }

  open fun isIsolated(): Boolean {
    return label.getGeometryCount() == 1
  }

  open fun isHole(): Boolean {
    //computePoints();
    return hole
  }

  open fun getCoordinate(i: Int): Coordinate = pts[i]
  open fun getLinearRing(): LinearRing? = ring
  open fun getLabel(): Label = label
  open fun isShell(): Boolean = shell == null
  open fun getShell(): EdgeRing? = shell
  open fun setShell(shell: EdgeRing?) {
    this.shell = shell
    if (shell != null) shell.addHole(this)
  }

  open fun addHole(ring: EdgeRing) { holes.add(ring) }

  open fun toPolygon(geometryFactory: GeometryFactory): Polygon {
    val holeLR = Array(holes.size) { i -> holes[i].getLinearRing()!! }
    val poly = geometryFactory.createPolygon(getLinearRing(), holeLR)
    return poly
  }

  /**
   * Compute a LinearRing from the point list previously collected.
   * Test if the ring is a hole (i.e. if it is CCW) and set the hole flag
   * accordingly.
   */
  open fun computeRing() {
    if (ring != null) return   // don't compute more than once
    val coord = Array(pts.size) { i -> pts[i] }
    ring = geometryFactory.createLinearRing(coord)
    hole = Orientation.isCCW(ring!!.getCoordinates())
//Debug.println( (isHole ? "hole - " : "shell - ") + WKTWriter.toLineString(new CoordinateArraySequence(ring.getCoordinates())));
  }

  abstract fun getNext(de: DirectedEdge): DirectedEdge?
  abstract fun setEdgeRing(de: DirectedEdge, er: EdgeRing)

  /**
   * Returns the list of DirectedEdges that make up this EdgeRing
   *
   * @return List of DirectedEdges
   */
  open fun getEdges(): MutableList<DirectedEdge> = edges

  /**
   * Collect all the points from the DirectedEdges of this ring into a contiguous list
   */
  protected open fun computePoints(start: DirectedEdge) {
//System.out.println("buildRing");
    startDe = start
    var de: DirectedEdge? = start
    var isFirstEdge = true
    do {
//      Assert.isTrue(de != null, "found null Directed Edge");
      if (de == null)
        throw TopologyException("Found null DirectedEdge")
      if (de.getEdgeRing() === this)
        throw TopologyException("Directed Edge visited twice during ring-building at " + de.getCoordinate())

      edges.add(de)
//Debug.println(de);
//Debug.println(de.getEdge());
      val label = de.getLabel()!!
      Assert.isTrue(label.isArea())
      mergeLabel(label)
      addPoints(de.getEdge(), de.isForward(), isFirstEdge)
      isFirstEdge = false
      setEdgeRing(de, this)
      de = getNext(de)
    } while (de !== startDe)
  }

  open fun getMaxNodeDegree(): Int {
    if (maxNodeDegree < 0) computeMaxNodeDegree()
    return maxNodeDegree
  }

  private fun computeMaxNodeDegree() {
    maxNodeDegree = 0
    var de: DirectedEdge? = startDe
    do {
      val node = de!!.getNode()!!
      val degree = (node.getEdges() as DirectedEdgeStar).getOutgoingDegree(this)
      if (degree > maxNodeDegree) maxNodeDegree = degree
      de = getNext(de)
    } while (de !== startDe)
    maxNodeDegree *= 2
  }

  open fun setInResult() {
    var de: DirectedEdge? = startDe
    do {
      de!!.getEdge().setInResult(true)
      de = de.getNext()
    } while (de !== startDe)
  }

  protected open fun mergeLabel(deLabel: Label) {
    mergeLabel(deLabel, 0)
    mergeLabel(deLabel, 1)
  }

  /**
   * Merge the RHS label from a DirectedEdge into the label for this EdgeRing.
   * The DirectedEdge label may be null.  This is acceptable - it results
   * from a node which is NOT an intersection node between the Geometries
   * (e.g. the end node of a LinearRing).  In this case the DirectedEdge label
   * does not contribute any information to the overall labelling, and is simply skipped.
   */
  protected open fun mergeLabel(deLabel: Label, geomIndex: Int) {
    val loc = deLabel.getLocation(geomIndex, Position.RIGHT)
    // no information to be had from this label
    if (loc == Location.NONE) return
    // if there is no current RHS value, set it
    if (label.getLocation(geomIndex) == Location.NONE) {
      label.setLocation(geomIndex, loc)
      return
    }
  }

  protected open fun addPoints(edge: Edge, isForward: Boolean, isFirstEdge: Boolean) {
    val edgePts = edge.getCoordinates()
    if (isForward) {
      var startIndex = 1
      if (isFirstEdge) startIndex = 0
      for (i in startIndex until edgePts.size) {
        pts.add(edgePts[i])
      }
    } else { // is backward
      var startIndex = edgePts.size - 2
      if (isFirstEdge) startIndex = edgePts.size - 1
      for (i in startIndex downTo 0) {
        pts.add(edgePts[i])
      }
    }
  }

  /**
   * This method will cause the ring to be computed.
   * It will also check any holes, if they have been assigned.
   *
   * @param p point
   * @return true of ring contains point
   */
  open fun containsPoint(p: Coordinate): Boolean {
    val shell = getLinearRing()!!
    val env = shell.getEnvelopeInternal()
    if (!env.contains(p)) return false
    if (!PointLocation.isInRing(p, shell.getCoordinates())) return false

    for (hole in holes) {
      if (hole.containsPoint(p))
        return false
    }
    return true
  }
}
