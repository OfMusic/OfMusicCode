package com.haziel.ofmusic.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {

    private const val TAG = "ApkInstaller"

    /**
     * Lanza el instalador del sistema con el APK descargado.
     * Si el usuario no ha dado permiso para "Instalar apps desconocidas",
     * lo llevamos a la pantalla de ajustes.
     */
    fun install(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Log.d(TAG, "Pidiendo permiso para instalar apps")
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            Log.d(TAG, "✓ Instalador lanzado")
        } catch (e: Exception) {
            Log.e(TAG, "Error lanzando instalador: ${e.message}", e)
        }
    }
}