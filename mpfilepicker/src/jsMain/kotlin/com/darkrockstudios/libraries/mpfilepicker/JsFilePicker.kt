package com.darkrockstudios.libraries.mpfilepicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.Document
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.ItemArrayLike
import org.w3c.dom.asList
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

public data class WebFile(
	override val path: String,
	override val platformFile: File,
) : MPFile<File> {
	public suspend fun getFileContents(): String = readFileAsText(platformFile)
	public override suspend fun getFileByteArray(): ByteArray = readFileAsByteArray(platformFile)
}

public actual fun filePicker(
	show: Boolean,
	initialDirectory: String?,
	fileExtensions: List<String>,
	title: String?,
	onFileSelected: FileSelected,
) {
	val fixedExtensions = fileExtensions.map { ".$it" }
	document.selectFilesFromDisk(fixedExtensions.joinToString(","), false) { files ->
		if (files != null) {
			onFileSelected(WebFile(files.first().name, files.first()))
		}
	}
}

@Composable
public actual fun MultipleFilePicker(
	show: Boolean,
	initialDirectory: String?,
	fileExtensions: List<String>,
	title: String?,
	onFileSelected: FilesSelected
) {
	LaunchedEffect(show) {
		if (show) {
			val fixedExtensions = fileExtensions.map { ".$it" }
			document.selectFilesFromDisk(fixedExtensions.joinToString(","), true) { files ->
				if (files != null) {
					onFileSelected(files.map { WebFile(it.name, it) })
				}
			}
		}
	}
}

@Composable
public actual fun DirectoryPicker(
	show: Boolean,
	initialDirectory: String?,
	title: String?,
	onFileSelected: (String?) -> Unit,
) {
	// in a browser we can not pick directories
	throw NotImplementedError("DirectoryPicker is not supported on the web")
}

private fun Document.selectFilesFromDisk(
	accept: String,
	isMultiple: Boolean,
	callback: (List<File>?) -> Unit,
) {
	val tempInput = (createElement("input") as HTMLInputElement).apply {
		type = "file"
		style.display = "none"
		this.accept = accept
		multiple = isMultiple
	}

	tempInput.onchange = { e ->
		callback(
			(e.target.asDynamic().files as? ItemArrayLike<File>)?.asList() ?: emptyList()
		)
	}

	body!!.append(tempInput)
	tempInput.click()
	//tempInput.remove() // NOTE: removing the input element breaks Mobile Chrome/Safari upload
}

public suspend fun readFileAsText(file: File): String = suspendCoroutine {
	val reader = FileReader()
	reader.onload = { loadEvt ->
		val content = loadEvt.target.asDynamic().result as String
		it.resumeWith(Result.success(content))
	}
	reader.readAsText(file, "UTF-8")
}

public suspend fun readFileAsByteArray(file: File): ByteArray = suspendCoroutine {
	val reader = FileReader()
	reader.onload = {loadEvt ->
		val content = loadEvt.target.asDynamic().result as ArrayBuffer
		val array = Uint8Array(content)
		val fileByteArray = ByteArray(array.length)
			for (i in 0 until array.length) {
				fileByteArray[i] = array[i]
			}
		it.resumeWith(Result.success(fileByteArray))
	}
	reader.onerror = { err -> it.resumeWithException(Exception("read $file failed")) }
	reader.onabort = { err -> it.resumeWithException(Exception("read $file aborted")) }
	reader.readAsArrayBuffer(file)
}
