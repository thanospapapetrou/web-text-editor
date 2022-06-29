
# Web Text Editor

Web Text Editor is a simple online text editor with auto-save support. Users can browse available files, create new files, open a file for editing, and delete files. Changes made to a file while editing are automatically saved and optimistic locking is used to resolve conflicts arising from multiple users editing the same file simultaneously. UI is simple, using just HTML and JavaScript and communicating with the backend using REST. Files can be persisted in memory, file system or a relational DB via JDBC. Implementation is based on the following:
- Scala 2.13.4
- sbt 1.6.2
- Akka 2.6.19
- doobie 1.0.0-RC1
- Flyway 7.4.0

## Running

- Run the application
    ```
    sbt run
    ```
- Run tests
    ```
    sbt test
    ```
- Cleanup everything
    ```
    sbt clean
    ```
- Start a PostgreSQL instance listening using Docker and create DB schema (required if using JDBC backend)
    ```
    docker run --name web-text-editor-postgres -e POSTGRES_PASSWORD=mysecretpassword -p 5432:5432 -d postgres
    sbt flywayMigrate
    ```

## Configuration

Application configuration can be modified using the following variables in `application.conf`

| Name                                     | Value                                                    | Description                                                                  |
| ---------------------------------------- | -------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `web-text-editor.registry.backend`       | `Memory`, `FileSystem` or `DB`                           | Backend to use                                                               |
| `web-text-editor.registry.baseDir`       | string, e.g. `/tmp`                                      | Path of directory containing files to edit (if using backend `FileSystem`)   |
| `web-text-editor.registry.jdbc.driver`   | string, e.g. `org.postgresql.Driver`                     | Fully qualified class name of the JDBC driver to use (if using backend `DB`) |
| `web-text-editor.registry.jdbc.url`      | string, e.g. `jdbc:postgresql://localhost:5432/postgres` | JDBC URL to use (if using backend `DB`)                                      |
| `web-text-editor.registry.jdbc.username` | string, e.g. `postgres`                                  | JDBC username to use (if using backend `DB`)                                 |
| `web-text-editor.registry.jdbc.password` | string, e.g. `mysecretpassword`                          | JDBC password to use (if using backend `DB`)                                 |
| `web-text-editor.routes.ask-timeout`     | Duration e.g. `5s`                                       | Request timeout                                                              |

## Manual Testing

You may use the Postman collection in `web-text-editor.postman_collection.json` to manually test the application.
