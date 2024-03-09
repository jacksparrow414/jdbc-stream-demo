package jdbc.stream.demo.listener;

import jdbc.stream.demo.util.JPAUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import lombok.extern.java.Log;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.logging.Level;

/**
 * @author jacksparrow414
 * @date 2021/11/27
 * 用来测试JNDI数据源是否配置成功，以及JPA是否启动成功
 * {@link jdbc.stream.demo.util.JPAUtil}
 */
@Log
public class DataSourceListener implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            DataSource ds = (DataSource) envCtx.lookup("jdbc/myLocalJPADB");
            JPAUtil.bootstrapJPA();
            log.info("Bootstrap JPA Success");
        } catch (NamingException ex) {
            log.log(Level.WARNING,"!!!! Got NamingException:", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        JPAUtil.shutdownEntityManagerFactory();
    }
}
