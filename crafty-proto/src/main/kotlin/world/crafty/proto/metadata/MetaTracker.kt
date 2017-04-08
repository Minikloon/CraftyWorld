package world.crafty.proto.metadata

abstract class MetaTracker {
    private val values = mutableMapOf<Int, Any?>()
    private val changed = mutableSetOf<Int>()
    
    operator fun get(fieldId: Int) : Any? {
        return values[fieldId]
    }
    
    operator fun set(fieldId: Int, value: Any?) {
        values[fieldId] = value
        changed.add(fieldId)
    }

    fun setFieldNoNotify(fieldId: Int, value: Any?) {
        values[fieldId] = value
    }
    
    fun getChangedAndCLear() : Collection<MetaValue> {
        val changedValues = changed.map { MetaValue(it, values[it]) }
        changed.clear()
        return changedValues
    }
    
    fun getAllValues() : Collection<MetaValue> {
        return values.map { MetaValue(it.key, it.value) }
    }
}