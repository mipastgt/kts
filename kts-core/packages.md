# Module jts-core

Package-level documentation for the JTS Topology Suite core module, migrated from the
per-package `package-info.java` files for use as Dokka `includes` in the Kotlin
Multiplatform build.

# Package org.locationtech.jts.algorithm.construct

Provides classes that implement various kinds of geometric constructions.

# Package org.locationtech.jts.algorithm.distance

Classes to compute distance metrics between geometries.

# Package org.locationtech.jts.algorithm.hull

Contains classes implementing algorithms to compute hulls
of geometry objects.

See also [ConvexHull].

# Package org.locationtech.jts.algorithm.locate

Classes to determine the topological location of points in geometries.

# Package org.locationtech.jts.algorithm.match

Classes to compute matching metrics between geometries.

# Package org.locationtech.jts.algorithm

Contains classes and interfaces implementing fundamental computational geometry algorithms.

**Robustness**

Geometrical algorithms involve a combination of combinatorial and numerical computation.  As with
all numerical computation using finite-precision numbers, the algorithms chosen are susceptible to
problems of robustness.  A robustness problem occurs when a numerical calculation produces an
incorrect answer for some inputs due to round-off errors.  Robustness problems are especially
serious in geometric computation, since they can result in errors during topology building.

There are many approaches to dealing with the problem of robustness in geometrical computation.
Not surprisingly, most robust algorithms are substantially more complex and less performant than
the non-robust versions.  Fortunately, JTS is sensitive to robustness problems in only a few key
functions (such as line intersection and the point-in-polygon test).  There are efficient robust
algorithms available for these functions, and these algorithms are implemented in JTS.

**Computational Performance**

Runtime performance is an important consideration for a production-quality implementation of
geometric algorithms.  The most computationally intensive algorithm used in JTS is intersection
detection.  JTS methods need to determine both all intersection between the line segments in a
single Geometry (self-intersection) and all intersections between the line segments of two different
Geometries.

The obvious naive algorithm for intersection detection (comparing every segment with every other)
has unacceptably slow performance.  There is a large literature of faster algorithms for intersection
detection.  Unfortunately, many of them involve substantial code complexity.  JTS tries to balance code
simplicity with performance gains.  It uses some simple techniques to produce substantial performance
gains for common types of input data.

