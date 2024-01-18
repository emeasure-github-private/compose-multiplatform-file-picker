package com.darkrockstudios.libraries.mpfilepicker

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.net.toFile

public data class AndroidFile(
	override val path: String,
	override val platformFile: Uri,
) : MPFile<Uri> {
	override suspend fun getFileByteArray(): ByteArray = platformFile.toFile().readBytes()
}

public actual fun filePicker(
	show: Boolean,
	initialDirectory: String?,
	fileExtensions: List<String>,
	title: String?,
	onFileSelected: FileSelected
) {
	TODO("""
	val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { result ->
		if (result != null) {
			onFileSelected(AndroidFile(result.toString(), result))
		} else {
			onFileSelected(null)
		}
	}

	val mimeTypeMap = MimeTypeMap.getSingleton()
	val mimeTypes = if (fileExtensions.isNotEmpty()) {
		fileExtensions.mapNotNull { ext ->
			mimeTypeMap.getMimeTypeFromExtension(ext)
		}.toTypedArray()
	} else {
		emptyArray()
	}

	LaunchedEffect(show) {
		if (show) {
			launcher.launch(mimeTypes)
		}
	}
	""")
}
@Composable
public actual fun MultipleFilePicker(
	show: Boolean,
	initialDirectory: String?,
	fileExtensions: List<String>,
	title: String?,
	onFileSelected: FilesSelected
) {
	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.OpenMultipleDocuments()
	) { result ->

		val files = result.mapNotNull { uri ->
			uri.path?.let {path ->
				AndroidFile(path, uri)
			}
		}

		if (files.isNotEmpty()) {
			onFileSelected(files)
		} else {
			onFileSelected(null)
		}
	}

	val mimeTypeMap = MimeTypeMap.getSingleton()
	val mimeTypes = if (fileExtensions.isNotEmpty()) {
		fileExtensions.mapNotNull { ext ->
			mimeTypeMap.getMimeTypeFromExtension(ext)
		}.toTypedArray()
	} else {
		emptyArray()
	}

	LaunchedEffect(show) {
		if (show) {
			launcher.launch(mimeTypes)
		}
	}
}

@Composable
public actual fun DirectoryPicker(
	show: Boolean,
	initialDirectory: String?,
	title: String?,
	onFileSelected: (String?) -> Unit
) {
	val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { result ->
		onFileSelected(result?.toString())
	}

	LaunchedEffect(show) {
		if (show) {
			launcher.launch(null)
		}
	}
}
