# Reference Article
- [stream json response](https://github.com/ratulSharker/stream-json-response)
# Tech Stack
- Weld-CDI
- RestEasy
- JPA/Hibernate
- PostgreSQL
- Tomcat
- Docker

# How To Run?

## Create a PostgreSQL database
```bash
cd config
docker-compose up -d
```
## Run the application
1. Run jdbc.stream.demo.GenerateDataSqlTest to generate data.sql, this sql will be placed in src/main/resources/META-INF/sql
2. Start the application using IDEA to insert data into the database
3. Stop the application
3. Comment out the last four properties of persistence.xml
4. set JVM options. IDEA > Run > Edit Configurations > VM options
```bash
-Xms50m -Xmx50m
```
5. Start the application using IDEA agian
6. Use curl to test the application