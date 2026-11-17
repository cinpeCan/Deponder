package com.cinpe.deponder.core

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Deterministic, pure Kotlin Multiplatform sparse topological layout solver (`O(K * (|V| + |E|))`).
 *
 * Extracts connected components, peels degree-1 tree limbs down to each component's 2-core,
 * unfolds cycles/meshes/grids via Farthest-First PivotMDS + bounded Sparse Stress Majorization,
 * fans subtrees radially into open exterior angular gaps, aligns the solved skeleton against
 * locked pins or established poses via 2D Orthogonal Procrustes, and packs multi-component islands
 * cleanly without overlap.
 *
 * Operates exclusively on primitive arrays with zero external dependencies or randomness, so
 * Tier-3 worker coroutines can instantiate isolated local instances safely.
 */
internal class TopologicalUnfolder {

    internal fun solve(
        n: Int,
        curX: FloatArray,
        curY: FloatArray,
        locked: BooleanArray,
        established: BooleanArray,
        edgeCount: Int,
        edgeStart: IntArray,
        edgeEnd: IntArray,
        edgeLength: FloatArray,
        viewportWidth: Float,
        viewportHeight: Float,
        wallRepulsionPx: Float,
        clampedScale: Float,
        outTargetX: FloatArray,
        outTargetY: FloatArray,
    ): Boolean {
        if (n <= 1 || edgeCount <= 0) return false

        val safeScale = if (clampedScale.isFinite()) max(1e-3f, clampedScale) else 1f
        val minEdgeLen = 24f * safeScale
        val maxEdgeLen = min(1e6f * safeScale, 1e9f)

        // Deduplicate undirected non-self edges into a simple graph and compute degrees.
        val dedupTable = SparsePairTable(edgeCount)
        for (e in 0 until edgeCount) {
            val u = edgeStart[e]
            val v = edgeEnd[e]
            if (u in 0 until n && v in 0 until n && u != v) {
                val rawLen = edgeLength[e]
                val sanitized = if (rawLen.isFinite()) rawLen.coerceIn(minEdgeLen, maxEdgeLen) else minEdgeLen
                dedupTable.putMaxWeight(u, v, sanitized, 1f / sanitized)
            }
        }
        val validEdgeCount = dedupTable.count
        if (validEdgeCount == 0) return false

        val deg = IntArray(n)
        var edgeLenSum = 0.0
        val uUnique = dedupTable.uArr
        val vUnique = dedupTable.vArr
        val lUnique = dedupTable.idealArr
        for (e in 0 until validEdgeCount) {
            deg[uUnique[e]] += 1
            deg[vUnique[e]] += 1
            edgeLenSum += lUnique[e].toDouble()
        }
        val globalAvgLen = (edgeLenSum / validEdgeCount.toDouble()).toFloat()

        // Build global CSR adjacency.
        val adjHead = IntArray(n + 1)
        for (i in 0 until n) {
            adjHead[i + 1] = adjHead[i] + deg[i]
        }
        val adjCursor = adjHead.copyOf()
        val adjTo = IntArray(validEdgeCount * 2)
        val adjLen = FloatArray(validEdgeCount * 2)
        for (e in 0 until validEdgeCount) {
            val u = uUnique[e]
            val v = vUnique[e]
            val len = lUnique[e]
            val pu = adjCursor[u]++
            adjTo[pu] = v
            adjLen[pu] = len
            val pv = adjCursor[v]++
            adjTo[pv] = u
            adjLen[pv] = len
        }

        // Stage 1: Extract connected components via BFS.
        val compId = IntArray(n) { -1 }
        val bfsQueue = IntArray(n)
        val compNodes = IntArray(n)
        var compNodesCursor = 0
        val compOffset = IntArray(n)
        val compSize = IntArray(n)
        var multiCount = 0

        val isoNodes = IntArray(n)
        var isoCount = 0

        for (root in 0 until n) {
            if (compId[root] != -1) continue
            if (adjHead[root] == adjHead[root + 1]) {
                compId[root] = -2
                isoNodes[isoCount++] = root
                outTargetX[root] = curX[root]
                outTargetY[root] = curY[root]
                continue
            }
            var qHead = 0
            var qTail = 0
            bfsQueue[qTail++] = root
            compId[root] = multiCount
            while (qHead < qTail) {
                val u = bfsQueue[qHead++]
                for (ei in adjHead[u] until adjHead[u + 1]) {
                    val v = adjTo[ei]
                    if (compId[v] == -1) {
                        compId[v] = multiCount
                        bfsQueue[qTail++] = v
                    }
                }
            }
            val startOff = compNodesCursor
            for (i in 0 until qTail) {
                compNodes[compNodesCursor++] = bfsQueue[i]
            }
            compOffset[multiCount] = startOff
            compSize[multiCount] = qTail
            multiCount += 1
        }

        if (multiCount == 0) return false

        // Stage 2-5 (per component): Solve local geometry & align to anchors/poses.
        val localOf = IntArray(n) { -1 }
        val compIsAnchored = BooleanArray(multiCount)
        val compUsedProcrustes = BooleanArray(multiCount)
        val compCenterX = FloatArray(multiCount)
        val compCenterY = FloatArray(multiCount)
        val compRadius = FloatArray(multiCount)

        var anyEstablishedOverall = false
        var minCurX = Float.POSITIVE_INFINITY
        var maxCurX = Float.NEGATIVE_INFINITY
        var minCurY = Float.POSITIVE_INFINITY
        var maxCurY = Float.NEGATIVE_INFINITY
        var sumCurX = 0.0
        var sumCurY = 0.0

        for (i in 0 until n) {
            val x = curX[i]
            val y = curY[i]
            sumCurX += x.toDouble()
            sumCurY += y.toDouble()
            if (x < minCurX) minCurX = x
            if (x > maxCurX) maxCurX = x
            if (y < minCurY) minCurY = y
            if (y > maxCurY) maxCurY = y
            if (established[i]) anyEstablishedOverall = true
        }

        val hasViewport = viewportWidth > 0f && viewportHeight > 0f
        val vpCx = if (hasViewport) viewportWidth * 0.5f else (sumCurX / n.toDouble()).toFloat()
        val vpCy = if (hasViewport) viewportHeight * 0.5f else (sumCurY / n.toDouble()).toFloat()

        for (c in 0 until multiCount) {
            val off = compOffset[c]
            val m = compSize[c]
            solveAndAlignComponent(
                c = c,
                m = m,
                compOffset = off,
                compNodes = compNodes,
                localOf = localOf,
                adjHead = adjHead,
                adjTo = adjTo,
                adjLen = adjLen,
                curX = curX,
                curY = curY,
                locked = locked,
                established = established,
                globalAvgLen = globalAvgLen,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
                vpCx = vpCx,
                vpCy = vpCy,
                hasViewport = hasViewport,
                outTargetX = outTargetX,
                outTargetY = outTargetY,
                compIsAnchored = compIsAnchored,
                compUsedProcrustes = compUsedProcrustes,
                compCenterX = compCenterX,
                compCenterY = compCenterY,
                compRadius = compRadius,
            )
            // Reset localOf for next component cleanly.
            for (li in 0 until m) {
                localOf[compNodes[off + li]] = -1
            }
        }

        // Pack components & isolated nodes around viewport without overlap.
        packComponentsAndIsolates(
            n = n,
            multiCount = multiCount,
            compOffset = compOffset,
            compSize = compSize,
            compNodes = compNodes,
            compIsAnchored = compIsAnchored,
            compUsedProcrustes = compUsedProcrustes,
            compCenterX = compCenterX,
            compCenterY = compCenterY,
            compRadius = compRadius,
            isoCount = isoCount,
            isoNodes = isoNodes,
            curX = curX,
            curY = curY,
            locked = locked,
            established = established,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            wallRepulsionPx = wallRepulsionPx,
            clampedScale = safeScale,
            vpCx = vpCx,
            vpCy = vpCy,
            hasViewport = hasViewport,
            anyEstablishedOverall = anyEstablishedOverall,
            outTargetX = outTargetX,
            outTargetY = outTargetY,
        )

        // Strict guarantee: locked nodes never receive a different guide target, and outputs stay finite.
        for (i in 0 until n) {
            if (locked[i]) {
                outTargetX[i] = curX[i]
                outTargetY[i] = curY[i]
            }
            if (!outTargetX[i].isFinite() || !outTargetY[i].isFinite()) {
                return false
            }
        }

        return true
    }

