package com.example.app

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.cinpe.deponder.copy
import com.cinpe.deponder.simpleOption
import com.cinpe.deponder.simplePlanet
import com.cinpe.deponder.simpleRoot
import com.cinpe.deponder.simpleRubber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KotlinModelDslTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun dsl_buildsModelsAndCopies() {
        val tv = TextView(context)
        val planet = simplePlanet {
            itemView = tv
            id = "p1"
            quality = 4.5f
            mInternalPressure = 180f
            elasticityCoefficient = 2.0f
        }
        assertSame(tv, planet.itemView())
        assertEquals("p1", planet.id())
        assertEquals(4.5f, planet.quality(), 0f)

        val heavier = planet.copy {
            quality = 9.0f
        }
        assertEquals(9.0f, heavier.quality(), 0f)
        assertEquals("p1", heavier.id())

        val line = View(context)
        val rubber = simpleRubber {
            itemView = line
            sId = "p1"
            eId = "p2"
            naturalLength = 250
        }
        assertEquals("p1p2", rubber.id())
        assertEquals(250, rubber.naturalLength())

        val longer = rubber.copy { naturalLength = 400 }
        assertEquals(400, longer.naturalLength())

        val frame = FrameLayout(context)
        val root = simpleRoot {
            itemView = frame
            initScale = 0.8f
            minInterval = -10L
        }
        assertEquals(0.8f, root.initScale(), 0f)
        assertEquals(0L, root.minInterval())

        val opt = simpleOption { itemView = tv }
        assertSame(tv, opt.itemView())
    }
}
