package com.yo1000.mk2data

import org.assertj.core.api.Assertions.assertThat
import org.h2.Driver
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.Date
import java.sql.DriverManager

class MarkdownTest {
    @Test
    fun test_markdown_to_data() {
        Class.forName(Driver::class.qualifiedName)
        DriverManager.getConnection("jdbc:h2:mem:testdb").use {
            setupTables(it)
            Markdown("""
                FREE Comment area

                | ID   | Name    | age | BLooD   | Birth_Date
                |------|---------|-----|---------|------------
                | '10' | 'Alice' | 20  | 'A'     | 2000-03-05
                | '20' | 'Bob'   | 18  |         | 2002-01-02
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
            """).toData(it).let {
                assertThat(it).containsExactlyInAnyOrder(
                        Table(
                                name = "owners",
                                data = listOf(
                                        listOf("ID", "Name" , "age", "BLooD", "Birth_Date"),
                                        listOf("10", "Alice", 20   , "A"    , Date.valueOf("2000-03-05")),
                                        listOf("20", "Bob"  , 18   , null   , Date.valueOf("2002-01-02"))
                                )
                        ),
                        Table(
                                name = "pets",
                                data = listOf(
                                        listOf("id"  , "name" , "category", "owners_id"),
                                        listOf("1000", "Max"  , "dogs"    , "10"       ),
                                        listOf("1001", "Bella", "dogs"    , "10"       ),
                                        listOf("1002", null   , "dogs"    , "10"       ),
                                        listOf("1003", null   , "dogs"    , "10"       ),
                                        listOf("1004", null   , "dogs"    , "10"       ),
                                        listOf("1005", ""     , "dogs"    , "10"       ),
                                        listOf("1006", "null" , "dogs"    , "10"       ),
                                        listOf("2000", "Tama" , "cats"    , "20"       ),
                                        listOf("9000", null   , "dogs"    , null       )
                                )
                        )
                )
            }
        }
    }

    @Test
    fun test_markdown_to_data_as_string() {
        Markdown("""
            FREE Comment area
            
            | id   | name    | age | blood   | birth_date
            |------|---------|-----|---------|------------
            | '10' | 'Alice' | 20  | 'A'     | 2000-03-05
            | '20' | 'Bob'   | 18  |         | 2002-01-02
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
        """).toData().let {
            assertThat(it).containsExactlyInAnyOrder(
                    Table(
                            name = "owners",
                            data = listOf(
                                    listOf("id", "name" , "age", "blood", "birth_date"),
                                    listOf("10", "Alice", "20" , "A"    , "2000-03-05"),
                                    listOf("20", "Bob"  , "18" , null   , "2002-01-02")
                            )
                    ),
                    Table(
                            name = "pets",
                            data = listOf(
                                    listOf("id"  , "name" , "category", "owners_id"),
                                    listOf("1000", "Max"  , "dogs"    , "10"       ),
                                    listOf("1001", "Bella", "dogs"    , "10"       ),
                                    listOf("1002", null   , "dogs"    , "10"       ),
                                    listOf("1003", null   , "dogs"    , "10"       ),
                                    listOf("1004", null   , "dogs"    , "10"       ),
                                    listOf("1005", ""     , "dogs"    , "10"       ),
                                    listOf("1006", "null" , "dogs"    , "10"       ),
                                    listOf("2000", "Tama" , "cats"    , "20"       ),
                                    listOf("9000", null   , "dogs"    , null       )
                            )
                    )
            )
        }
    }

