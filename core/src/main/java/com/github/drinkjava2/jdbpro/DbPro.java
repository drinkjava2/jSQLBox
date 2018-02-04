/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.drinkjava2.jdbpro;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.OutParameter;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import com.github.drinkjava2.jdbpro.inline.InlineQueryRunner;
import com.github.drinkjava2.jdbpro.template.SqlTemplateEngine;
import com.github.drinkjava2.jdbpro.template.TemplateQueryRunner;
import com.github.drinkjava2.jtransactions.ConnectionManager;

/**
 * DbPro is the enhanced version of Apache Commons DbUtils's QueryRunner, add
 * below improvements:
 * 
 * <pre>
 * 1)Use ConnectionManager to manage connection for better transaction support
 * 2)normal style methods but no longer throw SQLException, methods named as nXxxxx() format
 * 3)In-line style methods, methods named as iXxxxx() format
 * 4)SQL Template style methods, methods named as tXxxxx() format
 * </pre>
 * 
 * @author Yong Zhu
 * @since 1.7.0
 */
public class DbPro extends TemplateQueryRunner implements NormalJdbcTool {
	public DbPro() {
		super();
	}

	public DbPro(DataSource ds) {
		super(ds);
	}

	public DbPro(SqlTemplateEngine templateEngine) {
		super(templateEngine);
	}

	public DbPro(DataSource ds, Object... args) {
		super(ds);
		for (Object arg : args) {
			if (arg instanceof ConnectionManager)
				this.connectionManager = (ConnectionManager) arg;
			else if (arg instanceof SqlTemplateEngine)
				this.sqlTemplateEngine = (SqlTemplateEngine) arg;
			else if (arg instanceof DbProLogger)
				this.logger = (DbProLogger) arg;
		}
	}

	/**
	 * Clear all In-Line parameters and Template parameters stored in ThreadLocal
	 */
	public static void clearAll() {
		TemplateQueryRunner.clearBind();
		InlineQueryRunner.clearParams();
	}

	// ==========================================================
	// DbUtils style methods, throw SQLException

	/**
	 * Query for an Object, only return the first row and first column's value if
	 * more than one column or more than 1 rows returned, a null object may return
	 * if no result found, SQLException may be threw if some SQL operation Exception
	 * happen. Note: This method does not close connection.
	 * 
	 * @param sql
	 *            The SQL
	 * @param params
	 *            The parameters
	 * @return An Object or null, Object type determined by SQL content
	 * @throws SQLException
	 */
	public <T> T queryForObject(Connection conn, String sql, Object... params) throws SQLException {
		return query(conn, sql, new ScalarHandler<T>(1), params);
	}

	/**
	 * Query for an Object, only return the first row and first column's value if
	 * more than one column or more than 1 rows returned, a null object may return
	 * if no result found, SQLException may be threw if some SQL operation Exception
	 * happen.
	 * 
	 * @param sql
	 *            The SQL
	 * @param params
	 *            The parameters
	 * @return An Object or null, Object type determined by SQL content
	 * @throws SQLException
	 */
	public <T> T queryForObject(String sql, Object... params) throws SQLException {
		return query(sql, new ScalarHandler<T>(1), params);
	}

	/**
	 * Execute query and force return a String object, Note: This method does not
	 * close connection.
	 * 
	 * @param sql
	 *            The SQL
	 * @param params
	 *            The parameters
	 * @throws SQLException
	 */
	public String queryForString(Connection conn, String sql, Object... params) throws SQLException {
		return String.valueOf(queryForObject(conn, sql, params));
	}

	/**
	 * Execute query and force return a String object
	 * 
	 * @param sql
	 *            The SQL
	 * @param params
	 *            The parameters
	 * @throws SQLException
	 */
	public String queryForString(String sql, Object... params) throws SQLException {
		return String.valueOf(queryForObject(sql, params));
	}

	/**
	 * Execute query and force return a Long object, runtime exception may throw if
	 * result can not be cast to long, SQLException may throw if SQL exception
	 * happen, Note: This method does not close connection.
	 * 
	 * @param sql
	 *            The SQL
	 * @param params
	 *            The parameters
	 * @throws SQLException
	 */
	public long queryForLongValue(Connection conn, String sql, Object... params) throws SQLException {
		return ((Number) queryForObject(conn, sql, params)).longValue();// NOSONAR
	}

	/**
	 * Execute query and force return a Long object, runtime exception may throw if
	 * result can not be cast to long, SQLException may throw if SQL exception
	 * happen
	 * 
	 * @param sql
	 *            The SQL
	 * @param params
	 *            The parameters
	 * @throws SQLException
	 */
	public long queryForLongValue(String sql, Object... params) throws SQLException {
		return ((Number) queryForObject(sql, params)).longValue();// NOSONAR
	}

