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
package org.locationtech.jts.planargraph

import kotlin.jvm.JvmStatic

/**
 * The base class for all graph component classes.
 * Maintains flags of use in generic graph algorithms.
 *
 * @version 1.7
 */
abstract class GraphComponent {

  private var marked = false
  private var visited = false
  private var data: Any? = null

  /**
   * Tests if a component has been visited during the course of a graph algorithm
   * @return `true` if the component has been visited
   */
  open fun isVisited(): Boolean {
    return visited
  }

  /**
   * Sets the visited flag for this component.
   * @param isVisited the desired value of the visited flag
   */
  open fun setVisited(isVisited: Boolean) {
    this.visited = isVisited
  }

  /**
   * Tests if a component has been marked at some point during the processing
   * involving this graph.
   * @return `true` if the component has been marked
   */
  open fun isMarked(): Boolean {
    return marked
  }

  /**
   * Sets the marked flag for this component.
   * @param isMarked the desired value of the marked flag
   */
  open fun setMarked(isMarked: Boolean) {
    this.marked = isMarked
  }

  /**
   * Sets the user-defined data for this component.
   *
   * @param data an Object containing user-defined data
   */
  open fun setContext(data: Any?) {
    this.data = data
  }

  /**
   * Gets the user-defined data for this component.
   *
   * @return the user-defined data
   */
  open fun getContext(): Any? {
    return data
  }

  /**
   * Sets the user-defined data for this component.
   *
   * @param data an Object containing user-defined data
   */
  open fun setData(data: Any?) {
    this.data = data
  }

  /**
   * Gets the user-defined data for this component.
   *
   * @return the user-defined data
   */
  open fun getData(): Any? {
    return data
  }

  /**
   * Tests whether this component has been removed from its containing graph
   *
   * @return `true` if this component is removed
   */
  abstract fun isRemoved(): Boolean

  companion object {
    /**
     * Sets the Visited state for all [GraphComponent]s in an [Iterator]
     *
     * @param i the Iterator to scan
     * @param visited the state to set the visited flag to
     */
    @JvmStatic
    fun setVisited(i: Iterator<*>, visited: Boolean) {
      while (i.hasNext()) {
        val comp = i.next() as GraphComponent
        comp.setVisited(visited)
      }
    }

    /**
     * Sets the Marked state for all [GraphComponent]s in an [Iterator]
     *
     * @param i the Iterator to scan
     * @param marked the state to set the Marked flag to
     */
    @JvmStatic
    fun setMarked(i: Iterator<*>, marked: Boolean) {
      while (i.hasNext()) {
        val comp = i.next() as GraphComponent
        comp.setMarked(marked)
      }
    }

    /**
     * Finds the first [GraphComponent] in a [Iterator] set
     * which has the specified visited state.
     *
     * @param i an Iterator of GraphComponents
     * @param visitedState the visited state to test
     * @return the first component found, or `null` if none found
     */
    @JvmStatic
    fun getComponentWithVisitedState(i: Iterator<*>, visitedState: Boolean): GraphComponent? {
      while (i.hasNext()) {
        val comp = i.next() as GraphComponent
        if (comp.isVisited() == visitedState)
          return comp
      }
      return null
    }
  }
}