    @Test
    fun test_markdown_to_data_setup() {
        DriverManager.getConnection("jdbc:h2:mem:testdb").use {
            setupTables(it)
            assertThat(MarkdownUtils.setup(it, """
                | ID   | Name    | age | BLooD   | Birth_Date
                |------|---------|-----|---------|------------
                | '10' | 'Alice' | 20  | 'A'     | 2000-03-05
                | '20' | 'Bob'   | 18  |         | 2002-01-02
                [owners]
                
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
                
            """)).isEqualTo(11) // Insert rows

            it.createStatement().use {
                it.executeQuery("""
                    SELECT
                        id, name, age, blood, birth_date
                    FROM
                        owners
                    ORDER BY
                        id
                """.trimIndent()).use {
                    assertThat(it.next()).isTrue()
                    assertThat(it.getString("id")).isEqualTo("10")
                    assertThat(it.getString("name")).isEqualTo("Alice")
                    assertThat(it.getInt("age")).isEqualTo(20)
                    assertThat(it.getString("blood")).isEqualTo("A")
                    assertThat(it.getDate("birth_date")).isEqualTo(Date.valueOf("2000-03-05"))

                    assertThat(it.next()).isTrue()
                    assertThat(it.getString("id")).isEqualTo("20")
                    assertThat(it.getString("name")).isEqualTo("Bob")
                    assertThat(it.getInt("age")).isEqualTo(18)
                    assertThat(it.getString("blood")).isEqualTo(null)
                    assertThat(it.getDate("birth_date")).isEqualTo(Date.valueOf("2002-01-02"))

                    assertThat(it.next()).isFalse()
                }
            }

            it.createStatement().use {
                it.executeQuery("""
                    SELECT
                        id, name, category, owners_id
                    FROM
                        pets
                    ORDER BY
                        id
                """.trimIndent()).use {
                    assertThat(it.next()).isTrue()
                    assertThat(it.getString("id")).isEqualTo("1000")
                    assertThat(it.getString("name")).isEqualTo("Max")
                    assertThat(it.getString("category")).isEqualTo("dogs")
                    assertThat(it.getString("owners_id")).isEqualTo("10")

                    assertThat(it.next()).isTrue()
                    assertThat(it.getString("id")).isEqualTo("1001")
                    assertThat(it.getString("name")).isEqualTo("Bella")
                    assertThat(it.getString("category")).isEqualTo("dogs")
                    assertThat(it.getString("owners_id")).isEqualTo("10")

                    assertThat(it.next()).isTrue()
                    assertThat(it.getString("id")).isEqualTo("1002")
                    assertThat(it.getString("name")).isEqualTo(null)
                    assertThat(it.getString("category")).isEqualTo("dogs")
                    assertThat(it.getString("owners_id")).isEqualTo("10")

                    assertThat(it.next()).isTrue()
                    assertThat(it.getString("id")).isEqualTo("1003")
                    assertThat(it.getString("name")).isEqualTo(null)
                    assertThat(it.getString("category")).isEqualTo("dogs")
                    assertThat(it.getString("owners_id")).isEqualTo("10")

                    assertThat(it.next()).isTrue()
                    assertThat(it.getString("id")).isEqualTo("1004")
                    assertThat(it.getString("name")).isEqualTo(null)
                    assertThat(it.getString("category")).isEqualTo("dogs")
                    assertThat(it.getString("owners_id")).isEqualTo("10")

                    assertThat(it.next()).isTrue()
                    assertThat(it.getString("id")).isEqualTo("1005")
                    assertThat(it.getString("name")).isEqualTo("")
                    assertThat(it.getString("category")).isEqualTo("dogs")
                    assertThat(it.getString("owners_id")).isEqualTo("10")

                    assertThat(it.next()).isTrue()
                    assertThat(it.getString("id")).isEqualTo("1006")
                    assertThat(it.getString("name")).isEqualTo("null")
                    assertThat(it.getString("category")).isEqualTo("dogs")
                    assertThat(it.getString("owners_id")).isEqualTo("10")

                    assertThat(it.next()).isTrue()
                    assertThat(it.getString("id")).isEqualTo("2000")
                    assertThat(it.getString("name")).isEqualTo("Tama")
                    assertThat(it.getString("category")).isEqualTo("cats")
                    assertThat(it.getString("owners_id")).isEqualTo("20")

                    assertThat(it.next()).isTrue()
                    assertThat(it.getString("id")).isEqualTo("9000")
                    assertThat(it.getString("name")).isEqualTo(null)
                    assertThat(it.getString("category")).isEqualTo("dogs")
                    assertThat(it.getString("owners_id")).isEqualTo(null)

                    assertThat(it.next()).isFalse()
                }
            }
        }
    }

