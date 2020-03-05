package com.yo1000.mk2data

import java.sql.Connection
import java.sql.ResultSetMetaData

class MarkdownUtils {
    companion object {
        @JvmStatic
        fun setup(connection: Connection, markdown: String): Int = Markdown(markdown).toData(connection).map { table ->
            connection.prepareStatement("""
                INSERT INTO ${table.name} (
                    ${table.columns.joinToString(separator = ", ")}
                ) VALUES (
                    ${table.columns.map { "?" }.joinToString(separator = ", ")}
                )
            """.trimIndent()).use { statement ->
                table.rows.forEach {
                    it.values.forEachIndexed { index, v ->
                        statement.setObject(index + 1, v)
                    }
                    statement.addBatch()
                }
                statement.executeBatch().sum()
            }
        }.sum()

        @JvmStatic
        fun expect(connection: Connection, markdown: String, assert: (Boolean, Row<*>) -> Unit = { fetched, _ ->
            assert(fetched)
        }): Any = Markdown(markdown).toData(connection).map { table ->
            val columnTypes: List<Int> = connection.createStatement().use {
                it.executeQuery("SELECT ${table.columns.joinToString(separator = ", ")} FROM ${table.name} WHERE 1=0").use {
                    val meta: ResultSetMetaData = it.metaData

                    table.columns.mapIndexed { index, s ->
                        val columnIndex = index + 1
                        meta.getColumnType(columnIndex)
                    }
                }
            }.toList()

            table.rows.forEach { r ->
                connection.prepareStatement("""
                    SELECT
                        ${table.columns.joinToString(separator = ", ")}
                    FROM
                        ${table.name}
                    WHERE
                        ${table.columns
                        .mapIndexed { i, c -> if (r.values[i] != null) "$c = ?" else "$c IS NULL" }
                        .joinToString(separator = " AND ")} 
                """.trimIndent()).use { statement ->
                    var index = 1
                    r.values.forEach {
                        if (it != null) statement.setObject(index++, it)
                    }
                    statement.executeQuery().use {
                        assert(it.next(), r)
                    }
                    statement.clearParameters()
                }
            }
        }
    }
}