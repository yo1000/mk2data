#  :car: mk2data

Translate markdown formatted table to INSERT DML.

## Dependencies

```xml
<dependency>
    <groupId>com.yo1000</groupId>
    <artifactId>mk2data</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Examples
Details refer to `src/test/kotlin/com/yo1000/mk2data/MarkdownTest.kt`

```kotlin
val conn: Connection = ...

MarkdownUtils.setup(conn, """
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
""")

MarkdownUtils.expect(conn, """
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
""")
```
