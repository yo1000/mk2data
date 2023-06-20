package com.yo1000.mk2data

data class Table<T>(
        val name: String,
        private val data: List<List<T?>>
) {
    val columns: List<String> get() = data[0].map { it.toString() }
    val rows: List<Row<T>> get() = data.filterIndexed { index, _ -> index > 0 }.map { Row(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Table<*>

        if (name != other.name) return false
        if (data != other.data) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }
}

data class Row<T>(
        val values: List<T?>
)
