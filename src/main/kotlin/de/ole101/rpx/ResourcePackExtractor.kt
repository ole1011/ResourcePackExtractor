package de.ole101.rpx

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import net.fabricmc.api.ModInitializer

val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO) + SupervisorJob()

class ResourcePackExtractor : ModInitializer {

    override fun onInitialize() {
    }
}