**Package Specification**
- Java Topology Suite Technical Specifications
- [
OpenGIS Simple Features Specification for SQL](http://www.opengis.org/techno/specs.htm)

# Package org.locationtech.jts.coverage

Classes that operate on polygonal coverages.

A polygonal coverage is a set of polygonal geometries which is non-overlapping and edge-matched.
([Polygon]s or [MultiPolygon]s).
A set of polygonal geometries is a valid coverage if:
- Each geometry is valid
- The interiors of all polygons are disjoint (they are non-overlapping).
This is the case if no polygon has a boundary which intersects the interior of another polygon.
- Where polygons are adjacent (i.e. their boundaries intersect),
they are **edge-matched**: the vertices
(and thus line segments) of the common boundary section match exactly.

A coverage may contain holes and disjoint regions.

Coverage algorithms (such as [CoverageUnion])
generally require the input coverage to be valid to produce correct results.
Coverages can be validated using [CoverageValidator].

# Package org.locationtech.jts.densify

Classes to perform densification on geometries.

# Package org.locationtech.jts.geom.impl

Implementations of interfaces for geometric structures.

# Package org.locationtech.jts.geom

Contains the `Geometry` interface hierarchy and supporting classes.

The Java Topology Suite (JTS) is a Java API that implements a core set of spatial data operations using an explicit precision model and robust geometric algorithms. JTS is intended to be used in the development of applications that support the validation, cleaning, integration and querying of spatial datasets.

JTS attempts to implement the OpenGIS Simple Features Specification (SFS) as accurately as possible.  In some cases the SFS is unclear or omits a specification; in this case JTS attempts to choose a reasonable and consistent alternative.  Differences from and elaborations of the SFS are documented in this specification.

**Package Specification**
- Java Topology Suite Technical Specifications
- [
OpenGIS Simple Features Specification for SQL](http://www.opengis.org/techno/specs.htm)

# Package org.locationtech.jts.geom.prep

Classes to perform optimized geometric operations on suitably prepared geometries.

# Package org.locationtech.jts.geom.util

Provides classes that parse and modify Geometry objects.

# Package org.locationtech.jts.geomgraph.index

Contains classes that implement indexes for performing noding on geometry graph edges.

# Package org.locationtech.jts.geomgraph

Contains classes that implement topology graphs.

The Java Topology Suite (JTS) is a Java API that implements a core set of spatial data operations using an explicit precision model and robust geometric algorithms. JTS is intended to be used in the development of applications that support the validation, cleaning, integration and querying of spatial datasets.

JTS attempts to implement the OpenGIS Simple Features Specification (SFS) as accurately as possible.  In some cases the SFS is unclear or omits a specification; in this case JTS attempts to choose a reasonable and consistent alternative.  Differences from and elaborations of the SFS are documented in this specification.

**Package Specification**
- Java Topology Suite Technical Specifications
- [
OpenGIS Simple Features Specification for SQL](http://www.opengis.org/techno/specs.htm)

# Package org.locationtech.jts.index.bintree

Contains classes that implement a Binary Interval Tree index

# Package org.locationtech.jts.index.chain

Contains classes that implement Monotone Chains

# Package org.locationtech.jts.index.intervalrtree

Contains classes to implement an R-tree index for one-dimensional intervals.

# Package org.locationtech.jts.index.kdtree

Contains classes which implement a k-D tree index over 2-D point data.

# Package org.locationtech.jts.index

Provides classes for various kinds of spatial indexes.

# Package org.locationtech.jts.index.quadtree

Contains classes that implement a Quadtree spatial index

# Package org.locationtech.jts.index.strtree

Contains 2-D and 1-D versions of the Sort-Tile-Recursive (STR) tree, a query-only R-tree.

# Package org.locationtech.jts.index.sweepline

Contains classes which implement a sweepline algorithm for scanning geometric data structures.

# Package org.locationtech.jts.linearref

Contains classes and interfaces implementing linear referencing on linear geometries

**Linear Referencing**

Linear Referencing is a way of defining positions along linear geometries
(`LineStrings` and `MultiLineStrings`).
It is used extensively in linear network systems.
There are numerous possible **Linear Referencing Methods** which
can be used to define positions along linear geometry.
This package supports two:
- **Linear Location** - a linear location is a triple
`(component index, segment index, segment fraction)`
which precisely specifies a point on a linear geometry.
It allows for efficient mapping of the index value to actual coordinate values.
- **Length** - the natural concept of using the length along
the geometry to specify a position.

**Package Specification**
- Java Topology Suite Technical Specifications
- [
OpenGIS Simple Features Specification for SQL](http://www.opengis.org/techno/specs.htm)

# Package org.locationtech.jts.noding

Classes to compute nodings for arrangements of line segments and line segment sequences.

# Package org.locationtech.jts.noding.snapround

Contains classes to implement the Snap Rounding algorithm for noding linestrings.

# Package org.locationtech.jts.operation.buffer

Provides classes for computing buffers of geometries

# Package org.locationtech.jts.operation.buffer.validate

Classes to perform validation of the results of buffer operations.

# Package org.locationtech.jts.operation.distance

Provides classes for computing the distance between geometries

# Package org.locationtech.jts.operation.linemerge

Classes to perform line merging.

# Package org.locationtech.jts.operation.overlay

Contains classes that perform a topological overlay to compute boolean spatial functions.

The Overlay Algorithm is used in spatial analysis methods for computing set-theoretic
operations (boolean combinations) of input [org.locationtech.jts.geom.Geometry]s. The algorithm for
computing the overlay uses the intersection operations supported by topology graphs.
To compute an overlay it is necessary to explicitly compute the resultant graph formed
by the computed intersections.

The algorithm to compute a set-theoretic spatial analysis method has the following steps:
- Build topology graphs of the two input geometries.  For each geometry all
self-intersection nodes are computed and added to the graph.
- Compute nodes for all intersections between edges and nodes of the graphs.
- Compute the labeling for the computed nodes by merging the labels from the input graphs.
- Compute new edges between the compute intersection nodes.  Label the edges appropriately.
- Build the resultant graph from the new nodes and edges.
- Compute the labeling for isolated components of the graph.  Add the
isolated components to the resultant graph.
- Compute the result of the boolean combination by selecting the node and edges
with the appropriate labels. Polygonize areas and sew linear geometries together.

**Package Specification**
- Java Topology Suite Technical Specifications
- [
OpenGIS Simple Features Specification for SQL](http://www.opengis.org/techno/specs.htm)

# Package org.locationtech.jts.operation.overlay.snap

Classes to perform snapping on geometries to prepare them for overlay operations.

# Package org.locationtech.jts.operation.overlay.validate

Classes to validate the results of overlay operations.

# Package org.locationtech.jts.operation.overlayng

Contains classes that perform vector overlay
to compute boolean set-theoretic spatial functions.
Overlay operations are used in spatial analysis for computing set-theoretic
operations (boolean combinations) of input [org.locationtech.jts.geom.Geometry]s.

The [org.locationtech.jts.operation.overlayng.OverlayNG] class provides the standard Simple Features
boolean set-theoretic overlay operations.
These are:
- **Intersection** - all points which lie in both geometries
- **Union** - all points which lie in at least one geometry
- **Difference** - all points which lie in the first geometry but not the second
- **Symmetric Difference** - all points which lie in one geometry but not both

These operations are supported for all combinations of the basic geometry types and their homogeneous collections.

Additional operations include:
- [org.locationtech.jts.operation.overlayng.UnaryUnionNG] unions collections of geometries in an efficient way
- [org.locationtech.jts.operation.overlayng.CoverageUnion] provides enhanced performance for unioning
valid polygonal and lineal coverages
- [org.locationtech.jts.operation.overlayng.PrecisionReducer] allows reducing the precision of a geometry
in a topologically-valid way

**Semantics**

The requirements for overlay input are:
- Input geometries may have different dimension.
- Collections must be homogeneous
(all elements must have the same dimension).
- Inputs may be **simple** [GeometryCollection]s.
A GeometryCollection is simple if it can be flattened into a valid Multi-geometry;
i.e. it is homogeneous and does not contain any overlapping Polygons.
- In general, inputs must be valid geometries.
- However, polygonal inputs may contain the following two kinds of "mild" invalid topology:
- rings which self-touch at discrete points (sometimes called inverted shells and exverted holes).
- rings which touch along line segments (i.e. topology collapse).

The semantics of overlay output are:
- Results are always valid geometries.
In particular, result `MultiPolygon`s are valid.
- Repeated vertices are removed.
- Linear results include all nodes (endpoints) present in the input.
In some cases more nodes will be present.
(If merged lines are required see [org.locationtech.jts.operation.linemerge.LineMerger].)
- Polygon edges which undergo topology collapse to lines
(due to rounding or snapping) are included in the result.
This means that all operations may produce a heterogeneous result.
Usually this only occurs when using a fixed-precision model,
but it can happen due to snapping performed to improve robustness.
- The `intersection` operation result includes
all components of the intersection
for geometries which intersect in components of the same and/or lower dimension.
- The `difference` operation produces a homogeneous result
if no topology collapses are present.
In this case the result dimension is equal to that of the left-hand operand.
- The `union` and `symmetric difference` operations
may produce a heterogeneous result if the inputs are of mixed dimension.
- Homogeneous results are output as `Multi` geometries.
- Heterogeneous results are output as a `GeometryCollection`
containing a set of atomic geometries.
(This provides backwards compatibility with the original overlay implementation.
However, it loses the information that the polygonal results
have valid `MultiPolygon` topology.)
- Empty results are atomic `EMPTY` geometries
of dimension appropriate to the operation.
- As far as possible, results preserve the order and direction of the inputs.
For instance, a MultiLineString intersection with a Polygon
will have resultants which are in the same order and have the same direction
as the input lines (assuming the input lines are disjoint).
If an input line is split into two or more parts,
they are ordered in the direction of occurence along their parent line.

**Features**

**Functionality**
- **Precision Model** - operations are performed using a defined precision model
(finite or floating)
- **Robust Computation** - provides fully robust computation when an appropriate noder is used
- **Performance optimizations** - including:
- Short-circuiting for disjoint input envelopes
- Reduction of input segment count via clipping / limiting to overlap envelope
- Optimizations can be disabled if required (e.g. for testing or performance evaluation)
- **Pluggable Noding** - allows using different noders to change characteristics of performance and accuracy
- **Precision Reduction** - in a topologically correct way.
Implemented by unioning a single input with an empty geometry
- **Topology Correction / Conversion** - handles certain kinds
of polygonal inputs which are invalid
- **Fast Coverage Union** - of valid polygonal and linear coverages

**Pluggable Noding**

The noding phase of overlay uses a  [org.locationtech.jts.noding.Noder] subclass.
This is determine automatically based on the precision model of the input.
Or it can be provided explicity, which allows changing characteristics
of performance and robustness.
Examples of relevant noders include:
- [org.locationtech.jts.noding.MCIndexNoder] - a fast full-precision noder, which however may not produce
a valid noding in some situations.
Should be combined with a [org.locationtech.jts.noding.ValidatingNoder] wrapper to detect
noding failures.
- [org.locationtech.jts.noding.snap.SnappingNoder] - a robust full-precision noder
- [org.locationtech.jts.noding.snapround.SnapRoundingNoder] - a noder which enforces a supplied fixed precision model
by snapping vertices and intersections to a grid
- [org.locationtech.jts.noding.SegmentExtractingNoder] - a special-purpose noder that provides very fast noding
for valid polygonal coverages. Requires node-clean input to operate correctly.

**Topology Correction / Conversion**

As noted above, the overlay process
can handle polygonal inputs which are invalid according to the OGC topology model
in certain limited ways.
These invalid conditions are:
- rings which self-touch at discrete points (sometimes called inverted shells and exverted holes).
- rings which touch along line segments (i.e. topology collapse).

These invalidities are corrected during the overlay process.

Some of these invalidities are considered as valid in other geometry models.
By peforming a self-overlay these inputs can be converted
into the JTS OGC topological model.

**Codebase**
- Defines a simple, full-featured topology model, with clear semantics.
The topology model incorporates handling topology collapse, which is
essential for snapping and fixed-precision noding.
- Uses a simple topology graph data structure (based on the winged edge pattern).
- Decouples noding and topology-build phases.
This makes the code clearer, and makes it possible
to allow supplying alternate implementations and semantics for each phase.
- All optimizations are implemented internally,
so that clients do not have to add checks such as envelope overlap.

**Algorithm**

For non-point inputs the overlay algorithm is:
- Check for empty input geometries, and return a result appropriate for the specified operation
- Extract linework and points from input geometries, with topology location information
- (If optimization enabled) Apply overlap envelope optimizations:
- For Intersection, check if the input envelopes are disjoint
(using an envelope expansion adjustment to account for the precision grid).
- For Intersection and Difference, clip or limit the linework of the input geometries to the overlap envelope.
- If the optimized linework is empty, return an empty result of appropriate type.
- Node the linework.  For full robustness snap-rounding noding is used.
Other kinds of noder can be used as well (for instance, the full-precision noding algorithm as the original overlay code).
- Merge noded edges.
Coincident edges from the two input geometries are merged, along with their topological labelling.
Topology collapses are detected in this step, and are flagged in the labelling so they can be handled appropriately duing result polygon extraction
- Build a fully-labelled topology graph.  This includes:
- Create a graph structure on the noded, merged edges
- Propagate topology locations around nodes in the graph
- Label edges that have incomplete topology locations.
These occur when edges from an input geometry are isolated (disjoint from the edges of the other geometry in the graph).
- If result is empty return an empty geometry of appropriate type
- Generate the result geometry from the labelled graph:
- Build result polygons
- Mark edges which should be included in the result areas
- Link maximal rings together
- Convert maximal rings to minimal (valid) rings
- Determine nesting of holes
- Construct result polygons
- Build result linework
- Mark edges to be included in the result lines
- Extract edges as lines
- Build result points (certain intersection situations only)
- Output points occur where the inputs touch at single points
- Collect result elements into the result geometry

**Package Specification**
- [
OpenGIS Simple Features Specification for SQL](http://www.opengis.org/techno/specs.htm)

# Package org.locationtech.jts.operation

Provides classes for implementing operations on geometries

# Package org.locationtech.jts.operation.polygonize

An API for polygonizing sets of lines.

# Package org.locationtech.jts.operation.predicate

Classes which implement topological predicates optimized for particular kinds of geometries.

# Package org.locationtech.jts.operation.relate

Contains classes to implement the computation of the spatial relationships of `Geometry`s.

The `relate` algorithm computes the `IntersectionMatrix` describing the
relationship of two `Geometry`s.  The algorithm for computing `relate`
uses the intersection operations supported by topology graphs.  Although the `relate`
result depends on the resultant graph formed by the computed intersections, there is
no need to explicitly compute the entire graph.
It is sufficient to compute the local structure of the graph
at each intersection node.

The algorithm to compute `relate` has the following steps:
- Build topology graphs of the two input geometries. For each geometry
all self-intersection nodes are computed and added to the graph.
- Compute nodes for all intersections between edges and nodes of the graphs.
- Compute the labeling for the computed nodes by merging the labels from the input graphs.
- Compute the labeling for isolated components of the graph (see below)
- Compute the `IntersectionMatrix` from the labels on the nodes and edges.

**Labeling isolated components**

Isolated components are components (edges or nodes) of an input `Geometry` which
do not contain any intersections with the other input `Geometry`.  The
topological relationship of these components to the other input `Geometry`
must be computed in order to determine the complete labeling of the component.  This can
be done by testing whether the component lies in the interior or exterior of the other
`Geometry`.  If the other `Geometry` is 1-dimensional, the isolated
component must lie in the exterior (since otherwise it would have an intersection with an
edge of the `Geometry`).  If the other `Geometry` is 2-dimensional,
a Point-In-Polygon test can be used to determine whether the isolated component is in the
interior or exterior.

**Package Specification**
- Java Topology Suite Technical Specifications
- [
OpenGIS Simple Features Specification for SQL](http://www.opengis.org/techno/specs.htm)

# Package org.locationtech.jts.operation.relateng

Provides classes to implement the RelateNG algorithm
computes topological relationships of [Geometry]s.
Topology is evaluated based on the
[Dimensionally-Extended 9-Intersection Model](https://en.wikipedia.org/wiki/DE-9IM) (DE-9IM).
The [RelateNG] class supports computing the value of boolean topological predicates.
Standard OGC named predicates are provided by the [RelatePredicate] functions.
Custom relationships can be specified via testing against DE-9IM matrix patterns
(see [IntersectionMatrixPattern] for examples).
The full DE-9IM [IntersectionMatrix] can also be computed.

The algorithm has the following capabilities:
- Efficient short-circuited evaluation of topological predicates
    (including matching custom DE-9IM patterns)
- Optimized repeated evaluation of predicates against a single geometry
    via cached spatial indexes (AKA "prepared mode")
- Robust computation (since only point-local topology is required,
    so that invalid geometry topology cannot cause failures)
- Support for mixed-type and overlapping [GeometryCollection] inputs
    (using *union semantics*)
- Support for [BoundaryNodeRule]

RelateNG operates in 2D only; it ignores any Z ordinates.

**Optimized Short-Circuited Evaluation**

The RelateNG algorithm uses strategies to optimize the evaluation of
topological predicates, including matching DE-9IM matrix patterns.
These include fast tests of dimensions and envelopes, and short-circuited evaluation
once the predicate value is known
(either satisfied or failed) based on the value of matrix entries.
Named predicates used explicit strategy code.
DE-9IM matrix pattern matching are short-circuited where possible
based on analysis of the pattern matrix entries.
Spatial indexes are used to optimize topological computations
(such as locating points in geometry elements,
and analyzing the topological relationship between geometry edges).

**Execution Modes**

RelateNG provides two execution modes for evaluating predicates:
- **Single-shot** mode evaluates a predicate for a single case of two geometries.
It is provided by the [RelateNG] static functions which take two input geometries.
- **Prepared** mode optimizes repeated evaluation of predicates
against a fixed geometry.  It is used by creating an instance of [RelateNG]
on the required geometry with the `prepare` functions,
and then using the `evaluate` methods.
It provides much faster performance for repeated operations against a single geometry.

**Robustness**

RelateNG provides robust evaluation of topological relationships,
up to the precision of double-precision computation.
It computes topological relationships in the locality of discrete points,
without constructing a full topology graph of the inputs.
This means that invalid input geometries or numerical round-off do not cause exceptions
(although they may return incorrect answers).
However, it is necessary to node some inputs together (in particular, linear elements)
in order to provide consistent evaluation of the topological structure.

**GeometryCollection Handling**

[GeometryCollection]s may contain geometries of different dimensions, nested to any level.
The element geometries may overlap in any combination.
The OGC specification did not provide a definition for the topology
of GeometryCollections, or how they behave under the DE-9IM model.
RelateNG defines the topology for arbitrary collections of geometries
using "union semantics".
This is specified as:
- GeometryCollections are evaluated as if they were replaced by the topological union
of their elements.
- The topological location at a point is equal to its location in the geometry of highest
dimension which contains it.  For example, a point located in the interior of a Polygon
and the boundary of a LineString has location `Interior`.

**Zero-length LineString Handling**

Zero-length LineStrings are handled as topologically identical to a Point at the same coordinate.

**Package Specification**
- [
OpenGIS Simple Features Specification for SQL](http://www.opengis.org/techno/specs.htm)

# Package org.locationtech.jts.operation.union

Classes to perform efficient unioning of collections of geometries.

# Package org.locationtech.jts.operation.valid

Classes for testing the validity and simplicity of geometries,
as defined in the OGC Simple Features specification.

# Package org.locationtech.jts.planargraph.algorithm

Classes which implement graph algorithms on planar graphs.

# Package org.locationtech.jts.planargraph

Contains classes to implement a planar graph data structure.

# Package org.locationtech.jts.precision

Provides classes for analyzing and
manipulating the precision of Geometries.

The *Minimum Clearance* of a geometry is the smallest distance by which a vertex could be moved
to produce an invalid or degenerate geometry — a measure of the geometry's robustness to
coordinate rounding:

![Minimum Clearance illustration](../../images/minClearance.png)

# Package org.locationtech.jts.simplify

Classes which implement algorithms for simplifying or generalizing geometries.

# Package org.locationtech.jts.triangulate

Classes to compute Delaunay triangulations.

# Package org.locationtech.jts.triangulate.polygon

Classes for triangulating polygons.
[ConstrainedDelaunayTriangulator] can be used to provide high-quality
near-Delaunay triangulations of polygonal geometry.
The [PolygonTriangulator] produces lower-quality but faster triangulations.

# Package org.locationtech.jts.triangulate.quadedge

Classes to implement a topological subdivision of quadeges, to support creating triangulations
and Voronoi diagrams.

# Package org.locationtech.jts.triangulate.tri

Classes for representing a planar triangulation as a set of linked triangles.
Triangles are represented by memory-efficient [Tri] objects.
A set of triangles can be linked into a triangulation using [TriangulationBuilder].

# Package org.locationtech.jts.util

Contains support classes for the Java Topology Suite.