    @Test
    fun test_markdown_to_data_expect() {
        DriverManager.getConnection("jdbc:h2:mem:testdb").use {
            setupTables(it)
            it.createStatement().use { listOf(
                    "INSERT INTO owners (id, name, age, blood, birth_date) VALUES ('10', 'Alice', 20, 'A' , '2000-03-05')",
                    "INSERT INTO owners (id, name, age, blood, birth_date) VALUES ('20', 'Bob'  , 18, null, '2002-01-02')",

                    "INSERT INTO pets (id, name, category, owners_id) VALUES ('1000', 'Max'  , 'dogs', '10')",
                    "INSERT INTO pets (id, name, category, owners_id) VALUES ('1001', 'Bella', 'dogs', '10')",
                    "INSERT INTO pets (id, name, category, owners_id) VALUES ('1002', null   , 'dogs', '10')",
                    "INSERT INTO pets (id, name, category, owners_id) VALUES ('1003', null   , 'dogs', '10')",
                    "INSERT INTO pets (id, name, category, owners_id) VALUES ('1004', null   , 'dogs', '10')",
                    "INSERT INTO pets (id, name, category, owners_id) VALUES ('1005', ''     , 'dogs', '10')",
                    "INSERT INTO pets (id, name, category, owners_id) VALUES ('1006', 'null' , 'dogs', '10')",
                    "INSERT INTO pets (id, name, category, owners_id) VALUES ('2000', 'Tama' , 'cats', '20')",
                    "INSERT INTO pets (id, name, category, owners_id) VALUES ('9000', null   , 'dogs', null)"
            ).forEach(it::addBatch)
                it.executeBatch()
            }

            MarkdownUtils.expect(it, """
                | ID   | Name    | age | BLooD   | Birth_Date
                |------|---------|-----|---------|------------
                | '10' | 'Alice' | 20  | 'A'     | 2000-03-05
                | '20' | 'Bob'   | 18  |         | 2002-01-02
                [owners]
                
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
            """.trimIndent()) { fetched, row ->
                assertThat(fetched).isEqualTo(1)
            }
        }
    }

    @Test
    fun test_markdown_to_data_setup_and_expect() {
        DriverManager.getConnection("jdbc:h2:mem:testdb").use {
            setupTables(it)
            assertThat(MarkdownUtils.setup(it, """
                | ID   | Name    | age | BLooD   | Birth_Date
                |------|---------|-----|---------|------------
                | '10' | Alice   | 20  | 'A'     | 2000-03-05
                | '20' | Bob     | 18  |         | 2002-01-02
                [owners]
                
                id     | name     | category | owners_id
                -------|----------|----------|-----------
                '1000' | 'Max'    | dogs     | 10
                '1001' | 'Bella'  | dogs     | 10
                '1002' |          | dogs     | 10
                '1003' | null     | dogs     | 10
                '1004' | NULL     | dogs     | 10
                '1005' | ''       | dogs     | 10
                '1006' | 'null'   | dogs     | 10
                '2000' | 'Tama'   | cats     | 20
                '9000' |          | dogs     | null
                [pets]
            """)).isEqualTo(11) // Insert rows

            MarkdownUtils.expect(it, """
                | ID   | Name    | age | BLooD   | Birth_Date
                |------|---------|-----|---------|------------
                | '10' | Alice   | 20  | 'A'     | 2000-03-05
                | '20' | Bob     | 18  |         | 2002-01-02
                [owners]
                
                id     | name     | category | owners_id
                -------|----------|----------|-----------
                '1000' | 'Max'    | dogs     | 10
                '1001' | 'Bella'  | dogs     | 10
                '1002' |          | dogs     | 10
                '1003' | null     | dogs     | 10
                '1004' | NULL     | dogs     | 10
                '1005' | ''       | dogs     | 10
                '1006' | 'null'   | dogs     | 10
                '2000' | 'Tama'   | cats     | 20
                '9000' |          | dogs     | null
                [pets]
            """)
        }
    }

    private fun setupTables(conn: Connection) {
        conn.createStatement().use {
            it.execute("""
                    CREATE TABLE owners (
                        id          varchar(40) NOT NULL    PRIMARY KEY ,
                        name        varchar(40) NOT NULL                ,
                        age         int                                 ,
                        blood       varchar(1)                          ,
                        birth_date  date
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
}
