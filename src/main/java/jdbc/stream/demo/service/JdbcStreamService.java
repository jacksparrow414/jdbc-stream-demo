package jdbc.stream.demo.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.StreamingOutput;
import jdbc.stream.demo.entity.User;
import jdbc.stream.demo.util.JPAUtil;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.query.Query;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

@ApplicationScoped
@Log
public class JdbcStreamService {

    private static final int FETCH_SIZE = 50;

    private static final String JPQL = "SELECT u FROM User u";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public List<User> streamJdbcResultToResponseOOM() {
        EntityManager entityManager = JPAUtil.acquireEntityManager();
        return entityManager.createQuery(JPQL, User.class).getResultList();
    }

    @SneakyThrows
    public boolean streamJdbcResultToFile(String fileName, String mode) {
        if ("jdbc".equals(mode)) {
            jdbc(fileName);
        } else if ("hibernate".equals(mode)) {
            hibernate(fileName);
        } else if ("jpa".equals(mode)) {
            jpa(fileName);
        }else if ("session".equals(mode)) {
            session(fileName);
        } else {
            log.warning("Invalid mode: " + mode);
            return false;
        }
        return true;
    }

    /**
     * 原生JDBC方式设置FetchSize
     * https://jdbc.postgresql.org/documentation/query/#getting-results-based-on-a-cursor
     *
     * @param fileName
     */
    @SneakyThrows
    private void jdbc(String fileName) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres&password=12345&?currentSchema=public";
        Connection conn = DriverManager.getConnection(url);
        conn.setAutoCommit(false);
        Statement st = conn.createStatement();
        st.setFetchSize(FETCH_SIZE);
        ResultSet rs = st.executeQuery("SELECT * FROM public.user");
        try (FileWriter fileWriter = getFileWriter(fileName);) {
            while (rs.next()) {
                int userId = rs.getInt(1);
                String password = rs.getString(2);
                String roles = rs.getString(3);
                String introduction = rs.getString(4);
                User user = new User();
                user.setUserId(userId);
                user.setPassword(password);
                user.setRoles(roles);
                user.setIntroduction(introduction);
                writeToFile(fileWriter, user);
            }
        } finally {
            rs.close();
            st.close();
            conn.close();
        }

    }

    @SneakyThrows
    private FileWriter getFileWriter(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        } else {
            file.createNewFile();
        }
        return new FileWriter(file);
    }


    /**
     * 1、将JPA的Query转换为Hibernate的Query
     * 2、使用JPA的getResultStream
     * https://docs.jboss.org/hibernate/orm/6.3/userguide/html_single/Hibernate_User_Guide.html#jpql-api-stream
     * unwrap非常有用，可以使用Hibernate的Query接口，类似的还有EntityManager.unwrap(Session.class)
     * https://docs.jboss.org/hibernate/orm/6.3/userguide/html_single/Hibernate_User_Guide.html#pc-unwrap
     * @param fileName
     */
    @SneakyThrows
    private void hibernate(String fileName) {
        EntityManager entityManager = JPAUtil.acquireEntityManager();
        // JPA 原生Query没有提供setFetchSize方法，使用Hibernate的Query
        jakarta.persistence.Query query = entityManager.createQuery(JPQL, User.class);
        query.unwrap(Query.class);
        query.setHint(AvailableHints.HINT_FETCH_SIZE, FETCH_SIZE);
        try (FileWriter fileWriter = getFileWriter(fileName);) {
            query.getResultStream().forEach(object -> {
                User user = (User) object;
                writeToFile(fileWriter, user);
                // 必须detach，否则persistence context也会因为entity太多导致OOM
                entityManager.detach(user);
            });
        }finally {
            entityManager.close();
        }
    }

    /**
     * 1、将EntityManager转换为Session
     * 2、使用Hibernate的Query接口
     * 3、使用ScrollableResults
     * https://docs.jboss.org/hibernate/orm/6.3/userguide/html_single/Hibernate_User_Guide.html#hql-api-incremental
     * https://docs.jboss.org/hibernate/orm/6.3/userguide/html_single/Hibernate_User_Guide.html#pc-unwrap
     * @param fileName
     */
    @SneakyThrows
    private void session(String fileName) {
        EntityManager entityManager = JPAUtil.acquireEntityManager();
//        将EntityManager转换为Session
        Session session = entityManager.unwrap(Session.class);
        org.hibernate.query.Query<User> query = session.createQuery(JPQL, User.class);
        query.setFetchSize(FETCH_SIZE);
        try (FileWriter fileWriter = getFileWriter(fileName);
             ScrollableResults<User> scrollableResults = query.scroll()) {
            while (scrollableResults.next()) {
                User user = scrollableResults.get();
                writeToFile(fileWriter, user);
//                见注释, 和EntityManager.detach一样
                session.evict(user);
            }
        }finally {
            session.close();
        }
    }

    /**
     * debug时发现，只有autoCommit为false，才能保证FetchSize生效.
     * https://docs.jboss.org/hibernate/orm/6.3/userguide/html_single/Hibernate_User_Guide.html#_resource_local_transaction_connection_handling
     * https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#jpql-query-hints
     * 这种手动开启事务很蠢，因为根本不需要事务，只是为了FetchSize生效.
     * 相比这种，第一种方式将JPA的Query转换为Hibernate的Query,然后设置fetchSize的方式更好，不需要手动开启事务.
     * 而且debug时发现，Hibernate设置FetchSize是在QueryOptions中设置的，但是走JPA并不会去调用QueryOptions，可能是Hibernate对JPA的实现有差异.
     * @param fileName
     */
    @SneakyThrows
    private void jpa(String fileName) {
        EntityManager entityManager = JPAUtil.acquireEntityManager();
//        开启事务时, autocommit会被设置为false
        entityManager.getTransaction().begin();
        jakarta.persistence.Query query = entityManager.createQuery(JPQL, User.class);
//        JPA方式设置FetchSize
        query.setHint(AvailableHints.HINT_FETCH_SIZE, FETCH_SIZE);
        try (FileWriter fileWriter = getFileWriter(fileName)) {
            List<User> resultList = query.getResultList();
            resultList.forEach(user -> {
                writeToFile(fileWriter, user);
//                必须detach，否则persistence context也会因为entity太多导致OOM
                entityManager.detach(user);
            });
        }finally {
            entityManager.getTransaction().commit();
            entityManager.close();
        }
    }

    @SneakyThrows
    private void writeToFile(FileWriter fileWriter, User user) {
        try {
            OBJECT_MAPPER.writeValueAsString(user);
            fileWriter.write(OBJECT_MAPPER.writeValueAsString(user));
            fileWriter.write("\n");
        } catch (IOException e) {
            log.warning("Write to file failed: " + e.getMessage());
        }
    }

    /**
     * https://stackoverflow.com/questions/45635739/rest-streaming-json-output
     * @return
     */
    @SneakyThrows
    public StreamingOutput streamJdbcResultToResponse() {
         StreamingOutput stream = output -> {
            EntityManager entityManager = JPAUtil.acquireEntityManager();
            Query<User> query = entityManager.createQuery(JPQL, User.class).unwrap(Query.class);
            query.setFetchSize(FETCH_SIZE);
            try(ScrollableResults<User> scrollableResults = query.scroll();
                JsonGenerator jsonGenerator = OBJECT_MAPPER.getFactory().createGenerator(output);) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeArrayFieldStart("users");

                while (scrollableResults.next()) {
                    User user = scrollableResults.get();
                    jsonGenerator.writeObject(user);
                    entityManager.detach(user);
                }

                jsonGenerator.writeEndArray();
                jsonGenerator.writeEndObject();
            }
        };
        return stream;
    }
}
