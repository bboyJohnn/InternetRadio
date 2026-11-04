package com.tohn95.internetradio.ui.screens.favorites

import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverCropTest {
    @Test fun square_image_no_zoom_is_full() {
        assertEquals(IntRect(0, 0, 1000, 1000), coverCrop(1000, 1000, 300f, 1f, 0f, 0f))
    }

    @Test fun wide_panorama_crops_center_square() {
        assertEquals(IntRect(3750, 0, 4250, 500), coverCrop(8000, 500, 300f, 1f, 0f, 0f))
    }

    @Test fun tall_screenshot_crops_center() {
        assertEquals(IntRect(0, 660, 1080, 1740), coverCrop(1080, 2400, 300f, 1f, 0f, 0f))
    }

    @Test fun zoom_shrinks_crop() {
        assertEquals(IntRect(250, 250, 750, 750), coverCrop(1000, 1000, 300f, 2f, 0f, 0f))
    }

    @Test fun offset_clamped_to_image() {
        val crop = coverCrop(8000, 500, 300f, 2f, 100000f, -100000f)
        assertTrue(crop.left >= 0 && crop.top >= 0)
        assertTrue(crop.right <= 8000 && crop.bottom <= 500)
        assertEquals(0, crop.left)
        assertEquals(250, crop.top)
        assertEquals(250, crop.width)
        assertEquals(250, crop.height)
    }
}
