package com.cinpe.deponder.core

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PhysicsKernelTest {

    @Test
    fun edgeKey_preventsPrefixCollisions_andPreservesCaseSymmetry() {
        val k1 = EdgeKey("1", "23")
        val k2 = EdgeKey("12", "3")
        assertNotEquals(k1, k2)
        assertNotEquals(k1.encoded, k2.encoded)

        val lowerUpper = EdgeKey("a", "A")
        val upperLower = EdgeKey("A", "a")
        assertEquals(lowerUpper, upperLower)
        assertEquals(lowerUpper.hashCode(), upperLower.hashCode())
        assertEquals(lowerUpper.encoded, upperLower.encoded)
    }

    @Test
    fun sharedDpPhysicsConfigs_convertWithDensity_andPreserveInfiniteSentinel() {
        val rootCfg = RootPhysics.fromPreset(DeponderDefaults.Presets.GalaxyInfinite)
        val env = rootCfg.toEnvironmentSpec(density = 3.0f)
        assertEquals(DeponderDefaults.INFINITE_RANGE, env.wallRepulsionPx, 0f)

        val finitePlanet = PlanetPhysics(mass = 3.0f, repulsionRadiusDp = 90f, elasticity = 2.0f)
            .toPlanetSpec(id = "p1", density = 2.5f)
        assertEquals(225f, finitePlanet.repulsionRadiusPx, 1e-4f)

        val rubberSpec = RubberPhysics(elasticity = 1.5f, naturalLengthDp = 120f)
            .toRubberSpec(startId = "a", endId = "b", density = 2.0f)
        assertEquals(240f, rubberSpec.naturalLengthPx, 1e-4f)

        // Default RubberPhysics() is content-adaptive (`AUTO_LENGTH`):
        // - Empty/short content (0dp..30dp) floors cleanly at `minLengthDp` (`110dp`)
        // - Longer content (e.g. 95dp) expands to `95 + 80 = 175dp` (`350px` at 2.0x density)
        val defaultAuto = RubberPhysics()
        assertTrue(defaultAuto.isAutoLength)
        assertEquals(DeponderDefaults.AUTO_LENGTH, defaultAuto.naturalLengthDp, 0f)
        assertEquals(110f, defaultAuto.resolveNaturalLengthDp(0f), 1e-4f)
        assertEquals(110f, defaultAuto.resolveNaturalLengthDp(25f), 1e-4f)
        assertEquals(175f, defaultAuto.resolveNaturalLengthDp(95f), 1e-4f)
        assertEquals(
            350f,
            defaultAuto.toRubberSpec("a", "b", density = 2.0f, contentWidthPx = 190f).naturalLengthPx,
            1e-4f,
        )

        // Customized auto length (`padding`, `minLengthDp`, `maxLengthDp` ceiling) + copy/withElasticity preservation:
        val boundedAuto = RubberPhysics.auto(
            contentPaddingDp = 60f,
            minLengthDp = 85f,
            maxLengthDp = 220f,
            elasticity = 2.2f,
        ).withElasticity(2.8f)
        assertTrue(boundedAuto.isAutoLength)
        assertEquals(2.8f, boundedAuto.elasticity, 1e-4f)
        assertEquals(85f, boundedAuto.resolveNaturalLengthDp(10f), 1e-4f)
        assertEquals(160f, boundedAuto.resolveNaturalLengthDp(100f), 1e-4f)
        assertEquals(220f, boundedAuto.resolveNaturalLengthDp(300f), 1e-4f)

        // Fixed factory and preset seeding:
        val fixedRubber = RubberPhysics.fixed(naturalLengthDp = 140f, elasticity = 1.9f)
        assertFalse(fixedRubber.isAutoLength)
        assertEquals(140f, fixedRubber.resolveNaturalLengthDp(300f), 1e-4f)
        val galaxyRubber = RubberPhysics.fromPreset(DeponderDefaults.Presets.GalaxyInfinite)
        assertTrue(galaxyRubber.isAutoLength)
        assertEquals(100f, galaxyRubber.resolveNaturalLengthDp(0f), 1e-4f)
        assertEquals(200f, galaxyRubber.resolveNaturalLengthDp(120f), 1e-4f)

        // Validation guards:
        assertFailsWith<IllegalArgumentException> { RubberPhysics(naturalLengthDp = -5f) }
        assertFailsWith<IllegalArgumentException> { RubberPhysics.fixed(-1f) }
        assertFailsWith<IllegalArgumentException> { RubberPhysics.auto(contentPaddingDp = -4f) }
        assertFailsWith<IllegalArgumentException> { RubberPhysics.auto(minLengthDp = 150f, maxLengthDp = 100f) }
        assertFailsWith<IllegalArgumentException> { RootPhysics(initScale = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { RootPhysics(airDamping = Float.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { PlanetPhysics(repulsionRadiusDp = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { PlanetPhysics(elasticity = Float.NEGATIVE_INFINITY) }

        // -0.0f normalization preserves equals / hashCode contract:
        val negZeroAuto = RubberPhysics.auto(
            contentPaddingDp = -0.0f,
            minLengthDp = -0.0f,
            maxLengthDp = -0.0f,
            elasticity = -0.0f,
        )
        val posZeroAuto = RubberPhysics.auto(
            contentPaddingDp = 0f,
            minLengthDp = 0f,
            maxLengthDp = 0f,
            elasticity = 0f,
        )
        assertEquals(posZeroAuto, negZeroAuto)
        assertEquals(posZeroAuto.hashCode(), negZeroAuto.hashCode())

        val negZeroFixed = RubberPhysics(elasticity = -0.0f, naturalLengthDp = -0.0f)
        val posZeroFixed = RubberPhysics(elasticity = 0f, naturalLengthDp = 0f)
        assertEquals(posZeroFixed, negZeroFixed)
        assertEquals(posZeroFixed.hashCode(), negZeroFixed.hashCode())
    }

    @Test
    fun planetContourClearance_exposesRawMetrics_andResolvesContourFixedAndNoneInsets() = runTest {
        val world = SimulationWorld(EnvironmentSpec(wallRepulsionPx = 0f, airDamping = 4.0f))
        world.updateViewport(1000f, 1000f)

        // Asymmetric Planet sizes: `hub` is 100x60 (`halfW = 50`), `leaf` is 40x40 (`halfW = 20`).
        world.upsertPlanet(PlanetSpec(id = "hub", pinned = true, repulsionRadiusPx = 0f), 100f, 300f)
        world.upsertPlanet(PlanetSpec(id = "leaf", pinned = true, repulsionRadiusPx = 0f), 300f, 300f)
        world.setBodySize("hub", widthPx = 100f, heightPx = 60f)
        world.setBodySize("leaf", widthPx = 40f, heightPx = 40f)

        val pose = world.upsertRubber(
            RubberSpec(startId = "hub", endId = "leaf", naturalLengthPx = 80f),
        )
        assertEquals(200f, pose.centerDistance, 1e-3f)
        assertEquals(200f, pose.centerX, 1e-3f)
        assertEquals(50f, pose.startContourPx, 1e-3f)
        assertEquals(20f, pose.endContourPx, 1e-3f)

        // 1. Symmetric contour(5f): hub clearance = 55px (sxVis = 155), leaf clearance = 25px (exVis = 275).
        val bothInsets = DeponderRubberInsets.contour(5f)
        assertTrue(bothInsets.hasStartSafeArea && bothInsets.hasEndSafeArea)
        assertEquals(120f, pose.resolvedLength(bothInsets, 1f), 1e-3f)
        assertEquals(215f, pose.resolvedCenterX(bothInsets, 1f), 1e-3f)

        // Negative contour gap trims inward (e.g. padded touch target) and clamps cleanly at 0px!
        val inwardInsets = DeponderRubberInsets.contour(-10f)
        assertEquals(150f, pose.resolvedLength(inwardInsets, 1f), 1e-3f) // (50 - 10) + (20 - 10) = 50 -> 200 - 50 = 150px
        assertEquals(0f, pose.resolvedStartInsetPx(DeponderRubberInsets.contour(-80f), 1f), 1e-3f)

        // 2. Asymmetric endOnly(5f): start anchored at hub center (100, NONE), end stops at 275 -> len = 175, cx = 187.5.
        val endOnlyInsets = DeponderRubberInsets.endOnly(5f)
        assertFalse(endOnlyInsets.hasStartSafeArea)
        assertTrue(endOnlyInsets.hasEndSafeArea)
        assertEquals(175f, pose.resolvedLength(endOnlyInsets, 1f), 1e-3f)
        assertEquals(187.5f, pose.resolvedCenterX(endOnlyInsets, 1f), 1e-3f)

        // 3. Explicit Fixed mode: bypasses contour estimation (`fixed(0f, 30f)` normalizes start to NONE!).
        val asymFixed = DeponderRubberInsets.fixed(startDp = 0f, endDp = 30f)
        assertEquals(DeponderRubberInsets.Mode.NONE, asymFixed.startMode)
        assertEquals(DeponderRubberInsets.Mode.FIXED, asymFixed.endMode)
        assertFalse(asymFixed.hasStartSafeArea)
        assertTrue(asymFixed.hasEndSafeArea)
        assertEquals(140f, pose.resolvedLength(asymFixed, density = 2f), 1e-3f) // 200 - (30 * 2) = 140px
        assertEquals(DeponderRubberInsets.None, DeponderRubberInsets.fixed(0f))

        // 4. Edge-to-edge None: spans full 200px @ cx = 200 and stays active (> 0px) even when bounds overlap!
        assertFalse(DeponderRubberInsets.None.hasStartSafeArea || DeponderRubberInsets.None.hasEndSafeArea)
        assertEquals(200f, pose.resolvedLength(DeponderRubberInsets.None, 1f), 1e-3f)
        assertEquals(200f, pose.resolvedCenterX(DeponderRubberInsets.None, 1f), 1e-3f)

        // Reject NaN / Infinity / negative Fixed values so resolved transforms can never produce NaN!
        assertFailsWith<IllegalArgumentException> { DeponderRubberInsets.contour(Float.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { DeponderRubberInsets.contour(Float.NaN) }
        assertFailsWith<IllegalArgumentException> { DeponderRubberInsets.fixed(-1f) }
        assertFailsWith<IllegalArgumentException> { DeponderRubberInsets.fixed(Float.POSITIVE_INFINITY) }

        // Dynamic rubber elasticity update wakes simulation and updates spec cleanly.
        world.updateAllRubbersElasticity(4.5f)
        assertEquals(4.5f, pose.spec.elasticity, 1e-4f)

        // Asymmetric overlap clamping: `hub` @ 100 (sInset = 55), `leaf` @ 130 (eInset = 25) -> centerDist = 30px < 80px.
        // Resolved center must remain clamped strictly inside [100, 130] instead of overshooting past `leaf`!
        world.setBodyPosition("leaf", 130f, 300f)
        assertTrue(pose.isActive)
        assertEquals(0f, pose.resolvedLength(bothInsets, 1f), 1e-3f)
        val clampedCx = pose.resolvedCenterX(bothInsets, 1f)
        assertTrue(clampedCx in 100f..130f, "Collapsed resolvedCenterX ($clampedCx) must stay within [100, 130]")
        assertEquals(30f, pose.resolvedLength(DeponderRubberInsets.None, 1f), 1e-3f)
        assertEquals(115f, pose.resolvedCenterX(DeponderRubberInsets.None, 1f), 1e-3f)
    }

    @Test
    fun rubberElasticity_controlsSpringStiffnessSpeed_whilePreservingIdenticalNaturalRestLength() = runTest {
        suspend fun runStiffnessTrial(elasticity: Float): Pair<Float, Float> {
            val world = SimulationWorld(
                EnvironmentSpec(
                    wallRepulsionPx = 0f,
                    airDamping = DeponderDefaults.Root.AIR_DAMPING,
                    restSpeedPxPerSec = 0.05f,
                    restAccelPxPerSecSq = 0.1f,
                ),
            )
            world.updateViewport(1000f, 1000f)
            world.upsertPlanet(PlanetSpec(id = "anchor", pinned = true, repulsionRadiusPx = 80f), 300f, 500f)
            world.upsertPlanet(PlanetSpec(id = "free", repulsionRadiusPx = 80f), 600f, 500f) // stretched 300px > 120px
            world.upsertRubber(RubberSpec(startId = "anchor", endId = "free", elasticity = elasticity, naturalLengthPx = 120f))

            // Measure snap-back progress after 0.25s (30 frames @ 120Hz) under textbook F = ma (px/s^2)
            repeat(30) {
                world.step(dtSeconds = 1f / 120f, scale = 1.0f)
            }
            val earlyX = world.bodyOf("free")!!.x

            // Let settle completely to equilibrium
            var active = true
            var guard = 0
            while (active && guard < 2400) {
                active = world.step(dtSeconds = 1f / 120f, scale = 1.0f)
                guard += 1
            }
            val settledDist = world.bodyOf("free")!!.x - world.bodyOf("anchor")!!.x
            return earlyX to settledDist
        }

        val (earlySoft, settledSoft) = runStiffnessTrial(elasticity = 1.5f)
        val (earlyStiff, settledStiff) = runStiffnessTrial(elasticity = 20.0f)

        // Higher elasticity snaps toward anchor significantly faster in the first 0.25s...
        assertTrue(earlyStiff < earlySoft - 25f, "Stiff spring ($earlyStiff) must snap faster than soft ($earlySoft)")
        // ...while BOTH converge to the exact same naturalLengthPx (120px)!
        assertEquals(120f, settledSoft, 1.5f)
        assertEquals(120f, settledStiff, 1.5f)
    }

    @Test
    fun asymmetricRadiationRange_onlyActiveFieldFires_andIsOrderIndependent() {
        val fWhenAFirst = HybridQuadtree.pairwiseForceOnS(
            sx = 100f,
            sy = 200f,
            sR = 500f,
            sEc = 1.5f,
            sInfCharge = 0f,
            ex = 400f,
            ey = 200f,
            eR = 100f,
            eEc = 3.0f,
            infiniteChargeStrength = 180_000f,
            softeningSq = 64f,
        )
        val fWhenBFirst = HybridQuadtree.pairwiseForceOnS(
            sx = 400f,
            sy = 200f,
            sR = 100f,
            sEc = 3.0f,
            sInfCharge = 0f,
            ex = 100f,
            ey = 200f,
            eR = 500f,
            eEc = 1.5f,
            infiniteChargeStrength = 180_000f,
            softeningSq = 64f,
        )

        assertEquals(-300f, fWhenAFirst.x, 1e-4f)
        assertEquals(0f, fWhenAFirst.y, 1e-4f)
        assertEquals(300f, fWhenBFirst.x, 1e-4f)
        assertEquals(0f, fWhenBFirst.y, 1e-4f)
    }

    @Test
    fun hybridQuadtree_exactFiniteCulling_matchesBruteForceAcross120HeterogeneousNodes() {
        val n = 120
        val rng = Random(2026)
        val px = FloatArray(n) { if (it < 4) 250f else rng.nextFloat() * 1200f }
        val py = FloatArray(n) { if (it < 4) 250f else rng.nextFloat() * 1200f }
        val r = FloatArray(n) { if (it == 10) 900f else 40f + rng.nextFloat() * 180f }
        val ec = FloatArray(n) { 0.5f + rng.nextFloat() * 2.0f }

        val tree = HybridQuadtree()
        tree.build(n, px, py, r, ec, infiniteChargeStrength = 180_000f)
        val stack = IntArray(HybridQuadtree.REQUIRED_STACK_SIZE)

        for (s in 0 until n) {
            val treeF = tree.queryForceOnNode(
                s = s,
                px = px,
                py = py,
                effectiveRadius = r,
                elasticity = ec,
                infiniteChargeStrength = 180_000f,
                thetaSq = 0.81f,
                softeningSq = 64f,
                stack = stack,
            )
            var bruteFx = 0f
            var bruteFy = 0f
            for (e in 0 until n) {
                if (e == s) continue
                val f = HybridQuadtree.pairwiseForceOnS(
                    sx = px[s],
                    sy = py[s],
                    sR = r[s],
                    sEc = ec[s],
                    sInfCharge = 0f,
                    ex = px[e],
                    ey = py[e],
                    eR = r[e],
                    eEc = ec[e],
                    infiniteChargeStrength = 180_000f,
                    softeningSq = 64f,
                )
                bruteFx += f.x
                bruteFy += f.y
            }
            assertEquals(bruteFx, treeF.x, 1e-2f, "fx mismatch at node $s")
            assertEquals(bruteFy, treeF.y, 1e-2f, "fy mismatch at node $s")
        }
    }

    @Test
    fun frameRateIndependence_60Hz_120Hz_and_240Hz_produceConsistentRealTimeTrajectories() = runTest {
        suspend fun runForOneSecond(fps: Int): Float {
            val world = SimulationWorld(
                EnvironmentSpec(
                    airDamping = 1.2f,
                    wallRepulsionPx = 0f,
                    coolingEnabled = false,
                ),
            )
            world.updateViewport(1000f, 1000f)
            world.upsertPlanet(PlanetSpec(id = "anchor", pinned = true, repulsionRadiusPx = 0f), 300f, 500f)
            world.upsertPlanet(PlanetSpec(id = "free", mass = 2.0f, repulsionRadiusPx = 0f), 600f, 500f)
            world.upsertRubber(RubberSpec(startId = "anchor", endId = "free", elasticity = 4.0f, naturalLengthPx = 150f))

            val dt = 1f / fps.toFloat()
            repeat(fps) {
                world.step(dtSeconds = dt, scale = 1.0f)
            }
            return world.bodyOf("free")!!.x
        }

        val x60 = runForOneSecond(60)
        val x120 = runForOneSecond(120)
        val x240 = runForOneSecond(240)

        assertEquals(x120, x60, 1e-3f, "60Hz with 2 substeps must match 120Hz single step")
        assertTrue(abs(x240 - x120) < 1.5f, "240Hz ($x240) and 120Hz ($x120) must remain speed-consistent")
    }

    @Test
    fun settled120HzMicroDrag_immediatelyWakesConnectedNeighborWithoutDeadbandFreeze() = runTest {
        val world = SimulationWorld(
            EnvironmentSpec(
                airDamping = DeponderDefaults.Root.AIR_DAMPING,
                wallRepulsionPx = 0f,
            ),
        )
        world.updateViewport(800f, 800f)
        world.upsertPlanet(PlanetSpec(id = "a", repulsionRadiusPx = 0f), 300f, 400f)
        world.upsertPlanet(PlanetSpec(id = "b", repulsionRadiusPx = 0f), 400f, 400f)
        world.upsertRubber(RubberSpec(startId = "a", endId = "b", elasticity = 2.0f, naturalLengthPx = 100f))

        var awake = true
        var guard = 0
        while (awake && guard < 300) {
            awake = world.step(dtSeconds = 1f / 120f, scale = 1.0f)
            guard += 1
        }
        assertFalse(awake, "World should reach equilibrium sleep first")

        val bBefore = world.bodyOf("b")!!.x

        world.setBodyPressed("a", true)
        world.offsetBodyPosition("a", dx = -12f, dy = 0f)
        repeat(30) {
            val active = world.step(dtSeconds = 1f / 120f, scale = 1.0f)
            assertTrue(active, "World must remain awake while `a` is pressed and pulling `b`")
        }
        val bAfterEarlyFrames = world.bodyOf("b")!!.x
        assertTrue(
            bAfterEarlyFrames < bBefore - 0.2f,
            "Neighbor `b` ($bAfterEarlyFrames vs $bBefore) must respond immediately within 0.25s @ 120Hz without deadband freeze",
        )

        world.setBodyPressed("a", false)
        repeat(120) {
            world.step(dtSeconds = 1f / 120f, scale = 1.0f)
        }
        val bAfterFollowThrough = world.bodyOf("b")!!.x
        assertTrue(
            bAfterFollowThrough < bBefore - 1.5f,
            "Neighbor `b` ($bAfterFollowThrough vs $bBefore) must continue gliding smoothly toward equilibrium",
        )
    }

    @Test
    fun coincidentStackedOrigins_breakSymmetryDeterministically_andGateMissingRubberEndpoints() = runTest {
        val world = SimulationWorld(EnvironmentSpec(wallRepulsionPx = DeponderDefaults.INFINITE_RANGE))
        world.updateViewport(600f, 600f)

        val pose = world.upsertRubber(RubberSpec(startId = "n0", endId = "n1", naturalLengthPx = 80f))
        assertFalse(pose.isActive)

        repeat(4) { idx ->
            world.upsertPlanet(PlanetSpec(id = "n$idx", repulsionRadiusPx = 120f, elasticity = 2.0f), 0f, 0f)
        }
        assertTrue(pose.isActive, "Rubber activates automatically once both endpoints mount")

        repeat(60) {
            world.step(dtSeconds = 1f / 120f, scale = 1.0f)
        }
        val b0 = world.bodyOf("n0")!!
        val b1 = world.bodyOf("n1")!!
        val dist = hypot(b1.x - b0.x, b1.y - b0.y)
        assertTrue(dist > 20f, "Stacked coincident nodes must blossom apart via golden-angle perturbation (dist=$dist)")

        world.removePlanet("n1")
        assertFalse(pose.isActive)
    }

    @Test
    fun largeGraph_tier3ParallelQuadtree_convergesToSleep_andIsolatesConcurrentMutations() = runTest {
        val world = SimulationWorld(
            EnvironmentSpec(
                airDamping = 30f,
                wallRepulsionPx = DeponderDefaults.INFINITE_RANGE,
                coolingEnabled = true,
                alphaDecay = 0.08f,
            ),
        )
        world.updateViewport(1000f, 1000f)
        val nodes = (0 until 260).map { i ->
            PlanetSpec(id = "n$i", repulsionRadiusPx = DeponderDefaults.INFINITE_RANGE, elasticity = 0.5f)
        }
        val edges = (0 until 100).map { i ->
            RubberSpec(startId = "n$i", endId = "n${i + 1}", naturalLengthPx = 60f)
        }
        world.syncTopology(nodes, edges) { spec ->
            val idx = spec.id.removePrefix("n").toInt()
            Vec2(200f + (idx % 20) * 30f, 200f + (idx / 20) * 30f)
        }

        // Verify concurrent UI-thread topology/position mutations while Tier-3 step() runs on Dispatchers.Default
        // never throw ConcurrentModificationException/IndexOutOfBoundsException or overwrite dragged coordinates.
        repeat(8) { round ->
            val stepDeferred = async(start = CoroutineStart.UNDISPATCHED) {
                world.step(dtSeconds = 0.032f, scale = 1.0f)
            }
            world.setBodyPosition("n0", 123f + round, 456f)
            world.upsertPlanet(PlanetSpec(id = "dyn_$round"), 500f, 500f)
            world.removePlanet("dyn_$round")
            assertTrue(stepDeferred.await())
            assertEquals(123f + round, world.bodyOf("n0")!!.x, 1e-3f)
            assertEquals(456f, world.bodyOf("n0")!!.y, 1e-3f)
        }

        var active = true
        var steps = 0
        while (active && steps < 400) {
            active = world.step(dtSeconds = 0.016f, scale = 1.0f)
            steps += 1
        }
        assertFalse(active, "260-node Tier-3 parallel simulation must settle to 0% CPU sleep")
    }

    @Test
    fun collinearRowOrigins_break1DSaddleSymmetryInto2DLayout() = runTest {
        val world = SimulationWorld(
            EnvironmentSpec(
                airDamping = 1.5f,
                wallRepulsionPx = 150f,
            ),
        )
        world.updateViewport(600f, 600f)

        // Simulate 4 unpinned planets laid out along a strictly horizontal Row (`y = 300f`).
        for (i in 0 until 4) {
            world.upsertPlanet(
                PlanetSpec(id = "p$i", repulsionRadiusPx = 220f, elasticity = 1.5f),
                initialX = 150f + i * 60f,
                initialY = 300f,
            )
        }
        world.upsertRubber(RubberSpec(startId = "p0", endId = "p1", naturalLengthPx = 160f, elasticity = 2.0f))
        world.upsertRubber(RubberSpec(startId = "p1", endId = "p2", naturalLengthPx = 160f, elasticity = 2.0f))
        world.upsertRubber(RubberSpec(startId = "p2", endId = "p3", naturalLengthPx = 160f, elasticity = 2.0f))
        world.upsertRubber(RubberSpec(startId = "p3", endId = "p0", naturalLengthPx = 160f, elasticity = 2.0f))

        repeat(600) {
            world.step(dtSeconds = 1f / 120f, scale = 1.0f)
        }

        val ys = world.bodies.map { it.y }
        val ySpan = (ys.maxOrNull() ?: 0f) - (ys.minOrNull() ?: 0f)
        assertTrue(ySpan > 50f, "Collinear 3+ unpinned nodes must blossom into 2D instead of staying trapped on 1D y=300 (ySpan=$ySpan)")
    }

    @Test
    fun simulationWorld_rejectsNonFiniteMutators_andPreservesConcurrentTier3ReheatAlpha() = runTest {
        val world = SimulationWorld(
            EnvironmentSpec(
                airDamping = 20f,
                wallRepulsionPx = DeponderDefaults.INFINITE_RANGE,
                coolingEnabled = true,
                alphaDecay = 0.5f,
            ),
        )
        world.updateViewport(800f, 600f)
        world.upsertPlanet(PlanetSpec(id = "a"), 100f, 100f)

        assertFailsWith<IllegalArgumentException> {
            world.upsertPlanet(PlanetSpec(id = "bad"), Float.NaN, 100f)
        }
        assertFailsWith<IllegalArgumentException> {
            world.updateAllRubbersElasticity(Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            world.setBodyPosition("a", Float.POSITIVE_INFINITY, 100f)
        }
        assertFailsWith<IllegalArgumentException> {
            world.offsetBodyPosition("a", 0f, Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            world.reheat(Float.NaN)
        }

        // Non-finite step/viewport/size inputs must never poison body coordinates with NaN.
        world.updateViewport(Float.NaN, 600f)
        assertEquals(800f, world.viewportWidthPx, 0f)
        world.setBodySize("a", Float.NaN, 40f)
        assertTrue(world.step(Float.NaN, Float.NaN))
        assertTrue(world.bodyOf("a")!!.x.isFinite())
        assertTrue(world.bodyOf("a")!!.y.isFinite())

        // Tier-3 concurrency: reheat() called while Dispatchers.Default cools alpha must not be overwritten!
        val nodes = (0 until 260).map { i ->
            PlanetSpec(id = "t$i", repulsionRadiusPx = 0f, elasticity = 0.2f)
        }
        world.syncTopology(nodes, emptyList()) { spec ->
            val idx = spec.id.removePrefix("t").toInt()
            Vec2(100f + (idx % 20) * 25f, 100f + (idx / 20) * 25f)
        }
        // Cool alpha down first
        repeat(6) {
            world.step(0.032f, 1.0f)
        }
        assertTrue(world.alpha < 0.8f, "Alpha should have cooled prior to concurrent reheat test, was ${world.alpha}")

        val stepDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            world.step(dtSeconds = 0.032f, scale = 1.0f)
        }
        // Trigger reheat() WITHOUT mutating topology or body coordinates while Tier-3 is suspended!
        world.reheat(1.0f)
        assertTrue(stepDeferred.await())
        assertEquals(1.0f, world.alpha, 1e-6f)
    }

    @Test
    fun tier3CausalJournalReplay_serializesCoalescedOffsetsPinningAndReheatInExactOrder() = runTest {
        val world = SimulationWorld(
            EnvironmentSpec(
                airDamping = 20f,
                wallRepulsionPx = 0f,
                coolingEnabled = true,
                alphaDecay = 0.5f,
            ),
        )
        world.updateViewport(1000f, 1000f)
        val nodes = (0 until 260).map { i ->
            PlanetSpec(id = "t$i", repulsionRadiusPx = 0f, elasticity = 0.2f)
        }
        val edges = listOf(
            RubberSpec(startId = "t0", endId = "t1", naturalLengthPx = 80f, elasticity = 1.0f),
        )
        world.syncTopology(nodes, edges) { spec ->
            val idx = spec.id.removePrefix("t").toInt()
            Vec2(120f + (idx % 20) * 30f, 120f + (idx / 20) * 30f)
        }
        repeat(4) {
            world.step(0.032f, 1.0f)
        }
        assertTrue(world.alpha < 0.8f, "Alpha should have cooled prior to concurrent replay, was ${world.alpha}")

        // Round A: Reheat(0.85f) + pin without interaction-alpha boost -> exact 0.85f monotonic floor survives cooling!
        val pinAndReheatDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            world.step(dtSeconds = 0.032f, scale = 1.0f)
        }
        world.setBodyPinned("t2", true)
        world.reheat(0.85f)
        assertTrue(pinAndReheatDeferred.await())
        assertTrue(world.bodyOf("t2")!!.spec.pinned)
        assertEquals(0.85f, world.alpha, 1e-6f)

        val t0InitialX = world.bodyOf("t0")!!.x
        val t0InitialY = world.bodyOf("t0")!!.y

        // Round B: Coalesced offsets + pin + elasticity update -> exact cumulative position & INTERACTION_ALPHA = 1.0f!
        val stepDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            world.step(dtSeconds = 0.032f, scale = 1.0f)
        }
        world.offsetBodyPosition("t0", 15f, -10f, 1.0f)
        world.offsetBodyPosition("t0", 5f, 25f, 1.0f)
        world.setBodyPinned("t1", true)
        world.updateAllRubbersElasticity(4.5f)
        world.reheat(0.95f)

        assertTrue(stepDeferred.await())
        val t0 = world.bodyOf("t0")!!
        val t1 = world.bodyOf("t1")!!
        assertEquals(t0InitialX + 20f, t0.x, 1e-4f)
        assertEquals(t0InitialY + 15f, t0.y, 1e-4f)
        assertEquals(0f, t0.vx, 0f)
        assertEquals(0f, t0.vy, 0f)
        assertTrue(t1.spec.pinned)
        assertEquals(0f, t1.vx, 0f)
        assertEquals(0f, t1.vy, 0f)
        assertEquals(4.5f, world.rubberOf(EdgeKey("t0", "t1"))!!.spec.elasticity, 1e-4f)
        assertEquals(DeponderDefaults.Simulation.INTERACTION_ALPHA, world.alpha, 1e-6f)

        // Round C: Concurrent clear of all planets while Tier-3 is suspended -> step() returns false!
        val clearDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            world.step(dtSeconds = 0.032f, scale = 1.0f)
        }
        world.syncTopology(emptyList(), emptyList()) { Vec2.Zero }
        assertFalse(clearDeferred.await(), "Empty world after concurrent Tier-3 clear must return false")
    }

    @Test
    fun upsertRubberAndSyncTopology_rejectInvalidSpecsAndCoordinatesAtomically() {
        val world = SimulationWorld()
        world.upsertPlanet(PlanetSpec(id = "keep"), 50f, 60f)

        assertFailsWith<IllegalArgumentException> {
            world.upsertRubber(RubberSpec(startId = "keep", endId = "other", elasticity = Float.NaN))
        }
        assertFailsWith<IllegalArgumentException> {
            world.upsertRubber(RubberSpec(startId = "keep", endId = "other", naturalLengthPx = -1f))
        }
        assertFailsWith<IllegalArgumentException> {
            world.syncTopology(
                planets = listOf(
                    PlanetSpec(id = "fresh"),
                    PlanetSpec(id = "bad"),
                ),
                edges = emptyList(),
            ) { spec ->
                if (spec.id == "bad") Vec2(Float.NaN, 0f) else Vec2(10f, 20f)
            }
        }
        // Atomic rollback: existing body untouched, no partial nodes inserted!
        assertEquals(1, world.bodies.size)
        assertEquals("keep", world.bodies.first().spec.id)
        assertEquals(50f, world.bodyOf("keep")!!.x, 0f)
        assertEquals(null, world.bodyOf("fresh"))
    }

    private fun segmentsCross(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
        dx: Float,
        dy: Float,
    ): Boolean {
        fun orient(px: Float, py: Float, qx: Float, qy: Float, rx: Float, ry: Float): Float =
            (qx - px) * (ry - py) - (qy - py) * (rx - px)
        val o1 = orient(ax, ay, bx, by, cx, cy)
        val o2 = orient(ax, ay, bx, by, dx, dy)
        val o3 = orient(cx, cy, dx, dy, ax, ay)
        val o4 = orient(cx, cy, dx, dy, bx, by)
        return (o1 * o2 < 0f) && (o3 * o4 < 0f)
    }

    private fun countEdgeCrossings(world: SimulationWorld): Int {
        val rubbers = world.rubbers
        var crossings = 0
        for (i in rubbers.indices) {
            val r1 = rubbers[i].spec
            val a1 = world.bodyOf(r1.startId) ?: continue
            val b1 = world.bodyOf(r1.endId) ?: continue
            for (j in i + 1 until rubbers.size) {
                val r2 = rubbers[j].spec
                if (r1.startId == r2.startId || r1.startId == r2.endId ||
                    r1.endId == r2.startId || r1.endId == r2.endId
                ) {
                    continue
                }
                val a2 = world.bodyOf(r2.startId) ?: continue
                val b2 = world.bodyOf(r2.endId) ?: continue
                if (segmentsCross(a1.x, a1.y, b1.x, b1.y, a2.x, a2.y, b2.x, b2.y)) {
                    crossings += 1
                }
            }
        }
        return crossings
    }

    @Test
    fun rootPhysics_andEnvironmentSpec_preserveDefaultsBinaryCopyAndUnfoldToggle() {
        assertFalse(DeponderDefaults.Root.UNFOLD_ENABLED)
        val base = RootPhysics()
        assertFalse(base.unfoldEnabled)

        val unfolded = base.withUnfold()
        assertTrue(unfolded.unfoldEnabled)
        assertNotEquals(base, unfolded)
        assertEquals(base, unfolded.withUnfold(false))
        assertEquals(base.hashCode(), unfolded.withUnfold(false).hashCode())

        // All legacy fluent / copy helpers preserve unfoldEnabled cleanly.
        val chained = unfolded
            .withAirDamping(2.2f)
            .withScaleBounds(initScale = 1.0f, minScale = 0.5f, maxScale = 2.0f)
            .withWallRepulsionDp(90f)
            .withCooling(true, 0.02f)
            .copy(initScale = 1.1f)
        assertTrue(chained.unfoldEnabled)
        assertEquals(2.2f, chained.airDamping, 1e-4f)
        assertEquals(90f, chained.wallRepulsionDp, 1e-4f)
        assertTrue(chained.coolingEnabled)

        val env = unfolded.toEnvironmentSpec(density = 2.0f)
        assertTrue(env.unfoldEnabled)
        assertFalse(EnvironmentSpec().unfoldEnabled)
        assertTrue(EnvironmentSpec().withUnfold().unfoldEnabled)
        assertEquals(EnvironmentSpec(), EnvironmentSpec().withUnfold(true).withUnfold(false))

        // -0.0f normalization preserves equals / hashCode on RootPhysics and EnvironmentSpec.
        val negRoot = RootPhysics(initScale = 1.0f, airDamping = -0.0f)
        val posRoot = RootPhysics(initScale = 1.0f, airDamping = 0.0f)
        assertEquals(posRoot, negRoot)
        assertEquals(posRoot.hashCode(), negRoot.hashCode())
    }

    @Test
    fun topologicalUnfold_untanglesScrambledCycleWithTail_andSleepsAtZeroCpu() = runTest {
        val edges = listOf(
            0 to 1,
            1 to 2,
            2 to 3,
            3 to 4,
            4 to 5,
            5 to 0,
            6 to 4,
        )
        val golden = 2.399963229728653

        for (trial in 0 until 8) {
            val env = RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f)
            val world = SimulationWorld(env)
            world.updateViewport(360f, 620f)

            val rng = Random(100 + trial)
            val perm = (0 until 7).shuffled(rng)
            for (i in 0 until 7) {
                val angle = (perm[i] + 1) * golden
                val r = 75.0
                world.upsertPlanet(
                    PlanetSpec(id = "n$i"),
                    initialX = 180f + (cos(angle) * r).toFloat(),
                    initialY = 310f + (sin(angle) * r).toFloat(),
                )
            }
            for ((u, v) in edges) {
                world.upsertRubber(
                    RubberSpec(startId = "n$u", endId = "n$v", naturalLengthPx = 135f),
                )
            }

            var active = true
            var steps = 0
            while (active && steps < 1800) {
                active = world.step(dtSeconds = 1f / 120f, scale = 1.0f)
                steps += 1
            }
            assertFalse(active, "Trial $trial must converge to 0% CPU sleep within 1800 sub-frames (steps=$steps)")
            assertEquals(
                0,
                countEdgeCrossings(world),
                "Trial $trial should settle with 0 crossings",
            )
            for (b in world.bodies) {
                assertTrue(b.x.isFinite() && b.y.isFinite())
                assertEquals(0f, b.vx, 0f)
                assertEquals(0f, b.vy, 0f)
            }
        }
    }

    @Test
    fun topologicalUnfold_handlesTreesGridsMultiCyclesDisconnectedIslands_andTier3LargeGraphs() = runTest {
        suspend fun runAndVerifyPlanar(
            nodeCount: Int,
            edgePairs: List<Pair<Int, Int>>,
            vpW: Float,
            vpH: Float,
        ): SimulationWorld {
            val world = SimulationWorld(RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f))
            world.updateViewport(vpW, vpH)
            val planets = (0 until nodeCount).map { PlanetSpec(id = "p$it") }
            val rubbers = edgePairs.map { (u, v) ->
                RubberSpec(startId = "p$u", endId = "p$v", naturalLengthPx = 110f)
            }
            world.syncTopology(planets, rubbers) { Vec2(vpW * 0.5f, vpH * 0.5f) }

            var active = true
            var guard = 0
            while (active && guard < 1800) {
                active = world.step(dtSeconds = 0.016f, scale = 1.0f)
                guard += 1
            }
            assertFalse(active, "Topology (N=$nodeCount) should settle cleanly (guard=$guard)")
            assertEquals(0, countEdgeCrossings(world), "Topology (N=$nodeCount) must settle with 0 crossings")
            return world
        }

        // 1. Binary tree (N = 31)
        val treeEdges = (0 until 15).flatMap { i -> listOf(i to (2 * i + 1), i to (2 * i + 2)) }
        runAndVerifyPlanar(31, treeEdges, 1100f, 1100f)

        // 2. Multi-cycle with pendant branches (N = 10)
        val multiCycleEdges = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 0, 2 to 5,
            6 to 1, 7 to 3, 8 to 4, 9 to 8,
        )
        runAndVerifyPlanar(10, multiCycleEdges, 700f, 700f)

        // 3. 5x5 planar grid (N = 25)
        val gridEdges = ArrayList<Pair<Int, Int>>()
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val u = r * 5 + c
                if (c + 1 < 5) gridEdges.add(u to (u + 1))
                if (r + 1 < 5) gridEdges.add(u to (u + 5))
            }
        }
        runAndVerifyPlanar(25, gridEdges, 900f, 900f)

        // 4. Disconnected islands + isolated nodes: 4-cycle + 3-node path + 2 isolated nodes
        val islandWorld = SimulationWorld(RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f))
        islandWorld.updateViewport(1000f, 1000f)
        val islandPlanets = (0 until 9).map { PlanetSpec(id = "i$it") }
        val islandEdges = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 0,
            4 to 5, 5 to 6,
        ).map { (u, v) -> RubberSpec(startId = "i$u", endId = "i$v", naturalLengthPx = 110f) }
        islandWorld.syncTopology(islandPlanets, islandEdges) { Vec2(500f, 500f) }
        var activeIsland = true
        var islandGuard = 0
        while (activeIsland && islandGuard < 1800) {
            activeIsland = islandWorld.step(0.016f, 1.0f)
            islandGuard += 1
        }
        assertFalse(activeIsland)
        assertEquals(0, countEdgeCrossings(islandWorld))
        var minInterIslandDist = Float.POSITIVE_INFINITY
        for (a in 0..3) {
            val ba = islandWorld.bodyOf("i$a")!!
            for (b in 4..8) {
                val bb = islandWorld.bodyOf("i$b")!!
                minInterIslandDist = min(minInterIslandDist, hypot(ba.x - bb.x, ba.y - bb.y))
            }
        }
        assertTrue(minInterIslandDist > 30f, "Disconnected islands must stay separated (minDist=$minInterIslandDist)")

        // 5. Tier-3 parallel graph (N = 280 >= PARALLEL_DISPATCH_THRESHOLD): 14x20 grid
        val tier3World = SimulationWorld(
            RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f),
        )
        tier3World.updateViewport(2800f, 2200f)
        val t3Planets = (0 until 280).map { PlanetSpec(id = "g$it") }
        val t3Edges = ArrayList<RubberSpec>()
        for (r in 0 until 14) {
            for (c in 0 until 20) {
                val u = r * 20 + c
                if (c + 1 < 20) t3Edges.add(RubberSpec("g$u", "g${u + 1}", naturalLengthPx = 90f))
                if (r + 1 < 14) t3Edges.add(RubberSpec("g$u", "g${u + 20}", naturalLengthPx = 90f))
            }
        }
        tier3World.syncTopology(t3Planets, t3Edges) { Vec2(1400f, 1100f) }
        repeat(150) {
            tier3World.step(0.016f, 1.0f)
        }
        for (b in tier3World.bodies) {
            assertTrue(b.x.isFinite() && b.y.isFinite(), "Tier-3 unfolded coordinates must remain finite")
        }
        assertEquals(0, countEdgeCrossings(tier3World), "Tier-3 14x20 grid must unfold cleanly with 0 crossings")
    }

    @Test
    fun topologicalUnfold_incrementalGrowthAndPinnedAnchors_remainSmoothAndRespectLocks() = runTest {
        // 1. Incremental edge-by-edge growth ("生长重演"): smooth motion + 0 final crossings.
        val growWorld = SimulationWorld(RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f))
        growWorld.updateViewport(420f, 680f)
        val growthEdges = listOf(0 to 1, 3 to 4, 1 to 2, 4 to 5, 2 to 3, 5 to 0, 6 to 4)
        var maxSingleFrameDelta = 0f
        for ((u, v) in growthEdges) {
            if (growWorld.bodyOf("g$u") == null) {
                growWorld.upsertPlanet(PlanetSpec("g$u"), 210f, 340f)
            }
            if (growWorld.bodyOf("g$v") == null) {
                growWorld.upsertPlanet(PlanetSpec("g$v"), 210f, 340f)
            }
            growWorld.upsertRubber(RubberSpec("g$u", "g$v", naturalLengthPx = 120f))
            repeat(18) {
                growWorld.step(0.016f, 1.0f)
                for (b in growWorld.bodies) {
                    maxSingleFrameDelta = max(maxSingleFrameDelta, hypot(b.dx, b.dy))
                }
            }
        }
        var activeGrow = true
        var growGuard = 0
        while (activeGrow && growGuard < 1800) {
            activeGrow = growWorld.step(0.016f, 1.0f)
            growGuard += 1
        }
        assertFalse(activeGrow)
        assertEquals(0, countEdgeCrossings(growWorld))
        assertTrue(maxSingleFrameDelta < 180f, "Growth unfolding must glide smoothly without teleporting (max=$maxSingleFrameDelta)")

        // 2. Pinned & pressed nodes remain strictly locked during unfolding.
        val pinWorld = SimulationWorld(RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f))
        pinWorld.updateViewport(600f, 600f)
        pinWorld.upsertPlanet(PlanetSpec("p0", pinned = true), 160f, 240f)
        pinWorld.upsertPlanet(PlanetSpec("p1"), 300f, 300f)
        pinWorld.upsertPlanet(PlanetSpec("p2"), 300f, 300f)
        pinWorld.upsertPlanet(PlanetSpec("p3", pinned = true), 340f, 400f)
        pinWorld.upsertPlanet(PlanetSpec("p4"), 300f, 300f)
        for ((u, v) in listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 2)) {
            pinWorld.upsertRubber(RubberSpec("p$u", "p$v", naturalLengthPx = 120f))
        }
        pinWorld.setBodyPressed("p4", true)
        val pressedStartX = pinWorld.bodyOf("p4")!!.x
        val pressedStartY = pinWorld.bodyOf("p4")!!.y
        repeat(80) {
            pinWorld.step(0.016f, 1.0f)
        }
        assertEquals(160f, pinWorld.bodyOf("p0")!!.x, 1e-4f)
        assertEquals(240f, pinWorld.bodyOf("p0")!!.y, 1e-4f)
        assertEquals(340f, pinWorld.bodyOf("p3")!!.x, 1e-4f)
        assertEquals(400f, pinWorld.bodyOf("p3")!!.y, 1e-4f)
        assertEquals(pressedStartX, pinWorld.bodyOf("p4")!!.x, 1e-4f)
        assertEquals(pressedStartY, pinWorld.bodyOf("p4")!!.y, 1e-4f)
        pinWorld.setBodyPressed("p4", false)
        var pinActive = true
        var pinGuard = 0
        while (pinActive && pinGuard < 1800) {
            pinActive = pinWorld.step(0.016f, 1.0f)
            pinGuard += 1
        }
        assertFalse(pinActive)
        assertEquals(0, countEdgeCrossings(pinWorld))

        // 3. Reheat untangles a manually crossed bowtie 4-cycle when unfoldEnabled = true.
        val reheatWorld = SimulationWorld(RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f))
        reheatWorld.updateViewport(600f, 600f)
        reheatWorld.upsertPlanet(PlanetSpec("c0"), 200f, 200f)
        reheatWorld.upsertPlanet(PlanetSpec("c1"), 400f, 400f)
        reheatWorld.upsertPlanet(PlanetSpec("c2"), 200f, 400f)
        reheatWorld.upsertPlanet(PlanetSpec("c3"), 400f, 200f)
        for ((u, v) in listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0)) {
            reheatWorld.upsertRubber(RubberSpec("c$u", "c$v", naturalLengthPx = 120f))
        }
        // Step past initial unfold, then manually cross into bowtie and call reheat()!
        repeat(100) { reheatWorld.step(0.016f, 1.0f) }
        reheatWorld.setBodyPosition("c0", 200f, 200f)
        reheatWorld.setBodyPosition("c1", 400f, 400f)
        reheatWorld.setBodyPosition("c2", 200f, 400f)
        reheatWorld.setBodyPosition("c3", 400f, 200f)
        assertEquals(1, countEdgeCrossings(reheatWorld))

        reheatWorld.reheat()
        var reheatActive = true
        var reheatGuard = 0
        while (reheatActive && reheatGuard < 1800) {
            reheatActive = reheatWorld.step(0.016f, 1.0f)
            reheatGuard += 1
        }
        assertFalse(reheatActive)
        assertEquals(0, countEdgeCrossings(reheatWorld))
    }

    @Test
    fun topologicalUnfold_staticAuditEdgeCases_delayedViewportWakeScaleLockedIsolatesBiCenteredTreesAndCancellation() = runTest {
        // 1. Builder preservation across copy/with* methods.
        val configured = RootPhysics()
            .withUnfold(true)
            .withAirDamping(2.0f)
            .withScaleBounds(initScale = 1.0f, minScale = 0.5f, maxScale = 2.0f)
        assertTrue(configured.unfoldEnabled)

        // 2. Stepping once at 0x0 viewport before updateViewport(360, 760) must still allow fresh
        //    portrait PCA alignment & centering once real layout dimensions arrive.
        val portraitWorld = SimulationWorld(RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f))
        val ladderPlanets = (0 until 10).map { PlanetSpec(id = "l$it") }
        val ladderEdges = ArrayList<RubberSpec>()
        for (i in 0 until 4) {
            ladderEdges.add(RubberSpec("l$i", "l${i + 1}", naturalLengthPx = 90f))
            ladderEdges.add(RubberSpec("l${i + 5}", "l${i + 6}", naturalLengthPx = 90f))
        }
        for (i in 0 until 5) {
            ladderEdges.add(RubberSpec("l$i", "l${i + 5}", naturalLengthPx = 90f))
        }
        portraitWorld.syncTopology(ladderPlanets, ladderEdges) { Vec2(0f, 0f) }
        portraitWorld.step(0.016f, 1.0f) // Frame at 0x0 viewport before layout!
        portraitWorld.updateViewport(360f, 760f)
        var portraitActive = true
        var portraitGuard = 0
        while (portraitActive && portraitGuard < 1800) {
            portraitActive = portraitWorld.step(0.016f, 1.0f)
            portraitGuard += 1
        }
        assertFalse(portraitActive)
        assertEquals(0, countEdgeCrossings(portraitWorld))
        var lMinX = Float.POSITIVE_INFINITY
        var lMaxX = Float.NEGATIVE_INFINITY
        var lMinY = Float.POSITIVE_INFINITY
        var lMaxY = Float.NEGATIVE_INFINITY
        for (b in portraitWorld.bodies) {
            if (b.x < lMinX) lMinX = b.x
            if (b.x > lMaxX) lMaxX = b.x
            if (b.y < lMinY) lMinY = b.y
            if (b.y > lMaxY) lMaxY = b.y
        }
        val spanX = lMaxX - lMinX
        val spanY = lMaxY - lMinY
        assertTrue(spanY > spanX, "Portrait viewport must align 2x5 ladder vertically (spanX=$spanX, spanY=$spanY)")
        assertEquals(180f, 0.5f * (lMinX + lMaxX), 45f)
        assertEquals(380f, 0.5f * (lMinY + lMaxY), 45f)

        // 3. wake() preserves manual bowtie arrangement, whereas reheat() untangles it.
        val wakeWorld = SimulationWorld(RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f))
        wakeWorld.updateViewport(600f, 600f)
        wakeWorld.upsertPlanet(PlanetSpec("w0"), 200f, 200f)
        wakeWorld.upsertPlanet(PlanetSpec("w1"), 400f, 400f)
        wakeWorld.upsertPlanet(PlanetSpec("w2"), 200f, 400f)
        wakeWorld.upsertPlanet(PlanetSpec("w3"), 400f, 200f)
        for ((u, v) in listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0)) {
            wakeWorld.upsertRubber(RubberSpec("w$u", "w$v", naturalLengthPx = 120f))
        }
        repeat(100) { wakeWorld.step(0.016f, 1.0f) }
        wakeWorld.setBodyPosition("w0", 200f, 200f)
        wakeWorld.setBodyPosition("w1", 400f, 400f)
        wakeWorld.setBodyPosition("w2", 200f, 400f)
        wakeWorld.setBodyPosition("w3", 400f, 200f)
        assertEquals(1, countEdgeCrossings(wakeWorld))
        wakeWorld.wake()
        var wakeActive = true
        var wakeGuard = 0
        while (wakeActive && wakeGuard < 1800) {
            wakeActive = wakeWorld.step(0.016f, 1.0f)
            wakeGuard += 1
        }
        assertFalse(wakeActive)
        assertEquals(1, countEdgeCrossings(wakeWorld), "wake() must NOT overwrite manual arrangement")
        wakeWorld.reheat()
        var reheatActive2 = true
        var reheatGuard2 = 0
        while (reheatActive2 && reheatGuard2 < 1800) {
            reheatActive2 = wakeWorld.step(0.016f, 1.0f)
            reheatGuard2 += 1
        }
        assertFalse(reheatActive2)
        assertEquals(0, countEdgeCrossings(wakeWorld), "reheat() must untangle crossed cycle")

        // 4. Locked isolated node acts as immovable obstacle during component/isolate packing.
        val lockedIsoWorld = SimulationWorld(RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f))
        lockedIsoWorld.updateViewport(1000f, 1000f)
        val isoPlanets = listOf(
            PlanetSpec("p0"),
            PlanetSpec("p1"),
            PlanetSpec("p2"),
            PlanetSpec("p3"),
            PlanetSpec("pinIso", pinned = true),
            PlanetSpec("freeIso"),
        )
        val isoEdges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0).map { (u, v) ->
            RubberSpec("p$u", "p$v", naturalLengthPx = 100f)
        }
        lockedIsoWorld.syncTopology(isoPlanets, isoEdges) { Vec2(500f, 500f) }
        val pinIsoStart = lockedIsoWorld.bodyOf("pinIso")!!
        val pinIsoStartX = pinIsoStart.x
        val pinIsoStartY = pinIsoStart.y
        var isoActive = true
        var isoGuard = 0
        while (isoActive && isoGuard < 1800) {
            isoActive = lockedIsoWorld.step(0.016f, 1.0f)
            isoGuard += 1
        }
        assertFalse(isoActive)
        assertEquals(0, countEdgeCrossings(lockedIsoWorld))
        val pinIso = lockedIsoWorld.bodyOf("pinIso")!!
        assertEquals(pinIsoStartX, pinIso.x, 1e-4f)
        assertEquals(pinIsoStartY, pinIso.y, 1e-4f)
        for (other in lockedIsoWorld.bodies) {
            if (other.spec.id == "pinIso") continue
            val d = hypot(other.x - pinIso.x, other.y - pinIso.y)
            assertTrue(d > 35f, "Node ${other.spec.id} must not overlap locked isolated node (d=$d)")
        }

        // 5. Bi-centered double-star tree & symmetric bowtie cut-vertex with pendant subtree.
        val dblStarWorld = SimulationWorld(RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f))
        dblStarWorld.updateViewport(1100f, 1100f)
        val dblEdges = listOf(
            0 to 1,
            0 to 2, 0 to 3, 0 to 4, 2 to 5, 2 to 6,
            1 to 7, 1 to 8, 1 to 9, 7 to 10, 7 to 11,
        ).map { (u, v) -> RubberSpec("d$u", "d$v", naturalLengthPx = 105f) }
        dblStarWorld.syncTopology((0 until 12).map { PlanetSpec("d$it") }, dblEdges) { Vec2(550f, 550f) }
        var dblActive = true
        var dblGuard = 0
        while (dblActive && dblGuard < 1800) {
            dblActive = dblStarWorld.step(0.016f, 1.0f)
            dblGuard += 1
        }
        assertFalse(dblActive)
        assertEquals(0, countEdgeCrossings(dblStarWorld))

        val bowtieWorld = SimulationWorld(RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f))
        bowtieWorld.updateViewport(900f, 900f)
        val bowtieEdges = listOf(
            0 to 1, 1 to 2, 2 to 0,
            0 to 3, 3 to 4, 4 to 0,
            0 to 5, 5 to 6, 5 to 7,
        ).map { (u, v) -> RubberSpec("b$u", "b$v", naturalLengthPx = 110f) }
        val rng = Random(42)
        val shuffledPos = (0 until 8).map { i ->
            val a = i * 2.39996
            Vec2(450f + cos(a).toFloat() * 80f, 450f + sin(a).toFloat() * 80f)
        }.shuffled(rng)
        bowtieWorld.syncTopology((0 until 8).map { PlanetSpec("b$it") }, bowtieEdges) { spec ->
            shuffledPos[spec.id.removePrefix("b").toInt()]
        }
        var btActive = true
        var btGuard = 0
        while (btActive && btGuard < 1800) {
            btActive = bowtieWorld.step(0.016f, 1.0f)
            btGuard += 1
        }
        assertFalse(btActive)
        assertEquals(0, countEdgeCrossings(bowtieWorld))

        // 6. Tier-3 cancellation recovery: cancelling mid-step must preserve unfoldDirty for next resume.
        val t3CancelWorld = SimulationWorld(RootPhysics().withUnfold(true).toEnvironmentSpec(density = 1.0f))
        t3CancelWorld.updateViewport(2800f, 2200f)
        val t3Planets = (0 until 270).map { PlanetSpec("c$it") }
        val t3Edges = ArrayList<RubberSpec>()
        for (r in 0 until 9) {
            for (c in 0 until 30) {
                val u = r * 30 + c
                if (c + 1 < 30) t3Edges.add(RubberSpec("c$u", "c${u + 1}", naturalLengthPx = 85f))
                if (r + 1 < 9) t3Edges.add(RubberSpec("c$u", "c${u + 30}", naturalLengthPx = 85f))
            }
        }
        val t3Perm = (0 until 270).shuffled(Random(77))
        t3CancelWorld.syncTopology(t3Planets, t3Edges) { spec ->
            val p = t3Perm[spec.id.removePrefix("c").toInt()]
            val a = p * 2.39996
            Vec2(1400f + cos(a).toFloat() * 140f, 1100f + sin(a).toFloat() * 140f)
        }
        val cancelledStep = async(start = CoroutineStart.UNDISPATCHED) {
            t3CancelWorld.step(0.016f, 1.0f)
        }
        cancelledStep.cancel()
        runCatching { cancelledStep.await() }
        repeat(150) {
            t3CancelWorld.step(0.016f, 1.0f)
        }
        assertEquals(0, countEdgeCrossings(t3CancelWorld), "Resumed Tier-3 world must still unfold cleanly")
    }
}
