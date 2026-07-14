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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Position

/**
 * A `Label` indicates the topological relationship of a component
 * of a topology graph to a given `Geometry`.
 * This class supports labels for relationships to two `Geometry`s,
 * which is sufficient for algorithms for binary operations.
 *
 *
 * Topology graphs support the concept of labeling nodes and edges in the graph.
 * The label of a node or edge specifies its topological relationship to one or
 * more geometries.  (In fact, since JTS operations have only two arguments labels
 * are required for only two geometries).  A label for a node or edge has one or
 * two elements, depending on whether the node or edge occurs in one or both of the
 * input `Geometry`s.  Elements contain attributes which categorize the
 * topological location of the node or edge relative to the parent
 * `Geometry`; that is, whether the node or edge is in the interior,
 * boundary or exterior of the `Geometry`.  Attributes have a value
 * from the set `{Interior, Boundary, Exterior}`.  In a node each
 * element has  a single attribute `<On>`.  For an edge each element has a
 * triplet of attributes `<Left, On, Right>`.
 *
 *
 * It is up to the client code to associate the 0 and 1 `TopologyLocation`s
 * with specific geometries.
 */
class Label {

  private val elt: Array<TopologyLocation>

  /**
   * Construct a Label with a single location for both Geometries.
   * Initialize the locations to Null
   *
   * @param onLoc On location
   */
  constructor(onLoc: Int) {
    elt = arrayOf(TopologyLocation(onLoc), TopologyLocation(onLoc))
  }

  /**
   * Construct a Label with a single location for both Geometries.
   * Initialize the location for the Geometry index.
   *
   * @param geomIndex Geometry index
   * @param onLoc On location
   */
  constructor(geomIndex: Int, onLoc: Int) {
    elt = arrayOf(TopologyLocation(Location.NONE), TopologyLocation(Location.NONE))
    elt[geomIndex].setLocation(onLoc)
  }

  /**
   * Construct a Label with On, Left and Right locations for both Geometries.
   * Initialize the locations for both Geometries to the given values.
   *
   * @param onLoc On location
   * @param rightLoc Right location
   * @param leftLoc Left location
   */
  constructor(onLoc: Int, leftLoc: Int, rightLoc: Int) {
    elt = arrayOf(
      TopologyLocation(onLoc, leftLoc, rightLoc),
      TopologyLocation(onLoc, leftLoc, rightLoc)
    )
  }

  /**
   * Construct a Label with On, Left and Right locations for both Geometries.
   * Initialize the locations for the given Geometry index.
   *
   * @param geomIndex Geometry index
   * @param onLoc On location
   * @param rightLoc Right location
   * @param leftLoc Left location
   */
  constructor(geomIndex: Int, onLoc: Int, leftLoc: Int, rightLoc: Int) {
    elt = arrayOf(
      TopologyLocation(Location.NONE, Location.NONE, Location.NONE),
      TopologyLocation(Location.NONE, Location.NONE, Location.NONE)
    )
    elt[geomIndex].setLocations(onLoc, leftLoc, rightLoc)
  }

  /**
   * Construct a Label with the same values as the argument Label.
   *
   * @param lbl Label
   */
  constructor(lbl: Label) {
    elt = arrayOf(TopologyLocation(lbl.elt[0]), TopologyLocation(lbl.elt[1]))
  }

  fun flip() {
    elt[0].flip()
    elt[1].flip()
  }

  fun getLocation(geomIndex: Int, posIndex: Int): Int = elt[geomIndex].get(posIndex)
  fun getLocation(geomIndex: Int): Int = elt[geomIndex].get(Position.ON)

  fun setLocation(geomIndex: Int, posIndex: Int, location: Int) {
    elt[geomIndex].setLocation(posIndex, location)
  }

  fun setLocation(geomIndex: Int, location: Int) {
    elt[geomIndex].setLocation(Position.ON, location)
  }

  fun setAllLocations(geomIndex: Int, location: Int) {
    elt[geomIndex].setAllLocations(location)
  }

  fun setAllLocationsIfNull(geomIndex: Int, location: Int) {
    elt[geomIndex].setAllLocationsIfNull(location)
  }

  fun setAllLocationsIfNull(location: Int) {
    setAllLocationsIfNull(0, location)
    setAllLocationsIfNull(1, location)
  }

  /**
   * Merge this label with another one.
   * Merging updates any null attributes of this label with the attributes from lbl.
   *
   * @param lbl Label to merge
   */
  fun merge(lbl: Label) {
    for (i in 0..1) {
      elt[i].merge(lbl.elt[i])
    }
  }

  fun getGeometryCount(): Int {
    var count = 0
    if (!elt[0].isNull()) count++
    if (!elt[1].isNull()) count++
    return count
  }

  fun isNull(geomIndex: Int): Boolean = elt[geomIndex].isNull()
  fun isAnyNull(geomIndex: Int): Boolean = elt[geomIndex].isAnyNull()

  fun isArea(): Boolean = elt[0].isArea() || elt[1].isArea()
  fun isArea(geomIndex: Int): Boolean {
    /*  Testing
    if (elt[0].getLocations().length != elt[1].getLocations().length) {
      System.out.println(this);
    }
      */
    return elt[geomIndex].isArea()
  }

  fun isLine(geomIndex: Int): Boolean = elt[geomIndex].isLine()

  fun isEqualOnSide(lbl: Label, side: Int): Boolean {
    return this.elt[0].isEqualOnSide(lbl.elt[0], side) &&
      this.elt[1].isEqualOnSide(lbl.elt[1], side)
  }

  fun allPositionsEqual(geomIndex: Int, loc: Int): Boolean {
    return elt[geomIndex].allPositionsEqual(loc)
  }

  /**
   * Converts one GeometryLocation to a Line location
   * @param geomIndex geometry location
   */
  fun toLine(geomIndex: Int) {
    if (elt[geomIndex].isArea())
      elt[geomIndex] = TopologyLocation(elt[geomIndex].location[0])
  }

  override fun toString(): String {
    val buf = StringBuilder()
    buf.append("A:")
    buf.append(elt[0].toString())
    buf.append(" B:")
    buf.append(elt[1].toString())
    return buf.toString()
  }

  companion object {
    // converts a Label to a Line label (that is, one with no side Locations)
    @JvmStatic
    fun toLineLabel(label: Label): Label {
      val lineLabel = Label(Location.NONE)
      for (i in 0..1) {
        lineLabel.setLocation(i, label.getLocation(i))
      }
      return lineLabel
    }
  }
}
