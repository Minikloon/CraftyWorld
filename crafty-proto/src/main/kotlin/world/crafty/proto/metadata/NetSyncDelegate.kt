package world.crafty.proto.metadata

import kotlin.reflect.KProperty

inline fun <reified T: Any?> netSync(metaTracker: MetaTracker, field: MetaField, initialValue: T) = NetSyncDelegate(metaTracker, field, initialValue)

class NetSyncDelegate<TField: Any?>(val metaTracker: MetaTracker, val field: MetaField, initialValue: TField) {
    init {
        metaTracker.setFieldNoNotify(field.id, initialValue)
    }
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): TField {
        return metaTracker[field.id] as TField
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: TField) {
        metaTracker[field.id] = value
    }
}