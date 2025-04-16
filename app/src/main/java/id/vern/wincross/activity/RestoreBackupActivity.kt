package id.vern.wincross.activity

import android.content.DialogInterface
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.*
import androidx.recyclerview.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import id.vern.wincross.R
import id.vern.wincross.databinding.*
import id.vern.wincross.helpers.UtilityHelper
import id.vern.wincross.operations.RestoreOperation
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import id.vern.wincross.managers.*
import id.vern.wincross.utils.*
import android.content.SharedPreferences
import android.content.Context

class RestoreBackupActivity : AppCompatActivity() {
  private lateinit var binding: ActivityRestoreBackupBinding
  private lateinit var adapter: PartitionAdapter
  private lateinit var sharedPreferences: SharedPreferences

  // Replace ProgressDialog with AlertDialog and custom progress view
  private lateinit var progressDialog: AlertDialog
  private lateinit var progressView: View
  private lateinit var progressBar: LinearProgressIndicator
  private lateinit var progressText: TextView

  private val backupDir by lazy {
    File("${Environment.getExternalStorageDirectory().path}/WINCross/Backup")
  }

  private val partitions = mutableListOf<PartitionItem>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    sharedPreferences = getSharedPreferences(ThemeManager.PREFS_NAME, Context.MODE_PRIVATE)
    ThemeManager(this).initializeTheme(this)

    binding = ActivityRestoreBackupBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setupToolbar()

    adapter =
    PartitionAdapter(partitions) {
      position, isChecked ->
      partitions[position].isSelected = isChecked
      updateButtonStates()
    }

    binding.recyclerViewPartitions.apply {
      layoutManager = LinearLayoutManager(this@RestoreBackupActivity)
      addItemDecoration(
        DividerItemDecoration(this@RestoreBackupActivity, DividerItemDecoration.VERTICAL))
      adapter = this@RestoreBackupActivity.adapter
    }

    binding.btnRestore.setOnClickListener {
      showRestoreConfirmation()
    }

    binding.checkboxSelectAll.setOnCheckedChangeListener {
      _, isChecked ->
      selectAllPartitions(isChecked)
    }

    // Setup the progress dialog with a custom view
    setupProgressDialog()

