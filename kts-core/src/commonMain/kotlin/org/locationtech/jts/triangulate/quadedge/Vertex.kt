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

package org.locationtech.jts.triangulate.quadedge

import kotlin.jvm.JvmStatic
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

import org.locationtech.jts.algorithm.HCoordinate
import org.locationtech.jts.algorithm.NotRepresentableException
import org.locationtech.jts.geom.Coordinate

/**
 * Models a site (node) in a [QuadEdgeSubdivision].
 * The sites can be points on a line string representing a
 * linear site.
 *
 * The vertex can be considered as a vector with a norm, length, inner product, cross
 * product, etc. Additionally, point relations (e.g., is a point to the left of a line, the circle
 * defined by this point and two others, etc.) are also defined in this class.
 *
 * It is common to want to attach user-defined data to
 * the vertices of a subdivision.
 * One way to do this is to subclass `Vertex`
 * to carry any desired information.
 *
 * @author David Skea
 * @author Martin Davis
 */
open class Vertex {

    private val p: Coordinate
    // private int edgeNumber = -1;

    constructor(_x: Double, _y: Double) {
        p = Coordinate(_x, _y)
    }

    constructor(_x: Double, _y: Double, _z: Double) {
        p = Coordinate(_x, _y, _z)
    }

    constructor(_p: Coordinate) {
        p = Coordinate(_p)
    }

    fun getX(): Double {
        return p.x
    }

    fun getY(): Double {
        return p.y
    }

    fun getZ(): Double {
        return p.getZ()
    }

    fun setZ(_z: Double) {
        p.setZ(_z)
    }

    fun getCoordinate(): Coordinate {
        return p
    }

    override fun toString(): String {
        return "POINT (" + p.x + " " + p.y + ")"
    }

    fun equals(_x: Vertex): Boolean {
        return if (p.x == _x.getX() && p.y == _x.getY()) {
            true
        } else {
            false
        }
    }

    fun equals(_x: Vertex, tolerance: Double): Boolean {
        return if (p.distance(_x.getCoordinate()) < tolerance) {
            true
        } else {
            false
        }
    }

    fun classify(p0: Vertex, p1: Vertex): Int {
        val p2 = this
        val a = p1.sub(p0)
        val b = p2.sub(p0)
        val sa = a.crossProduct(b)
        if (sa > 0.0)
            return LEFT
        if (sa < 0.0)
            return RIGHT
        if ((a.getX() * b.getX() < 0.0) || (a.getY() * b.getY() < 0.0))
            return BEHIND
        if (a.magn() < b.magn())
            return BEYOND
        if (p0.equals(p2))
            return ORIGIN
        if (p1.equals(p2))
            return DESTINATION
        return BETWEEN
    }

    /**
     * Computes the cross product k = u X v.
     *
     * @param v a vertex
     * @return returns the magnitude of u X v
     */
    private fun crossProduct(v: Vertex): Double {
        return (p.x * v.getY() - p.y * v.getX())
    }

    /**
     * Computes the inner or dot product
     *
     * @param v a vertex
     * @return returns the dot product u.v
     */
    private fun dot(v: Vertex): Double {
        return (p.x * v.getX() + p.y * v.getY())
    }

    /**
     * Computes the scalar product c(v)
     *
     * @param c the scalar factor
     * @return returns the scaled vector
     */
    private fun times(c: Double): Vertex {
        return (Vertex(c * p.x, c * p.y))
    }

    /* Vector addition */
    private fun sum(v: Vertex): Vertex {
        return (Vertex(p.x + v.getX(), p.y + v.getY()))
    }

    /* and subtraction */
    private fun sub(v: Vertex): Vertex {
        return (Vertex(p.x - v.getX(), p.y - v.getY()))
    }

    /* magnitude of vector */
    private fun magn(): Double {
        return (hypot(p.x, p.y))
    }

    /* returns k X v (cross product). this is a vector perpendicular to v */
    private fun cross(): Vertex {
        return (Vertex(p.y, -p.x))
    }

    /** ************************************************************* */
    /***********************************************************************************************
     * Geometric primitives /
     **********************************************************************************************/

    /**
     * Tests if the vertex is inside the circle defined by
     * the triangle with vertices a, b, c (oriented counter-clockwise).
     *
     * @param a a vertex of the triangle
     * @param b a vertex of the triangle
     * @param c a vertex of the triangle
     * @return true if this vertex is in the circumcircle of (a,b,c)
     */
    fun isInCircle(a: Vertex, b: Vertex, c: Vertex): Boolean {
        return TrianglePredicate.isInCircleRobust(a.p, b.p, c.p, this.p)
        // non-robust - best to not use
        //return TrianglePredicate.isInCircle(a.p, b.p, c.p, this.p);
    }

    /**
     * Tests whether the triangle formed by this vertex and two
     * other vertices is in CCW orientation.
     *
     * @param b a vertex
     * @param c a vertex
     * @return true if the triangle is oriented CCW
     */
    fun isCCW(b: Vertex, c: Vertex): Boolean {
        // is equal to the signed area of the triangle
        return (b.p.x - p.x) * (c.p.y - p.y) -
            (b.p.y - p.y) * (c.p.x - p.x) > 0
    }

    fun rightOf(e: QuadEdge): Boolean {
        return isCCW(e.dest()!!, e.orig()!!)
    }

    fun leftOf(e: QuadEdge): Boolean {
        return isCCW(e.orig()!!, e.dest()!!)
    }

