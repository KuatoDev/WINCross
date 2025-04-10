package id.vern.wincross.fragments

import android.content.*
import android.content.res.Resources
import android.graphics.Typeface
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import id.vern.wincross.R
import id.vern.wincross.activity.*
import id.vern.wincross.helpers.UtilityHelper

/**
 * Fragment for handling application settings and preferences
 */
class SettingsFragment : PreferenceFragmentCompat() {

  companion object {
    private const val TAG = "SettingsFragment"
    private const val PREFS_NAME = "WinCross_preferences"
  }

  private lateinit var sharedPreferences: SharedPreferences

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.settings, rootKey)
    sharedPreferences = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
    setupPreferences()
  }

  /**
     * Initialize all preferences with their listeners and default values
     */
  private fun setupPreferences() {
    setupThemePreference()
    setupThemeColorPreference()
    setupGetSavedPreferencesPreference()
    setupGithubPreference()
    setupTelegramPreference()
  }

  /**
     * Set up theme selector preference with current value and click listener
     */
  private fun setupThemePreference() {
    findPreference<Preference>(getString(R.string.key_theme))?.apply {
      summary = getCurrentThemeSummary()
      setOnPreferenceClickListener {
        showThemeSelectionDialog(this)
        true
      }
    }
  }

  /**
     * Set up theme color preference with current value and click listener
     */
  private fun setupThemeColorPreference() {
    findPreference<Preference>(getString(R.string.key_theme_color))?.apply {
      summary = getCurrentThemeColorSummary()
      setOnPreferenceClickListener {
        showThemeColorSelectionDialog(this)
        true
      }
    }
  }

  /**
     * Set up preference to display all saved preferences data
     */
  private fun setupGetSavedPreferencesPreference() {
    findPreference<Preference>(getString(R.string.key_getprefs))?.setOnPreferenceClickListener {
      showSavedPreferencesDialog()
      true
    }
  }

  /**
     * Set up GitHub link preference
     */
  private fun setupGithubPreference() {
    findPreference<Preference>(getString(R.string.key_github))?.setOnPreferenceClickListener {
      openUrl("https://github.com/KuatoDev/WINCross")
      true
    }
  }

  /**
     * Set up Telegram link preference
     */
  private fun setupTelegramPreference() {
    findPreference<Preference>(getString(R.string.key_telegram))?.setOnPreferenceClickListener {
      openUrl("https://t.me/vernkuato")
      true
    }
  }

  /**
     * Open URL in browser
     * @param url The URL to open
     */
  private fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
      data = android.net.Uri.parse(url)
    }
    startActivity(intent)
  }

  /**
     * Display theme selection dialog
     * @param preference The preference to update after selection
     */
  private fun showThemeSelectionDialog(preference: Preference) {
    val safeContext = context ?: return
    val themes = arrayOf("System Default", "Dark Mode", "Light Mode")
    val themeValues = arrayOf("system", "dark", "light")
    val currentTheme =
    sharedPreferences.getString(getString(R.string.key_theme), "system") ?: "system"
    val selectedIndex = themeValues.indexOf(currentTheme)

    showDialog(safeContext, "Choose Theme", themes, selectedIndex) {
      which ->
      val selectedTheme = themeValues[which]
      sharedPreferences.edit().putString(getString(R.string.key_theme), selectedTheme).apply()
      applyTheme(selectedTheme)
      preference.summary = getCurrentThemeSummary()
    }
  }

  /**
     * Display theme color selection dialog
     * @param preference The preference to update after selection
     */
  private fun showThemeColorSelectionDialog(preference: Preference) {
    val safeContext = context ?: return
    val colors = arrayOf("Default", "Blue", "Red", "Green", "Yellow")
    val colorValues = arrayOf("default", "blue", "red", "green", "yellow")
    val currentColor =
    sharedPreferences.getString(getString(R.string.key_theme_color), "default") ?: "default"
    val selectedIndex = colorValues.indexOf(currentColor)

    showDialog(safeContext, "Choose Theme Color", colors, selectedIndex) {
      which ->
      val selectedColor = colorValues[which]
      sharedPreferences.edit().putString(getString(R.string.key_theme_color), selectedColor).apply()
      applyThemeColor(selectedColor)
      preference.summary = getCurrentThemeColorSummary()
    }
  }

  /**
     * Generic dialog display helper with blurred background
     * @param context Context to create dialog
     * @param title Dialog title
     * @param items List of items to display
     * @param selectedIndex Currently selected item index
     * @param onItemSelected Callback when user selects an item
     */
  private fun showDialog(
    context: Context,
    title: String,
    items: Array<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
  ) {
    val titleView = createDialogTitle(title)
    showBlur()
    MaterialAlertDialogBuilder(context)
    .setCustomTitle(titleView)
    .setSingleChoiceItems(items, selectedIndex) {
      dialog, which ->
      onItemSelected(which)
      removeBlur()
      dialog.dismiss()
    }
    .setOnDismissListener {
      removeBlur()
    }
    .create()
    .show()
  }

  /**
     * Apply selected theme mode
     * @param selectedTheme Theme value to apply
     */
  private fun applyTheme(selectedTheme: String) {
    val mode = when (selectedTheme) {
      "dark" -> AppCompatDelegate.MODE_NIGHT_YES
      "light" -> AppCompatDelegate.MODE_NIGHT_NO
      else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    AppCompatDelegate.setDefaultNightMode(mode)
    restartMainActivity()
  }

  /**
     * Apply selected theme color
     * @param selectedThemeColor Color value to apply
     */
  private fun applyThemeColor(selectedThemeColor: String) {
    val themeId = when (selectedThemeColor) {
      "blue" -> R.style.Theme_MyApp_Blue
      "red" -> R.style.Theme_MyApp_Red
      "green" -> R.style.Theme_MyApp_Green
      "yellow" -> R.style.Theme_MyApp_Yellow
      else -> R.style.Theme_MyApp_Default
    }
    activity?.setTheme(themeId)
    restartMainActivity()
  }

  /**
     * Restart the main activity to apply theme changes
     */
  private fun restartMainActivity() {
    Handler(Looper.getMainLooper()).postDelayed({
      activity?.let {
        safeActivity ->
        val intent = Intent(safeActivity, MainActivity::class.java).apply {
          addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        safeActivity.finish()
      }
    }, 100)
  }

  /**
     * Get user-friendly description of current theme setting
     * @return String representation of current theme
     */
  private fun getCurrentThemeSummary(): String {
    return when (sharedPreferences.getString(getString(R.string.key_theme), "system")) {
      "dark" -> "Dark Theme"
      "light" -> "Light Theme"
      else -> "System Default"
    }
  }

  /**
     * Get user-friendly description of current theme color setting
     * @return String representation of current theme color
     */
  private fun getCurrentThemeColorSummary(): String {
    return when (sharedPreferences.getString(getString(R.string.key_theme_color), "default")) {
      "blue" -> "Blue"
      "red" -> "Red"
      "green" -> "Green"
      "yellow" -> "Yellow"
      else -> "Default"
    }
  }

  /**
     * Create a styled TextView for dialog titles
     * @param title The title text
     * @return A styled TextView
     */
  private fun createDialogTitle(title: String): TextView {
    val ctx = context ?: return TextView(requireContext()).apply {
      text = title
      textSize = 20f
      setTypeface(null, Typeface.BOLD)
      gravity = Gravity.CENTER
      setPadding(0, 20, 0, 20)
    }

    return if (isAdded && layoutResourceExists(R.layout.dialog_title)) {
      val inflater = LayoutInflater.from(ctx)
      (inflater.inflate(R.layout.dialog_title, null) as TextView).apply {
        text = title
      }
    } else {
      TextView(ctx).apply {
        text = title
        textSize = 20f
        setTypeface(null, Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(0, 20, 0, 20)
      }
    }
  }

  /**
     * Check if a layout resource exists
     * @param resourceId Resource ID to check
     * @return True if resource exists
     */
  private fun layoutResourceExists(resourceId: Int): Boolean {
    return try {
      context?.resources?.getResourceName(resourceId) != null
    } catch (e: Resources.NotFoundException) {
      false
    }
  }

  /**
     * Remove blur effect from background
     */
  private fun removeBlur() {
    activity?.window?.decorView?.rootView?.let {
      UtilityHelper.removeBlurBackground(it)
    }
  }

  /**
     * Show blur effect on background
     */
  private fun showBlur() {
    activity?.window?.decorView?.rootView?.let {
      UtilityHelper.showBlurBackground(it)
    }
  }

  /**
     * Get all stored preferences as formatted strings
     * @return List of preference key-value pairs as strings
     */
  private fun getAllPreferencesData(): List<String> {
    val allEntries = mutableListOf<String>()
    val allPrefs = sharedPreferences.all

    if (allPrefs.isEmpty()) return listOf("No data stored")

    for ((key, value) in allPrefs) {
      val entryString = when (value) {
        is Boolean -> "$key: ${if (value) "Yes" else "No"}"
        is Int -> "$key: $value"
        is Float -> "$key: $value"
        is Long -> "$key: $value"
        is String -> "$key: $value"
        is Set<*> -> "$key: ${value.joinToString(", ")}"
        null -> "$key: null"
        else -> "$key: ${value.toString()}"
      }
      allEntries.add(entryString)
    }

    return allEntries.sorted()
  }

  /**
     * Display a dialog showing all saved preferences
     */
  private fun showSavedPreferencesDialog() {
    val safeContext = context ?: return
    val preferencesData = getAllPreferencesData()
    val titleView = createDialogTitle("Saved Preferences Data")
    showBlur()
    val listView = ListView(safeContext).apply {
      adapter = ArrayAdapter(safeContext, android.R.layout.simple_list_item_1, preferencesData)
    }

    MaterialAlertDialogBuilder(safeContext)
    .setCustomTitle(titleView)
    .setView(listView)
    .setOnDismissListener {
      removeBlur()
    }
    .create()
    .show()
  }
}