package com.firetv.launcher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.firetv.launcher.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppLoader {

    suspend fun loadInstalledApps(context: Context): List<AppItem> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val myPackageName = context.packageName
        val appMap = mutableMapOf<String, AppItem>()

        // 1. First query Leanback / Android TV apps (highest priority)
        val tvIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val tvApps: List<ResolveInfo> = pm.queryIntentActivities(tvIntent, 0)
        for (info in tvApps) {
            val pkg = info.activityInfo.packageName
            if (pkg != myPackageName) {
                val label = info.loadLabel(pm)?.toString() ?: info.activityInfo.name
                val icon = info.loadIcon(pm)
                appMap[pkg] = AppItem(
                    label = label,
                    packageName = pkg,
                    activityName = info.activityInfo.name,
                    icon = icon,
                    isLeanback = true
                )
            }
        }

        // 2. Query standard launcher apps (for sideloaded regular Android apps)
        val standardIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val standardApps: List<ResolveInfo> = pm.queryIntentActivities(standardIntent, 0)
        for (info in standardApps) {
            val pkg = info.activityInfo.packageName
            if (pkg != myPackageName && !appMap.containsKey(pkg)) {
                val label = info.loadLabel(pm)?.toString() ?: info.activityInfo.name
                val icon = info.loadIcon(pm)
                appMap[pkg] = AppItem(
                    label = label,
                    packageName = pkg,
                    activityName = info.activityInfo.name,
                    icon = icon,
                    isLeanback = false
                )
            }
        }

        return@withContext appMap.values.sortedBy { it.label.lowercase() }
    }
}
