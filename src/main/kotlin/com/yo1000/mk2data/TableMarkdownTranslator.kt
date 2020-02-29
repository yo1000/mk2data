package com.yo1000.mk2data

import com.vladsch.flexmark.ext.tables.TableExtractingVisitor
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.DataKey
import com.vladsch.flexmark.util.data.DataSet
import com.vladsch.flexmark.util.data.MutableDataSet
import java.lang.IllegalArgumentException
import kotlin.reflect.full.staticProperties

/**
 * Translate markdown formatted table to INSERT DML.
 *
 * @author yo1000
 */
class TableMarkdownTranslator(
        private val options: Map<DataKey<Any>, Any> = emptyMap()
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
            val table: String = it.caption?.
                    rows?.takeIf { it.isNotEmpty() }?.first()?.
                    cells?.takeIf { it.isNotEmpty() }?.first()?.
                    text?.unescape()
                    ?: throw IllegalArgumentException("Table name was missing")

            val columns: List<String> = it.header?.
                    rows?.takeIf { it.isNotEmpty() }?.first()?.
                    cells?.takeIf { it.isNotEmpty() }?.map { it.text.unescape() }
                    ?: throw IllegalArgumentException("Table columns was missing")

            val values: List<List<String>> = it.body?.
                    rows?.map { it.
                    cells.map { it.text.unescape() } }
                    ?: throw IllegalArgumentException("Table values was missing")

            // Validation
            values.forEach {
                assert(it.size == columns.size) { "Columns size and Values size was not same" }
            }

            table to values.map { """
                INSERT INTO $table (${
                    columns.joinToString(separator = ", ")
                }) VALUES (${
                    it.map { if (it.trim().isEmpty()) "null" else it }.joinToString(separator = ", ")
                })
                """.trimIndent() }
        }.toMap()
    }
}
