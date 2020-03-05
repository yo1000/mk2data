package com.yo1000.mk2data

import com.vladsch.flexmark.ext.tables.TableExtractingVisitor
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.DataKey
import com.vladsch.flexmark.util.data.DataSet
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.format.MarkdownTable
import java.lang.reflect.Method
import java.sql.Connection
import java.sql.ResultSetMetaData
import kotlin.reflect.full.staticProperties

class Markdown(
        val value: String,
        val unwrapEnclosure: Boolean = true,
        options: Map<DataKey<Any>, Any> = emptyMap()
) {
    val options: DataSet

    init {
        this.options = MutableDataSet()
                .set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
                .also { dataSet ->
                    TablesExtension::class.staticProperties.map { it.get() }.let { tableOptionKeys ->
                        options.filter { it.key in tableOptionKeys && it.key != TablesExtension.WITH_CAPTION }
                                .forEach { dataSet.set(it.key, it.value) }
                    }
                }
    }

    private data class ValueParser(
            val columnName: String,
            val parser: (String) -> Any
    )

    fun toData(connection: Connection): List<Table<Any>> = toMarkdownTable().map {
        parseMarkdownTable(it) { table, columns, rows ->
            // Validate table name
            connection.metaData.getTables(null, null, "%", null).use {
                var existsTable: Boolean = false

                while (it.next()) {
                    if (it.getString("TABLE_NAME").toUpperCase() == table.toUpperCase()) {
                        existsTable = true
                        break
                    }
                }

                if (!existsTable) {
                    throw IllegalArgumentException("Table is not exists in database ($table)")
                }
            }

            // Validate column names
            connection.createStatement().use {
                it.executeQuery("SELECT * FROM $table WHERE 1=0").use {
                    val meta: ResultSetMetaData = it.metaData
                    val columnNames: List<String> = (1..meta.columnCount).map { i ->
                        meta.getColumnName(i).toUpperCase()
                    }

                    columns.forEach {
                        if (it.toUpperCase() !in columnNames) {
                            throw IllegalArgumentException("Column is not exists in database ($it)")
                        }
                    }
                }
            }

            connection.createStatement().use {
                it.executeQuery("SELECT ${columns.joinToString(separator = ", ")} FROM $table WHERE 1=0").use {
                    val meta: ResultSetMetaData = it.metaData
                    val indexedParsers: List<ValueParser> = (1..meta.columnCount).map { i ->
                        val columnNameFromTable: String = meta.getColumnLabel(i).toUpperCase()
                        val columnName: String = columns.find { it.toUpperCase() == columnNameFromTable }!!
                        val valueParseInvoker: Method? = meta.getColumnClassName(i).let {
                            Class.forName(it)
                        }.takeIf {
                            it != String::class.java
                        }?.getMethod("valueOf", String::class.java)
                        val valueParser: (String) -> Any = fun(s: String): Any {
                            return valueParseInvoker?.invoke(null, s) ?: s
                        }

                        ValueParser(
                                columnName = columnName,
                                parser = valueParser
                        )
                    }

                    rows.map {
                        it.mapIndexed { index, value ->
                            value.let {
                                if (it.isEmpty() || it.toLowerCase() == "null") null
                                else unwrapEnclosure(it)
                            }.let { s ->
                                indexedParsers[index].let { valueParser ->
                                    s?.let { valueParser.parser(it) }
                                }
                            }
                        }
                    }.let {
                        Table(
                                name = table,
                                data = listOf(indexedParsers.map { it.columnName }) + it
                        )
                    }
                }
            }
        }
    }

    fun toData(): List<Table<String>> = toMarkdownTable().map {
        parseMarkdownTable(it) { table, columns, rows ->
            rows.map {
                it.mapIndexed { index, value ->
                    value.let {
                        if (it.isEmpty() || it.toLowerCase() == "null") null
                        else unwrapEnclosure(it)
                    }
                }
            }.let {
                Table(
                        name = table,
                        data = listOf(columns) + it
                )
            }
        }
    }

    private fun unwrapEnclosure(s: String): String? =
            if (unwrapEnclosure && s.startsWith('\'') && s.endsWith('\'')) s.substring(1, s.length - 1)
            else s

    private fun toMarkdownTable(): List<MarkdownTable> = Parser.builder(options)
            .build().parse(value.trimIndent())
            .let { TableExtractingVisitor(options).getTables(it) }
            .toList()

    private fun <T> parseMarkdownTable(markdownTable: MarkdownTable, handleTable: (String, List<String>, List<List<String>>) -> T): T {
        val table: String = markdownTable.caption?.rows?.takeIf { it.isNotEmpty() }?.first()?.cells?.takeIf { it.isNotEmpty() }?.first()?.text?.unescape()?.trim()
                ?: throw IllegalArgumentException("Table name is missing")

        val columns: List<String> = markdownTable.header?.rows?.takeIf { it.isNotEmpty() }?.first()?.cells?.takeIf { it.isNotEmpty() }?.map { it.text.unescape().trim() }
                ?: throw IllegalArgumentException("Table columns is missing")

        val rows: List<List<String>> = markdownTable.body?.rows?.map { it.cells.map { it.text.unescape().trim() } }
                ?: throw IllegalArgumentException("Table values is missing")

        // Validate column and value sizes
        rows.forEach {
            assert(it.size == columns.size) { "Columns size and Values size are not same" }
        }

        return handleTable(table, columns, rows)
    }
}