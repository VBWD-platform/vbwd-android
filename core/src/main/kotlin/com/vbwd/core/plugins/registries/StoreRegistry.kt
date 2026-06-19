package com.vbwd.core.plugins.registries

/**
 * Registry of plugin stores. Port of the iOS `StoreRegistry`: a plugin registers
 * an object (typically a `StateFlow`-backed store) by id; duplicate ids are
 * rejected.
 */
class StoreRegistry {
    private val stores = LinkedHashMap<String, Any>()

    fun create(id: String, store: Any) {
        if (stores.containsKey(id)) throw RegistryError.DuplicateStoreId(id)
        stores[id] = store
    }

    fun get(id: String): Any? = stores[id]

    fun all(): Map<String, Any> = stores.toMap()
}
