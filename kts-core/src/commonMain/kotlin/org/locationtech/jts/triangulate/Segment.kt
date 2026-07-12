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

package org.locationtech.jts.triangulate

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LineSegment

/**
 * Models a constraint segment in a triangulation.
 * A constraint segment is an oriented straight line segment between a start point
 * and an end point.
 *
 * @author David Skea
 * @author Martin Davis
 */
class Segment {
    private val ls: LineSegment
    private var data: Any? = null

    /**
     * Creates a new instance for the given ordinates.
     */
    constructor(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double) :
        this(Coordinate(x1, y1, z1), Coordinate(x2, y2, z2))

    /**
     * Creates a new instance for the given ordinates,  with associated external data.
     */
    constructor(
        x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, data: Any?
    ) : this(Coordinate(x1, y1, z1), Coordinate(x2, y2, z2), data)

    /**
     * Creates a new instance for the given points, with associated external data.
     *
     * @param p0 the start point
     * @param p1 the end point
     * @param data an external data object
     */
    constructor(p0: Coordinate, p1: Coordinate, data: Any?) {
        ls = LineSegment(p0, p1)
        this.data = data
    }

    /**
     * Creates a new instance for the given points.
     *
     * @param p0 the start point
     * @param p1 the end point
     */
    constructor(p0: Coordinate, p1: Coordinate) {
        ls = LineSegment(p0, p1)
    }

    /**
     * Gets the start coordinate of the segment
     *
     * @return a Coordinate
     */
    fun getStart(): Coordinate {
        return ls.getCoordinate(0)
    }

    /**
     * Gets the end coordinate of the segment
     *
     * @return a Coordinate
     */
    fun getEnd(): Coordinate {
        return ls.getCoordinate(1)
    }

    /**
     * Gets the start X ordinate of the segment
     *
     * @return the X ordinate value
     */
    fun getStartX(): Double {
        val p = ls.getCoordinate(0)
        return p.x
    }

    /**
     * Gets the start Y ordinate of the segment
     *
     * @return the Y ordinate value
     */
    fun getStartY(): Double {
        val p = ls.getCoordinate(0)
        return p.y
    }

    /**
     * Gets the start Z ordinate of the segment
     *
     * @return the Z ordinate value
     */
    fun getStartZ(): Double {
        val p = ls.getCoordinate(0)
        return p.getZ()
    }

    /**
     * Gets the end X ordinate of the segment
     *
     * @return the X ordinate value
     */
    fun getEndX(): Double {
        val p = ls.getCoordinate(1)
        return p.x
    }

    /**
     * Gets the end Y ordinate of the segment
     *
     * @return the Y ordinate value
     */
    fun getEndY(): Double {
        val p = ls.getCoordinate(1)
        return p.y
    }

    /**
     * Gets the end Z ordinate of the segment
     *
     * @return the Z ordinate value
     */
    fun getEndZ(): Double {
        val p = ls.getCoordinate(1)
        return p.getZ()
    }

    /**
     * Gets a `LineSegment` modelling this segment.
     *
     * @return a LineSegment
     */
    fun getLineSegment(): LineSegment {
        return ls
    }

    /**
     * Gets the external data associated with this segment
     *
     * @return a data object
     */
    fun getData(): Any? {
        return data
    }

    /**
     * Sets the external data to be associated with this segment
     *
     * @param data a data object
     */
    fun setData(data: Any?) {
        this.data = data
    }

    /**
     * Determines whether two segments are topologically equal.
     * I.e. equal up to orientation.
     *
     * @param s a segment
     * @return true if the segments are topologically equal
     */
    fun equalsTopo(s: Segment): Boolean {
        return ls.equalsTopo(s.getLineSegment())
    }

    /**
     * Computes the intersection point between this segment and another one.
     *
     * @param s a segment
     * @return the intersection point, or `null` if there is none
     */
    fun intersection(s: Segment): Coordinate? {
        return ls.intersection(s.getLineSegment())
    }

    /**
     * Computes a string representation of this segment.
     *
     * @return a string
     */
    override fun toString(): String {
        return ls.toString()
    }
}
