package de.ole101.rpx.util

import de.ole101.rpx.exception.ExtractionDirectoryException
import de.ole101.rpx.exception.ExtractionFailedException
import de.ole101.rpx.exception.InvalidResourcePackException
import de.ole101.rpx.extraction.ExtractionEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.zip.ZipFile

object ZipUtil {
    private const val BUFFER_SIZE = 4096

    /**
     * Extracts resource pack from zip file using coroutines and Flow for progress.
     *
     * @param file          resource pack
     * @param destDirectory destination directory
     * @return Flow of extraction events
     */
    fun extractZipFlow(file: File, destDirectory: File): Flow<ExtractionEvent> = flow {
        validateInputs(file)

        try {
            createDestinationDirectory(destDirectory)
        } catch (e: Exception) {
            throw ExtractionDirectoryException(destDirectory.absolutePath, e)
        }

        val totalSize = file.length()
        emit(ExtractionEvent.Started(totalSize))
        emit(ExtractionEvent.Message("Extracting resource pack ${file.name} to directory ${destDirectory.name}"))

        var elementCount = 0

        try {
            ZipFile(file).use { zipFile ->
                val zipEntriesEnum = zipFile.entries()
                while (zipEntriesEnum.hasMoreElements()) {
                    currentCoroutineContext().ensureActive()
                    val zipEntry = zipEntriesEnum.nextElement()

                    if (isUnsafePath(zipEntry.name)) {
                        emit(ExtractionEvent.Message("Skipping ${zipEntry.name}: Invalid path"))
                        continue
                    }

                    val outFile = File(destDirectory, zipEntry.name)
                    if (zipEntry.isDirectory) {
                        outFile.mkdirs()
                        continue
                    }

                    outFile.parentFile?.mkdirs()
                    emit(ExtractionEvent.Message("Extracting ${zipEntry.name}"))

                    try {
                        zipFile.getInputStream(zipEntry).use { entryInputStream ->
                            val bytes = extractFile(entryInputStream, outFile)
                            emit(ExtractionEvent.Progress(bytes, zipEntry.name))
                            elementCount++
                        }
                    } catch (e: Exception) {
                        throw ExtractionFailedException(zipEntry.name, e)
                    }
                }
            }
        } catch (e: ExtractionFailedException) {
            emit(ExtractionEvent.Error(e))
            return@flow
        } catch (e: Exception) {
            emit(ExtractionEvent.Error(InvalidResourcePackException("Failed to read zip file: ${e.message}")))
            return@flow
        }

        emit(ExtractionEvent.Message("Successfully extracted $elementCount elements"))
        emit(ExtractionEvent.Completed)
    }.flowOn(Dispatchers.IO)

    private fun validateInputs(file: File) {
        if (!file.exists()) {
            throw InvalidResourcePackException("Source file does not exist: ${file.absolutePath}")
        }
        if (!file.isFile) {
            throw InvalidResourcePackException("Source is not a file: ${file.absolutePath}")
        }
        if (file.length() == 0L) {
            throw InvalidResourcePackException("Source file is empty: ${file.absolutePath}")
        }
    }

    private fun createDestinationDirectory(destDirectory: File) {
        if (!destDirectory.exists()) {
            destDirectory.mkdirs()
        }
    }

    private fun isUnsafePath(path: String): Boolean {
        return path.contains("..") || path.startsWith("/") || path.contains("\\..\\")
    }

    /**
     * Extracts a single file from a zip entry into the target file (blocking).
     *
     * @param entryInputStream input stream of the zip entry
     * @param targetFile file to write to
     * @return number of bytes written
     */
    private fun extractFile(entryInputStream: InputStream, targetFile: File): Long {
        targetFile.parentFile?.mkdirs()
        Files.newOutputStream(targetFile.toPath()).use { output ->
            return entryInputStream.copyTo(output, BUFFER_SIZE)
        }
    }
}
