package de.ole101.rpx.util

import de.ole101.rpx.exception.ResourcePackNotFoundException
import net.minecraft.resource.ResourcePackProfile
import net.minecraft.resource.ResourcePackSource
import java.io.File

object ResourcePackUtil {

    /**
     * Gets the short ID for a resource pack (last 36 characters).
     */
    fun getShortId(profile: ResourcePackProfile): String {
        return profile.id.takeLast(36)
    }

    /**
     * Gets the source file for a resource pack.
     */
    fun getSourceFile(profile: ResourcePackProfile, runDirectory: File): File {
        val id = getShortId(profile)
        val sourceDirectory = runDirectory.resolve("downloads").resolve(id)
        return sourceDirectory.listFiles()?.firstOrNull()
            ?: throw ResourcePackNotFoundException("No source file found for resource pack: $id")
    }

    /**
     * Gets the extraction directory for a resource pack.
     */
    fun getExtractionDirectory(profile: ResourcePackProfile, runDirectory: File): File {
        val id = getShortId(profile)
        return runDirectory.resolve("extracted").resolve(id)
    }

    /**
     * Filters resource pack profiles to only include server and world packs.
     */
    fun filterExtractableProfiles(profiles: Collection<ResourcePackProfile>): List<ResourcePackProfile> {
        return profiles.filter { profile ->
            profile.source == ResourcePackSource.SERVER || profile.source == ResourcePackSource.WORLD
        }
    }

    fun getSafeDisplayName(profile: ResourcePackProfile): String {
        return try {
            profile.displayName.string
        } catch (_: Throwable) {
            profile.toString()
        }
    }
}
