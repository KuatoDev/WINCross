package id.vern.wincross.utils

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.*
import coil.load
import com.google.android.material.button.MaterialButton
import id.vern.wincross.R
import android.content.Context

object DeviceModels {

  fun setDeviceInfo(
    context: Context,
    tvDevice: TextView,
    imageView: ImageView,
    tvTotalRam: TextView,
    tvTotalStorage: TextView,
    tvPanel: TextView,
    tvActiveSlot: TextView,
    tvBatteryCapacity: TextView,
    tvKernelPowerProfile: TextView,
    guide: MaterialButton,
    group: MaterialButton

  ) {
    val deviceModel = context.getSharedPreferences("WinCross_preferences", Context.MODE_PRIVATE)
    .getString("device_model", null)?.lowercase() ?: "unknown model"

    val imageUrl =
    mapOf(
      "vayu" to "https://i.ibb.co/rG0xmW5c/6c2ecb6efe4f.webp",
      "bhima" to "https://i.ibb.co/rG0xmW5c/6c2ecb6efe4f.webp",
      "surya" to "https://i.ibb.co/rG0xmW5c/6c2ecb6efe4f.webp",
      "karna" to "https://i.ibb.co/rG0xmW5c/6c2ecb6efe4f.webp",
      "a52sxq" to "https://i.ibb.co/j92FC5f3/22d125848e1c.webp",
      "alioth" to "https://i.ibb.co/21gGvr8V/43e3da24d7a8.webp",
      "alphaplus" to "https://i.ibb.co/RGcG0fzc/75e8610e2798.webp",
      "beryllium" to "https://i.ibb.co/PZd789hq/46fdfba0988e.webp",
      "betalm" to "https://i.ibb.co/JjMFhhXf/9580fd6b115c.webp",
      "beyond1" to "https://i.ibb.co/HfNCmsrP/5c22a1f2610f.webp",
      "cepheus" to "https://i.ibb.co/nqKfm3BW/54a5a37c60f7.webp",
      "cheeseburger" to "https://i.ibb.co/WNygwW2D/4a85d1a63ac8.webp",
      "chiron" to "https://i.ibb.co/qYYVKm11/ac60f0b26b2b.webp",
      "dipper" to "https://i.ibb.co/G3v2hsPD/cba80a3fc415.webp",
      "dumpling" to "https://i.ibb.co/4gVMrpwx/d5e30904c6a1.webp",
      "enchilada" to "https://i.ibb.co/SD1bkX1T/b1ed1e2f1b16.webp",
      "equuleus" to "https://i.ibb.co/fdDBdhVk/8c15368184ed.webp",
      "fajita" to "https://i.ibb.co/wZCt3nnD/81e8d4946328.webp",
      "flashlmdd" to "https://i.ibb.co/qFrPTYVd/8d665acab3d8.webp",
      "gts6l" to "https://i.ibb.co/ycBq7LYv/4814b5e44a8b.webp",
      "guacamole" to "https://i.ibb.co/TDV7YqSy/cf3d9b51dafd.webp",
      "hotdog" to "https://i.ibb.co/d4gvMmK5/ddd8828b9f3d.webp",
      "ingress" to "https://i.ibb.co/QFNH35HB/ece4acd5f3ba.webp",
      "lisa" to "https://i.ibb.co/pjhbWXGj/345531b58318.webp",
      "mh2lm" to "https://i.ibb.co/qFPFvsXg/23a016a7fb6d.webp",
      "miatoll" to "https://i.ibb.co/m5TF0v0Y/8a666c91c532.webp",
      "nabu" to "https://i.ibb.co/9myjr18w/993a6940ec07.webp",
      "perseus" to "https://i.ibb.co/Gf204nwX/871e50554a73.webp",
      "pipa" to "https://i.ibb.co/JRVKtvJF/baaa45ac622f.webp",
      "polaris" to "https://i.ibb.co/xST5Vhv5/4152f5bc813d.webp",
      "q2q" to "https://i.ibb.co/dw6M0FWY/58779a1f2bff.webp",
      "raphael" to "https://i.ibb.co/tTKVDDpt/c74352d2f6aa.webp",
      "rmx2061" to "https://i.ibb.co/Pzm8JhT3/123e163f3b38.webp",
      "rmx2170" to "https://i.ibb.co/xqcPtzc5/506861e11bfc.webp",
      "sargo" to "https://i.ibb.co/2zDsKDL/c5bba7372a89.webp",
      "venus" to "https://i.ibb.co/PZLptGtb/31018dc55c62.webp",
      "winner" to "https://i.ibb.co/wxQ7nVy/c30f1435c952.webp",
      "xpeng" to "https://i.ibb.co/d0P5f0BT/7ca18dcdbf61.webp",
      // add other device here
    )
    .getOrDefault(deviceModel, "https://i.ibb.co/3mDHJzqT/188cd745a999.webp")

    val guideLinks =
    mapOf(
      "vayu" to "https://github.com/woa-vayu/POCOX3Pro-Guides/blob/main/en/installation-selection.md",
      "bhima" to "https://github.com/woa-vayu/POCOX3Pro-Guides/blob/main/en/installation-selection.md",
      "a52sxq" to "https://github.com/n00b69/woa-a52s",
      "alioth" to "https://github.com/Robotix22/WoA-Guides/blob/main/Mu-Qcom/README.md",
      "alphaplus" to "https://github.com/n00b69/woa-alphaplus",
      "beryllium" to "https://github.com/n00b69/woa-beryllium",
      "betalm" to "https://github.com/n00b69/woa-betalm",
      "beyond1" to "https://github.com/sonic011gamer/Mu-Samsung",
      "cepheus" to "https://github.com/ivnvrvnn/Port-Windows-XiaoMI-9",
      "cheeseburger" to "https://renegade-project.tech/",
      "chiron" to "https://renegade-project.tech/",
      "dipper" to "https://github.com/n00b69/woa-dipper",
      "dumpling" to "https://renegade-project.tech/",
      "enchilada" to "https://github.com/n00b69/woa-op6",
      "equuleus" to "https://github.com/n00b69/woa-equuleus",
      "ursa" to "https://github.com/n00b69/woa-equuleus",
      "fajita" to "https://github.com/n00b69/woa-op6",
      "flashlmdd" to "https://github.com/n00b69/woa-flashlmdd",
      "gts6l" to "https://project-aloha.github.io/",
      "guacamole" to "https://project-aloha.github.io/",
      "hotdog" to "https://github.com/n00b69/woa-op7",
      "ingress" to "https://github.com/Robotix22/WoA-Guides/blob/main/Mu-Qcom/README.md",
      "lisa" to "https://github.com/n00b69/woa-lisa",
      "mh2lm" to "https://github.com/n00b69/woa-mh2lm",
      "miatoll" to "https://github.com/woa-miatoll/Port-Windows-11-Redmi-Note-9-Pro",
      "nabu" to "https://github.com/erdilS/Port-Windows-11-Xiaomi-Pad-5",
      "perseus" to "https://github.com/n00b69/woa-perseus",
      "pipa" to "https://github.com/Robotix22/WoA-Guides/blob/main/Mu-Qcom/README.md",
      "polaris" to "https://github.com/n00b69/woa-polaris",
      "q2q" to "https://project-aloha.github.io/",
      "raphael" to "https://github.com/new-WoA-Raphael/woa-raphael",
    )
    .getOrDefault(deviceModel, "https://t.me/woahelperchat")

    val groupLinks =
    mapOf(
      "vayu" to "https://t.me/windowsonvayu",
      "bhima" to "https://t.me/windowsonvayu",
      "a52sxq" to "https://t.me/a52sxq_uefi",
      "beryllium" to "https://t.me/WinOnF1",
      "cepheus" to "http://t.me/woacepheus",
      "cheeseburger" to "https://t.me/joinchat/MNjTmBqHIokjweeN0SpoyA",
      "chiron" to "https://t.me/joinchat/MNjTmBqHIokjweeN0SpoyA",
      "dipper" to "https://t.me/woadipper",
      "dumpling" to "https://t.me/joinchat/MNjTmBqHIokjweeN0SpoyA",
      "enchilada" to "https://t.me/WinOnOP6",
      "equuleus" to "https://t.me/woaequuleus",
      "ursa" to "https://t.me/woaequuleus",
      "fajita" to "https://t.me/WinOnOP6",
      "guacamole" to "https://t.me/onepluswoachat",
      "hotdog" to "https://t.me/onepluswoachat",
      "ingress" to "https://discord.gg/Dx2QgMx7Sv",
      "lisa" to "https://t.me/woalisa",
      "miatoll" to "http://t.me/woamiatoll",
      "nabu" to "https://t.me/nabuwoa",
      "perseus" to "https://t.me/woaperseus",
      "polaris" to "https://t.me/WinOnMIX2S",
      "raphael" to "https://t.me/woaraphael",
      "surya" to "https://t.me/windows_on_pocox3_nfc",
    )
    .getOrDefault(deviceModel, "https://t.me/kuatodevprojects")

    Log.d("DeviceModels", "Loading image from: $imageUrl")
    imageView.load(imageUrl) {
      crossfade(true)
    }

    tvDevice.text = context.getString(R.string.device_model, deviceModel.uppercase())
    tvKernelPowerProfile.text = context.getString(
      R.string.power_profile,
      Utils.getBatteryKernelProfile() ?: "N/A"
    )

    tvBatteryCapacity.text = context.getString(
      R.string.battery_capacity,
      Utils.getBatteryCapacity(context).toInt()
    )

    tvTotalRam.text = context.getString(R.string.total_ram, Utils.getTotalRam(context))
    tvTotalStorage.text = context.getString(R.string.total_storage, Utils.getTotalStorage())
    tvPanel.text = context.getString(R.string.panel_type, Utils.getPanelType())
    tvActiveSlot.text = context.getString(R.string.label_active_slot, Utils.getActiveSlot(context))

    guide.setOnClickListener {
      Log.d("DeviceModels", "Opening Guide Link: $guideLinks")
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(guideLinks))
      guide.context.startActivity(intent)
    }

    group.setOnClickListener {
      Log.d("DeviceModels", "Opening Group Link: $groupLinks")
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(groupLinks))
      group.context.startActivity(intent)
    }
  }
}