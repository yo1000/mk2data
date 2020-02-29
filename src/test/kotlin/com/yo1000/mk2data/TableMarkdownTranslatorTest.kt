package com.yo1000.mk2data

import org.assertj.core.api.Assertions.assertThat
import org.h2.Driver
import org.junit.jupiter.api.Test
import java.sql.DriverManager

class TableMarkdownTranslatorTest {
    @Test
    fun testFlexmark() {
        Class.forName(Driver::class.qualifiedName)
        DriverManager.getConnection("jdbc:h2:mem:testdb").use {
            it.createStatement().use {
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

            it.createStatement().use { stmt ->
                TableMarkdownTranslator().translateToInsertSqls("""
                    | id   | name    | age | blood   |
                    |------|---------|-----|---------|
                    | '10' | 'Alice' | 20  | 'A'     |
                    | '20' | 'Bob'   | 18  |         |
                    [owners]
                    
                    | id     | name     | category | owners_id
                    |--------|----------|----------|-----------
                    | '1000' | 'Max'    | 'dogs'   | '10'
                    | '1001' | 'Bella'  | 'dogs'   | '10'
                    | '2000' | 'Tama'   | 'cats'   | '20'
                    | '9000' |          | 'dogs'   | null
                    [pets]
                """.trimIndent()).forEach {
                    it.value.forEach(stmt::addBatch)
                }

                stmt.executeBatch()
            }

            it.prepareStatement("""
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
            """.trimIndent()).use {
                it.executeQuery().use {
                    val items = mutableListOf<Triple<*, *, *>>()

                    while (it.next()) {
                        items += Triple(
                                it.getString("owners_name"),
                                it.getString("pets_name"),
                                it.getString("pets_category")
                        )
                    }

                    assertThat(items).containsExactlyInAnyOrder(
                            Triple("Alice", "Max", "dogs"),
                            Triple("Alice", "Bella", "dogs"),
                            Triple("Bob", "Tama", "cats")
                    )
                }
            }
        }
    }
}
