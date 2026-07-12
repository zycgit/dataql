//package net.hasor.dataql.sqlproc.execute.transaction;
//
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.UUID;
//import org.junit.Before;
//import org.junit.Test;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertSame;
//import static org.junit.Assert.assertTrue;
//
//public class LocalTransactionManagerTest {
//    private String                 jdbcUrl;
//    private TransactionManagerImpl connectionManager;
//    private TransactionManager     transactionManager;
//
//    @Before
//    public void setupDatabase() throws Exception {
//        this.jdbcUrl = "jdbc:h2:mem:manager_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";
//        this.connectionManager = new TransactionManagerImpl(name -> this.rawConnection());
//        this.transactionManager = this.connectionManager.getTransactionManager(null);
//        try (Connection connection = this.rawConnection(); Statement statement = connection.createStatement()) {
//            statement.execute("CREATE TABLE tx_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, label VARCHAR(100))");
//        }
//    }
//
//    @Test
//    public void nestedRollbackKeepsOuterTransaction() throws Exception {
//        TransactionStatus outer = this.transactionManager.begin();
//        this.insert("outer");
//        TransactionStatus nested = this.transactionManager.begin(Propagation.NESTED);
//        this.insert("nested");
//
//        this.transactionManager.rollBack(nested);
//        this.transactionManager.commit(outer);
//
//        assertEquals(1, this.count("outer"));
//        assertEquals(0, this.count("nested"));
//    }
//
//    @Test
//    public void rollbackOnlyAndReadOnlyRollbackWork() throws Exception {
//        TransactionStatus rollbackOnly = this.transactionManager.begin();
//        this.insert("rollbackOnly");
//        rollbackOnly.setRollback();
//        this.transactionManager.commit(rollbackOnly);
//
//        TransactionStatus readOnly = this.transactionManager.begin();
//        this.insert("readOnly");
//        readOnly.setReadOnly();
//        this.transactionManager.commit(readOnly);
//
//        assertEquals(0, this.count("rollbackOnly"));
//        assertEquals(0, this.count("readOnly"));
//    }
//
//    @Test
//    public void isolationAndTopStatusAreManaged() throws Exception {
//        assertSame(this.connectionManager.getTransactionTemplate(null), this.connectionManager.getTransactionTemplate(null));
//        TransactionStatus status = this.transactionManager.begin(Propagation.REQUIRED, Isolation.SERIALIZABLE);
//        assertTrue(this.transactionManager.hasTransaction());
//        assertTrue(this.transactionManager.isTopTransaction(status));
//        try (Connection connection = this.connectionManager.getConnection(null)) {
//            assertEquals(Connection.TRANSACTION_SERIALIZABLE, connection.getTransactionIsolation());
//        }
//
//        this.transactionManager.commit();
//
//        assertTrue(status.isCompleted());
//        assertFalse(this.transactionManager.hasTransaction());
//    }
//
//    private void insert(String label) throws Exception {
//        try (Connection connection = this.connectionManager.getConnection(null); Statement statement = connection.createStatement()) {
//            statement.executeUpdate("INSERT INTO tx_item (label) VALUES ('" + label + "')");
//        }
//    }
//
//    private int count(String label) throws Exception {
//        try (Connection connection = this.rawConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM tx_item WHERE label = '" + label + "'")) {
//            resultSet.next();
//            return resultSet.getInt(1);
//        }
//    }
//
//    private Connection rawConnection() throws SQLException {
//        return DriverManager.getConnection(this.jdbcUrl, "sa", "");
//    }
//}
