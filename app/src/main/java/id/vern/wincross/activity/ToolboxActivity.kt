package id.vern.wincross.activity

import android.content.*
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.*
import androidx.lifecycle.lifecycleScope
import id.vern.wincross.R
import id.vern.wincross.databinding.ActivityToolboxBinding
import id.vern.wincross.helpers.*
import id.vern.wincross.managers.*
import id.vern.wincross.operations.*
import id.vern.wincross.utils.*
import kotlinx.coroutines.*

class ToolboxActivity : AppCompatActivity() {
  private lateinit var binding: ActivityToolboxBinding
  private lateinit var sharedPreferences: SharedPreferences

  companion object {
    private const val TAG = "ToolboxActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    sharedPreferences = getSharedPreferences(ThemeManager.PREFS_NAME, Context.MODE_PRIVATE)
    ThemeManager(this).initializeTheme(this)
    binding = ActivityToolboxBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setupToolbar()
    binding.cvSTA.setOnClickListener {
      DialogHelper.showSTACreator(this, window.decorView.rootView) {
        lifecycleScope.launch { CreateSTA.RunSTAAsync(this@ToolboxActivity) }
      }
    }

    binding.cvARMSoftware.setOnClickListener {
      DialogHelper.showARMSoftware(this, window.decorView.rootView) {
        lifecycleScope.launch(Dispatchers.Main) {
          ARMSoftware.extractARMSoftware(this@ToolboxActivity)
        }
      }
    }

    binding.cvRotationToggle.setOnClickListener {
      DialogHelper.showRotationToggle(this, window.decorView.rootView) {
        lifecycleScope.launch(Dispatchers.Main) {
          RotationToggle.extractScript(this@ToolboxActivity)
        }
      }
    }
    binding.cvDownloadFramework.setOnClickListener {
      DialogHelper.showDownloadFrameworks(this, window.decorView.rootView) {
        lifecycleScope.launch(Dispatchers.Main) {
          FrameworkDownloader.downloadFrameworks(this@ToolboxActivity)
        }
      }
    }
    binding.cvReviAtlas.setOnClickListener {
      DialogHelper.showReviAtlas(this, window.decorView.rootView)
    }

    binding.cvDownloadUsbHostMode.setOnClickListener {
      DialogHelper.showUsbHostmode(this, window.decorView.rootView) {
        lifecycleScope.launch(Dispatchers.Main) {
          UsbHostmodeDownloader.downloadUsbHostmode(this@ToolboxActivity)
        }
      }
    }

    binding.cvDownloadTaskbarControl?.setOnClickListener {
      DialogHelper.showTaskbarControl(this, window.decorView.rootView) {
        lifecycleScope.launch(Dispatchers.Main) {
          TaskbarDownloader.downloadTaskbarControl(this@ToolboxActivity)
        }
      }
    }
    binding.cvDownloadBootAutoflasher.setOnClickListener {
      DialogHelper.showBootAutoflasher(this, window.decorView.rootView) {
        lifecycleScope.launch(Dispatchers.Main) {
          BootAutoflasherDownloader.downloadBootAutoflasher(this@ToolboxActivity)
        }
      }
    }
  }

  private fun setupToolbar() {
    binding.toolbarlayout.toolbar.apply {
      title = getString(R.string.toolbox_title)
      setSupportActionBar(this)
      setNavigationIcon(R.drawable.ic_toolbox)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }
    return super.onOptionsItemSelected(item)
  }
}