    private fun solveAndAlignComponent(
        c: Int,
        m: Int,
        compOffset: Int,
        compNodes: IntArray,
        localOf: IntArray,
        adjHead: IntArray,
        adjTo: IntArray,
        adjLen: FloatArray,
        curX: FloatArray,
        curY: FloatArray,
        locked: BooleanArray,
        established: BooleanArray,
        globalAvgLen: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        vpCx: Float,
        vpCy: Float,
        hasViewport: Boolean,
        outTargetX: FloatArray,
        outTargetY: FloatArray,
        compIsAnchored: BooleanArray,
        compUsedProcrustes: BooleanArray,
        compCenterX: FloatArray,
        compCenterY: FloatArray,
        compRadius: FloatArray,
    ) {
        for (li in 0 until m) {
            localOf[compNodes[compOffset + li]] = li
        }

        // Build component-local CSR adjacency.
        val lHead = IntArray(m + 1)
        var halfEdges = 0
        var compEdgeLenSum = 0f
        for (li in 0 until m) {
            val g = compNodes[compOffset + li]
            val degG = adjHead[g + 1] - adjHead[g]
            lHead[li + 1] = lHead[li] + degG
            halfEdges += degG
            for (ei in adjHead[g] until adjHead[g + 1]) {
                compEdgeLenSum += adjLen[ei]
            }
        }
        val avgLen = if (halfEdges > 0) compEdgeLenSum / halfEdges.toFloat() else globalAvgLen
        val lTo = IntArray(halfEdges)
        val lLen = FloatArray(halfEdges)
        val lCursor = lHead.copyOf()
        for (li in 0 until m) {
            val g = compNodes[compOffset + li]
            for (ei in adjHead[g] until adjHead[g + 1]) {
                val pos = lCursor[li]++
                lTo[pos] = localOf[adjTo[ei]]
                lLen[pos] = adjLen[ei]
            }
        }

        // Stage 2: Level-by-level degree-1 peeling down to the 2-core.
        val remDeg = IntArray(m) { li -> lHead[li + 1] - lHead[li] }
        val peeled = BooleanArray(m)
        val peelOrder = IntArray(m)
        val peelParent = IntArray(m) { -1 }
        val peelEdgeLen = FloatArray(m) { avgLen }
        var peelCount = 0

        var curLeaves = IntArray(m)
        var curLeafCount = 0
        var nextLeaves = IntArray(m)
        for (li in 0 until m) {
            if (remDeg[li] == 1) {
                curLeaves[curLeafCount++] = li
            }
        }

        var remaining = m
        while (curLeafCount > 0 && remaining > 2) {
            var nextLeafCount = 0
            val roundLimit = if (remaining - curLeafCount < 1) remaining - 1 else curLeafCount
            for (k in 0 until roundLimit) {
                if (remaining <= 1) break
                val u = curLeaves[k]
                if (peeled[u] || remDeg[u] != 1) continue
                var parent = -1
                var parentLen = avgLen
                for (ei in lHead[u] until lHead[u + 1]) {
                    val nb = lTo[ei]
                    if (!peeled[nb]) {
                        parent = nb
                        parentLen = lLen[ei]
                        break
                    }
                }
                if (parent == -1) continue
                peeled[u] = true
                remaining -= 1
                peelParent[u] = parent
                peelEdgeLen[u] = parentLen
                peelOrder[peelCount++] = u
                remDeg[parent] -= 1
                if (remDeg[parent] == 1) {
                    nextLeaves[nextLeafCount++] = parent
                }
            }
            val tmp = curLeaves
            curLeaves = nextLeaves
            nextLeaves = tmp
            curLeafCount = nextLeafCount
        }

        // Collect core vertices.
        val coreIdxOfLocal = IntArray(m) { -1 }
        val coreNodes = IntArray(m)
        var mCore = 0
        for (li in 0 until m) {
            if (!peeled[li]) {
                coreIdxOfLocal[li] = mCore
                coreNodes[mCore++] = li
            }
        }

        val posX = FloatArray(m)
        val posY = FloatArray(m)

        // Stage 3: Solve the core layout.
        if (mCore == 1) {
            val r = coreNodes[0]
            posX[r] = 0f
            posY[r] = 0f
        } else if (mCore == 2) {
            val u = coreNodes[0]
            val v = coreNodes[1]
            var uvLen = avgLen
            for (ei in lHead[u] until lHead[u + 1]) {
                if (lTo[ei] == v) {
                    uvLen = lLen[ei]
                    break
                }
            }
            posX[u] = -uvLen * 0.5f
            posY[u] = 0f
            posX[v] = uvLen * 0.5f
            posY[v] = 0f
        } else {
            solveGeneralOrCycleCore(
                mCore = mCore,
                coreNodes = coreNodes,
                coreIdxOfLocal = coreIdxOfLocal,
                lHead = lHead,
                lTo = lTo,
                lLen = lLen,
                avgLen = avgLen,
                posX = posX,
                posY = posY,
            )
        }

        // Stage 4: Radial subtree fan re-attachment.
        if (peelCount > 0) {
            reattachPeeledSubtrees(
                m = m,
                mCore = mCore,
                coreNodes = coreNodes,
                peeled = peeled,
                peelCount = peelCount,
                peelOrder = peelOrder,
                peelParent = peelParent,
                peelEdgeLen = peelEdgeLen,
                lHead = lHead,
                lTo = lTo,
                posX = posX,
                posY = posY,
            )
        }

        // Stage 5: Pose alignment against locked pins / established poses / viewport aspect ratio.
        var lockedCount = 0
        var estCount = 0
        var minLockX = Float.POSITIVE_INFINITY
        var maxLockX = Float.NEGATIVE_INFINITY
        var minLockY = Float.POSITIVE_INFINITY
        var maxLockY = Float.NEGATIVE_INFINITY
        var minEstX = Float.POSITIVE_INFINITY
        var maxEstX = Float.NEGATIVE_INFINITY
        var minEstY = Float.POSITIVE_INFINITY
        var maxEstY = Float.NEGATIVE_INFINITY
        var firstLockedLocal = -1

        val lockMask = BooleanArray(m)
        val estMask = BooleanArray(m)

        for (li in 0 until m) {
            val g = compNodes[compOffset + li]
            val gx = curX[g]
            val gy = curY[g]
            if (locked[g]) {
                lockMask[li] = true
                if (firstLockedLocal == -1) firstLockedLocal = li
                lockedCount += 1
                if (gx < minLockX) minLockX = gx
                if (gx > maxLockX) maxLockX = gx
                if (gy < minLockY) minLockY = gy
                if (gy > maxLockY) maxLockY = gy
            }
            if (established[g]) {
                estMask[li] = true
                estCount += 1
                if (gx < minEstX) minEstX = gx
                if (gx > maxEstX) maxEstX = gx
                if (gy < minEstY) minEstY = gy
                if (gy > maxEstY) maxEstY = gy
            }
        }

        val estSpread = if (estCount >= 2) max(maxEstX - minEstX, maxEstY - minEstY) else 0f
        val lockSpread = if (lockedCount >= 2) max(maxLockX - minLockX, maxLockY - minLockY) else 0f

        compIsAnchored[c] = lockedCount > 0
        var usedProcrustes = false

        when {
            lockedCount >= 2 -> {
                usedProcrustes = true
                if (lockSpread > 1e-3f) {
                    alignProcrustes(
                        m = m,
                        compOffset = compOffset,
                        compNodes = compNodes,
                        fitMask = lockMask,
                        fitCount = lockedCount,
                        tieBreakMask = if (estCount > lockedCount) estMask else null,
                        curX = curX,
                        curY = curY,
                        posX = posX,
                        posY = posY,
                    )
                } else {
                    // Coincident locked pins: keep locked centroid at exact pin position, preserve spread.
                    val gx = curX[compNodes[compOffset + firstLockedLocal]]
                    val gy = curY[compNodes[compOffset + firstLockedLocal]]
                    val dx = gx - posX[firstLockedLocal]
                    val dy = gy - posY[firstLockedLocal]
                    for (li in 0 until m) {
                        posX[li] += dx
                        posY[li] += dy
                    }
                }
                for (li in 0 until m) {
                    if (lockMask[li]) {
                        val g = compNodes[compOffset + li]
                        posX[li] = curX[g]
                        posY[li] = curY[g]
                    }
                }
                if (lockedCount < m) {
                    relaxConstrainedStress(
                        m = m,
                        lHead = lHead,
                        lTo = lTo,
                        lLen = lLen,
                        avgLen = avgLen,
                        fixedMask = lockMask,
                        posX = posX,
                        posY = posY,
                    )
                }
            }

            lockedCount == 1 -> {
                usedProcrustes = true
                val p0 = firstLockedLocal
                val gp0 = compNodes[compOffset + p0]
                val anchorX = curX[gp0]
                val anchorY = curY[gp0]
                if (estCount >= 2 && estSpread > EST_SPREAD_THRESHOLD_PX) {
                    alignAnchoredProcrustes(
                        m = m,
                        pivotLocal = p0,
                        anchorX = anchorX,
                        anchorY = anchorY,
                        compOffset = compOffset,
                        compNodes = compNodes,
                        fitMask = estMask,
                        curX = curX,
                        curY = curY,
                        posX = posX,
                        posY = posY,
                    )
                } else {
                    var sumX = 0f
                    var sumY = 0f
                    for (li in 0 until m) {
                        sumX += posX[li]
                        sumY += posY[li]
                    }
                    val cx = sumX / m.toFloat() - posX[p0]
                    val cy = sumY / m.toFloat() - posY[p0]
                    val cDist = hypot(cx, cy)
                    if (hasViewport && cDist > 1e-3f) {
                        val vx = vpCx - anchorX
                        val vy = vpCy - anchorY
                        if (hypot(vx, vy) > 1e-3f) {
                            val rot = atan2(vy.toDouble(), vx.toDouble()) - atan2(cy.toDouble(), cx.toDouble())
                            val cr = cos(rot).toFloat()
                            val sr = sin(rot).toFloat()
                            val px0 = posX[p0]
                            val py0 = posY[p0]
                            for (li in 0 until m) {
                                val rx = posX[li] - px0
                                val ry = posY[li] - py0
                                posX[li] = px0 + rx * cr - ry * sr
                                posY[li] = py0 + rx * sr + ry * cr
                            }
                        }
                    }
                    val shiftX = anchorX - posX[p0]
                    val shiftY = anchorY - posY[p0]
                    for (li in 0 until m) {
                        posX[li] += shiftX
                        posY[li] += shiftY
                    }
                }
            }

            estCount >= 2 && estSpread > EST_SPREAD_THRESHOLD_PX -> {
                usedProcrustes = true
                alignProcrustes(
                    m = m,
                    compOffset = compOffset,
                    compNodes = compNodes,
                    fitMask = estMask,
                    fitCount = estCount,
                    tieBreakMask = null,
                    curX = curX,
                    curY = curY,
                    posX = posX,
                    posY = posY,
                )
            }

            else -> {
                // Fresh mount: align principal axis with the viewport's longer dimension.
                var sumX = 0f
                var sumY = 0f
                for (li in 0 until m) {
                    sumX += posX[li]
                    sumY += posY[li]
                }
                val cx = sumX / m.toFloat()
                val cy = sumY / m.toFloat()
                var cxx = 0.0
                var cyy = 0.0
                var cxy = 0.0
                for (li in 0 until m) {
                    val dx = (posX[li] - cx).toDouble()
                    val dy = (posY[li] - cy).toDouble()
                    cxx += dx * dx
                    cyy += dy * dy
                    cxy += dx * dy
                }
                val principalAngle = 0.5 * atan2(2.0 * cxy, cxx - cyy)
                val targetAngle = if (!hasViewport || viewportWidth >= viewportHeight) 0.0 else PI * 0.5
                val rot = targetAngle - principalAngle
                val cr = cos(rot).toFloat()
                val sr = sin(rot).toFloat()
                for (li in 0 until m) {
                    val rx = posX[li] - cx
                    val ry = posY[li] - cy
                    posX[li] = rx * cr - ry * sr
                    posY[li] = rx * sr + ry * cr
                }
                // Center bounding box around (0, 0) ready for viewport packing.
                var bMinX = Float.POSITIVE_INFINITY
                var bMaxX = Float.NEGATIVE_INFINITY
                var bMinY = Float.POSITIVE_INFINITY
                var bMaxY = Float.NEGATIVE_INFINITY
                for (li in 0 until m) {
                    if (posX[li] < bMinX) bMinX = posX[li]
                    if (posX[li] > bMaxX) bMaxX = posX[li]
                    if (posY[li] < bMinY) bMinY = posY[li]
                    if (posY[li] > bMaxY) bMaxY = posY[li]
                }
                val boxCx = 0.5f * (bMinX + bMaxX)
                val boxCy = 0.5f * (bMinY + bMaxY)
                for (li in 0 until m) {
                    posX[li] = posX[li] - boxCx + vpCx
                    posY[li] = posY[li] - boxCy + vpCy
                }
            }
        }

        compUsedProcrustes[c] = usedProcrustes

        var bMinX = Float.POSITIVE_INFINITY
        var bMaxX = Float.NEGATIVE_INFINITY
        var bMinY = Float.POSITIVE_INFINITY
        var bMaxY = Float.NEGATIVE_INFINITY
        for (li in 0 until m) {
            val g = compNodes[compOffset + li]
            val x = posX[li]
            val y = posY[li]
            outTargetX[g] = x
            outTargetY[g] = y
            if (x < bMinX) bMinX = x
            if (x > bMaxX) bMaxX = x
            if (y < bMinY) bMinY = y
            if (y > bMaxY) bMaxY = y
        }
        val bcx = 0.5f * (bMinX + bMaxX)
        val bcy = 0.5f * (bMinY + bMaxY)
        var maxR = 0f
        for (li in 0 until m) {
            val r = hypot(posX[li] - bcx, posY[li] - bcy)
            if (r > maxR) maxR = r
        }
        compCenterX[c] = bcx
        compCenterY[c] = bcy
        compRadius[c] = maxR
    }