    private fun bisector(a: Vertex, b: Vertex): HCoordinate {
        // returns the perpendicular bisector of the line segment ab
        val dx = b.getX() - a.getX()
        val dy = b.getY() - a.getY()
        val l1 = HCoordinate(a.getX() + dx / 2.0, a.getY() + dy / 2.0, 1.0)
        val l2 = HCoordinate(a.getX() - dy + dx / 2.0, a.getY() + dx + dy / 2.0, 1.0)
        return HCoordinate(l1, l2)
    }

    private fun distance(v1: Vertex, v2: Vertex): Double {
        return sqrt(
            (v2.getX() - v1.getX()).pow(2.0) +
                (v2.getY() - v1.getY()).pow(2.0)
        )
    }

    /**
     * Computes the value of the ratio of the circumradius to shortest edge. If smaller than some
     * given tolerance B, the associated triangle is considered skinny. For an equal lateral
     * triangle this value is 0.57735. The ratio is related to the minimum triangle angle theta by:
     * circumRadius/shortestEdge = 1/(2sin(theta)).
     *
     * @param b second vertex of the triangle
     * @param c third vertex of the triangle
     * @return ratio of circumradius to shortest edge.
     */
    fun circumRadiusRatio(b: Vertex, c: Vertex): Double {
        val x = this.circleCenter(b, c)
        val radius = distance(x!!, b)
        var edgeLength = distance(this, b)
        var el = distance(b, c)
        if (el < edgeLength) {
            edgeLength = el
        }
        el = distance(c, this)
        if (el < edgeLength) {
            edgeLength = el
        }
        return radius / edgeLength
    }

    /**
     * returns a new vertex that is mid-way between this vertex and another end point.
     *
     * @param a the other end point.
     * @return the point mid-way between this and that.
     */
    fun midPoint(a: Vertex): Vertex {
        val xm = (p.x + a.getX()) / 2.0
        val ym = (p.y + a.getY()) / 2.0
        val zm = (p.getZ() + a.getZ()) / 2.0
        return Vertex(xm, ym, zm)
    }

    /**
     * Computes the centre of the circumcircle of this vertex and two others.
     *
     * @param b
     * @param c
     * @return the Coordinate which is the circumcircle of the 3 points.
     */
    fun circleCenter(b: Vertex, c: Vertex): Vertex? {
        val a = Vertex(this.getX(), this.getY())
        // compute the perpendicular bisector of cord ab
        val cab = bisector(a, b)
        // compute the perpendicular bisector of cord bc
        val cbc = bisector(b, c)
        // compute the intersection of the bisectors (circle radii)
        val hcc = HCoordinate(cab, cbc)
        var cc: Vertex? = null
        try {
            cc = Vertex(hcc.getX(), hcc.getY())
        } catch (nre: NotRepresentableException) {
            //Debug.println("a: " + a + "  b: " + b + "  c: " + c);
            //Debug.println(nre);
        }
        return cc
    }

    /**
     * For this vertex enclosed in a triangle defined by three vertices v0, v1 and v2, interpolate
     * a z value from the surrounding vertices.
     */
    fun interpolateZValue(v0: Vertex, v1: Vertex, v2: Vertex): Double {
        val x0 = v0.getX()
        val y0 = v0.getY()
        val a = v1.getX() - x0
        val b = v2.getX() - x0
        val c = v1.getY() - y0
        val d = v2.getY() - y0
        val det = a * d - b * c
        val dx = this.getX() - x0
        val dy = this.getY() - y0
        val t = (d * dx - b * dy) / det
        val u = (-c * dx + a * dy) / det
        val z = v0.getZ() + t * (v1.getZ() - v0.getZ()) + u * (v2.getZ() - v0.getZ())
        return z
    }

    companion object {
        const val LEFT = 0
        const val RIGHT = 1
        const val BEYOND = 2
        const val BEHIND = 3
        const val BETWEEN = 4
        const val ORIGIN = 5
        const val DESTINATION = 6

        /**
         * Interpolates the Z-value (height) of a point enclosed in a triangle
         * whose vertices all have Z values.
         * The containing triangle must not be degenerate
         * (in other words, the three vertices must enclose a
         * non-zero area).
         *
         * @param p the point to interpolate the Z value of
         * @param v0 a vertex of a triangle containing the p
         * @param v1 a vertex of a triangle containing the p
         * @param v2 a vertex of a triangle containing the p
         * @return the interpolated Z-value (height) of the point
         */
        @JvmStatic
        fun interpolateZ(p: Coordinate, v0: Coordinate, v1: Coordinate, v2: Coordinate): Double {
            val x0 = v0.x
            val y0 = v0.y
            val a = v1.x - x0
            val b = v2.x - x0
            val c = v1.y - y0
            val d = v2.y - y0
            val det = a * d - b * c
            val dx = p.x - x0
            val dy = p.y - y0
            val t = (d * dx - b * dy) / det
            val u = (-c * dx + a * dy) / det
            val z = v0.getZ() + t * (v1.getZ() - v0.getZ()) + u * (v2.getZ() - v0.getZ())
            return z
        }

        /**
         * Computes the interpolated Z-value for a point p lying on the segment p0-p1
         *
         * @param p
         * @param p0
         * @param p1
         * @return the interpolated Z value
         */
        @JvmStatic
        fun interpolateZ(p: Coordinate, p0: Coordinate, p1: Coordinate): Double {
            val segLen = p0.distance(p1)
            val ptLen = p.distance(p0)
            val dz = p1.getZ() - p0.getZ()
            val pz = p0.getZ() + dz * (ptLen / segLen)
            return pz
        }
    }
}
