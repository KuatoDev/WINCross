package id.vern.wincross.fragments

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import id.vern.wincross.R
import id.vern.wincross.BuildConfig

class CreditsPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle
) : Preference(context, attrs, defStyleAttr) {

    init {
        layoutResource = R.layout.preference_credits
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
    super.onBindViewHolder(holder)
    val versionView = holder.findViewById(R.id.appversion) as? TextView
    val packageName = context.packageName
    val version = BuildConfig.VERSION_NAME
    versionView?.text = context.getString(R.string.version_format, packageName, version)
}
}