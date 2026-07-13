package org.locationtech.jts.kmptest

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.util.AffineTransformation
import org.locationtech.jts.geom.util.AffineTransformationBuilder
// property-style accessors under test
import org.locationtech.jts.geom.util.determinant
import org.locationtech.jts.geom.util.inverse
import org.locationtech.jts.geom.util.isIdentity
import org.locationtech.jts.geom.util.matrixEntries
import org.locationtech.jts.geom.util.transformation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the additive Kotlin property-style accessors for [AffineTransformation]
 * (`geom/util/PropertyAccessors.kt`) delegate to the underlying Java getters. Runs on every KMP
 * target.
 */
class AffineTransformationPropertyAccessorTest {

    @Test
    fun accessors_match_getters() {
        val xform = AffineTransformation.translationInstance(2.0, 3.0)

        assertEquals(xform.getDeterminant(), xform.determinant)
        assertEquals(xform.isIdentity(), xform.isIdentity)
        assertTrue(xform.getMatrixEntries().contentEquals(xform.matrixEntries))
        assertEquals(xform.getInverse(), xform.inverse)
    }

    @Test
    fun identity_flag() {
        assertTrue(AffineTransformation().isIdentity)
        assertTrue(AffineTransformation.translationInstance(1.0, 0.0).isIdentity.not())
    }

    @Test
    fun builder_transformation_accessor() {
        // Map a triangle onto itself shifted by (2, 3): the builder yields the translation.
        val builder = AffineTransformationBuilder(
            Coordinate(0.0, 0.0), Coordinate(1.0, 0.0), Coordinate(0.0, 1.0),
            Coordinate(2.0, 3.0), Coordinate(3.0, 3.0), Coordinate(2.0, 4.0),
        )
        assertEquals(builder.getTransformation(), builder.transformation)
        assertEquals(AffineTransformation.translationInstance(2.0, 3.0), builder.transformation)
    }
}
