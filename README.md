
docker run --name web-text-editor-postgres -e POSTGRES_PASSWORD=mysecretpassword -p 5432:5432 -d postgres

sbt flywayMigrate


