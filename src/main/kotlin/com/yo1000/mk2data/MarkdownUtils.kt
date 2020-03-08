package com.yo1000.mk2data

import java.sql.Connection

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
        fun expect(
                connection: Connection, markdown: String,
                assert: (Int, Row<*>) -> Unit = { fetchedCount, _ -> assert(fetchedCount == 1) }
        ) {
            Markdown(markdown).toData(connection).map { table ->
                table.rows.forEach { r ->
                    connection.prepareStatement("""
                        SELECT
                            ${table.columns.joinToString(separator = ", ")}
                        FROM
                            ${table.name}
                        WHERE
                            ${table.columns
                            .mapIndexed { i, c -> if (r.values[i] != null) "$c = ?" else "$c IS NULL" }
                            .joinToString(separator = " AND ")
                    }
                    """.trimIndent()).use { statement ->
                        var index = 1
                        r.values.forEach {
                            if (it != null) statement.setObject(index++, it)
                        }
                        statement.executeQuery().use {
                            var fetchedCount = 0
                            while (it.next()) fetchedCount++
                            assert(fetchedCount, r)
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun expect(connection: Connection, markdown: String) = expect(connection, markdown) { fetchedCount, _ ->
            assert(fetchedCount == 1)
        }
    }
}
