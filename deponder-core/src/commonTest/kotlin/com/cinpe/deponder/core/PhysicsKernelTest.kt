package com.cinpe.deponder.core

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
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

        val rubberSpec = RubberPhysics(elasticity = 1.5f, naturalLengthDp = 120f, endpointGapDp = 5f)
            .toRubberSpec(startId = "a", endId = "b", density = 2.0f)
        assertEquals(240f, rubberSpec.naturalLengthPx, 1e-4f)
        assertEquals(10f, rubberSpec.endpointGapPx, 1e-4f)
    }

    @Test
    fun planetContourClearance_trimsRubberPoseOutsideAsymmetricPlanetBounds_andPreventsOverlap() = runTest {
        val world = SimulationWorld(EnvironmentSpec(wallRepulsionPx = 0f, airDamping = 4.0f))
        world.updateViewport(1000f, 1000f)

        // Asymmetric Planet sizes: `hub` is 100x60 (`halfW = 50`), `leaf` is 40x40 (`halfW = 20`).
        world.upsertPlanet(PlanetSpec(id = "hub", pinned = true, repulsionRadiusPx = 0f), 100f, 300f)
        world.upsertPlanet(PlanetSpec(id = "leaf", pinned = true, repulsionRadiusPx = 0f), 300f, 300f)
        world.setBodySize("hub", widthPx = 100f, heightPx = 60f)
        world.setBodySize("leaf", widthPx = 40f, heightPx = 40f)

        val pose = world.upsertRubber(
            RubberSpec(startId = "hub", endId = "leaf", naturalLengthPx = 80f, endpointGapPx = 5f),
        )
        // Center-to-center = 200px; hub clearance = 50 + 5 = 55px (sxVis = 155); leaf clearance = 20 + 5 = 25px (exVis = 275).
        assertEquals(200f, pose.centerDistance, 1e-3f)
        assertEquals(120f, pose.length, 1e-3f) // 275 - 155 = 120px!
        assertEquals(215f, pose.centerX, 1e-3f) // (155 + 275) / 2 = 215px!

        // Dynamic rubber elasticity update wakes simulation and updates spec cleanly.
        world.updateAllRubbersElasticity(4.5f)
        assertEquals(4.5f, pose.spec.elasticity, 1e-4f)
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
}
