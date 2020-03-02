package com.yo1000.mk2data

import org.assertj.core.api.Assertions.assertThat
import org.h2.Driver
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

class MarkdownTableTranslatorTest {
    @Test
    fun test_TranslateToInsertSqlMap() {
        testTemplate {
            it.createStatement().use { stmt ->
                MarkdownTableTranslator().translateToInsertSqlMap("""
                    FREE Comment area

                    | id   | name    | age | blood   |
                    |------|---------|-----|---------|
                    | '10' | 'Alice' | 20  | 'A'     |
                    | '20' | 'Bob'   | 18  |         |
                    [owners]
                    
                    FREE Comment area

                    | id     | name     | category | owners_id
                    |--------|----------|----------|-----------
                    | '1000' | 'Max'    | 'dogs'   | '10'
                    | '1001' | 'Bella'  | 'dogs'   | '10'
                    | '1002' |          | 'dogs'   | '10'
                    | '1003' | null     | 'dogs'   | '10'
                    | '1004' | NULL     | 'dogs'   | '10'
                    | '1005' | ''       | 'dogs'   | '10'
                    | '1006' | 'null'   | 'dogs'   | '10'
                    | '2000' | 'Tama'   | 'cats'   | '20'
                    | '9000' |          | 'dogs'   | null
                    [pets]
                    
                    FREE Comment area
                """.trimIndent()).forEach {
                    it.value.forEach(stmt::addBatch)
                }
                stmt.executeBatch()
            }
        }
    }

    @Test
    fun test_TranslateToInsertSqlMap_with_Connection() {
        testTemplate {
            it.createStatement().use { stmt ->
                MarkdownTableTranslator().translateToInsertSqlMap("""
                    FREE Comment area
                    
                    | id   | name    | age | blood   |
                    |------|---------|-----|---------|
                    | '10' | Alice   | 20  | 'A'     |
                    | '20' | Bob     | 18  |         |
                    [owners]
                    
                    ``       -> null value
                    `null`   -> null value
                    `NULL`   -> null value
                    `NuLL`   -> null value
                    `''`     -> Empty string value
                    `'null'` -> "null" string value
                    
                    | id   | name   | category | owners_id
                    |------|--------|----------|-----------
                    | 1000 | Max    | dogs     | '10'
                    | 1001 | Bella  | dogs     | '10'
                    | 1002 |        | dogs     | '10'
                    | 1003 | null   | dogs     | '10'
                    | 1004 | NULL   | dogs     | '10'
                    | 1005 | ''     | dogs     | '10'
                    | 1006 | 'null' | dogs     | '10'
                    | 2000 | Tama   | cats     | '20'
                    | 9000 |        | dogs     | null
                    [pets]
                    
                    FREE Comment area 
                """.trimIndent(), it).forEach {
                    it.value.forEach(stmt::addBatch)
                }
                stmt.executeBatch()
            }
        }
    }

    @Test
    fun test_TranslateToInsertSqlMap_from_alias() {
        testTemplate {
            it.createStatement().use { stmt ->
                TableMarkdownTranslator().translateToInsertSqlMap("""
                    FREE Comment area

                    | id   | name    | age | blood   |
                    |------|---------|-----|---------|
                    | '10' | 'Alice' | 20  | 'A'     |
                    | '20' | 'Bob'   | 18  |         |
                    [owners]
                    
                    FREE Comment area

                    | id     | name     | category | owners_id
                    |--------|----------|----------|-----------
                    | '1000' | 'Max'    | 'dogs'   | '10'
                    | '1001' | 'Bella'  | 'dogs'   | '10'
                    | '1002' |          | 'dogs'   | '10'
                    | '1003' | null     | 'dogs'   | '10'
                    | '1004' | NULL     | 'dogs'   | '10'
                    | '1005' | ''       | 'dogs'   | '10'
                    | '1006' | 'null'   | 'dogs'   | '10'
                    | '2000' | 'Tama'   | 'cats'   | '20'
                    | '9000' |          | 'dogs'   | null
                    [pets]
                    
                    FREE Comment area
                """.trimIndent()).forEach {
                    it.value.forEach(stmt::addBatch)
                }
                stmt.executeBatch()
            }
        }
    }

    private fun testTemplate(testBody: (Connection) -> Unit) {
        Class.forName(Driver::class.qualifiedName)
        DriverManager.getConnection("jdbc:h2:mem:testdb").use {
            setupTables(it)
            testBody(it)
            assertTables(it)
        }
    }

    private fun setupTables(conn: Connection) {
        conn.createStatement().use {
            it.execute("""
                    CREATE TABLE owners (
                        id      varchar(40)     NOT NULL    PRIMARY KEY ,
                        name    varchar(40)     NOT NULL                ,
                        age     int                                     ,
                        blood   varchar(1)
                    );
                    
                    CREATE TABLE pets (
                        id          varchar(40) NOT NULL    PRIMARY KEY ,
                        name        varchar(40)                         ,
                        category    varchar(20) NOT NULL                ,
                        owners_id   varchar(40)
                    );
                """.trimIndent())
        }

        conn.createStatement().use {
            it.execute("""
                    TRUNCATE TABLE owners;
                    TRUNCATE TABLE pets;
                """.trimIndent())
        }
    }

    private fun assertTables(conn: Connection) {
        conn.prepareStatement("""
                SELECT
                    owners.id       AS owners_id        ,
                    owners.name     AS owners_name      ,
                    pets.id         AS pets_id          ,
                    pets.name       AS pets_name        ,
                    pets.category   AS pets_category
                FROM
                    owners
                INNER JOIN
                    pets
                    ON  owners.id = pets.owners_id
                ORDER BY
                    owners_id,
                    pets_id
            """.trimIndent()
        ).use {
            it.executeQuery().use {
                val items = mutableListOf<Triple<*, *, *>>()

                while (it.next()) {
                    items += Triple(
                            it.getString("owners_name"),
                            it.getString("pets_name"),
                            it.getString("pets_category")
                    )
                }

                assertThat(items).containsExactly(
                        Triple("Alice", "Max", "dogs"),
                        Triple("Alice", "Bella", "dogs"),
                        Triple("Alice", null, "dogs"),
                        Triple("Alice", null, "dogs"),
                        Triple("Alice", null, "dogs"),
                        Triple("Alice", "", "dogs"),
                        Triple("Alice", "null", "dogs"),
                        Triple("Bob", "Tama", "cats")
                )
            }
        }
    }
}
