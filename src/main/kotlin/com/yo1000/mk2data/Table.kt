package com.yo1000.mk2data

data class Table<T>(
        val name: String,
        private val data: List<List<T?>>
) {
    val columns: List<String> get() = data[0].map { it.toString() }
    val rows: List<Row<T>> get() = data.filterIndexed { index, _ -> index > 0 }.map { Row(it) }
}

data class Row<T>(
        val values: List<T?>
)