	/**
	 * Execute a query and wrap result to Map List, Note: This method does not close
	 * connection.
	 * 
	 * @param sql
	 *            The SQL String
	 * @param params
	 *            The parameters
	 * @return A MapList result
	 * @throws SQLException
	 */
	public List<Map<String, Object>> queryForMapList(Connection conn, String sql, Object... params)
			throws SQLException {
		return query(conn, sql, new MapListHandler(), params);
	}

	/**
	 * Execute a query and wrap result to Map List
	 * 
	 * @param sql
	 *            The SQL String
	 * @param params
	 *            The parameters
	 * @return A MapList result
	 * @throws SQLException
	 */
	public List<Map<String, Object>> queryForMapList(String sql, Object... params) throws SQLException {
		return query(sql, new MapListHandler(), params);
	}

	// =======================================================================
	// Normal style methods but transfer SQLException to DbProRuntimeException

	/**
	 * Executes the given SELECT SQL query and returns a result object. Transaction
	 * mode is determined by connectionManager property. Note: this method does not
	 * close connection
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param sql
	 *            the SQL
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code>.
	 * @param params
	 *            the parameters if have
	 * @return An object generated by the handler.
	 * 
	 */
	public <T> T nQuery(Connection conn, ResultSetHandler<T> rsh, String sql, Object... params) {
		try {
			return query(conn, sql, rsh, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Query for an Object, only return the first row and first column's value if
	 * more than one column or more than 1 rows returned, a null object may return
	 * if no result found , DbProRuntimeException may be threw if some SQL operation
	 * Exception happen. Note: This method does not close connection.
	 * 
	 * @param sql
	 * @param params
	 * @return An Object or null, Object type determined by SQL content
	 */
	public <T> T nQueryForObject(Connection conn, String sql, Object... params) {
		return nQuery(conn, new ScalarHandler<T>(1), sql, params);
	}

	/**
	 * Execute query and force return a String object, no need catch SQLException.
	 * Note: This method does not close connection.
	 */
	public String nQueryForString(Connection conn, String sql, Object... params) {
		return String.valueOf(nQueryForObject(conn, sql, params));
	}

	/**
	 * Execute query and force return a Long object, no need catch SQLException,
	 * runtime exception may throw if result can not be cast to long. Note: This
	 * method does not close connection.
	 */
	public long nQueryForLongValue(Connection conn, String sql, Object... params) {
		try {
			return queryForLongValue(conn, sql, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute query and force return a List<Map<String, Object>> type result, no
	 * need catch SQLException. Note: This method does not close connection.
	 */
	public List<Map<String, Object>> nQueryForMapList(Connection conn, String sql, Object... params) {
		try {
			return query(conn, sql, new MapListHandler(), params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the given INSERT, UPDATE, or DELETE SQL statement. Note: This method
	 * does not close connection.
	 * 
	 * @param sql
	 *            the SQL
	 * @param params
	 *            the parameters if have
	 * @return The number of rows updated.
	 */
	public int nUpdate(Connection conn, String sql, Object... params) {
		try {
			return update(conn, sql, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the given INSERT SQL statement. Note: This method does not close
	 * connection.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param sql
	 *            the SQL
	 * @param params
	 *            the parameters if have
	 * @return An object generated by the handler.
	 * 
	 */
	public <T> T nInsert(Connection conn, ResultSetHandler<T> rsh, String sql, Object... params) {
		try {
			return insert(conn, sql, rsh, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the given Batch INSERT SQL statement. Note: This method does not
	 * close connection.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param sql
	 *            the SQL
	 * @param params
	 *            the parameter array
	 * @return An object generated by the handler.
	 */
	public <T> T nInsertBatch(Connection conn, ResultSetHandler<T> rsh, String sql, List<List<?>> paramList) {
		try {
			return insertBatch(conn, sql, rsh, paramList);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an statement, including a stored procedure call, which does not
	 * return any result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters. Note: This method
	 * does not close connection.
	 * <p>
	 * Use this method when invoking a stored procedure with OUT parameters that
	 * does not return any result sets.
	 * 
	 * @param sql
	 *            the SQL
	 * @return The number of rows updated.
	 */
	public int nExecute(Connection conn, String sql, Object... params) {
		try {
			return execute(conn, sql, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an statement, including a stored procedure call, which returns one or
	 * more result sets. Any parameters which are instances of {@link OutParameter}
	 * will be registered as OUT parameters. Note: This method does not close
	 * connection.
	 * 
	 * Use this method when: a) running SQL statements that return multiple result
	 * sets; b) invoking a stored procedure that return result sets and OUT
	 * parameters.
	 *
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The result set handler
	 * @param sql
	 *            the SQL
	 * @return A list of objects generated by the handler
	 * 
	 */
	public <T> List<T> nExecute(Connection conn, ResultSetHandler<T> rsh, String sql, Object... params) {
		try {
			return execute(conn, sql, rsh, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute a batch of SQL INSERT, UPDATE, or DELETE queries. The
	 * <code>Connection</code> is retrieved from the <code>DataSource</code> set in
	 * the constructor. Note: This method does not close connection.
	 *
	 * @param sql
	 *            The SQL to execute.
	 * @param params
	 *            An array of query replacement parameters. Each row in this array
	 *            is one set of batch replacement values.
	 * @return The number of rows updated per statement.
	 */
	public int[] nBatch(Connection conn, String sql, Object[][] params) {
		try {
			return batch(conn, sql, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the given batch of INSERT SQL statements. The
	 * <code>Connection</code> is retrieved from the <code>DataSource</code> set in
	 * the constructor. Note: This method does not close connection.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param sql
	 *            The SQL statement to execute.
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param params
	 *            Initializes the PreparedStatement's IN (i.e. '?')
	 * @return The result generated by the handler.
	 */
	public <T> T nInsertBatch(Connection conn, String sql, ResultSetHandler<T> rsh, Object[][] params) {
		try {
			return insertBatch(conn, sql, rsh, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the given SELECT SQL query and returns a result object. Transaction
	 * mode is determined by connectionManager property.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param sql
	 *            the SQL
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code>.
	 * @param params
	 *            the parameters if have
	 * @return An object generated by the handler.
	 * 
	 */
	public <T> T nQuery(ResultSetHandler<T> rsh, String sql, Object... params) {
		try {
			return query(sql, rsh, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Query for an Object, only return the first row and first column's value if
	 * more than one column or more than 1 rows returned, a null object may return
	 * if no result found , DbProRuntimeException may be threw if some SQL operation
	 * Exception happen.
	 * 
	 * @param sql
	 * @param params
	 * @return An Object or null, Object type determined by SQL content
	 */
	@Override
	public <T> T nQueryForObject(String sql, Object... params) {
		return nQuery(new ScalarHandler<T>(1), sql, params);
	}

	/**
	 * Execute query and force return a String object, no need catch SQLException
	 */
	public String nQueryForString(String sql, Object... params) {
		return String.valueOf(nQueryForObject(sql, params));
	}

	/**
	 * Execute query and force return a Long object, no need catch SQLException,
	 * runtime exception may throw if result can not be cast to long
	 */
	public long nQueryForLongValue(String sql, Object... params) {
		try {
			return queryForLongValue(sql, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute query and force return a List<Map<String, Object>> type result, no
	 * need catch SQLException
	 */
	public List<Map<String, Object>> nQueryForMapList(String sql, Object... params) {
		try {
			return query(sql, new MapListHandler(), params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the given INSERT, UPDATE, or DELETE SQL statement. Transaction mode
	 * is determined by connectionManager property.
	 * 
	 * @param sql
	 *            the SQL
	 * @param params
	 *            the parameters if have
	 * @return The number of rows updated.
	 */
	@Override
	public int nUpdate(String sql, Object... params) {
		try {
			return update(sql, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the given INSERT SQL statement. Transaction mode is determined by
	 * connectionManager property.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param sql
	 *            the SQL
	 * @param params
	 *            the parameters if have
	 * @return An object generated by the handler.
	 * 
	 */
	public <T> T nInsert(ResultSetHandler<T> rsh, String sql, Object... params) {
		try {
			return insert(sql, rsh, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the given Batch INSERT SQL statement. Transaction mode is determined
	 * by connectionManager property.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param sql
	 *            the SQL
	 * @param params
	 *            the parameter array
	 * @return An object generated by the handler.
	 */
	public <T> T nInsertBatch(ResultSetHandler<T> rsh, String sql, List<List<?>> paramList) {
		try {
			return insertBatch(sql, rsh, paramList);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an statement, including a stored procedure call, which does not
	 * return any result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters. Transaction mode
	 * is determined by connectionManager property.
	 * <p>
	 * Use this method when invoking a stored procedure with OUT parameters that
	 * does not return any result sets.
	 * 
	 * @param sql
	 *            the SQL
	 * @return The number of rows updated.
	 */
	@Override
	public int nExecute(String sql, Object... params) {
		try {
			return execute(sql, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an statement, including a stored procedure call, which returns one or
	 * more result sets. Any parameters which are instances of {@link OutParameter}
	 * will be registered as OUT parameters. Transaction mode is determined by
	 * connectionManager property.
	 * 
	 * Use this method when: a) running SQL statements that return multiple result
	 * sets; b) invoking a stored procedure that return result sets and OUT
	 * parameters.
	 *
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The result set handler
	 * @param sql
	 *            the SQL
	 * @return A list of objects generated by the handler
	 * 
	 */
	public <T> List<T> nExecute(ResultSetHandler<T> rsh, String sql, Object... params) {
		try {
			return execute(sql, rsh, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute a batch of SQL INSERT, UPDATE, or DELETE queries. The
	 * <code>Connection</code> is retrieved from the <code>DataSource</code> set in
	 * the constructor. Transaction mode is determined by connectionManager
	 * property.
	 *
	 * @param sql
	 *            The SQL to execute.
	 * @param params
	 *            An array of query replacement parameters. Each row in this array
	 *            is one set of batch replacement values.
	 * @return The number of rows updated per statement.
	 */
	public int[] nBatch(String sql, Object[][] params) {
		try {
			return batch(sql, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the given batch of INSERT SQL statements. The
	 * <code>Connection</code> is retrieved from the <code>DataSource</code> set in
	 * the constructor. Transaction mode is determined by connectionManager
	 * property.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param sql
	 *            The SQL statement to execute.
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param params
	 *            Initializes the PreparedStatement's IN (i.e. '?')
	 * @return The result generated by the handler.
	 */
	public <T> T nInsertBatch(String sql, ResultSetHandler<T> rsh, Object[][] params) {
		try {
			return insertBatch(sql, rsh, params);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	// ====================================================================
	// In-Line style and transfer SQLException to DbProRuntimeException
	/**
	 * Executes the given SELECT SQL query and returns a result object. Note: this
	 * method does not close connection
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code>.
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return An object generated by the handler.
	 * 
	 */
	public <T> T iQuery(Connection conn, ResultSetHandler<T> rsh, String... inlineSQL) {
		try {
			return this.inlineQuery(conn, rsh, inlineSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an In-line style query for an Object, only return the first row and
	 * first column's value if more than one column or more than 1 rows returned, a
	 * null object may return if no result found , DbProRuntimeException may be
	 * threw if some SQL operation Exception happen. Note: this method does not
	 * close connection
	 * 
	 * @param sql
	 * @param params
	 * @return An Object or null, Object type determined by SQL content
	 */
	public <T> T iQueryForObject(Connection conn, String... inlineSQL) {
		return iQuery(conn, new ScalarHandler<T>(1), inlineSQL);
	}

	/**
	 * In-line style execute query and force return a String object. Note: This
	 * method does not close connection.
	 */
	public String iQueryForString(Connection conn, String... inlineSQL) {
		return String.valueOf(iQueryForObject(conn, inlineSQL));
	}

	/**
	 * In-line style execute query and force return a long value, runtime exception
	 * may throw if result can not be cast to long. Note: this method does not close
	 * connection
	 */
	public long iQueryForLongValue(Connection conn, String... inlineSQL) {
		return ((Number) iQueryForObject(conn, inlineSQL)).longValue();// NOSONAR
	}

	/**
	 * In-Line style execute query and force return a List<Map<String, Object>> type
	 * result. Note: this method does not close connection
	 */
	public List<Map<String, Object>> iQueryForMapList(Connection conn, String... inlineSQL) {
		return iQuery(conn, new MapListHandler(), inlineSQL);
	}

	/**
	 * Executes the given INSERT, UPDATE, or DELETE SQL statement. Note: this method
	 * does not close connection
	 * 
	 * @param inlineSQL
	 *            the in-line style SQL *
	 * @return The number of rows updated.
	 */
	public int iUpdate(Connection conn, String... inlineSQL) {
		try {
			return this.inlineUpdate(conn, inlineSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the given INSERT SQL statement. Note: this method does not close
	 * connection
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return An object generated by the handler.
	 * 
	 */
	public <T> T iInsert(Connection conn, ResultSetHandler<T> rsh, String... inlineSQL) {
		try {
			return this.inlineInsert(conn, rsh, inlineSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an statement, including a stored procedure call, which does not
	 * return any result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters. Note: this method
	 * does not close connection
	 * <p>
	 * Use this method when invoking a stored procedure with OUT parameters that
	 * does not return any result sets.
	 *
	 * @param inlineSQL
	 *            the in-line style SQL.
	 * @return The number of rows updated.
	 */
	public int iExecute(Connection conn, String... inlineSQL) {
		try {
			return this.inlineExecute(conn, inlineSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an statement, including a stored procedure call, which returns one or
	 * more result sets. Any parameters which are instances of {@link OutParameter}
	 * will be registered as OUT parameters. Note: this method does not close
	 * connection
	 * <p>
	 * Use this method when: a) running SQL statements that return multiple result
	 * sets; b) invoking a stored procedure that return result sets and OUT
	 * parameters.
	 *
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The result set handler
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return A list of objects generated by the handler
	 */
	public <T> List<T> iExecute(Connection conn, ResultSetHandler<T> rsh, String... inlineSQL) {
		try {
			return this.inlineExecute(conn, rsh, inlineSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the given SELECT SQL query and returns a result object.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code>.
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return An object generated by the handler.
	 * 
	 */
	public <T> T iQuery(ResultSetHandler<T> rsh, String... inlineSQL) {
		try {
			return this.inlineQuery(rsh, inlineSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an In-line style query for an Object, only return the first row and
	 * first column's value if more than one column or more than 1 rows returned, a
	 * null object may return if no result found , DbProRuntimeException may be
	 * threw if some SQL operation Exception happen.
	 * 
	 * @param sql
	 * @param params
	 * @return An Object or null, Object type determined by SQL content
	 */
	public <T> T iQueryForObject(String... inlineSQL) {
		return iQuery(new ScalarHandler<T>(1), inlineSQL);
	}

	/** In-line style execute query and force return a String object */
	public String iQueryForString(String... inlineSQL) {
		return String.valueOf(iQueryForObject(inlineSQL));
	}

	/**
	 * In-line style execute query and force return a long value, runtime exception
	 * may throw if result can not be cast to long
	 */
	public long iQueryForLongValue(String... inlineSQL) {
		return ((Number) iQueryForObject(inlineSQL)).longValue();// NOSONAR
	}

	/**
	 * In-Line style execute query and force return a List<Map<String, Object>> type
	 * result
	 */
	public List<Map<String, Object>> iQueryForMapList(String... inlineSQL) {
		return iQuery(new MapListHandler(), inlineSQL);
	}

	/**
	 * Executes the given INSERT, UPDATE, or DELETE SQL statement. Transaction mode
	 * is determined by connectionManager property.
	 * 
	 * @param inlineSQL
	 *            the in-line style SQL *
	 * @return The number of rows updated.
	 */
	public int iUpdate(String... inlineSQL) {
		try {
			return this.inlineUpdate(inlineSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the given INSERT SQL statement. Transaction mode is determined by
	 * connectionManager property.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return An object generated by the handler.
	 * 
	 */
	public <T> T iInsert(ResultSetHandler<T> rsh, String... inlineSQL) {
		try {
			return this.inlineInsert(rsh, inlineSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an statement, including a stored procedure call, which does not
	 * return any result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters. Transaction mode
	 * is determined by connectionManager property.
	 * <p>
	 * Use this method when invoking a stored procedure with OUT parameters that
	 * does not return any result sets.
	 *
	 * @param inlineSQL
	 *            the in-line style SQL.
	 * @return The number of rows updated.
	 */
	public int iExecute(String... inlineSQL) {
		try {
			return this.inlineExecute(inlineSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an statement, including a stored procedure call, which returns one or
	 * more result sets. Any parameters which are instances of {@link OutParameter}
	 * will be registered as OUT parameters.Transaction mode is determined by
	 * connectionManager property.
	 * <p>
	 * Use this method when: a) running SQL statements that return multiple result
	 * sets; b) invoking a stored procedure that return result sets and OUT
	 * parameters.
	 *
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The result set handler
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return A list of objects generated by the handler
	 */
	public <T> List<T> iExecute(ResultSetHandler<T> rsh, String... inlineSQL) {
		try {
			return this.inlineExecute(rsh, inlineSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	// ====================================================================
	// SQL Template style and transfer SQLException to DbProRuntimeException
	/**
	 * Executes the Mixed-style(Inline+Template) given SELECT SQL query and returns
	 * a result object. Note: This method does not close connection.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code>.
	 * @param templateSQL
	 *            the SQL template
	 * @return An object generated by the handler.
	 */
	public <T> T xQuery(Connection conn, ResultSetHandler<T> rsh, String... templateSQL) {
		try {
			return this.mixedQuery(conn, rsh, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an Mixed-style(Inline+Template) SQL query for an Object, only return
	 * the first row and first column's value if more than one column or more than 1
	 * rows returned, a null object may return if no result found ,
	 * DbProRuntimeException may be threw if some SQL operation Exception happen.
	 * Note: This method does not close connection.
	 * 
	 * @param templateSQL
	 * @return An Object or null, Object type determined by SQL content
	 */
	public <T> T xQueryForObject(Connection conn, String... templateSQL) {
		return xQuery(conn, new ScalarHandler<T>(), templateSQL);
	}

	/**
	 * Executes the Mixed-style(Inline+Template) given INSERT, UPDATE, or DELETE SQL
	 * statement. Note: This method does not close connection.
	 * 
	 * @param templateSQL
	 *            the SQL template
	 * @return The number of rows updated.
	 */
	public int xUpdate(Connection conn, String... templateSQL) {
		try {
			return this.mixedUpdate(conn, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the Mixed-style(Inline+Template) given INSERT SQL statement. Note:
	 * This method does not close connection.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param templateSQL
	 *            the SQL template
	 * @return An object generated by the handler.
	 */
	public <T> T xInsert(Connection conn, ResultSetHandler<T> rsh, String... templateSQL) {
		try {
			return this.mixedInsert(conn, rsh, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an Mixed-style(Inline+Template) SQL statement, including a stored
	 * procedure call, which does not return any result sets. Any parameters which
	 * are instances of {@link OutParameter} will be registered as OUT parameters.
	 * Note: This method does not close connection.
	 * <p>
	 * Use this method when invoking a stored procedure with OUT parameters that
	 * does not return any result sets.
	 *
	 * @param templateSQL
	 *            the SQL template.
	 * @return The number of rows updated.
	 */
	public int xExecute(Connection conn, String... templateSQL) {
		try {
			return this.mixedExecute(conn, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an Mixed-style(Inline+Template) SQL statement, including a stored
	 * procedure call, which returns one or more result sets. Any parameters which
	 * are instances of {@link OutParameter} will be registered as OUT parameters.
	 * Note: This method does not close connection.
	 * <p>
	 * Use this method when: a) running SQL statements that return multiple result
	 * sets; b) invoking a stored procedure that return result sets and OUT
	 * parameters.
	 *
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The result set handler
	 * @param templateSQL
	 *            the SQL template
	 * @return A list of objects generated by the handler
	 */
	public <T> List<T> xExecute(Connection conn, ResultSetHandler<T> rsh, String... templateSQL) {
		try {
			return this.mixedExecute(conn, rsh, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	// ====================================================================
	// SQL Template style and transfer SQLException to DbProRuntimeException,
	// parameters carried by a Map<String, Object> instance

	/**
	 * Executes the template style given SELECT SQL query and returns a result
	 * object. Note: This method does not close connection
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code>.
	 * @param templateSQL
	 *            the SQL template
	 * @return An object generated by the handler.
	 */
	public <T> T tQuery(Connection conn, Map<String, Object> paramMap, ResultSetHandler<T> rsh, String... templateSQL) {
		try {
			return this.templateQuery(conn, paramMap, rsh, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an SQL Template query for an Object, only return the first row and
	 * first column's value if more than one column or more than 1 rows returned, a
	 * null object may return if no result found , DbProRuntimeException may be
	 * threw if some SQL operation Exception happen. Note: This method does not
	 * close connection
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param templateSQL
	 *            the SQL template
	 * @return An Object or null, Object type determined by SQL content
	 */
	public <T> T tQueryForObject(Connection conn, Map<String, Object> paramMap, String... templateSQL) {
		return tQuery(conn, paramMap, new ScalarHandler<T>(), templateSQL);
	}

	/**
	 * Template style execute SQL query, force return a String value. Note: This
	 * method does not close connection
	 */
	public String tQueryForString(Connection conn, Map<String, Object> paramMap, String... templateSQL) {
		return String.valueOf(tQueryForObject(conn, paramMap, templateSQL));
	}

	/**
	 * Mixed-style(Inline+Template) execute SQL query, force return a String value.
	 * Note: This method does not close connection
	 */
	public String xQueryForString(Connection conn, String... templateSQL) {
		return String.valueOf(xQueryForObject(conn, templateSQL));
	}

	/**
	 * Template style execute SQL query, force return a Long value, runtime
	 * Exception may throw if result can not cast to long. Note: This method does
	 * not close connection
	 */
	public long tQueryForLongValue(Connection conn, Map<String, Object> paramMap, String... templateSQL) {
		return ((Number) tQueryForObject(conn, paramMap, templateSQL)).longValue();// NOSONAR
	}

	/**
	 * Mixed-style(Inline+Template) execute SQL query, force return a Long value,
	 * runtime Exception may throw if result can not cast to long. Note: This method
	 * does not close connection
	 */
	public long xQueryForLongValue(Connection conn, String... templateSQL) {
		return ((Number) xQueryForObject(conn, templateSQL)).longValue();// NOSONAR
	}

	/**
	 * Mixed-style(Inline+Template) query and force return a List<Map<String,
	 * Object>> type result. Note: This method does not close connection
	 */
	public List<Map<String, Object>> xQueryForMapList(Connection conn, String... templateSQL) {
		return this.xQuery(conn, new MapListHandler(), templateSQL);
	}

	/**
	 * Template style query and force return a List<Map<String, Object>> type
	 * result. Note: This method does not close connection
	 */
	public List<Map<String, Object>> tQueryForMapList(Connection conn, Map<String, Object> paramMap,
			String... templateSQL) {
		return this.tQuery(conn, paramMap, new MapListHandler(), templateSQL);
	}

	/**
	 * Executes the template style given INSERT, UPDATE, or DELETE SQL statement.
	 * Note: This method does not close connection
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param templateSQL
	 *            the SQL template
	 * @return The number of rows updated.
	 */
	public int tUpdate(Connection conn, Map<String, Object> paramMap, String... templateSQL) {
		try {
			return this.templateUpdate(conn, paramMap, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the template style given INSERT SQL statement. Note: This method
	 * does not close connection
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param templateSQL
	 *            the SQL template
	 * @return An object generated by the handler.
	 */
	public <T> T tInsert(Connection conn, Map<String, Object> paramMap, ResultSetHandler<T> rsh,
			String... templateSQL) {
		try {
			return this.templateInsert(conn, paramMap, rsh, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an SQL template statement, including a stored procedure call, which
	 * does not return any result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters. Note: This method
	 * does not close connection
	 * <p>
	 * Use this method when invoking a stored procedure with OUT parameters that
	 * does not return any result sets.
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param templateSQL
	 *            the SQL template.
	 * @return The number of rows updated.
	 */
	public int tExecute(Connection conn, Map<String, Object> paramMap, String... templateSQL) {
		try {
			return this.templateExecute(conn, paramMap, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an SQL template statement, including a stored procedure call, which
	 * returns one or more result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters. Note: This method
	 * does not close connection
	 * <p>
	 * Use this method when: a) running SQL statements that return multiple result
	 * sets; b) invoking a stored procedure that return result sets and OUT
	 * parameters.
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The result set handler
	 * @param templateSQL
	 *            the SQL template
	 * @return A list of objects generated by the handler
	 */
	public <T> List<T> tExecute(Connection conn, Map<String, Object> paramMap, ResultSetHandler<T> rsh,
			String... templateSQL) {
		try {
			return this.templateExecute(conn, paramMap, rsh, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the Mixed-style(Inline+Template) given SELECT SQL query and returns
	 * a result object. Transaction mode is determined by connectionManager
	 * property.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code>.
	 * @param templateSQL
	 *            the SQL template
	 * @return An object generated by the handler.
	 */
	public <T> T xQuery(ResultSetHandler<T> rsh, String... templateSQL) {
		try {
			return this.mixedQuery(rsh, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an Mixed-style(Inline+Template) SQL query for an Object, only return
	 * the first row and first column's value if more than one column or more than 1
	 * rows returned, a null object may return if no result found ,
	 * DbProRuntimeException may be threw if some SQL operation Exception happen.
	 * 
	 * @param templateSQL
	 * @return An Object or null, Object type determined by SQL content
	 */
	public <T> T xQueryForObject(String... templateSQL) {
		return xQuery(new ScalarHandler<T>(), templateSQL);
	}

	/**
	 * Executes the Mixed-style(Inline+Template) given INSERT, UPDATE, or DELETE SQL
	 * statement. Transaction mode is determined by connectionManager property.
	 * 
	 * @param templateSQL
	 *            the SQL template
	 * @return The number of rows updated.
	 */
	public int xUpdate(String... templateSQL) {
		try {
			return this.mixedUpdate(templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the Mixed-style(Inline+Template) given INSERT SQL statement.
	 * Transaction mode is determined by connectionManager property.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param templateSQL
	 *            the SQL template
	 * @return An object generated by the handler.
	 */
	public <T> T xInsert(ResultSetHandler<T> rsh, String... templateSQL) {
		try {
			return this.mixedInsert(rsh, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an Mixed-style(Inline+Template) SQL statement, including a stored
	 * procedure call, which does not return any result sets. Any parameters which
	 * are instances of {@link OutParameter} will be registered as OUT parameters.
	 * Transaction mode is determined by connectionManager property.
	 * <p>
	 * Use this method when invoking a stored procedure with OUT parameters that
	 * does not return any result sets.
	 *
	 * @param templateSQL
	 *            the SQL template.
	 * @return The number of rows updated.
	 */
	public int xExecute(String... templateSQL) {
		try {
			return this.mixedExecute(templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an Mixed-style(Inline+Template) SQL statement, including a stored
	 * procedure call, which returns one or more result sets. Any parameters which
	 * are instances of {@link OutParameter} will be registered as OUT parameters.
	 * Transaction mode is determined by connectionManager property.
	 * <p>
	 * Use this method when: a) running SQL statements that return multiple result
	 * sets; b) invoking a stored procedure that return result sets and OUT
	 * parameters.
	 *
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The result set handler
	 * @param templateSQL
	 *            the SQL template
	 * @return A list of objects generated by the handler
	 */
	public <T> List<T> xExecute(ResultSetHandler<T> rsh, String... templateSQL) {
		try {
			return this.mixedExecute(rsh, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	// ====================================================================
	// SQL Template style and transfer SQLException to DbProRuntimeException,
	// parameters carried by a Map<String, Object> instance

	/**
	 * Executes the template style given SELECT SQL query and returns a result
	 * object. Transaction mode is determined by connectionManager property.
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code>.
	 * @param templateSQL
	 *            the SQL template
	 * @return An object generated by the handler.
	 */
	public <T> T tQuery(Map<String, Object> paramMap, ResultSetHandler<T> rsh, String... templateSQL) {
		try {
			return this.templateQuery(paramMap, rsh, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an SQL Template query for an Object, only return the first row and
	 * first column's value if more than one column or more than 1 rows returned, a
	 * null object may return if no result found , DbProRuntimeException may be
	 * threw if some SQL operation Exception happen.
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param templateSQL
	 *            the SQL template
	 * @return An Object or null, Object type determined by SQL content
	 */
	public <T> T tQueryForObject(Map<String, Object> paramMap, String... templateSQL) {
		return tQuery(paramMap, new ScalarHandler<T>(), templateSQL);
	}

	/** Template style execute SQL query, force return a String value */
	public String tQueryForString(Map<String, Object> paramMap, String... templateSQL) {
		return String.valueOf(tQueryForObject(paramMap, templateSQL));
	}

	/**
	 * Mixed-style(Inline+Template) execute SQL query, force return a String value
	 */
	public String xQueryForString(String... templateSQL) {
		return String.valueOf(xQueryForObject(templateSQL));
	}

	/**
	 * Template style execute SQL query, force return a Long value, runtime
	 * Exception may throw if result can not cast to long
	 */
	public long tQueryForLongValue(Map<String, Object> paramMap, String... templateSQL) {
		return ((Number) tQueryForObject(paramMap, templateSQL)).longValue();// NOSONAR
	}

	/**
	 * Mixed-style(Inline+Template) execute SQL query, force return a Long value,
	 * runtime Exception may throw if result can not cast to long
	 */
	public long xQueryForLongValue(String... templateSQL) {
		return ((Number) xQueryForObject(templateSQL)).longValue();// NOSONAR
	}

	/**
	 * Mixed-style(Inline+Template) query and force return a List<Map<String,
	 * Object>> type result
	 */
	public List<Map<String, Object>> xQueryForMapList(String... templateSQL) {
		return this.xQuery(new MapListHandler(), templateSQL);
	}

	/**
	 * Template style query and force return a List<Map<String, Object>> type result
	 */
	public List<Map<String, Object>> tQueryForMapList(Map<String, Object> paramMap, String... templateSQL) {
		return this.tQuery(paramMap, new MapListHandler(), templateSQL);
	}

	/**
	 * Executes the template style given INSERT, UPDATE, or DELETE SQL statement.
	 * Transaction mode is determined by connectionManager property.
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param templateSQL
	 *            the SQL template
	 * @return The number of rows updated.
	 */
	public int tUpdate(Map<String, Object> paramMap, String... templateSQL) {
		try {
			return this.templateUpdate(paramMap, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the template style given INSERT SQL statement. Transaction mode is
	 * determined by connectionManager property.
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The handler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param templateSQL
	 *            the SQL template
	 * @return An object generated by the handler.
	 */
	public <T> T tInsert(Map<String, Object> paramMap, ResultSetHandler<T> rsh, String... templateSQL) {
		try {
			return this.templateInsert(paramMap, rsh, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an SQL template statement, including a stored procedure call, which
	 * does not return any result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters. Transaction mode
	 * is determined by connectionManager property.
	 * <p>
	 * Use this method when invoking a stored procedure with OUT parameters that
	 * does not return any result sets.
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param templateSQL
	 *            the SQL template.
	 * @return The number of rows updated.
	 */
	public int tExecute(Map<String, Object> paramMap, String... templateSQL) {
		try {
			return this.templateExecute(paramMap, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an SQL template statement, including a stored procedure call, which
	 * returns one or more result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters. Transaction mode
	 * is determined by connectionManager property.
	 * <p>
	 * Use this method when: a) running SQL statements that return multiple result
	 * sets; b) invoking a stored procedure that return result sets and OUT
	 * parameters.
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The result set handler
	 * @param templateSQL
	 *            the SQL template
	 * @return A list of objects generated by the handler
	 */
	public <T> List<T> tExecute(Map<String, Object> paramMap, ResultSetHandler<T> rsh, String... templateSQL) {
		try {
			return this.templateExecute(paramMap, rsh, templateSQL);
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/** Quite execute a SQL, do not throw any exception */
	public int quiteExecute(String sql, Object... params) {
		try {
			return execute(sql, params);
		} catch (Exception e) {
			return -1;
		}
	}

}
