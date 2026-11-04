package com.firetv.launcher.util

import android.content.Context
import android.content.SharedPreferences
import com.firetv.launcher.model.AppItem

/**
 * Manages persisting and retrieving favorite apps.
 * Defaults to the first 5 installed apps on fresh launch.
 */
class FavoritesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("firetv_launcher_favorites", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FAVORITES = "favorite_packages"
        const val MAX_FAVORITES = 5
    }

    fun getFavoritePackageNames(): List<String> {
        val raw = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun isFavorite(packageName: String): Boolean {
        return getFavoritePackageNames().contains(packageName)
    }

    fun addFavorite(packageName: String) {
        val current = getFavoritePackageNames().toMutableList()
        if (!current.contains(packageName)) {
            if (current.size >= MAX_FAVORITES) {
                // Remove the last item to keep exactly MAX_FAVORITES in the bar
                current.removeAt(current.size - 1)
            }
            current.add(0, packageName)
            saveFavorites(current)
        }
    }

    fun removeFavorite(packageName: String) {
        val current = getFavoritePackageNames().toMutableList()
        current.remove(packageName)
        saveFavorites(current)
    }

    fun toggleFavorite(packageName: String): Boolean {
        return if (isFavorite(packageName)) {
            removeFavorite(packageName)
            false
        } else {
            addFavorite(packageName)
            true
        }
    }

    private fun saveFavorites(list: List<String>) {
        prefs.edit().putString(KEY_FAVORITES, list.joinToString(",")).apply()
    }

    fun getFavoriteApps(allApps: List<AppItem>): List<AppItem> {
        val favPackages = getFavoritePackageNames()

        // If no favorites are saved yet, initialize with the first 5 installed apps
        if (favPackages.isEmpty() && allApps.isNotEmpty()) {
            val defaults = allApps.take(MAX_FAVORITES).map { it.packageName }
            saveFavorites(defaults)
            return allApps.take(MAX_FAVORITES)
        }

        val appMap = allApps.associateBy { it.packageName }
        val result = mutableListOf<AppItem>()
        for (pkg in favPackages) {
            appMap[pkg]?.let { result.add(it) }
        }
        return result
    }
}
