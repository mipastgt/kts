package org.locationtech.jts.kmptest

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.PrecisionModel
// property-style accessors under test
import org.locationtech.jts.geom.area
import org.locationtech.jts.geom.coordinates
import org.locationtech.jts.geom.dimension
import org.locationtech.jts.geom.envelopeInternal
import org.locationtech.jts.geom.exteriorRing
import org.locationtech.jts.geom.factory
import org.locationtech.jts.geom.isClosed
import org.locationtech.jts.geom.isEmpty
import org.locationtech.jts.geom.isValid
import org.locationtech.jts.geom.length
import org.locationtech.jts.geom.numPoints
import org.locationtech.jts.geom.scale
import org.locationtech.jts.geom.width
import org.locationtech.jts.geom.height
import org.locationtech.jts.geom.SRID
import org.locationtech.jts.geom.startPoint
import org.locationtech.jts.geom.x
import org.locationtech.jts.geom.y
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Verifies the additive Kotlin property-style accessors (`PropertyAccessors.kt`) delegate to the
 * underlying Java getters — value identity, from a package other than `org.locationtech.jts.geom`
 * (so member/field shadowing would show up here). Runs on every KMP target.
 */
class PropertyAccessorTest {

    private val gf = GeometryFactory()

    @Test
    fun geometry_accessors_match_getters() {
        val poly = gf.createPolygon(
            gf.createLinearRing(
                arrayOf(
                    Coordinate(0.0, 0.0), Coordinate(4.0, 0.0),
                    Coordinate(4.0, 3.0), Coordinate(0.0, 3.0), Coordinate(0.0, 0.0),
                ),
            ),
        )
        assertEquals(poly.getArea(), poly.area)
        assertEquals(12.0, poly.area)
        assertEquals(poly.getLength(), poly.length)
        assertEquals(poly.getDimension(), poly.dimension)
        assertEquals(poly.getNumPoints(), poly.numPoints)
        assertEquals(poly.getCoordinates().size, poly.coordinates.size)
        assertEquals(poly.isEmpty(), poly.isEmpty)
        assertEquals(poly.isValid(), poly.isValid)
        assertSame(poly.getFactory(), poly.factory)
        assertEquals(poly.getSRID(), poly.SRID)
        assertEquals(poly.getExteriorRing().getNumPoints(), poly.exteriorRing.numPoints)
    }

    @Test
    fun point_and_linestring_accessors() {
        val pt = gf.createPoint(Coordinate(1.5, 2.5))
        assertEquals(1.5, pt.x)
        assertEquals(2.5, pt.y)

        val line = gf.createLineString(
            arrayOf(Coordinate(0.0, 0.0), Coordinate(1.0, 1.0), Coordinate(0.0, 0.0)),
        )
        assertEquals(line.isClosed(), line.isClosed)
        assertTrue(line.isClosed)
        assertEquals(line.getStartPoint(), line.startPoint)
    }

    @Test
    fun value_type_accessors() {
        val env: Envelope = Envelope(0.0, 4.0, 0.0, 3.0)
        assertEquals(env.getArea(), env.area)
        assertEquals(4.0, env.width)
        assertEquals(3.0, env.height)

        val seg = LineSegment(0.0, 0.0, 3.0, 4.0)
        assertEquals(5.0, seg.length)

        val pm = PrecisionModel(1000.0)
        assertEquals(pm.getScale(), pm.scale)

        val c = Coordinate(1.0, 2.0)
        assertEquals(c.getX(), c.x) // Point.x is separate; Coordinate.x is the public field
    }

    @Test
    fun envelopeInternal_delegates() {
        val g = gf.createPoint(Coordinate(7.0, 9.0))
        assertEquals(g.getEnvelopeInternal(), g.envelopeInternal)
    }
}
