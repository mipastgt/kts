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
package org.locationtech.jts.io.gml2;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;

import junit.framework.TestCase;

/**
 * Test Case framework for GML unit tests.
 * 
 * @author David Zwiers, Vivid Solutions.
 * @author Martin Davis 
 */
public abstract class WritingTestCase extends TestCase 
{
	
	/**
	 * @param arg
	 */
	public WritingTestCase(String arg){
		super(arg);
	}
	
	protected static PrecisionModel precisionModel = new PrecisionModel(1000);
	protected static GeometryFactory geometryFactory = new GeometryFactory(precisionModel);

	protected void checkRoundTrip(Geometry g)
	{
		GMLWriter out = new GMLWriter();
		out.setPrefix(null);
		out.setNamespace(true);
		out.setSrsName("foo");
		// this markup is not currently work with GMLReader
//		out.setCustomElements(new String[] { "<test>1</test>" } );
		String gml = out.write(g);

		//System.out.println(gml);

		GMLReader in = new GMLReader();
		try {
			Geometry g2 = in.read(gml, geometryFactory);
			assertTrue("The input Geometry is not the same as the output Geometry",
					g.equalsExact(g2));
		} catch (ParseException e) {
			fail(e.getMessage());
		}
	}
}
