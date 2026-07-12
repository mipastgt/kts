/*
 * Copyright (c) 2016 Martin Davis.
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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Triangle
import org.locationtech.jts.math.DD

/**
 * Algorithms for computing values and predicates
 * associated with triangles.
 * For some algorithms extended-precision
 * implementations are provided, which are more robust
 * (i.e. they produce correct answers in more cases).
 * Also, some more robust formulations of
 * some algorithms are provided, which utilize
 * normalization to the origin.
 *
 * @author Martin Davis
 */
class TrianglePredicate {
    companion object {
        /**
         * Tests if a point is inside the circle defined by
         * the triangle with vertices a, b, c (oriented counter-clockwise).
         * This test uses simple
         * double-precision arithmetic, and thus may not be robust.
         */
        @JvmStatic
        fun isInCircleNonRobust(
            a: Coordinate, b: Coordinate, c: Coordinate,
            p: Coordinate
        ): Boolean {
            val isInCircle =
                (a.x * a.x + a.y * a.y) * triArea(b, c, p) -
                    (b.x * b.x + b.y * b.y) * triArea(a, c, p) +
                    (c.x * c.x + c.y * c.y) * triArea(a, b, p) -
                    (p.x * p.x + p.y * p.y) * triArea(a, b, c) > 0
            return isInCircle
        }

        /**
         * Tests if a point is inside the circle defined by
         * the triangle with vertices a, b, c (oriented counter-clockwise).
         * This test uses simple
         * double-precision arithmetic, and thus is not 100% robust.
         * However, by using normalization to the origin
         * it provides improved robustness and increased performance.
         *
         * Based on code by J.R.Shewchuk.
         */
        @JvmStatic
        fun isInCircleNormalized(
            a: Coordinate, b: Coordinate, c: Coordinate,
            p: Coordinate
        ): Boolean {
            val adx = a.x - p.x
            val ady = a.y - p.y
            val bdx = b.x - p.x
            val bdy = b.y - p.y
            val cdx = c.x - p.x
            val cdy = c.y - p.y

            val abdet = adx * bdy - bdx * ady
            val bcdet = bdx * cdy - cdx * bdy
            val cadet = cdx * ady - adx * cdy
            val alift = adx * adx + ady * ady
            val blift = bdx * bdx + bdy * bdy
            val clift = cdx * cdx + cdy * cdy

            val disc = alift * bcdet + blift * cadet + clift * abdet
            return disc > 0
        }

        /**
         * Computes twice the area of the oriented triangle (a, b, c), i.e., the area is positive if the
         * triangle is oriented counterclockwise.
         */
        private fun triArea(a: Coordinate, b: Coordinate, c: Coordinate): Double {
            return (b.x - a.x) * (c.y - a.y) -
                (b.y - a.y) * (c.x - a.x)
        }

        /**
         * Tests if a point is inside the circle defined by
         * the triangle with vertices a, b, c (oriented counter-clockwise).
         * This method uses more robust computation.
         */
        @JvmStatic
        fun isInCircleRobust(
            a: Coordinate, b: Coordinate, c: Coordinate,
            p: Coordinate
        ): Boolean {
            //checkRobustInCircle(a, b, c, p);
            //    return isInCircleNonRobust(a, b, c, p);
            return isInCircleNormalized(a, b, c, p)
        }

        /**
         * Tests if a point is inside the circle defined by
         * the triangle with vertices a, b, c (oriented counter-clockwise).
         * The computation uses [DD] arithmetic for robustness.
         */
        @JvmStatic
        fun isInCircleDDSlow(
            a: Coordinate, b: Coordinate, c: Coordinate,
            p: Coordinate
        ): Boolean {
            val px = DD.valueOf(p.x)
            val py = DD.valueOf(p.y)
            val ax = DD.valueOf(a.x)
            val ay = DD.valueOf(a.y)
            val bx = DD.valueOf(b.x)
            val by = DD.valueOf(b.y)
            val cx = DD.valueOf(c.x)
            val cy = DD.valueOf(c.y)

            val aTerm = (ax.multiply(ax).add(ay.multiply(ay)))
                .multiply(triAreaDDSlow(bx, by, cx, cy, px, py))
            val bTerm = (bx.multiply(bx).add(by.multiply(by)))
                .multiply(triAreaDDSlow(ax, ay, cx, cy, px, py))
            val cTerm = (cx.multiply(cx).add(cy.multiply(cy)))
                .multiply(triAreaDDSlow(ax, ay, bx, by, px, py))
            val pTerm = (px.multiply(px).add(py.multiply(py)))
                .multiply(triAreaDDSlow(ax, ay, bx, by, cx, cy))

            val sum = aTerm.subtract(bTerm).add(cTerm).subtract(pTerm)
            val isInCircle = sum.doubleValue() > 0

            return isInCircle
        }

        /**
         * Computes twice the area of the oriented triangle (a, b, c), i.e., the area
         * is positive if the triangle is oriented counterclockwise.
         * The computation uses [DD] arithmetic for robustness.
         */
        @JvmStatic
        fun triAreaDDSlow(
            ax: DD, ay: DD,
            bx: DD, by: DD, cx: DD, cy: DD
        ): DD {
            return (bx.subtract(ax).multiply(cy.subtract(ay)).subtract(
                by.subtract(ay).multiply(cx.subtract(ax))
            ))
        }

        @JvmStatic
        fun isInCircleDDFast(
            a: Coordinate, b: Coordinate, c: Coordinate,
            p: Coordinate
        ): Boolean {
            val aTerm = (DD.sqr(a.x).selfAdd(DD.sqr(a.y)))
                .selfMultiply(triAreaDDFast(b, c, p))
            val bTerm = (DD.sqr(b.x).selfAdd(DD.sqr(b.y)))
                .selfMultiply(triAreaDDFast(a, c, p))
            val cTerm = (DD.sqr(c.x).selfAdd(DD.sqr(c.y)))
                .selfMultiply(triAreaDDFast(a, b, p))
            val pTerm = (DD.sqr(p.x).selfAdd(DD.sqr(p.y)))
                .selfMultiply(triAreaDDFast(a, b, c))

            val sum = aTerm.selfSubtract(bTerm).selfAdd(cTerm).selfSubtract(pTerm)
            val isInCircle = sum.doubleValue() > 0

            return isInCircle
        }

        @JvmStatic
        fun triAreaDDFast(
            a: Coordinate, b: Coordinate, c: Coordinate
        ): DD {
            val t1 = DD.valueOf(b.x).selfSubtract(a.x)
                .selfMultiply(
                    DD.valueOf(c.y).selfSubtract(a.y)
                )

            val t2 = DD.valueOf(b.y).selfSubtract(a.y)
                .selfMultiply(
                    DD.valueOf(c.x).selfSubtract(a.x)
                )

            return t1.selfSubtract(t2)
        }

        @JvmStatic
        fun isInCircleDDNormalized(
            a: Coordinate, b: Coordinate, c: Coordinate,
            p: Coordinate
        ): Boolean {
            val adx = DD.valueOf(a.x).selfSubtract(p.x)
            val ady = DD.valueOf(a.y).selfSubtract(p.y)
            val bdx = DD.valueOf(b.x).selfSubtract(p.x)
            val bdy = DD.valueOf(b.y).selfSubtract(p.y)
            val cdx = DD.valueOf(c.x).selfSubtract(p.x)
            val cdy = DD.valueOf(c.y).selfSubtract(p.y)

            val abdet = adx.multiply(bdy).selfSubtract(bdx.multiply(ady))
            val bcdet = bdx.multiply(cdy).selfSubtract(cdx.multiply(bdy))
            val cadet = cdx.multiply(ady).selfSubtract(adx.multiply(cdy))
            val alift = adx.multiply(adx).selfAdd(ady.multiply(ady))
            val blift = bdx.multiply(bdx).selfAdd(bdy.multiply(bdy))
            val clift = cdx.multiply(cdx).selfAdd(cdy.multiply(cdy))

            val sum = alift.selfMultiply(bcdet)
                .selfAdd(blift.selfMultiply(cadet))
                .selfAdd(clift.selfMultiply(abdet))

            val isInCircle = sum.doubleValue() > 0

            return isInCircle
        }

        /**
         * Computes the inCircle test using distance from the circumcentre.
         * Uses standard double-precision arithmetic.
         *
         * In general this doesn't
         * appear to be any more robust than the standard calculation. However, there
         * is at least one case where the test point is far enough from the
         * circumcircle that this test gives the correct answer.
         */
        @JvmStatic
        fun isInCircleCC(
            a: Coordinate, b: Coordinate, c: Coordinate,
            p: Coordinate
        ): Boolean {
            val cc = Triangle.circumcentre(a, b, c)
            val ccRadius = a.distance(cc)
            val pRadiusDiff = p.distance(cc) - ccRadius
            return pRadiusDiff <= 0
        }
    }
}
