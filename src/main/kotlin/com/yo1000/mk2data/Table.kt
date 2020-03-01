package com.yo1000.mk2data

data class Table(
        val name: String,
        val columns: List<Column>,
        val rows: List<Row>
)

data class Column(
        val name: String
)

data class Row(
        val columnMappedValues: Map<Column, Value>
) {
    val values: Collection<Value> get() = columnMappedValues.values
}

data class Value(
        val rawValue: String?
) {
    fun rawValueWithEnclosure(enclosure: Enclosure): String? = rawValue?.let {
        when (enclosure) {
            Enclosure.NEVER -> rawValue
            Enclosure.ALWAYS,
            Enclosure.AUTO -> "'$rawValue'"
        }
    }
}
