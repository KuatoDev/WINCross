package id.vern.wincross.utils

import android.content.Context
import android.graphics.*
import android.view.View
import androidx.core.graphics.applyCanvas
import kotlin.math.roundToInt

object BlurUtil {

  fun blur(@Suppress("UNUSED_PARAMETER") context: Context?, image: Bitmap, radius: Float): Bitmap {
    val width = image.width
    val height = image.height
    val scaleFactor = 1f
    val scaledWidth = (width / scaleFactor).roundToInt()
    val scaledHeight = (height / scaleFactor).roundToInt()

    var bitmap = Bitmap.createScaledBitmap(image, scaledWidth, scaledHeight, true)

    bitmap = applyMaskFilter(bitmap, radius)
    if (scaleFactor > 1) {
      bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    return bitmap
  }

  private fun applyMaskFilter(bitmap: Bitmap, radius: Float): Bitmap {
    val r = radius.coerceIn(1f, 25f)
    val outputBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)

    val paint =
        Paint().apply {
          isAntiAlias = true
          maskFilter =
              android.graphics.BlurMaskFilter(r, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }

    outputBitmap.applyCanvas { drawBitmap(bitmap, 0f, 0f, paint) }

    return outputBitmap
  }

  fun blurView(view: View, radius: Float): Bitmap {
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return blur(view.context, bitmap, radius)
  }

  fun fastBlur(bitmap: Bitmap, radius: Float): Bitmap {
    if (radius <= 0) return bitmap

    val r = radius.coerceIn(1f, 25f).roundToInt()
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)

    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val w = width
    val h = height
    val wm = w - 1
    val div = r + r + 1

    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (y in 0 until h) {
      var sum = 0
      for (x in 0 until r) {
        sum += pixels[y * w]
      }

      for (x in 0 until w) {
        if (x > r) {
          sum -= pixels[y * w + x - r - 1]
        }
        if (x < wm - r) {
          sum += pixels[y * w + x + r]
        }

        if (x >= r && x <= wm - r) {
          pixels[y * w + x] = sum / div
        }
      }
    }

    result.setPixels(pixels, 0, width, 0, 0, width, height)
    return result
  }
}
