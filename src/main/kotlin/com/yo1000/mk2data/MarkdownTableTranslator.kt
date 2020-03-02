package com.yo1000.mk2data

import com.vladsch.flexmark.ext.tables.TableExtractingVisitor
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.DataKey
import com.vladsch.flexmark.util.data.DataSet
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.format.MarkdownTable
import java.sql.Connection
import java.sql.Types
import kotlin.reflect.full.staticProperties

/**
 * Translate markdown formatted table to INSERT DML.
 *
 * @author yo1000
 */
class TableMarkdownTranslator(
        val options: Map<DataKey<Any>, Any> = emptyMap(),
        val enclosure: Enclosure = Enclosure.AUTO
) {
    private val parserOptions: DataSet

    init {
        parserOptions = MutableDataSet().set(Parser.EXTENSIONS, listOf(TablesExtension.create())).also { dataSet ->
            val allowedKeys = TablesExtension::class.staticProperties.map { it.get() }
            options.filter { it.key in allowedKeys && it.key != TablesExtension.WITH_CAPTION }.forEach {
                dataSet.set(it.key, it.value)
            }
        }
    }

    @Deprecated(
            message = "Replace with com.yo1000.mk2data.TableMarkdownTranslator.translateToInsertSqlMap(String)",
            replaceWith = ReplaceWith("translateToInsertSqlMap(markdown)")
    )
    fun translateToInsertSqls(markdown: String): Map<String, List<String>> = translateToInsertSqlMap(markdown)

    fun translateToInsertSqlMap(markdown: String): Map<String, List<String>> = Parser.builder(parserOptions).build().parse(markdown).let {
        TableExtractingVisitor(parserOptions).getTables(it).map {
            parseMarkdownTable(it) { table, columns, rows ->
                table to rows.map {
                    """
                    INSERT INTO $table (${
                        columns.joinToString(separator = ", ")
                    }) VALUES (${
                        it.map { if (it.trim().isEmpty()) "null" else it }.joinToString(separator = ", ")
                    })
                    """.trimIndent()
                }
            }
        }.toMap()
    }

    fun translateToInsertSqlMap(markdown: String, connection: Connection): Map<String, List<String>> = translateToTables(markdown, connection).map { table ->
        table.name to table.rows.map {
            """
            INSERT INTO ${table.name} (${
                table.columns.map { it.name }.joinToString(separator = ", ")
            }) VALUES (${
                it.columnMappedValues.values.map { it.rawValueWithEnclosure(enclosure) }.joinToString(separator = ", ")
            })
            """.trimIndent()
        }
    }.toMap()

    fun translateToTables(markdown: String, connection: Connection): List<Table> = Parser.builder(parserOptions).build().parse(markdown).let {
        TableExtractingVisitor(parserOptions).getTables(it).map {
            parseMarkdownTable(it) { table, columns, rows ->
                connection.createStatement().use { it.executeQuery("""
                    SELECT ${columns.joinToString(separator = ", ")} FROM ${table} WHERE 1=0
                """.trimIndent()).let {
                        it.use {
                            val meta = it.metaData
                            val columnEnclosurePairs: MutableList<Pair<Column, Boolean>> = mutableListOf()

                            for (i in 0 until meta.columnCount) {
                                val columnNumber = i + 1
                                val columnType = meta.getColumnType(columnNumber)
                                columnEnclosurePairs += Column(meta.getColumnName(columnNumber)) to when (columnType) {
                                    Types.CHAR,
                                    Types.NCHAR,
                                    Types.VARCHAR,
                                    Types.NVARCHAR,
                                    Types.LONGVARCHAR,
                                    Types.LONGNVARCHAR,
                                    Types.DATE,
                                    Types.TIME,
                                    Types.TIME_WITH_TIMEZONE,
                                    Types.TIMESTAMP,
                                    Types.TIMESTAMP_WITH_TIMEZONE -> true
                                    else -> false
                                }
                            }

                            rows.map {
                                it.mapIndexed { index, value ->
                                    val s = value.trim()
                                    columnEnclosurePairs[index].first to Value(when {
                                        s.isEmpty() || s.toLowerCase() == "null" ->
                                            null
                                        (columnEnclosurePairs[index].second) -> when (enclosure) {
                                            Enclosure.ALWAYS,
                                            Enclosure.NEVER -> s
                                            Enclosure.AUTO -> when {
                                                (s.startsWith('\'') && s.endsWith('\'')) ->
                                                    s.substring(1, s.length - 1)
                                                (s.isEmpty() || s.toLowerCase() == "null") ->
                                                    null
                                                else -> s
                                            }
                                        }
                                        else -> s
                                    })
                                }
                            }.map {
                                Row(columnMappedValues = it.toMap())
                            }.let {
                                Table(
                                        name = table,
                                        columns = columnEnclosurePairs.map { it.first },
                                        rows = it
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun <T> parseMarkdownTable(markdownTable: MarkdownTable, handleTable: (String, List<String>, List<List<String>>) -> T): T {
        val table: String = markdownTable.caption?.rows?.takeIf { it.isNotEmpty() }?.first()?.cells?.takeIf { it.isNotEmpty() }?.first()?.text?.unescape()
                ?: throw IllegalArgumentException("Table name was missing")

        val columns: List<String> = markdownTable.header?.rows?.takeIf { it.isNotEmpty() }?.first()?.cells?.takeIf { it.isNotEmpty() }?.map { it.text.unescape() }
                ?: throw IllegalArgumentException("Table columns was missing")

        val rows: List<List<String>> = markdownTable.body?.rows?.map { it.cells.map { it.text.unescape() } }
                ?: throw IllegalArgumentException("Table values was missing")

        // Validation
        rows.forEach {
            assert(it.size == columns.size) { "Columns size and Values size was not same" }
        }

        return handleTable(table, columns, rows)
    }
}