    loadBackupPartitions()
  }

  private fun setupProgressDialog() {
    // Inflate custom progress view
    progressView = layoutInflater.inflate(R.layout.dialog_progress, null)
    progressBar = progressView.findViewById(R.id.progressBar)
    progressText = progressView.findViewById(R.id.progressText)

    // Create the progress dialog
    progressDialog = MaterialAlertDialogBuilder(this)
    .setTitle("Restoring Partitions")
    .setView(progressView)
    .setCancelable(false)
    .setNegativeButton("Cancel") {
      _, _ ->
      RestoreOperation.cancelAllRestoreOperations()
      UtilityHelper.removeBlurBackground(binding.root)
    }
    .create()
  }

  private fun setupToolbar() {
    binding.toolbarlayout.toolbar.apply {
      title = getString(R.string.restore_title)
      setSupportActionBar(this)
      setNavigationIcon(R.drawable.ic_restore)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      finish()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  private fun loadBackupPartitions() {
    partitions.clear()

    if (!backupDir.exists() || !backupDir.isDirectory) {
      UtilityHelper.showToast(this, "Backup directory not found")
      return
    }

    val backupFiles = backupDir.listFiles {
      file -> file.name.endsWith(".img")
    }
    if (backupFiles == null || backupFiles.isEmpty()) {
      UtilityHelper.showToast(this, "No backup files found")
      return
    }

    backupFiles.forEach {
      file ->
      val partitionName = file.name.removeSuffix(".img")

      if (partitionName == "persist") {
        return@forEach
      }

      val lastModified =
      SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
      .format(Date(file.lastModified()))
      val fileSize = formatFileSize(file.length())

      partitions.add(
        PartitionItem(
          name = partitionName,
          filePath = file.absolutePath,
          date = lastModified,
          size = fileSize,
          isSelected = false))
    }

    partitions.sortBy {
      it.name
    }

    adapter.notifyDataSetChanged()
    updateButtonStates()
  }

  private fun formatFileSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0

    return when {
      mb >= 1.0 -> String.format("%.2f MB", mb)
      kb >= 1.0 -> String.format("%.2f KB", kb)
      else -> "$size bytes"
    }
  }

  private fun selectAllPartitions(select: Boolean) {
    partitions.forEach {
      it.isSelected = select
    }
    adapter.notifyDataSetChanged()
    updateButtonStates()
  }

  private fun updateButtonStates() {
    val anySelected = partitions.any {
      it.isSelected
    }
    val allSelected = partitions.all {
      it.isSelected
    } && partitions.isNotEmpty()
    binding.btnRestore.isEnabled = anySelected
    binding.checkboxSelectAll.isChecked = allSelected
  }

  private fun showRestoreConfirmation() {
    val selectedPartitions = partitions.filter {
      it.isSelected
    }
    if (selectedPartitions.isEmpty()) {
      return
    }

    val partitionNames = selectedPartitions.joinToString("\n") {
      "• ${it.name}"
    }

    AlertDialog.Builder(this)
    .setTitle("Confirm Restore")
    .setMessage(
      "Are you sure you want to restore the following partitions?\n\n$partitionNames\n\nThis process can't be undone.")
    .setPositiveButton("Restore") {
      _, _ -> startRestoreProcess(selectedPartitions)
    }
    .setNegativeButton("Cancel", null)
    .show()
  }

  private fun startRestoreProcess(selectedPartitions: List<PartitionItem>) {
    RestoreOperation.setRestoreProgressListener(
      object : RestoreOperation.RestoreProgressListener {
        override fun onRestoreStarted(totalPartitions: Int) {
          runOnUiThread {
            UtilityHelper.showBlurBackground(binding.root)
            // Update progress bar max value
            progressBar.max = totalPartitions * 100
            progressBar.progress = 0
            progressText.text = "Preparing..."
            progressDialog.show()
          }
        }

        override fun onPartitionRestoreStarted(partitionName: String, index: Int, total: Int) {
          runOnUiThread {
            // Update progress text
            progressText.text = "Restoring: $partitionName ($index/$total)"
          }
        }

        override fun onPartitionRestoreCompleted(partitionName: String, success: Boolean) {
          val status = if (success) "successful" else "failed"
          runOnUiThread {
            progressText.text = "$partitionName: $status"
            // Increment progress by 1 partition (100 units per partition)
            progressBar.progress += 100
          }
        }

        override fun onAllRestoresCompleted() {
          runOnUiThread {
            progressDialog.dismiss()
            UtilityHelper.removeBlurBackground(binding.root)
            UtilityHelper.showToast(this@RestoreBackupActivity, "Restore process completed")
          }
        }
      })

    val partitionNames = selectedPartitions.map {
      it.name
    }
    RestoreOperation.restoreAll(this, partitionNames)
  }

  data class PartitionItem(
    val name: String,
    val filePath: String,
    val date: String,
    val size: String,
    var isSelected: Boolean
  )

  inner class PartitionAdapter(
    private val items: List<PartitionItem>,
    private val onItemCheckChanged: (position: Int, isChecked: Boolean) -> Unit
  ) : RecyclerView.Adapter<PartitionAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPartitionBinding) :
    RecyclerView.ViewHolder(binding.root) {
      init {
        binding.root.setOnClickListener {
          val position = adapterPosition
          if (position != RecyclerView.NO_POSITION) {
            binding.checkboxPartition.isChecked = !binding.checkboxPartition.isChecked
          }
        }

        binding.checkboxPartition.setOnCheckedChangeListener {
          _, isChecked ->
          val position = adapterPosition
          if (position != RecyclerView.NO_POSITION) {
            onItemCheckChanged(position, isChecked)
          }
        }
      }

      fun bind(item: PartitionItem) {
        binding.checkboxPartition.isChecked = item.isSelected
        binding.textPartitionName.text = item.name
        binding.textPartitionInfo.text = "${item.date} • ${item.size}"
      }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val binding = ItemPartitionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
      return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      holder.bind(items[position])
    }

    override fun getItemCount() = items.size
  }
}