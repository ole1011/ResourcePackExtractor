package de.ole101.rpx.extraction

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.*
import java.util.zip.CRC32

internal object PngCrcRepairer {
    private const val BUFFER_SIZE = 64 * 1024
    private const val SIGNATURE_SIZE = 8
    private const val CHUNK_HEADER_SIZE = 8
    private const val CHUNK_CRC_SIZE = 4

    private val pngSignature = byteArrayOf(
        0x89.toByte(),
        0x50,
        0x4E,
        0x47,
        0x0D,
        0x0A,
        0x1A,
        0x0A
    )

    enum class Result {
        NOT_PNG,
        UNCHANGED,
        REPAIRED
    }

    fun repair(file: Path): Result {
        val invalidCrcCount = FileChannel.open(file, StandardOpenOption.READ).use { channel ->
            if (!hasPngSignature(channel)) {
                return Result.NOT_PNG
            }
            scanChunks(channel, repair = false)
        }

        if (invalidCrcCount == 0) {
            return Result.UNCHANGED
        }

        val temporaryFile = Files.createTempFile(file.parent, ".${file.fileName}.", ".png-crc")
        try {
            FileChannel.open(file, StandardOpenOption.READ).use { source ->
                FileChannel.open(
                    temporaryFile,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
                ).use { repaired ->
                    copy(source, repaired)
                    val repairedCrcCount = scanChunks(repaired, repair = true)
                    if (repairedCrcCount != invalidCrcCount) {
                        throw IOException("PNG changed while repairing it: expected $invalidCrcCount invalid CRCs, found $repairedCrcCount")
                    }
                    repaired.force(true)
                }
            }

            replace(file, temporaryFile)
            return Result.REPAIRED
        } finally {
            Files.deleteIfExists(temporaryFile)
        }
    }

    private fun hasPngSignature(channel: FileChannel): Boolean {
        if (channel.size() < SIGNATURE_SIZE) {
            return false
        }

        val signature = ByteBuffer.allocate(SIGNATURE_SIZE)
        readFully(channel, signature, 0L)
        return signature.array().contentEquals(pngSignature)
    }

    private fun scanChunks(channel: FileChannel, repair: Boolean): Int {
        val fileSize = channel.size()
        val header = ByteBuffer.allocate(CHUNK_HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
        val storedCrc = ByteBuffer.allocate(CHUNK_CRC_SIZE).order(ByteOrder.BIG_ENDIAN)
        val dataBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
        val calculatedCrc = CRC32()

        var chunkOffset = SIGNATURE_SIZE.toLong()
        var invalidCrcCount = 0

        while (true) {
            if (chunkOffset !in SIGNATURE_SIZE..fileSize) {
                throw PngFormatException("Invalid PNG chunk offset $chunkOffset for a $fileSize-byte file")
            }
            val bytesRemaining = fileSize - chunkOffset
            if (bytesRemaining < CHUNK_HEADER_SIZE + CHUNK_CRC_SIZE) {
                throw PngFormatException("PNG ended before a complete chunk header and CRC at offset $chunkOffset")
            }

            header.clear()
            readFully(channel, header, chunkOffset)

            val dataLength = Integer.toUnsignedLong(header.getInt(0))
            val dataOffset = chunkOffset + CHUNK_HEADER_SIZE
            val bytesAfterDataOffset = fileSize - dataOffset
            if (bytesAfterDataOffset < CHUNK_CRC_SIZE || dataLength > bytesAfterDataOffset - CHUNK_CRC_SIZE) {
                throw PngFormatException("PNG chunk at offset $chunkOffset declares $dataLength data bytes, but only ${maxOf(0L, bytesAfterDataOffset - CHUNK_CRC_SIZE)} are available")
            }

            val crcOffset = dataOffset + dataLength
            val nextChunkOffset = crcOffset + CHUNK_CRC_SIZE

            calculatedCrc.reset()
            calculatedCrc.update(header.array(), 4, 4)

            var currentDataOffset = dataOffset
            var dataRemaining = dataLength
            while (dataRemaining > 0L) {
                dataBuffer.clear()
                dataBuffer.limit(minOf(dataRemaining, dataBuffer.capacity().toLong()).toInt())
                readFully(channel, dataBuffer, currentDataOffset)
                calculatedCrc.update(dataBuffer)

                val bytesRead = dataBuffer.limit().toLong()
                currentDataOffset += bytesRead
                dataRemaining -= bytesRead
            }

            storedCrc.clear()
            readFully(channel, storedCrc, crcOffset)
            val actualCrc = Integer.toUnsignedLong(storedCrc.getInt(0))
            val expectedCrc = calculatedCrc.value
            if (actualCrc != expectedCrc) {
                invalidCrcCount++
                if (repair) {
                    storedCrc.clear()
                    storedCrc.putInt(expectedCrc.toInt())
                    storedCrc.flip()
                    writeFully(channel, storedCrc, crcOffset)
                }
            }

            val isEndChunk =
                header.get(4) == 'I'.code.toByte()
                        && header.get(5) == 'E'.code.toByte()
                        && header.get(6) == 'N'.code.toByte()
                        && header.get(7) == 'D'.code.toByte()
            if (isEndChunk) {
                return invalidCrcCount
            }

            chunkOffset = nextChunkOffset
        }
    }

    private fun copy(source: FileChannel, target: FileChannel) {
        source.position(0L)
        target.position(0L)
        target.truncate(0L)

        val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
        while (true) {
            buffer.clear()
            val bytesRead = source.read(buffer)
            if (bytesRead < 0) {
                return
            }
            if (bytesRead == 0) {
                throw IOException("Unable to make progress while copying PNG for CRC repair")
            }
            buffer.flip()
            while (buffer.hasRemaining()) {
                if (target.write(buffer) <= 0) {
                    throw IOException("Unable to make progress while copying PNG for CRC repair")
                }
            }
        }
    }

    private fun replace(original: Path, repaired: Path) {
        try {
            Files.move(
                repaired,
                original,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(repaired, original, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun readFully(channel: FileChannel, buffer: ByteBuffer, startOffset: Long) {
        var offset = startOffset
        while (buffer.hasRemaining()) {
            val bytesRead = channel.read(buffer, offset)
            if (bytesRead <= 0) {
                throw IOException("Unexpected end of file at offset $offset")
            }
            offset += bytesRead
        }
        buffer.flip()
    }

    private fun writeFully(channel: FileChannel, buffer: ByteBuffer, startOffset: Long) {
        var offset = startOffset
        while (buffer.hasRemaining()) {
            val bytesWritten = channel.write(buffer, offset)
            if (bytesWritten <= 0) {
                throw IOException("Unable to write repaired PNG CRC at offset $offset")
            }
            offset += bytesWritten
        }
    }

    private class PngFormatException(message: String) : IOException(message)
}