    private fun solveGeneralOrCycleCore(
        mCore: Int,
        coreNodes: IntArray,
        coreIdxOfLocal: IntArray,
        lHead: IntArray,
        lTo: IntArray,
        lLen: FloatArray,
        avgLen: Float,
        posX: FloatArray,
        posY: FloatArray,
    ) {
        // Build core CSR adjacency + unique core edge list.
        val cDeg = IntArray(mCore)
        var halfCoreEdges = 0
        for (ci in 0 until mCore) {
            val li = coreNodes[ci]
            var d = 0
            for (ei in lHead[li] until lHead[li + 1]) {
                if (coreIdxOfLocal[lTo[ei]] >= 0) d += 1
            }
            cDeg[ci] = d
            halfCoreEdges += d
        }
        val cHead = IntArray(mCore + 1)
        for (ci in 0 until mCore) {
            cHead[ci + 1] = cHead[ci] + cDeg[ci]
        }
        val cTo = IntArray(halfCoreEdges)
        val cLen = FloatArray(halfCoreEdges)
        val cCursor = cHead.copyOf()

        val maxUniqueCoreEdges = max(1, halfCoreEdges / 2)
        val ceU = IntArray(maxUniqueCoreEdges)
        val ceV = IntArray(maxUniqueCoreEdges)
        val ceL = FloatArray(maxUniqueCoreEdges)
        var eCore = 0
        var coreLenSum = 0f
        var allDegTwo = true

        for (ci in 0 until mCore) {
            if (cDeg[ci] != 2) allDegTwo = false
            val li = coreNodes[ci]
            for (ei in lHead[li] until lHead[li + 1]) {
                val cj = coreIdxOfLocal[lTo[ei]]
                if (cj >= 0) {
                    val pos = cCursor[ci]++
                    val len = lLen[ei]
                    cTo[pos] = cj
                    cLen[pos] = len
                    if (ci < cj && eCore < maxUniqueCoreEdges) {
                        ceU[eCore] = ci
                        ceV[eCore] = cj
                        ceL[eCore] = len
                        coreLenSum += len
                        eCore += 1
                    }
                }
            }
        }

        val cAvgLen = if (eCore > 0) coreLenSum / eCore.toFloat() else avgLen
        val cPosX = FloatArray(mCore)
        val cPosY = FloatArray(mCore)

        // Fast-path for simple cycles (`E_core == M_core` and every core node has degree 2).
        if (allDegTwo && eCore == mCore) {
            val order = IntArray(mCore)
            val edgeWalkLen = FloatArray(mCore)
            val visited = BooleanArray(mCore)
            var cur = 0
            var prev = -1
            var perimeter = 0f
            var walked = 0
            for (step in 0 until mCore) {
                order[step] = cur
                visited[cur] = true
                walked += 1
                var nextNode = -1
                var nextLen = cAvgLen
                for (ei in cHead[cur] until cHead[cur + 1]) {
                    val nb = cTo[ei]
                    if (nb != prev && (!visited[nb] || step == mCore - 1)) {
                        if (nextNode == -1 || nb < nextNode) {
                            nextNode = nb
                            nextLen = cLen[ei]
                        }
                    }
                }
                if (nextNode == -1) break
                edgeWalkLen[step] = nextLen
                perimeter += nextLen
                prev = cur
                cur = nextNode
            }

            if (walked == mCore && cur == 0) {
                val circumR = (perimeter / (2.0 * mCore * sin(PI / mCore))).toFloat()
                var theta = -PI * 0.5
                for (step in 0 until mCore) {
                    val v = order[step]
                    cPosX[v] = (circumR * cos(theta)).toFloat()
                    cPosY[v] = (circumR * sin(theta)).toFloat()
                    theta += (2.0 * PI) * (edgeWalkLen[step].toDouble() / max(1e-4, perimeter.toDouble()))
                }
                // Run 12 stress passes with polygon chord distances to adapt cleanly to varied edge lengths.
                val pairTable = SparsePairTable(mCore * 4)
                for (e in 0 until eCore) {
                    pairTable.putMaxWeight(ceU[e], ceV[e], ceL[e], 2.0f / (ceL[e] * ceL[e]))
                }
                val interiorAngleCos = -cos(2.0 * PI / mCore)
                for (step in 0 until mCore) {
                    val u = order[step]
                    val w = order[(step + 2) % mCore]
                    val l1 = edgeWalkLen[step].toDouble()
                    val l2 = edgeWalkLen[(step + 1) % mCore].toDouble()
                    val chord = sqrt(max(1e-4, l1 * l1 + l2 * l2 - 2.0 * l1 * l2 * interiorAngleCos)).toFloat()
                    pairTable.putMaxWeight(u, w, chord, 0.9f / (chord * chord))
                    val oppStep = (step + mCore / 2) % mCore
                    val opp = order[oppStep]
                    if (opp != u) {
                        val oppChord = max(cAvgLen, circumR * 2f * sin(PI * (mCore / 2) / mCore).toFloat())
                        pairTable.putMaxWeight(u, opp, oppChord, 0.6f / (oppChord * oppChord))
                    }
                }
                runStressIterations(mCore, pairTable, cPosX, cPosY, iterations = 12)
                for (ci in 0 until mCore) {
                    val li = coreNodes[ci]
                    posX[li] = cPosX[ci]
                    posY[li] = cPosY[ci]
                }
                return
            }
        }

        // General Core: Farthest-First Landmark Dijkstra + PivotMDS + Sparse Stress Majorization.
        val kPiv = min(mCore, MAX_PIVOTS)
        val pivots = IntArray(kPiv)
        val pivDists = FloatArray(kPiv * mCore)
        val minDistToPiv = FloatArray(mCore) { Float.POSITIVE_INFINITY }
        val heap = IndexedMinHeap(mCore)

        var curP = 0
        for (k in 0 until kPiv) {
            pivots[k] = curP
            val distOffset = k * mCore
            dijkstraIntoSlice(
                m = mCore,
                src = curP,
                cHead = cHead,
                cTo = cTo,
                cLen = cLen,
                heap = heap,
                outDist = pivDists,
                outOffset = distOffset,
            )
            var bestNext = 0
            var bestVal = -1f
            for (u in 0 until mCore) {
                val d = pivDists[distOffset + u]
                if (d < minDistToPiv[u]) {
                    minDistToPiv[u] = d
                }
                if (minDistToPiv[u].isFinite() && minDistToPiv[u] > bestVal) {
                    bestVal = minDistToPiv[u]
                    bestNext = u
                }
            }
            curP = bestNext
        }

        // PivotMDS double-centering & 2D eigenspace projection.
        val bMat = DoubleArray(mCore * kPiv)
        val rowMean = DoubleArray(mCore)
        val colMean = DoubleArray(kPiv)
        var grandSum = 0.0
        for (i in 0 until mCore) {
            var rSum = 0.0
            for (k in 0 until kPiv) {
                val d = pivDists[k * mCore + i].toDouble()
                val d2 = if (d.isFinite()) d * d else (cAvgLen * mCore).toDouble().let { it * it }
                bMat[i * kPiv + k] = d2
                rSum += d2
                colMean[k] += d2
            }
            rowMean[i] = rSum / kPiv.toDouble()
            grandSum += rowMean[i]
        }
        val invM = 1.0 / mCore.toDouble()
        for (k in 0 until kPiv) {
            colMean[k] *= invM
        }
        val grandMean = grandSum * invM
        for (i in 0 until mCore) {
            val rm = rowMean[i]
            val base = i * kPiv
            for (k in 0 until kPiv) {
                bMat[base + k] = -0.5 * (bMat[base + k] - rm - colMean[k] + grandMean)
            }
        }

        val matM = DoubleArray(kPiv * kPiv)
        for (k1 in 0 until kPiv) {
            for (k2 in k1 until kPiv) {
                var sum = 0.0
                for (i in 0 until mCore) {
                    sum += bMat[i * kPiv + k1] * bMat[i * kPiv + k2]
                }
                matM[k1 * kPiv + k2] = sum
                matM[k2 * kPiv + k1] = sum
            }
        }

        val eig1 = DoubleArray(kPiv) { k -> sin((k + 1) * 1.7 + 0.3) }
        val eig2 = DoubleArray(kPiv) { k -> cos((k + 1) * 2.3 + 0.7) }
        powerEigenPair(kPiv, matM, eig1, eig2)

        var normXSq = 0.0
        var normYSq = 0.0
        val rawX = DoubleArray(mCore)
        val rawY = DoubleArray(mCore)
        for (i in 0 until mCore) {
            var sx = 0.0
            var sy = 0.0
            val base = i * kPiv
            for (k in 0 until kPiv) {
                val bik = bMat[base + k]
                sx += bik * eig1[k]
                sy += bik * eig2[k]
            }
            rawX[i] = sx
            rawY[i] = sy
            normXSq += sx * sx
            normYSq += sy * sy
        }

        // Degenerate 1D core guard (e.g. collinear embedding).
        if (normYSq < 1e-8 * max(1.0, normXSq)) {
            val spreadScale = max(1.0, sqrt(normXSq / mCore.toDouble())) * 0.25
            for (i in 0 until mCore) {
                rawY[i] = sin((i + 1) * GOLDEN_ANGLE_DOUBLE) * spreadScale
            }
        }

        var euclidEdgeSum = 0.0
        for (e in 0 until eCore) {
            val u = ceU[e]
            val v = ceV[e]
            euclidEdgeSum += hypot(rawX[u] - rawX[v], rawY[u] - rawY[v])
        }
        val scale = if (euclidEdgeSum > 1e-6) (coreLenSum.toDouble() / euclidEdgeSum).toFloat() else 1f
        for (i in 0 until mCore) {
            cPosX[i] = rawX[i].toFloat() * scale
            cPosY[i] = rawY[i].toFloat() * scale
        }

        // Sparse Stress constraints: 1-hop core edges + bounded 2-hop wedges + K pivots.
        val estConstraints = eCore + mCore * 10 + kPiv * mCore
        val pairTable = SparsePairTable(max(16, estConstraints))

        for (e in 0 until eCore) {
            val len = ceL[e]
            pairTable.putMaxWeight(ceU[e], ceV[e], len, 2.0f / (len * len))
        }

        for (k in 0 until kPiv) {
            val p = pivots[k]
            val distBase = k * mCore
            for (u in 0 until mCore) {
                if (u == p) continue
                val d = pivDists[distBase + u]
                if (d.isFinite() && d > 1e-3f) {
                    val ideal = d * 0.88f
                    pairTable.putMaxWeight(p, u, ideal, 0.7f / (ideal * ideal))
                }
            }
        }

        // Bounded 2-hop wedges around each core vertex `h`.
        val sharedStamp = IntArray(mCore)
        var stampGen = 1
        for (h in 0 until mCore) {
            val start = cHead[h]
            val end = cHead[h + 1]
            val d = end - start
            if (d < 2) continue
            if (d <= HUB_DEGREE_EXACT_LIMIT) {
                for (a in start until end) {
                    val u = cTo[a]
                    val lu = cLen[a]
                    stampGen += 1
                    for (ue in cHead[u] until cHead[u + 1]) {
                        sharedStamp[cTo[ue]] = stampGen
                    }
                    for (b in a + 1 until end) {
                        val v = cTo[b]
                        if (u == v) continue
                        val lv = cLen[b]
                        var shared = 0
                        for (ve in cHead[v] until cHead[v + 1]) {
                            if (sharedStamp[cTo[ve]] == stampGen) {
                                shared += 1
                                if (shared >= 2) break
                            }
                        }
                        val ideal = max(cAvgLen * 0.35f, (lu + lv) * if (shared >= 2) 0.71f else 0.86f)
                        pairTable.putMaxWeight(u, v, ideal, 0.9f / (ideal * ideal))
                    }
                }
            } else {
                // High-degree hub: sort neighbors by PivotMDS angle around hub and connect cyclic strides.
                val hx = cPosX[h]
                val hy = cPosY[h]
                val order = IntArray(d) { idx -> start + idx }
                val angles = FloatArray(d) { idx ->
                    val nb = cTo[start + idx]
                    atan2((cPosY[nb] - hy).toDouble(), (cPosX[nb] - hx).toDouble()).toFloat()
                }
                sortIndicesByAngle(order, angles, d)
                val stride2 = max(3, d / 6)
                val stride3 = max(4, d / 2)
                for (a in 0 until d) {
                    val eiA = order[a]
                    val u = cTo[eiA]
                    val lu = cLen[eiA]
                    for (stepIdx in 0..3) {
                        val step = when (stepIdx) {
                            0 -> 1
                            1 -> 2
                            2 -> stride2
                            else -> stride3
                        }
                        val b = (a + step) % d
                        if (b == a) continue
                        val eiB = order[b]
                        val v = cTo[eiB]
                        if (u == v) continue
                        val lv = cLen[eiB]
                        val minStep = min(step, d - step)
                        val chord = max(cAvgLen * 0.25f, (lu + lv) * sin(PI * minStep / d.toDouble()).toFloat())
                        pairTable.putMaxWeight(u, v, chord, 0.9f / (chord * chord))
                    }
                }
            }
        }

        runStressIterations(mCore, pairTable, cPosX, cPosY, iterations = 35)

        for (ci in 0 until mCore) {
            val li = coreNodes[ci]
            posX[li] = cPosX[ci]
            posY[li] = cPosY[ci]
        }
    }

