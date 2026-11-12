package com.cinpe.deponder.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Zero-allocation, flat-array Hybrid Quadtree supporting simultaneous:
 * 1. Exact AABB range culling for arbitrary per-node finite repulsion fields (`R_i > 0`, zero approximation error).
 * 2. Barnes-Hut center-of-mass acceleration (`O(N log N)`) for unbounded [DeponderDefaults.INFINITE_RANGE] (`R_i < 0`) fields.
 *
 * Once [build] finishes on the current frame's read-only coordinate snapshot, [queryForceOnNode] is
 * 100% read-only and lock-free across concurrent worker coroutines.
 */
internal class HybridQuadtree(initialNodeCap: Int = 128) {

    private var nodeCap = max(16, initialNodeCap)
    private var nextInLeaf = IntArray(nodeCap)

    private var boxCap = max(64, nodeCap * 2)
    private var boxCx = FloatArray(boxCap)
    private var boxCy = FloatArray(boxCap)
    private var boxHalf = FloatArray(boxCap)
    private var boxMaxFiniteR = FloatArray(boxCap)
    private var boxInfChargeSum = FloatArray(boxCap)
    private var boxInfAbsChargeSum = FloatArray(boxCap)
    private var boxInfComX = FloatArray(boxCap)
    private var boxInfComY = FloatArray(boxCap)
    private var boxTotalCount = IntArray(boxCap)
    private var boxCenterSumX = FloatArray(boxCap)
    private var boxCenterSumY = FloatArray(boxCap)
    private var boxChildren = IntArray(boxCap * 4)
    private var boxLeafHead = IntArray(boxCap)
    private var boxIsInternal = BooleanArray(boxCap)

    var allocatedBoxes: Int = 0
        private set

    private fun ensureNodeCapacity(n: Int) {
        if (n <= nodeCap) return
        nodeCap = max(n, nodeCap * 2)
        nextInLeaf = IntArray(nodeCap)
    }

    private fun allocBox(cx: Float, cy: Float, half: Float): Int {
        val idx = allocatedBoxes
        if (idx >= boxCap) {
            val newCap = boxCap * 2
            boxCx = boxCx.copyOf(newCap)
            boxCy = boxCy.copyOf(newCap)
            boxHalf = boxHalf.copyOf(newCap)
            boxMaxFiniteR = boxMaxFiniteR.copyOf(newCap)
            boxInfChargeSum = boxInfChargeSum.copyOf(newCap)
            boxInfAbsChargeSum = boxInfAbsChargeSum.copyOf(newCap)
            boxInfComX = boxInfComX.copyOf(newCap)
            boxInfComY = boxInfComY.copyOf(newCap)
            boxTotalCount = boxTotalCount.copyOf(newCap)
            boxCenterSumX = boxCenterSumX.copyOf(newCap)
            boxCenterSumY = boxCenterSumY.copyOf(newCap)
            boxChildren = boxChildren.copyOf(newCap * 4)
            boxLeafHead = boxLeafHead.copyOf(newCap)
            boxIsInternal = boxIsInternal.copyOf(newCap)
            boxCap = newCap
        }
        allocatedBoxes = idx + 1
        boxCx[idx] = cx
        boxCy[idx] = cy
        boxHalf[idx] = half
        boxMaxFiniteR[idx] = 0f
        boxInfChargeSum[idx] = 0f
        boxInfAbsChargeSum[idx] = 0f
        boxInfComX[idx] = cx
        boxInfComY[idx] = cy
        boxTotalCount[idx] = 0
        boxCenterSumX[idx] = 0f
        boxCenterSumY[idx] = 0f
        val base = idx shl 2
        boxChildren[base] = -1
        boxChildren[base + 1] = -1
        boxChildren[base + 2] = -1
        boxChildren[base + 3] = -1
        boxLeafHead[idx] = -1
        boxIsInternal[idx] = false
        return idx
    }

