/*
 * Copyright (c) 2023 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.relateng

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Location

/**
 * Creates predicate instances for evaluating OGC-standard named topological relationships.
 * Predicates can be evaluated for geometries using [RelateNG].
 *
 * @author Martin Davis
 *
 */
object RelatePredicate {

    /**
     * Creates a predicate to determine whether two geometries intersect.
     *
     * @return the predicate instance
     * @see disjoint
     */
    @JvmStatic
    fun intersects(): TopologyPredicate {
        return object : BasicPredicate() {

            override fun name(): String = "intersects"

            override fun requireSelfNoding(): Boolean {
                //-- self-noding is not required to check for a simple interaction
                return false
            }

            override fun requireExteriorCheck(isSourceA: Boolean): Boolean {
                //-- intersects only requires testing interaction
                return false
            }

            override fun init(envA: Envelope, envB: Envelope) {
                require(envA.intersects(envB))
            }

            override fun updateDimension(locA: Int, locB: Int, dimension: Int) {
                setValueIf(true, BasicPredicate.isIntersection(locA, locB))
            }

            override fun finish() {
                //-- if no intersecting locations were found
                setValue(false)
            }
        }
    }

    /**
     * Creates a predicate to determine whether two geometries are disjoint.
     *
     * @return the predicate instance
     * @see intersects
     */
    @JvmStatic
    fun disjoint(): TopologyPredicate {
        return object : BasicPredicate() {

            override fun name(): String = "disjoint"

            override fun requireSelfNoding(): Boolean {
                //-- self-noding is not required to check for a simple interaction
                return false
            }

            override fun requireInteraction(): Boolean {
                //-- ensure entire matrix is computed
                return false
            }

            override fun requireExteriorCheck(isSourceA: Boolean): Boolean {
                //-- disjoint only requires testing interaction
                return false
            }

            override fun init(envA: Envelope, envB: Envelope) {
                setValueIf(true, envA.disjoint(envB))
            }

            override fun updateDimension(locA: Int, locB: Int, dimension: Int) {
                setValueIf(false, BasicPredicate.isIntersection(locA, locB))
            }

            override fun finish() {
                //-- if no intersecting locations were found
                setValue(true)
            }
        }
    }

    /**
     * Creates a predicate to determine whether a geometry contains another geometry.
     *
     * @return the predicate instance
     * @see within
     */
    @JvmStatic
    fun contains(): TopologyPredicate {
        return object : IMPredicate() {

            override fun name(): String = "contains"

            override fun requireCovers(isSourceA: Boolean): Boolean {
                return isSourceA == RelateGeometry.GEOM_A
            }

            override fun requireExteriorCheck(isSourceA: Boolean): Boolean {
                //-- only need to check B against Exterior of A
                return isSourceA == RelateGeometry.GEOM_B
            }

            override fun init(dimA: Int, dimB: Int) {
                super.init(dimA, dimB)
                require(IMPredicate.isDimsCompatibleWithCovers(dimA, dimB))
            }

            override fun init(envA: Envelope, envB: Envelope) {
                requireCovers(envA, envB)
            }

            override fun isDetermined(): Boolean {
                return intersectsExteriorOf(RelateGeometry.GEOM_A)
            }

            override fun valueIM(): Boolean {
                return intMatrix.isContains()
            }
        }
    }

    /**
     * Creates a predicate to determine whether a geometry is within another geometry.
     *
     * @return the predicate instance
     * @see contains
     */
    @JvmStatic
    fun within(): TopologyPredicate {
        return object : IMPredicate() {

            override fun name(): String = "within"

            override fun requireCovers(isSourceA: Boolean): Boolean {
                return isSourceA == RelateGeometry.GEOM_B
            }

            override fun requireExteriorCheck(isSourceA: Boolean): Boolean {
                //-- only need to check A against Exterior of B
                return isSourceA == RelateGeometry.GEOM_A
            }

            override fun init(dimA: Int, dimB: Int) {
                super.init(dimA, dimB)
                require(IMPredicate.isDimsCompatibleWithCovers(dimB, dimA))
            }

            override fun init(envA: Envelope, envB: Envelope) {
                requireCovers(envB, envA)
            }

            override fun isDetermined(): Boolean {
                return intersectsExteriorOf(RelateGeometry.GEOM_B)
            }

            override fun valueIM(): Boolean {
                return intMatrix.isWithin()
            }
        }
    }

    /**
     * Creates a predicate to determine whether a geometry covers another geometry.
     *
     * @return the predicate instance
     * @see coveredBy
     */
    @JvmStatic
    fun covers(): TopologyPredicate {
        return object : IMPredicate() {

            override fun name(): String = "covers"

            override fun requireCovers(isSourceA: Boolean): Boolean {
                return isSourceA == RelateGeometry.GEOM_A
            }

            override fun requireExteriorCheck(isSourceA: Boolean): Boolean {
                //-- only need to check B against Exterior of A
                return isSourceA == RelateGeometry.GEOM_B
            }

            override fun init(dimA: Int, dimB: Int) {
                super.init(dimA, dimB)
                require(IMPredicate.isDimsCompatibleWithCovers(dimA, dimB))
            }

            override fun init(envA: Envelope, envB: Envelope) {
                requireCovers(envA, envB)
            }

            override fun isDetermined(): Boolean {
                return intersectsExteriorOf(RelateGeometry.GEOM_A)
            }

            override fun valueIM(): Boolean {
                return intMatrix.isCovers()
            }
        }
    }