    private fun reattachPeeledSubtrees(
        m: Int,
        mCore: Int,
        coreNodes: IntArray,
        peeled: BooleanArray,
        peelCount: Int,
        peelOrder: IntArray,
        peelParent: IntArray,
        peelEdgeLen: FloatArray,
        lHead: IntArray,
        lTo: IntArray,
        posX: FloatArray,
        posY: FloatArray,
    ) {
        val firstChild = IntArray(m) { -1 }
        val nextSibling = IntArray(m) { -1 }
        val leafWeight = IntArray(m)

        for (idx in 0 until peelCount) {
            val u = peelOrder[idx]
            if (firstChild[u] == -1) {
                leafWeight[u] = 1
            }
            val p = peelParent[u]
            if (p >= 0) {
                nextSibling[u] = firstChild[p]
                firstChild[p] = u
                leafWeight[p] += leafWeight[u]
            }
        }

        var coreSumX = 0f
        var coreSumY = 0f
        for (ci in 0 until mCore) {
            val r = coreNodes[ci]
            coreSumX += posX[r]
            coreSumY += posY[r]
        }
        val coreCx = coreSumX / mCore.toFloat()
        val coreCy = coreSumY / mCore.toFloat()
        var coreMaxR = 0f
        for (ci in 0 until mCore) {
            val r = coreNodes[ci]
            val d = hypot(posX[r] - coreCx, posY[r] - coreCy)
            if (d > coreMaxR) coreMaxR = d
        }

        val incAngles = DoubleArray(m)
        for (ci in 0 until mCore) {
            val r = coreNodes[ci]
            if (firstChild[r] == -1) continue
            if (mCore == 1) {
                placeSubtreeSector(
                    root = r,
                    centerAngle = -PI * 0.5,
                    sectorSpan = 2.0 * PI,
                    firstChild = firstChild,
                    nextSibling = nextSibling,
                    leafWeight = leafWeight,
                    peelEdgeLen = peelEdgeLen,
                    posX = posX,
                    posY = posY,
                )
            } else {
                var coreDeg = 0
                val rx = posX[r]
                val ry = posY[r]
                for (ei in lHead[r] until lHead[r + 1]) {
                    val nb = lTo[ei]
                    if (!peeled[nb]) {
                        incAngles[coreDeg++] = atan2((posY[nb] - ry).toDouble(), (posX[nb] - rx).toDouble())
                    }
                }
                val rDist = hypot(rx - coreCx, ry - coreCy)
                val radialConfidence = if (coreMaxR > 1e-3f) {
                    (rDist / coreMaxR).coerceIn(0f, 1f).toDouble()
                } else {
                    0.0
                }
                val outAng = atan2((ry - coreCy).toDouble(), (rx - coreCx).toDouble())
                val bestMid: Double
                val bestSpan: Double
                if (coreDeg == 0) {
                    bestMid = outAng
                    bestSpan = PI * 1.2
                } else if (coreDeg == 1) {
                    bestMid = incAngles[0] + PI
                    bestSpan = PI * 0.92
                } else {
                    sortDoubles(incAngles, coreDeg)
                    var chosenMid = outAng
                    var chosenSpan = PI * 0.8
                    var bestScore = -1e18
                    for (idx in 0 until coreDeg) {
                        val a1 = incAngles[idx]
                        val a2 = incAngles[(idx + 1) % coreDeg]
                        var gap = a2 - a1
                        if (gap <= 0.0) gap += 2.0 * PI
                        val mid = a1 + gap * 0.5
                        val align = cos(mid - outAng)
                        val score = gap * (1.0 + 0.6 * radialConfidence * align)
                        if (score > bestScore) {
                            bestScore = score
                            chosenMid = mid
                            chosenSpan = min(gap * 0.78, PI * 1.2)
                        }
                    }
                    bestMid = chosenMid
                    bestSpan = max(0.35, chosenSpan)
                }
                placeSubtreeSector(
                    root = r,
                    centerAngle = bestMid,
                    sectorSpan = bestSpan,
                    firstChild = firstChild,
                    nextSibling = nextSibling,
                    leafWeight = leafWeight,
                    peelEdgeLen = peelEdgeLen,
                    posX = posX,
                    posY = posY,
                )
            }
        }
    }

