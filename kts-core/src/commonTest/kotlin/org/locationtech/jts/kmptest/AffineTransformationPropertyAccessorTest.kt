package org.locationtech.jts.kmptest

import org.locationtech.jts.geom.util.AffineTransformation
// property-style accessors under test
import org.locationtech.jts.geom.util.determinant
import org.locationtech.jts.geom.util.inverse
import org.locationtech.jts.geom.util.isIdentity
import org.locationtech.jts.geom.util.matrixEntries
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
}
