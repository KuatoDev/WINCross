package id.vern.wincross.operations

import android.content.Context
import android.os.*
import android.app.*
import android.util.Log
import androidx.core.app.NotificationCompat
import id.vern.wincross.R
import id.vern.wincross.helpers.*
import id.vern.wincross.utils.*
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/**
 * Object responsible for patching Android kernel
 * to support dual booting with Windows (WINCross).
 * 
 * Its main functionality is to modify the boot image to enable dual boot capabilities.
 */
object DualbootKernelPatcher {
  // Unique ID for the notification channel
  private const val NOTIFICATION_CHANNEL_ID = "dbkp_patch_channel"
  // ID for the patching process notification
  private const val PATCH_NOTIFICATION_ID = 1002
  // ID for the download process notification
  private const val DOWNLOAD_NOTIFICATION_ID = 1003
  // Buffer size for I/O operations (8KB)
  private const val BUFFER_SIZE = 8192
  // Flag to mark if notification channel has been created
  private var notificationChannelCreated = false

  /**
   * Data class to store file locations used in the patching process
   * 
   * @param dbkpDir Main directory for DBKP process
   * @param originalBootImg Path for the original boot image backup
   * @param patchedBootImg Path for the patched boot image
   * @param bootImg Path for the boot image being processed
   * @param kernelFile Path for the extracted kernel file
   * @param newBootImg Path for the new boot image after repacking
   */
  private data class PatchPaths(
    val dbkpDir: String = "${Environment.getExternalStorageDirectory().path}/WINCross/DBKP",
    val originalBootImg: String = "${Environment.getExternalStorageDirectory().path}/WINCross/DBKP/original-boot.img",
    val patchedBootImg: String = "${Environment.getExternalStorageDirectory().path}/WINCross/DBKP/patched-boot.img",
    val bootImg: String = "${Environment.getExternalStorageDirectory().path}/WINCross/DBKP/boot.img",
    val kernelFile: String = "${Environment.getExternalStorageDirectory().path}/WINCross/DBKP/kernel",
    val newBootImg: String = "${Environment.getExternalStorageDirectory().path}/WINCross/DBKP/new-boot.img"
  )

  /**
   * Validates and prepares the directory structure needed for patching
   * Creates the main DBKP directory after cleaning any existing one
   * 
   * @param paths PatchPaths object containing paths to validate
   * @return Boolean indicating success or failure
   */
  private fun validateDirectories(paths: PatchPaths): Boolean {
    try {
      val dbkpDir = File(paths.dbkpDir)

      // Clean up existing directory if present
      if (dbkpDir.exists()) {
        dbkpDir.deleteRecursively()
      }

      // Create a fresh directory
      if (!dbkpDir.mkdirs()) {
        Log.e("PatchDBKP", "Failed to create directory: ${paths.dbkpDir}")
        return false
      }
      return true
    } catch (e: Exception) {
      Log.e("PatchDBKP", "Error validating directories: ${e.message}", e)
      return false
    }
  }

