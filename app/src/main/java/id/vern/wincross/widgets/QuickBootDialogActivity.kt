package id.vern.wincross.widgets

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import id.vern.wincross.R
import id.vern.wincross.operations.*
import id.vern.wincross.helpers.*
import kotlinx.coroutines.*

class QuickBootDialogActivity : Activity() {
  private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    MaterialAlertDialogBuilder(this)
    .setTitle(getString(R.string.switch_to_windows))
    .setMessage(getString(R.string.msg_flash_uefi))
    .setPositiveButton(getString(R.string.yes)) {
      _, _ ->
      coroutineScope.launch {
        FlashOperation.flashIt(this@QuickBootDialogActivity)
        finish()
      }
    }
    .setNegativeButton(getString(R.string.cancel)) {
      _, _ ->
      finish()
    }
    .setOnCancelListener {
      finish()
    }
    .show()
  }

  override fun onDestroy() {
    coroutineScope.cancel()
    super.onDestroy()
  }
}