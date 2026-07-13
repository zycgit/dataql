package net.hasor.dataql.sqlproc.execute.transaction;

import java.sql.*;
import java.util.UUID;
import net.hasor.dataql.sqlproc.execute.support.ConnectionProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TransactionManagerImplTest {
    private String                        jdbcUrl;
    private TransactionConnectionProvider connectionProvider;
    private TransactionManager            transactionManager;

    @Before
    public void setupDatabase() throws Exception {
        this.jdbcUrl = "jdbc:h2:mem:manager_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=2000";
        ConnectionProvider provider = (name, hints) -> this.rawConnection();
        this.connectionProvider = new TransactionConnectionProvider(provider);
        this.transactionManager = this.connectionProvider.findTransactionManager(null);
        try (Connection connection = this.rawConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE tx_item (id BIGINT AUTO_INCREMENT PRIMARY KEY, label VARCHAR(100))");
        }
    }

    @After
    public void closeTransactionProvider() throws Exception {
        if (this.connectionProvider != null) {
            this.connectionProvider.close();
        }
    }

    @Test
    public void requiredJoinsOuterTransaction() throws Exception {
        TransactionStatus outer = this.begin(Propagation.REQUIRED);
        this.insert("outer");
        TransactionStatus inner = this.begin(Propagation.REQUIRED);
        this.insert("inner");

        this.transactionManager.commit(inner);
        assertEquals(0, this.count("outer"));
        assertEquals(0, this.count("inner"));

        this.transactionManager.commit(outer);
        assertEquals(1, this.count("outer"));
        assertEquals(1, this.count("inner"));
        assertTrue(outer.isCompleted());
        assertTrue(inner.isCompleted());
    }

    @Test
    public void requiredInnerRollbackRollsBackWholeTransaction() throws Exception {
        TransactionStatus outer = this.begin(Propagation.REQUIRED);
        this.insert("outer");
        TransactionStatus inner = this.begin(Propagation.REQUIRED);
        this.insert("inner");

        this.transactionManager.rollBack(inner);
        this.transactionManager.rollBack(outer);

        assertEquals(0, this.count("outer"));
        assertEquals(0, this.count("inner"));
    }

    @Test
    public void requiresNewCommitsIndependentlyFromOuterRollback() throws Exception {
        TransactionStatus outer = this.begin(Propagation.REQUIRED);
        TransactionStatus inner = this.begin(Propagation.REQUIRES_NEW);
        this.insert("inner");

        this.transactionManager.commit(inner);
        this.transactionManager.rollBack(outer);

        assertEquals(1, this.count("inner"));
        assertTrue(inner.isCompleted());
        assertTrue(outer.isCompleted());
    }

    @Test
    public void nestedRollbackKeepsOuterTransaction() throws Exception {
        TransactionStatus outer = this.begin(Propagation.REQUIRED);
        this.insert("outer");
        TransactionStatus nested = this.begin(Propagation.NESTED);
        this.insert("nested");

        this.transactionManager.rollBack(nested);
        this.transactionManager.commit(outer);

        assertEquals(1, this.count("outer"));
        assertEquals(0, this.count("nested"));
    }

    @Test
    public void supportsUsesExistingTransactionAndAutocommitsWithoutOne() throws Exception {
        TransactionStatus outer = this.begin(Propagation.REQUIRED);
        this.insert("outer");
        TransactionStatus supports = this.begin(Propagation.SUPPORTS);
        this.insert("supports-inner");

        this.transactionManager.commit(supports);
        this.transactionManager.rollBack(outer);

        assertEquals(0, this.count("outer"));
        assertEquals(0, this.count("supports-inner"));

        TransactionStatus noTransaction = this.begin(Propagation.SUPPORTS);
        this.insert("supports-alone");
        this.transactionManager.rollBack(noTransaction);

        assertEquals(1, this.count("supports-alone"));
    }

    @Test
    public void notSupportedSuspendsOuterAndAutocommitsInnerWork() throws Exception {
        TransactionStatus outer = this.begin(Propagation.REQUIRED);
        this.insert("outer");
        TransactionStatus notSupported = this.begin(Propagation.NOT_SUPPORTED);
        this.insert("not-supported");

        this.transactionManager.rollBack(notSupported);
        this.transactionManager.rollBack(outer);

        assertEquals(0, this.count("outer"));
        assertEquals(1, this.count("not-supported"));
    }

    @Test
    public void neverAutocommitsWithoutTransactionAndFailsInsideOne() throws Exception {
        TransactionStatus noTransaction = this.begin(Propagation.NEVER);
        this.insert("never-alone");
        this.transactionManager.rollBack(noTransaction);
        assertEquals(1, this.count("never-alone"));

        TransactionStatus outer = this.begin(Propagation.REQUIRED);
        try {
            this.begin(Propagation.NEVER);
            fail("NEVER should fail inside an existing transaction.");
        } catch (SQLException e) {
            assertTrue(e.getMessage().contains("Existing transaction"));
        } finally {
            this.transactionManager.rollBack(outer);
        }
    }

    @Test
    public void mandatoryFailsWithoutTransactionAndJoinsExistingOne() throws Exception {
        try {
            this.begin(Propagation.MANDATORY);
            fail("MANDATORY should fail without an existing transaction.");
        } catch (SQLException e) {
            assertTrue(e.getMessage().contains("No existing transaction"));
        }

        TransactionStatus outer = this.begin(Propagation.REQUIRED);
        TransactionStatus mandatory = this.begin(Propagation.MANDATORY);
        this.insert("mandatory");

        this.transactionManager.commit(mandatory);
        this.transactionManager.commit(outer);

        assertEquals(1, this.count("mandatory"));
        assertTrue(mandatory.isCompleted());
    }

    @Test
    public void isolationAndTopStatusAreManaged() throws Exception {
        TransactionStatus status = this.transactionManager.begin(null, Propagation.REQUIRED, Isolation.SERIALIZABLE);
        assertTrue(this.transactionManager.isTopTransaction(status));
        try (Connection connection = this.connectionProvider.findConnection(null, null)) {
            assertEquals(Connection.TRANSACTION_SERIALIZABLE, connection.getTransactionIsolation());
        }

        this.transactionManager.commit(status);

        assertTrue(status.isCompleted());
        assertFalse(this.transactionManager.isTopTransaction(status));
    }

    @Test
    public void rollbackOnlyAndReadOnlyRollbackWork() throws Exception {
        TransactionStatus rollbackOnly = this.begin(Propagation.REQUIRED);
        this.insert("rollbackOnly");
        rollbackOnly.setRollback();
        this.transactionManager.commit(rollbackOnly);

        TransactionStatus readOnly = this.begin(Propagation.REQUIRED);
        this.insert("readOnly");
        readOnly.setReadOnly();
        this.transactionManager.commit(readOnly);

        assertEquals(0, this.count("rollbackOnly"));
        assertEquals(0, this.count("readOnly"));
    }

    private TransactionStatus begin(Propagation propagation) throws SQLException {
        return this.transactionManager.begin(null, propagation, Isolation.DEFAULT);
    }

    private void insert(String label) throws Exception {
        try (Connection connection = this.connectionProvider.findConnection(null, null); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO tx_item (label) VALUES ('" + label + "')");
        }
    }

    private int count(String label) throws Exception {
        try (Connection connection = this.rawConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM tx_item WHERE label = '" + label + "'")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private Connection rawConnection() throws SQLException {
        return DriverManager.getConnection(this.jdbcUrl, "sa", "");
    }
}
