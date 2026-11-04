package com.firetv.launcher

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.firetv.launcher.adapter.AppGridAdapter
import com.firetv.launcher.databinding.ActivityMainBinding
import com.firetv.launcher.model.AppItem
import com.firetv.launcher.util.AppLoader
import com.firetv.launcher.util.DpadFocusHelper
import com.firetv.launcher.util.FavoritesManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var favoritesAdapter: AppGridAdapter
    private lateinit var allAppsAdapter: AppGridAdapter
    private lateinit var favoritesManager: FavoritesManager

    private var allLoadedApps: List<AppItem> = emptyList()

    // Broadcast receiver to detect app install/uninstall dynamically
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadApps()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        favoritesManager = FavoritesManager(this)

        setupScreenViewport()
        setupRecyclerViews()
        setupTopBar()
        loadApps()
        registerPackageReceiver()
        ensureAccessibilityEnabled()
        startHomeWatcherService()
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(0, 0)
        ensureAccessibilityEnabled()
        startHomeWatcherService()
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        overridePendingTransition(0, 0)
        setIntent(intent)
        focusFirstFavorite()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(packageReceiver)
    }

    /**
     * Ensures the Favorites page precisely equals the screen height so the
     * favorites row is centered in the viewport and the full app list starts offscreen.
     */
    private fun setupScreenViewport() {
        binding.root.post {
            val totalHeight = binding.root.height
            if (totalHeight > 0) {
                val params = binding.pageFavorites.layoutParams
                params.height = totalHeight
                binding.pageFavorites.layoutParams = params
            }
        }
    }

    private fun setupRecyclerViews() {
        favoritesAdapter = AppGridAdapter(
            onItemClick = { app -> launchApp(app) },
            onItemLongClick = { app -> showAppOptionsDialog(app) },
            onItemFocused = { _, hasFocus ->
                if (hasFocus) {
                    // Snap back up to centered favorites
                    binding.scrollContainer.smoothScrollTo(0, 0)
                }
            }
        )

        allAppsAdapter = AppGridAdapter(
            onItemClick = { app -> launchApp(app) },
            onItemLongClick = { app -> showAppOptionsDialog(app) },
            onItemFocused = { _, hasFocus ->
                if (hasFocus) {
                    val targetY = binding.pageAllApps.top
                    // Snap down to full list when scrolled into
                    if (binding.scrollContainer.scrollY < targetY - 10) {
                        binding.scrollContainer.smoothScrollTo(0, targetY)
                    }
                }
            }
        )

        // 5 cards across the screen in Favorites bar
        binding.recyclerFavorites.layoutManager = GridLayoutManager(this, 5)
        binding.recyclerFavorites.adapter = favoritesAdapter
        binding.recyclerFavorites.itemAnimator = null

        // 5 columns across the screen in All Apps
        binding.recyclerApps.layoutManager = GridLayoutManager(this, 5)
        binding.recyclerApps.adapter = allAppsAdapter
        binding.recyclerApps.itemAnimator = null
    }

    private fun setupTopBar() {
        DpadFocusHelper.attachFocusAnimation(binding.btnSettings)
        binding.btnSettings.setOnClickListener {
            openSystemSettings()
        }
    }

    private fun loadApps() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val apps = AppLoader.loadInstalledApps(this@MainActivity)
                allLoadedApps = apps
                allAppsAdapter.submitList(apps)
                binding.tvAppCount.text = "(${apps.size})"

                val favs = favoritesManager.getFavoriteApps(apps)
                favoritesAdapter.submitList(favs)

                // Place initial focus on the first favorite
                focusFirstFavorite()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to load apps: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Focuses the first element of the favorites row and snaps view to the top.
     */
    private fun focusFirstFavorite() {
        binding.scrollContainer.post {
            binding.scrollContainer.smoothScrollTo(0, 0)
            binding.recyclerFavorites.post {
                val firstView = binding.recyclerFavorites.layoutManager?.findViewByPosition(0)
                firstView?.requestFocus()
            }
        }
    }

    private fun launchApp(app: AppItem) {
        try {
            val intent = if (app.isLeanback) {
                packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
                    ?: packageManager.getLaunchIntentForPackage(app.packageName)
            } else {
                packageManager.getLaunchIntentForPackage(app.packageName)
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
                    setClassName(app.packageName, app.activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(explicitIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to launch ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSystemSettings() {
        // Pause Home watcher so opening Settings is not misidentified as a Home button press
        com.firetv.launcher.service.HomeWatcherService.pauseFor(2500L)

        val candidates = listOf(
            // 1. Fire TV standard root settings
            Intent().setComponent(android.content.ComponentName("com.amazon.tv.settings", "com.amazon.tv.settings.RootSettings")),
            // 2. Fire TV main settings activity
            Intent().setComponent(android.content.ComponentName("com.amazon.tv.settings", "com.amazon.tv.settings.tv.MainSettingsActivity")),
            // 3. Fire TV settings package launcher intent
            packageManager.getLaunchIntentForPackage("com.amazon.tv.settings"),
            // 4. Fire TV SSM Control Panel
            Intent().setComponent(android.content.ComponentName("com.amazon.ssm", "com.amazon.ssm.ControlPanel")),
            // 5. Fire TV launcher internal settings activity
            Intent().setComponent(android.content.ComponentName("com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.SettingsActivity")),
            // 6. AOSP standard settings fallback
            Intent(Settings.ACTION_SETTINGS)
        )

        for (candidate in candidates) {
            if (candidate != null) {
                try {
                    candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(candidate)
                    return
                } catch (_: Exception) {
                    // Try next candidate
                }
            }
        }

        Toast.makeText(this, "Settings unavailable", Toast.LENGTH_SHORT).show()
    }

    private fun showAppOptionsDialog(app: AppItem) {
        val dialog = Dialog(this, R.style.DialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_app_options)

        val ivIcon = dialog.findViewById<ImageView>(R.id.dialog_app_icon)
        val tvTitle = dialog.findViewById<TextView>(R.id.dialog_app_title)
        val btnOpen = dialog.findViewById<Button>(R.id.btn_action_open)
        val btnFavorite = dialog.findViewById<Button>(R.id.btn_action_favorite)
        val btnInfo = dialog.findViewById<Button>(R.id.btn_action_info)
        val btnUninstall = dialog.findViewById<Button>(R.id.btn_action_uninstall)

        ivIcon.setImageDrawable(app.icon)
        tvTitle.text = app.label

        val isFav = favoritesManager.isFavorite(app.packageName)
        btnFavorite.text = if (isFav) {
            getString(R.string.action_remove_favorite)
        } else {
            getString(R.string.action_add_favorite)
        }

        // Standard D-pad navigation without layout displacement
        btnOpen.setOnClickListener {
            dialog.dismiss()
            launchApp(app)
        }

        btnFavorite.setOnClickListener {
            dialog.dismiss()
            favoritesManager.toggleFavorite(app.packageName)
            val updatedFavs = favoritesManager.getFavoriteApps(allLoadedApps)
            favoritesAdapter.submitList(updatedFavs)
            val message = if (isFav) "Removed from favorites" else "Added to favorites"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        btnInfo.setOnClickListener {
            dialog.dismiss()
            openAppDetails(app.packageName)
        }

        btnUninstall.setOnClickListener {
            dialog.dismiss()
            uninstallApp(app.packageName)
        }

        dialog.show()
        btnOpen.requestFocus()
    }

    private fun openAppDetails(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not trigger uninstall", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
    }

    private fun startHomeWatcherService() {
        try {
            startService(Intent(this, com.firetv.launcher.service.HomeWatcherService::class.java))
        } catch (_: Exception) {}
    }

    private fun ensureAccessibilityEnabled() {
        try {
            val serviceComponent = "$packageName/${com.firetv.launcher.service.FireTvAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            if (!enabledServices.contains(serviceComponent)) {
                val updatedServices = if (enabledServices.isEmpty()) serviceComponent else "$enabledServices:$serviceComponent"
                Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updatedServices)
            }
            Settings.Secure.putInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        } catch (_: SecurityException) {}
    }

    /**
     * Prevents the Launcher from closing. Resets focus back to the first favorite.
     */
    override fun onBackPressed() {
        focusFirstFavorite()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
