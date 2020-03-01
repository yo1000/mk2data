#  :car: mk2data

Translate markdown formatted table to INSERT DML.

## Dependencies

```xml
<dependency>
    <groupId>com.yo1000</groupId>
    <artifactId>mk2data</artifactId>
    <version>0.2.0</version>
</dependency>
```

## Examples
Details refer to `src/test/kotlin/com/yo1000/mk2data/TableMarkdownTranslatorTest.kt`

```kotlin
val stmt: Statement = ...

TableMarkdownTranslator().translateToInsertSqlMap("""
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
```

## :warning: Warning
Translated values dont be escape. Module is designed for testing.