    private fun placeSubtreeSector(
        root: Int,
        centerAngle: Double,
        sectorSpan: Double,
        firstChild: IntArray,
        nextSibling: IntArray,
        leafWeight: IntArray,
        peelEdgeLen: FloatArray,
        posX: FloatArray,
        posY: FloatArray,
    ) {
        if (firstChild[root] == -1) return
        var totalW = 0
        var c = firstChild[root]
        while (c != -1) {
            totalW += leafWeight[c]
            c = nextSibling[c]
        }
        if (totalW <= 0) return

        var curAng = centerAngle - sectorSpan * 0.5
        c = firstChild[root]
        while (c != -1) {
            val spanC = sectorSpan * (leafWeight[c].toDouble() / totalW.toDouble())
            val midAng = curAng + spanC * 0.5
            curAng += spanC
            val rStep = peelEdgeLen[c]
            posX[c] = posX[root] + cos(midAng).toFloat() * rStep
            posY[c] = posY[root] + sin(midAng).toFloat() * rStep
            if (firstChild[c] != -1) {
                placeSubtreeSector(
                    root = c,
                    centerAngle = midAng,
                    sectorSpan = min(spanC * 0.92, PI * 0.92),
                    firstChild = firstChild,
                    nextSibling = nextSibling,
                    leafWeight = leafWeight,
                    peelEdgeLen = peelEdgeLen,
                    posX = posX,
                    posY = posY,
                )
            }
            c = nextSibling[c]
        }
    }

    private fun alignProcrustes(
        m: Int,
        compOffset: Int,
        compNodes: IntArray,
        fitMask: BooleanArray,
        fitCount: Int,
        tieBreakMask: BooleanArray?,
        curX: FloatArray,
        curY: FloatArray,
        posX: FloatArray,
        posY: FloatArray,
    ) {
        var cxRef = 0.0
        var cyRef = 0.0
        var cxCur = 0.0
        var cyCur = 0.0
        for (li in 0 until m) {
            if (!fitMask[li]) continue
            val g = compNodes[compOffset + li]
            cxRef += posX[li]
            cyRef += posY[li]
            cxCur += curX[g]
            cyCur += curY[g]
        }
        val inv = 1.0 / fitCount.toDouble()
        cxRef *= inv
        cyRef *= inv
        cxCur *= inv
        cyCur *= inv

        var bestReflect = 1.0
        var bestTheta = 0.0
        var bestFitErr = Double.POSITIVE_INFINITY
        var bestTieErr = Double.POSITIVE_INFINITY

        for (reflectSign in 0..1) {
            val reflect = if (reflectSign == 0) 1.0 else -1.0
            var num = 0.0
            var den = 0.0
            for (li in 0 until m) {
                if (!fitMask[li]) continue
                val g = compNodes[compOffset + li]
                val rx = (posX[li] - cxRef) * reflect
                val ry = posY[li] - cyRef
                val qx = curX[g] - cxCur
                val qy = curY[g] - cyCur
                num += rx * qy - ry * qx
                den += rx * qx + ry * qy
            }
            val theta = atan2(num, den)
            val ct = cos(theta)
            val st = sin(theta)
            var fitErr = 0.0
            var tieErr = 0.0
            for (li in 0 until m) {
                val checkFit = fitMask[li]
                val checkTie = tieBreakMask != null && tieBreakMask[li]
                if (!checkFit && !checkTie) continue
                val g = compNodes[compOffset + li]
                val rx = (posX[li] - cxRef) * reflect
                val ry = posY[li] - cyRef
                val tx = cxCur + rx * ct - ry * st
                val ty = cyCur + rx * st + ry * ct
                val dx = tx - curX[g]
                val dy = ty - curY[g]
                val d2 = dx * dx + dy * dy
                if (checkFit) fitErr += d2
                if (checkTie) tieErr += d2
            }
            val better = fitErr + 1e-2 < bestFitErr ||
                (kotlin.math.abs(fitErr - bestFitErr) <= 1e-2 && tieErr < bestTieErr)
            if (better) {
                bestFitErr = fitErr
                bestTieErr = tieErr
                bestReflect = reflect
                bestTheta = theta
            }
        }

        val ct = cos(bestTheta).toFloat()
        val st = sin(bestTheta).toFloat()
        val rfl = bestReflect.toFloat()
        val crx = cxRef.toFloat()
        val cry = cyRef.toFloat()
        val cux = cxCur.toFloat()
        val cuy = cyCur.toFloat()
        for (li in 0 until m) {
            val rx = (posX[li] - crx) * rfl
            val ry = posY[li] - cry
            posX[li] = cux + rx * ct - ry * st
            posY[li] = cuy + rx * st + ry * ct
        }
    }