  /**
   * Main function to patch the kernel for dual boot capabilities
   * Handles the entire patching workflow with progress notifications
   * 
   * @param context Application context
   * @param filesDir Directory containing required binary files
   * @param deviceModel Device model string to determine patching approach
   */
  fun patchKernel(context: Context, filesDir: String, deviceModel: String) {
    Log.d("PatchDBKP", "Starting kernel patching for device: $deviceModel")
    createNotificationChannel(context)

    // Set up notification to show progress
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
    .setContentTitle("DBKP Patching")
    .setContentText("Starting patching process...")
    .setSmallIcon(android.R.drawable.stat_sys_download)
    .setPriority(NotificationCompat.PRIORITY_LOW)
    .setOngoing(true)
    .setAutoCancel(false)
    .setProgress(100, 0, true)

    notificationManager.notify(PATCH_NOTIFICATION_ID, notificationBuilder.build())

    // Launch patching in background thread using coroutines
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val paths = PatchPaths()

        // Step 1: Validate directories
        if (!validateDirectories(paths)) {
          throw Exception("Failed to create required directories")
        }

        // Step 2: Backup boot image
        updateNotification(context, notificationBuilder, "Creating directories and backing up boot image", 10)
        backupBootImage(paths)

        // Step 3: Copy configuration files
        updateNotification(context, notificationBuilder, "Copying configuration files", 20)
        File("$filesDir/dbkp8150.cfg").copyTo(File("${paths.dbkpDir}/dbkp8150.cfg"), overwrite = true)

        // Step 4: Download DBKP tools
        updateNotification(context, notificationBuilder, "Downloading DBKP tools", 30)
        val dbkpDownloaded = downloadFile(
          context,
          "https://github.com/n00b69/woa-op7/releases/download/DBKP/dbkp",
          paths.dbkpDir,
          "dbkp",
          notificationBuilder
        )

        if (!dbkpDownloaded) {
          throw Exception("Failed to download DBKP tool")
        }

        // Copy DBKP tool to app's files directory and set executable permissions
        File("${paths.dbkpDir}/dbkp").copyTo(File("$filesDir/dbkp"), overwrite = true)
        Utils.executeShellCommand("chmod 777 $filesDir/dbkp")

        // Step 5: Patch kernel based on device model
        updateNotification(context, notificationBuilder, "Patching kernel for $deviceModel", 40)

        var success = true
        var resultMsg: String

        // Device-specific patching based on model
        when {
          // OnePlus 7 Pro models
          deviceModel == "guacamole" || deviceModel == "OnePlus7Pro" || deviceModel == "OnePlus7Pro4G" -> {
            patchDeviceKernel(
              context,
              notificationBuilder,
              filesDir = filesDir,
              binFile = "dbkp.hotdog.bin",
              fdFile = "guacamole.fd",
              fdUrl = "https://github.com/n00b69/woa-op7/releases/download/DBKP/guacamole.fd",
              bootPartitions = arrayOf("boot_a", "boot_b"),
              paths = paths
            )
            resultMsg = "op7"
          }
          // OnePlus 7T Pro models
          deviceModel == "hotdog" || deviceModel == "OnePlus7TPro" || deviceModel == "OnePlus7TPro4G" -> {
            patchDeviceKernel(
              context,
              notificationBuilder,
              filesDir = filesDir,
              binFile = "dbkp.hotdog.bin",
              fdFile = "hotdog.fd",
              fdUrl = "https://github.com/n00b69/woa-op7/releases/download/DBKP/hotdog.fd",
              bootPartitions = arrayOf("boot_a", "boot_b"),
              paths = paths
            )
            resultMsg = "op7"
          }
          // Xiaomi Mi 9 (cepheus)
          deviceModel == "cepheus" -> {
            patchDeviceKernel(
              context,
              notificationBuilder,
              filesDir = filesDir,
              binFile = "dbkp.cepheus.bin",
              fdFile = "cepheus.fd",
              fdUrl = "https://github.com/n00b69/woa-everything/releases/download/Files/cepheus.fd",
              bootPartitions = arrayOf("boot"),
              paths = paths
            )
            resultMsg = "cepheus"
          }
          // Xiaomi Pad 5 (nabu)
          deviceModel == "nabu" -> {
            patchDeviceKernel(
              context,
              notificationBuilder,
              filesDir = filesDir,
              binFile = "dbkp.nabu.bin",
              fdFile = "nabu.fd",
              fdUrl = "https://github.com/erdilS/Port-Windows-11-Xiaomi-Pad-5/releases/download/1.0/nabu.fd",
              bootPartitions = arrayOf("boot_a", "boot_b"),
              paths = paths
            )
            resultMsg = "nabu"
          }
          // Xiaomi Pad 6 (pipa)
          deviceModel == "pipa" -> {
            patchDeviceKernel(
              context,
              notificationBuilder,
              filesDir = filesDir,
              binFile = "dbkp.pipa.bin",
              fdFile = "pipa.fd",
              fdUrl = "https://github.com/n00b69/woa-everything/releases/download/Files/pipa.fd",
              bootPartitions = arrayOf("boot_a", "boot_b"),
              paths = paths
            )
            resultMsg = "nabu"
          } else -> {
            // Unsupported device
            success = false
            resultMsg = "unsupported"
          }
        }

        // Step 6: Show completion notification
        withContext(Dispatchers.Main) {
          if (success) {
            // Success notification
            val completedNotification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Kernel Patching Complete")
            .setContentText("Kernel patched successfully")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

            notificationManager.notify(PATCH_NOTIFICATION_ID, completedNotification)
            DialogHelper.showPopupNotifications(context, context.getString(R.string.dbkp_success))
          } else {
            // Error notification for unsupported device
            val errorNotification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Kernel Patching Failed")
            .setContentText("Failed to patch kernel: $resultMsg")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

            notificationManager.notify(PATCH_NOTIFICATION_ID, errorNotification)
            DialogHelper.showPopupNotifications(context, context.getString(R.string.patch_error))
          }
        }
      } catch (e: Exception) {
        // Handle any exceptions during the patching process
        Log.e("PatchDBKP", "Error during patching: ${e.message}", e)
        withContext(Dispatchers.Main) {
          val errorNotification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
          .setContentTitle("Kernel Patching Failed")
          .setContentText("Error: ${e.message}")
          .setSmallIcon(android.R.drawable.stat_notify_error)
          .setPriority(NotificationCompat.PRIORITY_LOW)
          .setOngoing(false)
          .setAutoCancel(true)
          .build()

          notificationManager.notify(PATCH_NOTIFICATION_ID, errorNotification)
          DialogHelper.showPopupNotifications(context, context.getString(R.string.patch_error))
        }
      }
    }
  }

  /**
   * Creates a notification channel for Android O and above
   * Sets up a silent notification channel with low importance
   * 
   * @param context Application context
   */
  private fun createNotificationChannel(context: Context) {
    if (!notificationChannelCreated) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Kernel Patching"
        val descriptionText = "Shows kernel patching progress with minimal interruption"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
          description = descriptionText
          enableVibration(false)
          enableLights(false)
          setSound(null, null)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d("PatchDBKP", "Silent notification channel created: $NOTIFICATION_CHANNEL_ID")
      }
      notificationChannelCreated = true
    }
  }

  /**
   * Updates the notification with current progress information
   * 
   * @param context Application context
   * @param builder NotificationCompat.Builder to update
   * @param text Text description of current operation
   * @param progress Numeric progress (0-100)
   */
  private suspend fun updateNotification(
    context: Context,
    builder: NotificationCompat.Builder,
    text: String,
    progress: Int
  ) {
    withContext(Dispatchers.Main) {
      try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val updatedNotification = builder
        .setContentText(text)
        .setProgress(100, progress, false)
        .build()
        notificationManager.notify(PATCH_NOTIFICATION_ID, updatedNotification)
      } catch (e: Exception) {
        Log.e("PatchDBKP", "Error updating notification: ${e.message}", e)
      }
    }
  }

  /**
   * Downloads a file from a URL with progress tracking
   * 
   * @param context Application context
   * @param url Source URL to download from
   * @param destinationPath Local directory to save the file
   * @param fileName Name to save the file as
   * @param notificationBuilder NotificationCompat.Builder for progress updates
   * @return Boolean indicating download success or failure
   */
  private suspend fun downloadFile(
    context: Context,
    url: String,
    destinationPath: String,
    fileName: String,
    notificationBuilder: NotificationCompat.Builder
  ): Boolean {
    return withContext(Dispatchers.IO) {
      try {
        // Set up HTTP connection
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        connection.connect()

        // Check if connection is successful
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
          Log.e("downloadFile", "HTTP error: ${connection.responseCode}")
          return@withContext false
        }

        // Get file size and create temp file
        val fileLength = connection.contentLength
        val tempFile = File(context.cacheDir, fileName)
        var bytesRead: Int
        var totalRead = 0
        val buffer = ByteArray(BUFFER_SIZE)
        val inputStream = BufferedInputStream(connection.inputStream)
        val outputStream = FileOutputStream(tempFile)

        var lastProgressUpdate = 0

        // Read file in chunks and update progress
        try {
          while (inputStream.read(buffer).also {
            bytesRead = it
          } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalRead += bytesRead
            val progress = if (fileLength > 0) {
              (totalRead * 100 / fileLength.toFloat()).roundToInt()
            } else {
              -1
            }
            // Update progress notification every 5% or at completion
            if (progress >= lastProgressUpdate + 5 || progress == 100) {
              lastProgressUpdate = progress
              withContext(Dispatchers.Main) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val updatedNotification = notificationBuilder
                .setContentText("Downloading $fileName: $progress%")
                .setProgress(100, progress, progress == -1)
                .build()
                notificationManager.notify(DOWNLOAD_NOTIFICATION_ID, updatedNotification)
              }
            }
          }
        } finally {
          // Close streams
          outputStream.close()
          inputStream.close()
        }

        // Move from cache to destination path
        val destinationFile = File(destinationPath, fileName)
        tempFile.copyTo(destinationFile, overwrite = true)
        tempFile.delete()

        // Verify downloaded file
        if (!destinationFile.exists() || destinationFile.length() == 0L) {
          Log.e("downloadFile", "Downloaded file is empty or doesn't exist")
          return@withContext false
        }

        Log.d("downloadFile", "Download completed: $destinationFile")
        true
      } catch (e: Exception) {
        Log.e("downloadFile", "Failed to download $fileName", e)
        false
      }
    }
  }

  /**
   * Backs up the original boot image from device
   * Creates a copy in the DBKP directory for patching
   * 
   * @param paths PatchPaths object containing file paths
   */
  private fun backupBootImage(paths: PatchPaths) {
    try {
      // Verify boot partition exists
      val bootPartition = File("/dev/block/bootdevice/by-name/boot_a")
      if (!bootPartition.exists()) {
        throw Exception("Boot partition not found")
      }

      // Copy boot partition to file
      Utils.executeShellCommand("dd if=/dev/block/bootdevice/by-name/boot_a of=${paths.bootImg} bs=16m")

      // Verify backup was successful
      val bootImgFile = File(paths.bootImg)
      if (!bootImgFile.exists() || bootImgFile.length() == 0L) {
        throw Exception("Boot image backup failed or empty")
      }

      // Create backup copy
      bootImgFile.copyTo(File(paths.originalBootImg), overwrite = true)
    } catch (e: Exception) {
      Log.e("PatchDBKP", "Error backing up boot image: ${e.message}", e)
      throw e
    }
  }

  /**
   * Patches the kernel for a specific device
   * Handles the unique requirements for each supported device model
   * 
   * @param context Application context
   * @param notificationBuilder NotificationCompat.Builder for progress updates
   * @param filesDir Directory containing binary files
   * @param binFile Binary file specific to this device
   * @param fdFile Firmware descriptor file name
   * @param fdUrl URL to download the firmware descriptor
   * @param bootPartitions Array of boot partition names to flash
   * @param paths PatchPaths object containing file paths
   */
  private suspend fun patchDeviceKernel(
    context: Context,
    notificationBuilder: NotificationCompat.Builder,
    filesDir: String,
    binFile: String,
    fdFile: String,
    fdUrl: String,
    bootPartitions: Array<String>,
    paths: PatchPaths
  ) {
    try {
      // Step 1: Copy binary files
      updateNotification(context, notificationBuilder, "Copying binary files", 50)
      val binSource = File("$filesDir/$binFile")
      val binDest = File("${paths.dbkpDir}/$binFile")
      if (!binSource.exists()) {
        throw Exception("Binary file not found: $binFile")
      }
      binSource.copyTo(binDest, overwrite = true)

      // Step 2: Download firmware descriptor file
      updateNotification(context, notificationBuilder, "Downloading firmware descriptor file", 60)
      val fdDownloaded = downloadFile(
        context,
        fdUrl,
        paths.dbkpDir,
        fdFile,
        notificationBuilder
      )

      if (!fdDownloaded) {
        throw Exception("Failed to download firmware descriptor file")
      }

      // Step 3: Unpack boot image using Magisk's magiskboot tool
      updateNotification(context, notificationBuilder, "Unpacking boot image", 70)
      Utils.executeShellCommand("cd ${paths.dbkpDir} && echo \"$(su -mm -c find /data/adb -name magiskboot) unpack boot.img\" | su -c sh")

      // Step 4: Patch kernel with DBKP tool
      updateNotification(context, notificationBuilder, "Patching kernel", 80)
      Utils.executeShellCommand("su -mm -c $filesDir/dbkp ${paths.kernelFile} ${paths.dbkpDir}/$fdFile ${paths.dbkpDir}/output ${paths.dbkpDir}/dbkp8150.cfg ${paths.dbkpDir}/$binFile")

      // Copy patched kernel over original
      File("${paths.dbkpDir}/output").copyTo(File(paths.kernelFile), overwrite = true)
      
      // Repack boot image with patched kernel
      Utils.executeShellCommand("cd ${paths.dbkpDir} && echo \"$(su -mm -c find /data/adb -name magiskboot) repack boot.img\" | su -c sh")

      // Save patched boot image
      File(paths.newBootImg).copyTo(File(paths.patchedBootImg), overwrite = true)

      // Step 5: Verify boot partitions exist
      updateNotification(context, notificationBuilder, "Checking boot partitions", 85)
      for (partition in bootPartitions) {
        val bootPartition = File("/dev/block/by-name/$partition")
        if (!bootPartition.exists()) {
          throw Exception("Boot partition $partition not found")
        }
      }

      // Step 6: Flash patched boot image to all boot partitions
      updateNotification(context, notificationBuilder, "Flashing patched boot image", 90)
      for (partition in bootPartitions) {
        Utils.executeShellCommand("dd if=${paths.patchedBootImg} of=/dev/block/by-name/$partition bs=16m")
      }

    } catch (e: Exception) {
      Log.e("PatchDBKP", "Error during kernel patching: ${e.message}", e)
      throw e
    } finally {
      // Clean up temporary files
      try {
        updateNotification(context, notificationBuilder, "Cleaning up", 95)
        File(paths.dbkpDir).deleteRecursively()
      } catch (e: Exception) {
        Log.e("PatchDBKP", "Error during cleanup: ${e.message}", e)
      }
    }
  }
}