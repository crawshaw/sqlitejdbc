package org.sqlite;

import java.sql.*;

class MetaData implements DatabaseMetaData
{
    private final Conn conn;

    MetaData(Conn conn) { this.conn = conn; }

    public Connection getConnection() { return conn; }

    public int getDatabaseMajorVersion() { return 3; }
    public int getDatabaseMinorVersion() { return 0; }
    public int getDriverMajorVersion() { return 1; }
    public int getDriverMinorVersion() { return 1; }
    public int getJDBCMajorVersion() { return 2; }
    public int getJDBCMinorVersion() { return 1; }
    public int getDefaultTransactionIsolation()
        { return Connection.TRANSACTION_SERIALIZABLE; }
    public int getMaxBinaryLiteralLength() { return 0; }
    public int getMaxCatalogNameLength() { return 0; }
    public int getMaxCharLiteralLength() { return 0; }
    public int getMaxColumnNameLength() { return 0; }
    public int getMaxColumnsInGroupBy() { return 0; }
    public int getMaxColumnsInIndex() { return 0; }
    public int getMaxColumnsInOrderBy() { return 0; }
    public int getMaxColumnsInSelect() { return 0; }
    public int getMaxColumnsInTable() { return 0; }
    public int getMaxConnections() { return 0; }
    public int getMaxCursorNameLength() { return 0; }
    public int getMaxIndexLength() { return 0; }
    public int getMaxProcedureNameLength() { return 0; }
    public int getMaxRowSize() { return 0; }
    public int getMaxSchemaNameLength() { return 0; }
    public int getMaxStatementLength() { return 0; }
    public int getMaxStatements() { return 0; }
    public int getMaxTableNameLength() { return 0; }
    public int getMaxTablesInSelect() { return 0; }
    public int getMaxUserNameLength() { return 0; }
    public int getResultSetHoldability()
        { return ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    public int getSQLStateType() { return sqlStateSQL99; }

    public String getDatabaseProductName() { return "SQLite"; }
    public String getDatabaseProductVersion() { return conn.libversion(); }
    public String getDriverName() { return "SQLiteJDBC"; }
    public String getDriverVersion() { return "1"; }
    public String getExtraNameCharacters() { return ""; }
    public String getCatalogSeparator() { return "."; }
    public String getCatalogTerm() { return "catalog"; }
    public String getSchemaTerm() { return "schema"; }
    public String getProcedureTerm() { return "not_implemented"; }
    public String getSearchStringEscape() { return null; }
    public String getIdentifierQuoteString() { return " "; }
    public String getSQLKeywords() { return ""; }
    public String getNumericFunctions() { return ""; }
    public String getStringFunctions() { return ""; }
    public String getSystemFunctions() { return ""; }
    public String getTimeDateFunctions() { return ""; }

    public String getURL() { return null; } // TODO: save url
    public String getUserName() { return null; }

    public boolean allProceduresAreCallable() { return false; }
    public boolean allTablesAreSelectable() { return true; }
    public boolean dataDefinitionCausesTransactionCommit() { return false; }
    public boolean dataDefinitionIgnoredInTransactions() { return false; }
    public boolean doesMaxRowSizeIncludeBlobs() { return false; }
    public boolean deletesAreDetected(int type) { return false; }
    public boolean insertsAreDetected(int type) { return false; }
    public boolean isCatalogAtStart() { return true; }
    public boolean locatorsUpdateCopy() { return false; }
    public boolean nullPlusNonNullIsNull() { return true; }
    public boolean nullsAreSortedAtEnd() { return !nullsAreSortedAtStart(); }
    public boolean nullsAreSortedAtStart() { return true; }
    public boolean nullsAreSortedHigh() { return true; }
    public boolean nullsAreSortedLow() { return !nullsAreSortedHigh(); }
    public boolean othersDeletesAreVisible(int type) { return false; }
    public boolean othersInsertsAreVisible(int type) { return false; }
    public boolean othersUpdatesAreVisible(int type) { return false; }
    public boolean ownDeletesAreVisible(int type) { return false; }
    public boolean ownInsertsAreVisible(int type) { return false; }
    public boolean ownUpdatesAreVisible(int type) { return false; }
    public boolean storesLowerCaseIdentifiers() { return false; }
    public boolean storesLowerCaseQuotedIdentifiers() { return false; }
    public boolean storesMixedCaseIdentifiers() { return true; }
    public boolean storesMixedCaseQuotedIdentifiers() { return false; }
    public boolean storesUpperCaseIdentifiers() { return false; }
    public boolean storesUpperCaseQuotedIdentifiers() { return false; }
    public boolean supportsAlterTableWithAddColumn() { return false; }
    public boolean supportsAlterTableWithDropColumn() { return false; }
    public boolean supportsANSI92EntryLevelSQL() { return false; }
    public boolean supportsANSI92FullSQL() { return false; }
    public boolean supportsANSI92IntermediateSQL() { return false; }
    public boolean supportsBatchUpdates() { return true; }
    public boolean supportsCatalogsInDataManipulation() { return false; }
    public boolean supportsCatalogsInIndexDefinitions() { return false; }
    public boolean supportsCatalogsInPrivilegeDefinitions() { return false; }
    public boolean supportsCatalogsInProcedureCalls() { return false; }
    public boolean supportsCatalogsInTableDefinitions() { return false; }
    public boolean supportsColumnAliasing() { return true; }
    public boolean supportsConvert() { return false; }
    public boolean supportsConvert(int fromType, int toType) { return false; }
    public boolean supportsCorrelatedSubqueries() { return false; }
    public boolean supportsDataDefinitionAndDataManipulationTransactions()
        { return true; }
    public boolean supportsDataManipulationTransactionsOnly() { return false; }
    public boolean supportsDifferentTableCorrelationNames() { return false; }
    public boolean supportsExpressionsInOrderBy() { return true; }
    public boolean supportsMinimumSQLGrammar() { return true; }
    public boolean supportsCoreSQLGrammar() { return true; }
    public boolean supportsExtendedSQLGrammar() { return false; }
    public boolean supportsLimitedOuterJoins() { return true; }
    public boolean supportsFullOuterJoins() { return false; }
    public boolean supportsGetGeneratedKeys() { return false; }
    public boolean supportsGroupBy() { return true; }
    public boolean supportsGroupByBeyondSelect() { return false; }
    public boolean supportsGroupByUnrelated() { return false; }
    public boolean supportsIntegrityEnhancementFacility() { return false; }
    public boolean supportsLikeEscapeClause() { return false; }
    public boolean supportsMixedCaseIdentifiers() { return true; }
    public boolean supportsMixedCaseQuotedIdentifiers() { return false; }
    public boolean supportsMultipleOpenResults() { return false; }
    public boolean supportsMultipleResultSets() { return false; }
    public boolean supportsMultipleTransactions() { return true; }
    public boolean supportsNamedParameters() { return true; }
    public boolean supportsNonNullableColumns() { return true; }
    public boolean supportsOpenCursorsAcrossCommit() { return false; }
    public boolean supportsOpenCursorsAcrossRollback() { return false; }
    public boolean supportsOpenStatementsAcrossCommit() { return false; }
    public boolean supportsOpenStatementsAcrossRollback() { return false; }
    public boolean supportsOrderByUnrelated() { return false; }
    public boolean supportsOuterJoins() { return true; }
    public boolean supportsPositionedDelete() { return false; }
    public boolean supportsPositionedUpdate() { return false; }
    public boolean supportsResultSetConcurrency(int t, int c)
        { return t == ResultSet.TYPE_FORWARD_ONLY
              && c == ResultSet.CONCUR_READ_ONLY; }
    public boolean supportsResultSetHoldability(int h)
        { return h == ResultSet.CLOSE_CURSORS_AT_COMMIT; }
    public boolean supportsResultSetType(int t)
        { return t == ResultSet.TYPE_FORWARD_ONLY; }
    public boolean supportsSavepoints() { return false; }
    public boolean supportsSchemasInDataManipulation() { return false; }
    public boolean supportsSchemasInIndexDefinitions() { return false; }
    public boolean supportsSchemasInPrivilegeDefinitions() { return false; }
    public boolean supportsSchemasInProcedureCalls() { return false; }
    public boolean supportsSchemasInTableDefinitions() { return false; }
    public boolean supportsSelectForUpdate() { return false; }
    public boolean supportsStatementPooling() { return false; }
    public boolean supportsStoredProcedures() { return false; }
    public boolean supportsSubqueriesInComparisons() { return false; }
    public boolean supportsSubqueriesInExists() { return true; } // TODO: check
    public boolean supportsSubqueriesInIns() { return true; } // TODO: check
    public boolean supportsSubqueriesInQuantifieds() { return false; }
    public boolean supportsTableCorrelationNames() { return false; }
    public boolean supportsTransactionIsolationLevel(int level)
        { return level == Connection.TRANSACTION_SERIALIZABLE; }
    public boolean supportsTransactions() { return true; }
    public boolean supportsUnion() { return true; }
    public boolean supportsUnionAll() { return true; }
    public boolean updatesAreDetected(int type) { return false; }
    public boolean usesLocalFilePerTable() { return false; }
    public boolean usesLocalFiles() { return true; }
    public boolean isReadOnly() throws SQLException
        { return conn.isReadOnly(); }

    public ResultSet getAttributes(String c, String s, String t, String a)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getBestRowIdentifier(String c, String s, String t,
                                          int scope, boolean n)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getCatalogs()
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getColumnPrivileges(String c, String s, String t,
                                         String colPat)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getColumns(String c, String s, String t, String colPat)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getCrossReference(String pc, String ps, String pt,
                                       String fc, String fs, String ft)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getSchemas()
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getPrimaryKeys(String c, String s, String t)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getExportedKeys(String c, String s, String t)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getImportedKeys(String c, String s, String t)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getIndexInfo(String c, String s, String t,
                                  boolean u, boolean approximate)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getProcedureColumns(String c, String s, String p,
                                         String colPat)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getProcedures(String c, String s, String p)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getSuperTables(String c, String s, String t)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getSuperTypes(String c, String s, String t)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getTablePrivileges(String c, String s, String t)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getTables(String c, String s, String t, String[] types)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getTableTypes()
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getTypeInfo()
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getUDTs(String c, String s, String t, int[] types)
        throws SQLException { throw new SQLException("not yet implemented"); }
    public ResultSet getVersionColumns(String c, String s, String t)
        throws SQLException { throw new SQLException("not yet implemented"); }
}