    private fun alignAnchoredProcrustes(
        m: Int,
        pivotLocal: Int,
        anchorX: Float,
        anchorY: Float,
        compOffset: Int,
        compNodes: IntArray,
        fitMask: BooleanArray,
        curX: FloatArray,
        curY: FloatArray,
        posX: FloatArray,
        posY: FloatArray,
    ) {
        val px0 = posX[pivotLocal].toDouble()
        val py0 = posY[pivotLocal].toDouble()
        val ax0 = anchorX.toDouble()
        val ay0 = anchorY.toDouble()

        var bestReflect = 1.0
        var bestTheta = 0.0
        var bestErr = Double.POSITIVE_INFINITY

        for (reflectSign in 0..1) {
            val reflect = if (reflectSign == 0) 1.0 else -1.0
            var num = 0.0
            var den = 0.0
            for (li in 0 until m) {
                if (!fitMask[li] || li == pivotLocal) continue
                val g = compNodes[compOffset + li]
                val rx = (posX[li] - px0) * reflect
                val ry = posY[li] - py0
                val qx = curX[g] - ax0
                val qy = curY[g] - ay0
                num += rx * qy - ry * qx
                den += rx * qx + ry * qy
            }
            val theta = atan2(num, den)
            val ct = cos(theta)
            val st = sin(theta)
            var err = 0.0
            for (li in 0 until m) {
                if (!fitMask[li] || li == pivotLocal) continue
                val g = compNodes[compOffset + li]
                val rx = (posX[li] - px0) * reflect
                val ry = posY[li] - py0
                val tx = ax0 + rx * ct - ry * st
                val ty = ay0 + rx * st + ry * ct
                val dx = tx - curX[g]
                val dy = ty - curY[g]
                err += dx * dx + dy * dy
            }
            if (err < bestErr) {
                bestErr = err
                bestReflect = reflect
                bestTheta = theta
            }
        }

        val ct = cos(bestTheta).toFloat()
        val st = sin(bestTheta).toFloat()
        val rfl = bestReflect.toFloat()
        val px0f = posX[pivotLocal]
        val py0f = posY[pivotLocal]
        for (li in 0 until m) {
            val rx = (posX[li] - px0f) * rfl
            val ry = posY[li] - py0f
            posX[li] = anchorX + rx * ct - ry * st
            posY[li] = anchorY + rx * st + ry * ct
        }
    }

    private fun relaxConstrainedStress(
        m: Int,
        lHead: IntArray,
        lTo: IntArray,
        lLen: FloatArray,
        avgLen: Float,
        fixedMask: BooleanArray,
        posX: FloatArray,
        posY: FloatArray,
    ) {
        val pairTable = SparsePairTable(max(16, (lHead[m] / 2) + m * 6))
        for (u in 0 until m) {
            for (ei in lHead[u] until lHead[u + 1]) {
                val v = lTo[ei]
                if (u < v) {
                    val len = lLen[ei]
                    pairTable.putMaxWeight(u, v, len, 2.0f / (len * len))
                }
            }
        }
        for (h in 0 until m) {
            val start = lHead[h]
            val end = lHead[h + 1]
            val d = end - start
            if (d in 2..HUB_DEGREE_EXACT_LIMIT) {
                for (a in start until end) {
                    val u = lTo[a]
                    val lu = lLen[a]
                    for (b in a + 1 until end) {
                        val v = lTo[b]
                        if (u == v) continue
                        val lv = lLen[b]
                        val ideal = max(avgLen * 0.35f, (lu + lv) * 0.85f)
                        pairTable.putMaxWeight(u, v, ideal, 0.8f / (ideal * ideal))
                    }
                }
            }
        }
        runStressIterations(m, pairTable, posX, posY, iterations = 15, fixedMask = fixedMask)
    }

    private fun packComponentsAndIsolates(
        n: Int,
        multiCount: Int,
        compOffset: IntArray,
        compSize: IntArray,
        compNodes: IntArray,
        compIsAnchored: BooleanArray,
        compUsedProcrustes: BooleanArray,
        compCenterX: FloatArray,
        compCenterY: FloatArray,
        compRadius: FloatArray,
        isoCount: Int,
        isoNodes: IntArray,
        curX: FloatArray,
        curY: FloatArray,
        locked: BooleanArray,
        established: BooleanArray,
        viewportWidth: Float,
        viewportHeight: Float,
        wallRepulsionPx: Float,
        clampedScale: Float,
        vpCx: Float,
        vpCy: Float,
        hasViewport: Boolean,
        anyEstablishedOverall: Boolean,
        outTargetX: FloatArray,
        outTargetY: FloatArray,
    ) {
        val margin = if (hasViewport) {
            if (wallRepulsionPx > 0f) {
                min(wallRepulsionPx * clampedScale * 0.7f, min(viewportWidth, viewportHeight) * 0.22f)
            } else {
                24f * clampedScale
            }
        } else {
            0f
        }

        var anchoredCount = 0
        var unlockedMultiCount = 0
        for (c in 0 until multiCount) {
            if (compIsAnchored[c]) anchoredCount += 1 else unlockedMultiCount += 1
        }
        var lockedIsoCount = 0
        for (i in 0 until isoCount) {
            if (locked[isoNodes[i]]) lockedIsoCount += 1
        }

        // Fast-path: single unlocked multi-node component and no isolated nodes.
        if (unlockedMultiCount == 1 && anchoredCount == 0 && isoCount == 0) {
            if (hasViewport && compUsedProcrustes[0]) {
                shiftComponentInsideMargin(
                    m = compSize[0],
                    off = compOffset[0],
                    compNodes = compNodes,
                    viewportWidth = viewportWidth,
                    viewportHeight = viewportHeight,
                    margin = margin,
                    vpCx = vpCx,
                    vpCy = vpCy,
                    outTargetX = outTargetX,
                    outTargetY = outTargetY,
                )
            }
            return
        }

        // General multi-component / isolated node packing via non-overlapping bounding discs.
        val discCountMax = multiCount + isoCount
        val discX = FloatArray(discCountMax)
        val discY = FloatArray(discCountMax)
        val discR = FloatArray(discCountMax)
        var discCount = 0

        val pad = 45f * clampedScale
        val isoRad = pad * 0.45f

        // 1. Place anchored components & locked isolated nodes first as immovable obstacles.
        for (c in 0 until multiCount) {
            if (compIsAnchored[c]) {
                discX[discCount] = compCenterX[c]
                discY[discCount] = compCenterY[c]
                discR[discCount] = compRadius[c]
                discCount += 1
            }
        }
        for (i in 0 until isoCount) {
            val g = isoNodes[i]
            if (locked[g]) {
                outTargetX[g] = curX[g]
                outTargetY[g] = curY[g]
                discX[discCount] = curX[g]
                discY[discCount] = curY[g]
                discR[discCount] = isoRad
                discCount += 1
            }
        }

        // 2. Pack unlocked components in descending node-count order.
        val unlockedOrder = IntArray(unlockedMultiCount)
        var uCursor = 0
        for (c in 0 until multiCount) {
            if (!compIsAnchored[c]) unlockedOrder[uCursor++] = c
        }
        sortComponentsBySizeDesc(unlockedOrder, unlockedMultiCount, compSize)

        var allFreshUnlocked = anchoredCount == 0 && lockedIsoCount == 0 && !anyEstablishedOverall
        for (idx in 0 until unlockedMultiCount) {
            val c = unlockedOrder[idx]
            if (compUsedProcrustes[c]) allFreshUnlocked = false
            val rad = compRadius[c]
            var cx = compCenterX[c]
            var cy = compCenterY[c]

            val initialClear = discCount == 0 || !overlapsAnyDisc(cx, cy, rad + pad, discX, discY, discR, discCount)
            if (!initialClear) {
                val startAng = (idx * GOLDEN_ANGLE_DOUBLE)
                val maxReach = computeMaxDiscReach(vpCx, vpCy, discX, discY, discR, discCount)
                val stepR = max(max(pad * 0.8f, 32f * clampedScale), (maxReach + rad + pad) / 14f)
                var placed = false
                for (attempt in 1..240) {
                    val ang = startAng + attempt * GOLDEN_ANGLE_DOUBLE
                    val dist = sqrt(attempt.toDouble()).toFloat() * stepR + rad * 0.5f
                    val tx = vpCx + cos(ang).toFloat() * dist
                    val ty = vpCy + sin(ang).toFloat() * dist
                    if (!overlapsAnyDisc(tx, ty, rad + pad, discX, discY, discR, discCount)) {
                        cx = tx
                        cy = ty
                        placed = true
                        break
                    }
                }
                if (!placed) {
                    cx = vpCx + cos(startAng).toFloat() * (maxReach + rad + pad * 2f)
                    cy = vpCy + sin(startAng).toFloat() * (maxReach + rad + pad * 2f)
                }
                val dx = cx - compCenterX[c]
                val dy = cy - compCenterY[c]
                val off = compOffset[c]
                val m = compSize[c]
                for (li in 0 until m) {
                    val g = compNodes[off + li]
                    outTargetX[g] += dx
                    outTargetY[g] += dy
                }
            }

            discX[discCount] = cx
            discY[discCount] = cy
            discR[discCount] = rad
            discCount += 1
        }

        // 3. Place unlocked isolated nodes without colliding with component discs or each other.
        for (i in 0 until isoCount) {
            val g = isoNodes[i]
            if (locked[g]) continue
            val gx = curX[g]
            val gy = curY[g]
            val insideMargins = !hasViewport || (
                gx >= margin && gx <= viewportWidth - margin &&
                    gy >= margin && gy <= viewportHeight - margin
                )
            val keepExisting = established[g] &&
                insideMargins &&
                !overlapsAnyDisc(gx, gy, isoRad + pad * 0.55f, discX, discY, discR, discCount)

            var tx = gx
            var ty = gy
            if (!keepExisting) {
                val baseAng = (i + 1) * GOLDEN_ANGLE_DOUBLE
                val maxReach = computeMaxDiscReach(vpCx, vpCy, discX, discY, discR, discCount)
                val stepR = max(max(pad * 0.7f, 28f * clampedScale), (maxReach + isoRad + pad) / 14f)
                var placed = false
                for (attempt in 1..240) {
                    val ang = baseAng + attempt * GOLDEN_ANGLE_DOUBLE
                    val dist = sqrt(attempt.toDouble()).toFloat() * stepR + isoRad
                    val cx = vpCx + cos(ang).toFloat() * dist
                    val cy = vpCy + sin(ang).toFloat() * dist
                    if (!overlapsAnyDisc(cx, cy, isoRad + pad * 0.55f, discX, discY, discR, discCount)) {
                        tx = cx
                        ty = cy
                        placed = true
                        break
                    }
                }
                if (!placed) {
                    tx = vpCx + cos(baseAng).toFloat() * (maxReach + isoRad + pad)
                    ty = vpCy + sin(baseAng).toFloat() * (maxReach + isoRad + pad)
                }
                outTargetX[g] = tx
                outTargetY[g] = ty
            }
            discX[discCount] = tx
            discY[discCount] = ty
            discR[discCount] = isoRad
            discCount += 1
        }

        // 4. Gently center / clamp the combined unlocked graph inside the viewport margins.
        if (anchoredCount == 0 && lockedIsoCount == 0 && hasViewport) {
            var gMinX = Float.POSITIVE_INFINITY
            var gMaxX = Float.NEGATIVE_INFINITY
            var gMinY = Float.POSITIVE_INFINITY
            var gMaxY = Float.NEGATIVE_INFINITY
            for (i in 0 until n) {
                val x = outTargetX[i]
                val y = outTargetY[i]
                if (x < gMinX) gMinX = x
                if (x > gMaxX) gMaxX = x
                if (y < gMinY) gMinY = y
                if (y > gMaxY) gMaxY = y
            }
            val shiftX: Float
            val shiftY: Float
            if (allFreshUnlocked) {
                shiftX = vpCx - 0.5f * (gMinX + gMaxX)
                shiftY = vpCy - 0.5f * (gMinY + gMaxY)
            } else {
                shiftX = computeAxisMarginShift(gMinX, gMaxX, margin, viewportWidth - margin, vpCx)
                shiftY = computeAxisMarginShift(gMinY, gMaxY, margin, viewportHeight - margin, vpCy)
            }
            if (shiftX != 0f || shiftY != 0f) {
                for (i in 0 until n) {
                    outTargetX[i] += shiftX
                    outTargetY[i] += shiftY
                }
            }
        }
    }