    /**
     * Creates a predicate to determine whether a geometry is covered by another geometry.
     *
     * @return the predicate instance
     * @see covers
     */
    @JvmStatic
    fun coveredBy(): TopologyPredicate {
        return object : IMPredicate() {

            override fun name(): String = "coveredBy"

            override fun requireCovers(isSourceA: Boolean): Boolean {
                return isSourceA == RelateGeometry.GEOM_B
            }

            override fun requireExteriorCheck(isSourceA: Boolean): Boolean {
                //-- only need to check A against Exterior of B
                return isSourceA == RelateGeometry.GEOM_A
            }

            override fun init(dimA: Int, dimB: Int) {
                super.init(dimA, dimB)
                require(IMPredicate.isDimsCompatibleWithCovers(dimB, dimA))
            }

            override fun init(envA: Envelope, envB: Envelope) {
                requireCovers(envB, envA)
            }

            override fun isDetermined(): Boolean {
                return intersectsExteriorOf(RelateGeometry.GEOM_B)
            }

            override fun valueIM(): Boolean {
                return intMatrix.isCoveredBy()
            }
        }
    }

    /**
     * Creates a predicate to determine whether a geometry crosses another geometry.
     *
     * @return the predicate instance
     */
    @JvmStatic
    fun crosses(): TopologyPredicate {
        return object : IMPredicate() {

            override fun name(): String = "crosses"

            override fun init(dimA: Int, dimB: Int) {
                super.init(dimA, dimB)
                val isBothPointsOrAreas = (dimA == Dimension.P && dimB == Dimension.P) ||
                    (dimA == Dimension.A && dimB == Dimension.A)
                require(!isBothPointsOrAreas)
            }

            override fun isDetermined(): Boolean {
                if (dimA == Dimension.L && dimB == Dimension.L) {
                    //-- L/L interaction can only be dim = P
                    if (getDimension(Location.INTERIOR, Location.INTERIOR) > Dimension.P)
                        return true
                } else if (dimA < dimB) {
                    if (isIntersects(Location.INTERIOR, Location.INTERIOR) &&
                        isIntersects(Location.INTERIOR, Location.EXTERIOR)
                    ) {
                        return true
                    }
                } else if (dimA > dimB) {
                    if (isIntersects(Location.INTERIOR, Location.INTERIOR) &&
                        isIntersects(Location.EXTERIOR, Location.INTERIOR)
                    ) {
                        return true
                    }
                }
                return false
            }

            override fun valueIM(): Boolean {
                return intMatrix.isCrosses(dimA, dimB)
            }
        }
    }

    /**
     * Creates a predicate to determine whether two geometries are topologically equal.
     *
     * @return the predicate instance
     */
    @JvmStatic
    fun equalsTopo(): TopologyPredicate {
        return object : IMPredicate() {

            override fun name(): String = "equals"

            override fun init(dimA: Int, dimB: Int) {
                super.init(dimA, dimB)
                require(dimA == dimB)
            }

            override fun init(envA: Envelope, envB: Envelope) {
                require(envA == envB)
            }

            override fun isDetermined(): Boolean {
                val isEitherExteriorIntersects =
                    isIntersects(Location.INTERIOR, Location.EXTERIOR) ||
                        isIntersects(Location.BOUNDARY, Location.EXTERIOR) ||
                        isIntersects(Location.EXTERIOR, Location.INTERIOR) ||
                        isIntersects(Location.EXTERIOR, Location.BOUNDARY)

                return isEitherExteriorIntersects
            }

            override fun valueIM(): Boolean {
                return intMatrix.isEquals(dimA, dimB)
            }
        }
    }

    /**
     * Creates a predicate to determine whether a geometry overlaps another geometry.
     *
     * @return the predicate instance
     */
    @JvmStatic
    fun overlaps(): TopologyPredicate {
        return object : IMPredicate() {

            override fun name(): String = "overlaps"

            override fun init(dimA: Int, dimB: Int) {
                super.init(dimA, dimB)
                require(dimA == dimB)
            }

            override fun isDetermined(): Boolean {
                if (dimA == Dimension.A || dimA == Dimension.P) {
                    if (isIntersects(Location.INTERIOR, Location.INTERIOR) &&
                        isIntersects(Location.INTERIOR, Location.EXTERIOR) &&
                        isIntersects(Location.EXTERIOR, Location.INTERIOR)
                    )
                        return true
                }
                if (dimA == Dimension.L) {
                    if (isDimension(Location.INTERIOR, Location.INTERIOR, Dimension.L) &&
                        isIntersects(Location.INTERIOR, Location.EXTERIOR) &&
                        isIntersects(Location.EXTERIOR, Location.INTERIOR)
                    )
                        return true
                }
                return false
            }

            override fun valueIM(): Boolean {
                return intMatrix.isOverlaps(dimA, dimB)
            }
        }
    }

    /**
     * Creates a predicate to determine whether a geometry touches another geometry.
     *
     * @return the predicate instance
     */
    @JvmStatic
    fun touches(): TopologyPredicate {
        return object : IMPredicate() {

            override fun name(): String = "touches"

            override fun init(dimA: Int, dimB: Int) {
                super.init(dimA, dimB)
                //-- Points have only interiors, so cannot touch
                val isBothPoints = dimA == 0 && dimB == 0
                require(!isBothPoints)
            }

            override fun isDetermined(): Boolean {
                //-- for touches interiors cannot intersect
                val isInteriorsIntersects = isIntersects(Location.INTERIOR, Location.INTERIOR)
                return isInteriorsIntersects
            }

            override fun valueIM(): Boolean {
                return intMatrix.isTouches(dimA, dimB)
            }
        }
    }

    /**
     * Creates a predicate that matches a DE-9IM matrix pattern.
     *
     * @param imPattern the pattern to match
     * @return a predicate that matches the pattern
     *
     * @see IntersectionMatrixPattern
     */
    @JvmStatic
    fun matches(imPattern: String): TopologyPredicate {
        return IMPatternMatcher(imPattern)
    }
}