    fun build(
        count: Int,
        px: FloatArray,
        py: FloatArray,
        effectiveRadius: FloatArray,
        elasticity: FloatArray,
        infiniteChargeStrength: Float,
    ) {
        allocatedBoxes = 0
        if (count <= 0) return
        ensureNodeCapacity(count)

        var minX = px[0]
        var maxX = px[0]
        var minY = py[0]
        var maxY = py[0]
        for (i in 1 until count) {
            val x = px[i]
            val y = py[i]
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        val span = max(max(maxX - minX, maxY - minY), 1f)
        val rootHalf = span * 0.5f + 1f
        val rootCx = (minX + maxX) * 0.5f
        val rootCy = (minY + maxY) * 0.5f

        allocBox(rootCx, rootCy, rootHalf)

        for (i in 0 until count) {
            nextInLeaf[i] = -1
            insertIterative(i, px, py)
        }

        // Bottom-up reverse sweep: parent indices are strictly smaller than child indices.
        for (b in allocatedBoxes - 1 downTo 0) {
            if (!boxIsInternal[b]) {
                var p = boxLeafHead[b]
                var maxFinR = 0f
                var infSum = 0f
                var infAbsSum = 0f
                var weightedInfX = 0f
                var weightedInfY = 0f
                var cnt = 0
                var sumX = 0f
                var sumY = 0f
                while (p != -1) {
                    val x = px[p]
                    val y = py[p]
                    val r = effectiveRadius[p]
                    if (r < 0f) {
                        val q = elasticity[p] * infiniteChargeStrength
                        val absQ = abs(q)
                        infSum += q
                        infAbsSum += absQ
                        weightedInfX += absQ * x
                        weightedInfY += absQ * y
                    } else if (r > maxFinR) {
                        maxFinR = r
                    }
                    cnt += 1
                    sumX += x
                    sumY += y
                    p = nextInLeaf[p]
                }
                boxMaxFiniteR[b] = maxFinR
                boxInfChargeSum[b] = infSum
                boxInfAbsChargeSum[b] = infAbsSum
                if (infAbsSum > 0f) {
                    boxInfComX[b] = weightedInfX / infAbsSum
                    boxInfComY[b] = weightedInfY / infAbsSum
                }
                boxTotalCount[b] = cnt
                boxCenterSumX[b] = sumX
                boxCenterSumY[b] = sumY
            } else {
                val base = b shl 2
                var maxFinR = 0f
                var infSum = 0f
                var infAbsSum = 0f
                var weightedInfX = 0f
                var weightedInfY = 0f
                var cnt = 0
                var sumX = 0f
                var sumY = 0f
                for (q in 0..3) {
                    val c = boxChildren[base + q]
                    if (c == -1) continue
                    val cFin = boxMaxFiniteR[c]
                    if (cFin > maxFinR) maxFinR = cFin
                    val cAbs = boxInfAbsChargeSum[c]
                    infSum += boxInfChargeSum[c]
                    infAbsSum += cAbs
                    weightedInfX += cAbs * boxInfComX[c]
                    weightedInfY += cAbs * boxInfComY[c]
                    cnt += boxTotalCount[c]
                    sumX += boxCenterSumX[c]
                    sumY += boxCenterSumY[c]
                }
                boxMaxFiniteR[b] = maxFinR
                boxInfChargeSum[b] = infSum
                boxInfAbsChargeSum[b] = infAbsSum
                if (infAbsSum > 0f) {
                    boxInfComX[b] = weightedInfX / infAbsSum
                    boxInfComY[b] = weightedInfY / infAbsSum
                }
                boxTotalCount[b] = cnt
                boxCenterSumX[b] = sumX
                boxCenterSumY[b] = sumY
            }
        }
    }

    private fun insertIterative(nodeIdx: Int, px: FloatArray, py: FloatArray) {
        val x = px[nodeIdx]
        val y = py[nodeIdx]
        var cur = 0
        var depth = 0

        while (true) {
            if (!boxIsInternal[cur]) {
                val existing = boxLeafHead[cur]
                if (existing == -1) {
                    boxLeafHead[cur] = nodeIdx
                    return
                }
                if (depth >= MAX_DEPTH || (px[existing] == x && py[existing] == y)) {
                    nextInLeaf[nodeIdx] = existing
                    boxLeafHead[cur] = nodeIdx
                    return
                }
                // Subdivide leaf into 4 quadrants and re-insert existing chain.
                boxIsInternal[cur] = true
                boxLeafHead[cur] = -1
                var p = existing
                while (p != -1) {
                    val next = nextInLeaf[p]
                    nextInLeaf[p] = -1
                    val q = quadrantOf(cur, px[p], py[p])
                    val child = getOrCreateChild(cur, q)
                    nextInLeaf[p] = boxLeafHead[child]
                    boxLeafHead[child] = p
                    p = next
                }
            }
            val q = quadrantOf(cur, x, y)
            cur = getOrCreateChild(cur, q)
            depth += 1
        }
    }

    private fun quadrantOf(box: Int, x: Float, y: Float): Int =
        (if (x >= boxCx[box]) 1 else 0) or (if (y >= boxCy[box]) 2 else 0)

    private fun getOrCreateChild(parent: Int, quadrant: Int): Int {
        val slot = (parent shl 2) + quadrant
        val existing = boxChildren[slot]
        if (existing != -1) return existing
        val childHalf = boxHalf[parent] * 0.5f
        val cx = boxCx[parent] + if ((quadrant and 1) != 0) childHalf else -childHalf
        val cy = boxCy[parent] + if ((quadrant and 2) != 0) childHalf else -childHalf
        val created = allocBox(cx, cy, childHalf)
        // Re-compute slot on boxChildren in case allocBox grew the array.
        boxChildren[(parent shl 2) + quadrant] = created
        return created
    }

    /**
     * Evaluates total pairwise planet-field force on `s` using [stack] (minimum length `64`).
     */
    fun queryForceOnNode(
        s: Int,
        px: FloatArray,
        py: FloatArray,
        effectiveRadius: FloatArray,
        elasticity: FloatArray,
        infiniteChargeStrength: Float,
        thetaSq: Float,
        softeningSq: Float,
        stack: IntArray,
    ): Vec2 {
        if (allocatedBoxes == 0) return Vec2.Zero

        val sx = px[s]
        val sy = py[s]
        val sR = effectiveRadius[s]
        val sEc = elasticity[s]
        val sIsInf = sR < 0f
        val sInfCharge = if (sIsInf) sEc * infiniteChargeStrength else 0f

        var fx = 0f
        var fy = 0f
        var sp = 0
        stack[sp++] = 0

        while (sp > 0) {
            val b = stack[--sp]
            val cnt = boxTotalCount[b]
            if (cnt == 0) continue

            val half = boxHalf[b]
            val dxBox = max(0f, abs(sx - boxCx[b]) - half)
            val dyBox = max(0f, abs(sy - boxCy[b]) - half)
            val minDistSqToBox = dxBox * dxBox + dyBox * dyBox

            val maxFinR = boxMaxFiniteR[b]
            val hasInfInBox = boxInfAbsChargeSum[b] > 0f
            val maxReach = max(if (sR > 0f) sR else 0f, maxFinR)
            val finiteMightInteract = maxReach > 0f && minDistSqToBox < maxReach * maxReach

            if (!finiteMightInteract && !hasInfInBox && !sIsInf) {
                continue
            }

            // Check if Barnes-Hut approximation can resolve all infinite interactions with this box at once
            // when no finite fields can reach this box and `s` lies outside this box (`minDistSqToBox > 0f`).
            if (!finiteMightInteract && minDistSqToBox > 0f && thetaSq > 0f) {
                val width = half * 2f
                val meanX = boxCenterSumX[b] / cnt
                val meanY = boxCenterSumY[b] / cnt
                val cdx = meanX - sx
                val cdy = meanY - sy
                val cDistSq = cdx * cdx + cdy * cdy
                if (cDistSq > 0f && (width * width) < thetaSq * cDistSq) {
                    if (hasInfInBox) {
                        val infDx = boxInfComX[b] - sx
                        val infDy = boxInfComY[b] - sy
                        val infDistSq = infDx * infDx + infDy * infDy
                        if (infDistSq > 0f) {
                            val invDist = 1f / sqrt(infDistSq)
                            val mag = boxInfChargeSum[b] / (infDistSq + softeningSq)
                            fx -= infDx * invDist * mag
                            fy -= infDy * invDist * mag
                        }
                    }
                    if (sIsInf) {
                        val invDist = 1f / sqrt(cDistSq)
                        val mag = (cnt * sInfCharge) / (cDistSq + softeningSq)
                        fx -= cdx * invDist * mag
                        fy -= cdy * invDist * mag
                    }
                    continue
                }
            }

            if (!boxIsInternal[b]) {
                var e = boxLeafHead[b]
                while (e != -1) {
                    if (e != s) {
                        val pairF = pairwiseForceOnS(
                            sx = sx,
                            sy = sy,
                            sR = sR,
                            sEc = sEc,
                            sInfCharge = sInfCharge,
                            ex = px[e],
                            ey = py[e],
                            eR = effectiveRadius[e],
                            eEc = elasticity[e],
                            infiniteChargeStrength = infiniteChargeStrength,
                            softeningSq = softeningSq,
                        )
                        fx += pairF.x
                        fy += pairF.y
                    }
                    e = nextInLeaf[e]
                }
            } else {
                val base = b shl 2
                for (q in 0..3) {
                    val child = boxChildren[base + q]
                    if (child != -1 && boxTotalCount[child] > 0) {
                        stack[sp++] = child
                    }
                }
            }
        }

        return Vec2(fx, fy)
    }

    companion object {
        private const val MAX_DEPTH: Int = 14
        const val REQUIRED_STACK_SIZE: Int = (MAX_DEPTH + 2) * 4

        /**
         * Computes the exact force exerted ON `s` by the independent radiation fields of `s` and `e`.
         */
        internal fun pairwiseForceOnS(
            sx: Float,
            sy: Float,
            sR: Float,
            sEc: Float,
            sInfCharge: Float,
            ex: Float,
            ey: Float,
            eR: Float,
            eEc: Float,
            infiniteChargeStrength: Float,
            softeningSq: Float,
        ): Vec2 {
            val diffX = ex - sx
            val diffY = ey - sy
            val distSq = diffX * diffX + diffY * diffY
            if (distSq == 0f) return Vec2.Zero

            var dist = -1f
            var repulsionMag = 0f

            // 1. Field of `s` (active if infinite or `dist < sR`).
            if (sR < 0f) {
                repulsionMag += sInfCharge / (distSq + softeningSq)
            } else if (sR > 0f && distSq < sR * sR) {
                dist = sqrt(distSq)
                repulsionMag += (sR - dist) * sEc
            }

            // 2. Field of `e` (active if infinite or `dist < eR`).
            if (eR < 0f) {
                repulsionMag += (eEc * infiniteChargeStrength) / (distSq + softeningSq)
            } else if (eR > 0f && distSq < eR * eR) {
                if (dist < 0f) dist = sqrt(distSq)
                repulsionMag += (eR - dist) * eEc
            }

            if (repulsionMag == 0f) return Vec2.Zero
            if (dist < 0f) dist = sqrt(distSq)
            val invDist = 1f / dist
            return Vec2(
                x = -diffX * invDist * repulsionMag,
                y = -diffY * invDist * repulsionMag,
            )
        }
    }
}