    private fun shiftComponentInsideMargin(
        m: Int,
        off: Int,
        compNodes: IntArray,
        viewportWidth: Float,
        viewportHeight: Float,
        margin: Float,
        vpCx: Float,
        vpCy: Float,
        outTargetX: FloatArray,
        outTargetY: FloatArray,
    ) {
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        for (li in 0 until m) {
            val g = compNodes[off + li]
            val x = outTargetX[g]
            val y = outTargetY[g]
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        val dx = computeAxisMarginShift(minX, maxX, margin, viewportWidth - margin, vpCx)
        val dy = computeAxisMarginShift(minY, maxY, margin, viewportHeight - margin, vpCy)
        if (dx != 0f || dy != 0f) {
            for (li in 0 until m) {
                val g = compNodes[off + li]
                outTargetX[g] += dx
                outTargetY[g] += dy
            }
        }
    }

    private fun computeAxisMarginShift(
        minVal: Float,
        maxVal: Float,
        boundMin: Float,
        boundMax: Float,
        vpCenter: Float,
    ): Float {
        if (boundMax <= boundMin) return vpCenter - 0.5f * (minVal + maxVal)
        val span = maxVal - minVal
        val innerSpan = boundMax - boundMin
        if (span > innerSpan) {
            return vpCenter - 0.5f * (minVal + maxVal)
        }
        if (minVal < boundMin) return boundMin - minVal
        if (maxVal > boundMax) return boundMax - maxVal
        return 0f
    }

    private fun computeMaxDiscReach(
        vpCx: Float,
        vpCy: Float,
        discX: FloatArray,
        discY: FloatArray,
        discR: FloatArray,
        discCount: Int,
    ): Float {
        var maxReach = 0f
        for (i in 0 until discCount) {
            val reach = hypot(discX[i] - vpCx, discY[i] - vpCy) + discR[i]
            if (reach > maxReach) maxReach = reach
        }
        return maxReach
    }

    private fun overlapsAnyDisc(
        cx: Float,
        cy: Float,
        rWithPad: Float,
        discX: FloatArray,
        discY: FloatArray,
        discR: FloatArray,
        discCount: Int,
    ): Boolean {
        for (i in 0 until discCount) {
            val minD = (rWithPad + discR[i]).toDouble()
            val dx = (cx - discX[i]).toDouble()
            val dy = (cy - discY[i]).toDouble()
            if (dx * dx + dy * dy < minD * minD) return true
        }
        return false
    }

    private fun runStressIterations(
        m: Int,
        pairTable: SparsePairTable,
        posX: FloatArray,
        posY: FloatArray,
        iterations: Int,
        fixedMask: BooleanArray? = null,
    ) {
        val cCount = pairTable.count
        if (cCount == 0) return
        val cU = pairTable.uArr
        val cV = pairTable.vArr
        val cIdeal = pairTable.idealArr
        val cW = pairTable.wArr

        val wSum = FloatArray(m)
        for (e in 0 until cCount) {
            val w = cW[e]
            wSum[cU[e]] += w
            wSum[cV[e]] += w
        }
        val invWSum = FloatArray(m) { i -> if (wSum[i] > 0f) 1f / wSum[i] else 0f }

        val sumX = FloatArray(m)
        val sumY = FloatArray(m)
        for (iter in 0 until iterations) {
            for (i in 0 until m) {
                sumX[i] = 0f
                sumY[i] = 0f
            }
            for (e in 0 until cCount) {
                val u = cU[e]
                val v = cV[e]
                val ideal = cIdeal[e]
                val w = cW[e]
                val px = posX[u]
                val py = posY[u]
                val qx = posX[v]
                val qy = posY[v]
                val dx = px - qx
                val dy = py - qy
                val dist = hypot(dx, dy)
                val offX: Float
                val offY: Float
                if (dist > 1e-4f) {
                    val ratio = ideal / dist
                    offX = dx * ratio
                    offY = dy * ratio
                } else {
                    val ang = u * GOLDEN_ANGLE_DOUBLE + v
                    offX = cos(ang).toFloat() * ideal
                    offY = sin(ang).toFloat() * ideal
                }
                sumX[u] += w * (qx + offX)
                sumY[u] += w * (qy + offY)
                sumX[v] += w * (px - offX)
                sumY[v] += w * (py - offY)
            }
            for (i in 0 until m) {
                if (fixedMask != null && fixedMask[i]) continue
                val inv = invWSum[i]
                if (inv > 0f) {
                    posX[i] = sumX[i] * inv
                    posY[i] = sumY[i] * inv
                }
            }
        }
    }

    private fun dijkstraIntoSlice(
        m: Int,
        src: Int,
        cHead: IntArray,
        cTo: IntArray,
        cLen: FloatArray,
        heap: IndexedMinHeap,
        outDist: FloatArray,
        outOffset: Int,
    ) {
        for (i in 0 until m) {
            outDist[outOffset + i] = Float.POSITIVE_INFINITY
        }
        outDist[outOffset + src] = 0f
        heap.reset()
        heap.pushOrDecrease(src, 0f)

        while (!heap.isEmpty()) {
            val u = heap.popMin()
            val du = outDist[outOffset + u]
            for (ei in cHead[u] until cHead[u + 1]) {
                val nb = cTo[ei]
                val nd = du + cLen[ei]
                if (nd < outDist[outOffset + nb]) {
                    outDist[outOffset + nb] = nd
                    heap.pushOrDecrease(nb, nd)
                }
            }
        }
    }

    private fun powerEigenPair(
        k: Int,
        mat: DoubleArray,
        v1: DoubleArray,
        v2: DoubleArray,
    ) {
        normalizeDouble(k, v1)
        val tmp = DoubleArray(k)
        for (iter in 0 until 60) {
            for (r in 0 until k) {
                var s = 0.0
                val rowOff = r * k
                for (c in 0 until k) {
                    s += mat[rowOff + c] * v1[c]
                }
                tmp[r] = s
            }
            val norm = normalizeDouble(k, tmp)
            if (norm < 1e-12) break
            for (i in 0 until k) v1[i] = tmp[i]
        }

        orthogonalizeDouble(k, v2, v1)
        for (iter in 0 until 60) {
            for (r in 0 until k) {
                var s = 0.0
                val rowOff = r * k
                for (c in 0 until k) {
                    s += mat[rowOff + c] * v2[c]
                }
                tmp[r] = s
            }
            orthogonalizeDouble(k, tmp, v1)
            val norm = normalizeDouble(k, tmp)
            if (norm < 1e-12) {
                // Pick deterministic orthogonal unit vector fallback if second eigenvalue vanishes.
                var bestAxis = 0
                var minAbs = Double.POSITIVE_INFINITY
                for (i in 0 until k) {
                    val a = kotlin.math.abs(v1[i])
                    if (a < minAbs) {
                        minAbs = a
                        bestAxis = i
                    }
                }
                for (i in 0 until k) v2[i] = if (i == bestAxis) 1.0 else 0.0
                orthogonalizeDouble(k, v2, v1)
                normalizeDouble(k, v2)
                break
            }
            for (i in 0 until k) v2[i] = tmp[i]
        }
    }

    private fun normalizeDouble(k: Int, v: DoubleArray): Double {
        var sq = 0.0
        for (i in 0 until k) sq += v[i] * v[i]
        val norm = sqrt(sq)
        if (norm > 1e-12) {
            val inv = 1.0 / norm
            for (i in 0 until k) v[i] *= inv
        }
        return norm
    }

    private fun orthogonalizeDouble(k: Int, v: DoubleArray, basis: DoubleArray) {
        var dot = 0.0
        for (i in 0 until k) dot += v[i] * basis[i]
        for (i in 0 until k) v[i] -= dot * basis[i]
    }

    private fun sortDoubles(arr: DoubleArray, len: Int) {
        for (i in 1 until len) {
            val key = arr[i]
            var j = i - 1
            while (j >= 0 && arr[j] > key) {
                arr[j + 1] = arr[j]
                j -= 1
            }
            arr[j + 1] = key
        }
    }

    private fun sortIndicesByAngle(order: IntArray, angles: FloatArray, len: Int) {
        for (i in 1 until len) {
            val keyOrd = order[i]
            val keyAng = angles[i]
            var j = i - 1
            while (j >= 0 && angles[j] > keyAng) {
                order[j + 1] = order[j]
                angles[j + 1] = angles[j]
                j -= 1
            }
            order[j + 1] = keyOrd
            angles[j + 1] = keyAng
        }
    }

    private fun sortComponentsBySizeDesc(order: IntArray, len: Int, compSize: IntArray) {
        for (i in 1 until len) {
            val key = order[i]
            val keySize = compSize[key]
            var j = i - 1
            while (j >= 0 && compSize[order[j]] < keySize) {
                order[j + 1] = order[j]
                j -= 1
            }
            order[j + 1] = key
        }
    }

    /**
     * Zero-boxing open-addressed deduplicating table of undirected `(u, v)` stress pairs.
     */
    private class SparsePairTable(initialExpectedCount: Int) {
        private val safeExpected = initialExpectedCount.coerceIn(16, 1 shl 28)
        private var cap = 32
        private var keys: LongArray
        private var slotIdx: IntArray

        var count: Int = 0
            private set
        var uArr = IntArray(safeExpected)
            private set
        var vArr = IntArray(safeExpected)
            private set
        var idealArr = FloatArray(safeExpected)
            private set
        var wArr = FloatArray(safeExpected)
            private set

        init {
            val targetCap = max(32, safeExpected * 2)
            while (cap < targetCap) cap = cap shl 1
            keys = LongArray(cap) { -1L }
            slotIdx = IntArray(cap) { -1 }
        }

        fun putMaxWeight(rawU: Int, rawV: Int, ideal: Float, weight: Float) {
            if (rawU == rawV || ideal <= 1e-4f || weight <= 0f) return
            val u: Int
            val v: Int
            if (rawU < rawV) {
                u = rawU
                v = rawV
            } else {
                u = rawV
                v = rawU
            }
            val key = (u.toLong() shl 32) or (v.toLong() and 0xFFFF_FFFFL)
            if ((count + 1) * 2 >= cap) rehash()

            val mask = cap - 1
            var pos = mixKey(key) and mask
            while (true) {
                val existingKey = keys[pos]
                if (existingKey == -1L) {
                    val idx = count
                    ensureEntryCapacity(idx + 1)
                    uArr[idx] = u
                    vArr[idx] = v
                    idealArr[idx] = ideal
                    wArr[idx] = weight
                    keys[pos] = key
                    slotIdx[pos] = idx
                    count = idx + 1
                    return
                }
                if (existingKey == key) {
                    val idx = slotIdx[pos]
                    if (weight > wArr[idx]) {
                        idealArr[idx] = ideal
                        wArr[idx] = weight
                    }
                    return
                }
                pos = (pos + 1) and mask
            }
        }

        private fun ensureEntryCapacity(needed: Int) {
            if (needed <= uArr.size) return
            val next = max(needed, uArr.size * 2)
            uArr = uArr.copyOf(next)
            vArr = vArr.copyOf(next)
            idealArr = idealArr.copyOf(next)
            wArr = wArr.copyOf(next)
        }

        private fun rehash() {
            val oldCap = cap
            val oldKeys = keys
            val oldSlots = slotIdx
            cap = oldCap shl 1
            keys = LongArray(cap) { -1L }
            slotIdx = IntArray(cap) { -1 }
            val mask = cap - 1
            for (i in 0 until oldCap) {
                val k = oldKeys[i]
                if (k != -1L) {
                    var pos = mixKey(k) and mask
                    while (keys[pos] != -1L) {
                        pos = (pos + 1) and mask
                    }
                    keys[pos] = k
                    slotIdx[pos] = oldSlots[i]
                }
            }
        }

        private fun mixKey(key: Long): Int {
            var z = key xor (key ushr 33)
            z *= -0xae502812aa7333L
            z = z xor (z ushr 33)
            return z.toInt()
        }
    }

    /**
     * Compact `O(V)` indexed binary min-heap supporting decrease-key without duplicate entries.
     */
    private class IndexedMinHeap(capacity: Int) {
        private val heapNodes = IntArray(capacity)
        private val heapDists = FloatArray(capacity)
        private val posInHeap = IntArray(capacity) { -1 }
        private var size = 0

        fun reset() {
            for (i in 0 until size) {
                posInHeap[heapNodes[i]] = -1
            }
            size = 0
        }

        fun isEmpty(): Boolean = size == 0

        fun pushOrDecrease(node: Int, dist: Float) {
            val existingPos = posInHeap[node]
            if (existingPos == -1) {
                val idx = size++
                heapNodes[idx] = node
                heapDists[idx] = dist
                posInHeap[node] = idx
                siftUp(idx)
            } else if (dist < heapDists[existingPos]) {
                heapDists[existingPos] = dist
                siftUp(existingPos)
            }
        }

        fun popMin(): Int {
            val minNode = heapNodes[0]
            posInHeap[minNode] = -1
            size -= 1
            if (size > 0) {
                val lastNode = heapNodes[size]
                val lastDist = heapDists[size]
                heapNodes[0] = lastNode
                heapDists[0] = lastDist
                posInHeap[lastNode] = 0
                siftDown(0)
            }
            return minNode
        }

        private fun siftUp(startIdx: Int) {
            var idx = startIdx
            val node = heapNodes[idx]
            val dist = heapDists[idx]
            while (idx > 0) {
                val parent = (idx - 1) ushr 1
                if (dist >= heapDists[parent]) break
                val pNode = heapNodes[parent]
                heapNodes[idx] = pNode
                heapDists[idx] = heapDists[parent]
                posInHeap[pNode] = idx
                idx = parent
            }
            heapNodes[idx] = node
            heapDists[idx] = dist
            posInHeap[node] = idx
        }

        private fun siftDown(startIdx: Int) {
            var idx = startIdx
            val node = heapNodes[idx]
            val dist = heapDists[idx]
            val half = size ushr 1
            while (idx < half) {
                var child = (idx shl 1) + 1
                var childDist = heapDists[child]
                val right = child + 1
                if (right < size && heapDists[right] < childDist) {
                    child = right
                    childDist = heapDists[right]
                }
                if (dist <= childDist) break
                val cNode = heapNodes[child]
                heapNodes[idx] = cNode
                heapDists[idx] = childDist
                posInHeap[cNode] = idx
                idx = child
            }
            heapNodes[idx] = node
            heapDists[idx] = dist
            posInHeap[node] = idx
        }
    }

    private companion object {
        private const val MAX_PIVOTS: Int = 32
        private const val HUB_DEGREE_EXACT_LIMIT: Int = 12
        private const val EST_SPREAD_THRESHOLD_PX: Float = 12.0f
        private const val GOLDEN_ANGLE_DOUBLE: Double = 2.399963229728653
    }
}
