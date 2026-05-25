package app.myzel394.alibi.ui.components.RecorderScreen.molecules

import android.Manifest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.myzel394.alibi.R
import app.myzel394.alibi.db.AppSettings
import app.myzel394.alibi.ui.components.RecorderScreen.atoms.BigButton
import app.myzel394.alibi.ui.components.atoms.PermissionRequester
import app.myzel394.alibi.ui.models.VideoRecorderModel
import app.myzel394.alibi.ui.utils.PermissionHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoRecordingStart(
    videoRecorder: VideoRecorderModel,
    appSettings: AppSettings,
    onHideAudioRecording: () -> Unit,
    onShowAudioRecording: () -> Unit,
    showPreview: Boolean,
    useLargeButtons: Boolean? = null,
) {
    val context = LocalContext.current

    var showSheet by rememberSaveable {
        mutableStateOf(false)
    }
    var startAfterStoragePermission by rememberSaveable {
        mutableStateOf(false)
    }

    fun startRecordingWithDefaults() {
        videoRecorder.init(context)
        videoRecorder.startRecording(context, appSettings)
    }

    if (showSheet) {
        VideoRecorderPreparationSheet(
            showPreview = showPreview,
            videoSettings = videoRecorder,
            appSettings = appSettings,
            onDismiss = {
                showSheet = false
            },
            onPreviewVisible = onHideAudioRecording,
            onPreviewHidden = onShowAudioRecording,
            onStartRecording = {
                videoRecorder.startRecording(context, appSettings)
            },
        )
    }

    PermissionRequester(
        permission = Manifest.permission.CAMERA,
        icon = Icons.Default.CameraAlt,
        onPermissionAvailable = {
            startRecordingWithDefaults()
        },
    ) { triggerCamera ->
        fun startAfterRequiredPermissions() {
            if (!PermissionHelper.hasGranted(context, Manifest.permission.CAMERA)) {
                triggerCamera()
                return
            }

            startRecordingWithDefaults()
        }

        PermissionRequester(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            icon = Icons.Default.GpsFixed,
            onPermissionAvailable = {
                startAfterRequiredPermissions()
            },
        ) { triggerLocation ->
            fun startAfterOptionalLocation() {
                if (
                    appSettings.videoRecorderSettings.overlaySettings.locationEnabled &&
                    !PermissionHelper.hasGranted(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                ) {
                    triggerLocation()
                } else {
                    startAfterRequiredPermissions()
                }
            }

            PermissionRequester(
                permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                onPermissionAvailable = {
                    if (startAfterStoragePermission) {
                        startAfterStoragePermission = false
                        startAfterOptionalLocation()
                    } else {
                        showSheet = true
                    }
                }
            ) { triggerExternalStorage ->
                BigButton(
                    label = stringResource(R.string.ui_videoRecorder_action_start_label),
                    description = stringResource(R.string.ui_videoRecorder_action_configure_label),
                    icon = Icons.Default.CameraAlt,
                    onLongClick = {
                        if (appSettings.requiresExternalStoragePermission(context)) {
                            startAfterStoragePermission = false
                            triggerExternalStorage()
                            return@BigButton
                        }

                        showSheet = true
                    },
                    onClick = {
                        if (appSettings.requiresExternalStoragePermission(context)) {
                            startAfterStoragePermission = true
                            triggerExternalStorage()
                        } else {
                            startAfterOptionalLocation()
                        }
                    },
                    isBig = useLargeButtons,
                )
            }
        }
    }
}
