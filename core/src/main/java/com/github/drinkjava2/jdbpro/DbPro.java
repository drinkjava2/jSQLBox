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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.dbutils.OutParameter;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import com.github.drinkjava2.jdbpro.template.SqlTemplateEngine;
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
@SuppressWarnings({ "unchecked", "rawtypes" })
public class DbPro extends ImprovedQueryRunner {// NOSONAR
	public DbPro() {
		super();
	}

	public DbPro(DataSource ds) {
		super(ds);
	}

	public DbPro(DbProConfig config) {
		super();
		this.connectionManager = config.getConnectionManager();
		this.sqlTemplateEngine = config.getTemplateEngine();
		this.allowShowSQL = config.getAllowSqlSql();
		this.logger = config.getLogger();
		this.batchSize = config.getBatchSize();
		this.handlers = config.getHandlers();
	}

	public DbPro(DataSource ds, DbProConfig config) {
		super(ds);
		this.connectionManager = config.getConnectionManager();
		this.sqlTemplateEngine = config.getTemplateEngine();
		this.allowShowSQL = config.getAllowSqlSql();
		this.logger = config.getLogger();
		this.batchSize = config.getBatchSize();
		this.handlers = config.getHandlers();
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

	/** Quite execute a SQL, do not throw any exception */
	public int quiteExecute(String sql, Object... params) {
		try {
			return execute(sql, params);
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Build a PreparedSQL instance by given template style SQL and parameters
	 * stored in ThreadLocal
	 * 
	 * @param sqlTemplate
	 * @return PreparedSQL instance
	 */
	protected PreparedSQL mixedToSqlAndParams(String... sqlTemplate) {
		try {
			String sql = null;
			if (sqlTemplate != null) {
				StringBuilder sb = new StringBuilder("");
				for (String str : sqlTemplate)
					sb.append(str);
				sql = sb.toString();
			}
			Map<String, Object> paramMap = templateThreadlocalParamMapCache.get();
			return sqlTemplateEngine.render(sql, paramMap, directReplaceKeysCache.get());
		} finally {
			clearBind();
		}
	}

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
	public <T> T xQuery(Connection conn, ResultSetHandler rsh, String... templateSQL) {
		try {
			PreparedSQL sp = mixedToSqlAndParams(templateSQL);
			return (T) this.query(conn, sp.getSql(), rsh, sp.getParamArray());
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
			PreparedSQL sp = mixedToSqlAndParams(templateSQL);
			return update(conn, sp.getSql(), sp.getParamArray());
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
	public <T> T xInsert(Connection conn, ResultSetHandler rsh, String... templateSQL) {
		try {
			PreparedSQL sp = mixedToSqlAndParams(templateSQL);
			return (T) insert(conn, sp.getSql(), rsh, sp.getParamArray());
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
			PreparedSQL sp = mixedToSqlAndParams(templateSQL);
			return this.execute(conn, sp.getSql(), sp.getParamArray());
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
	public <T> List<T> xExecute(Connection conn, ResultSetHandler rsh, String... templateSQL) {
		try {
			PreparedSQL sp = mixedToSqlAndParams(templateSQL);
			return (List<T>) this.execute(conn, sp.getSql(), rsh, sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	// ====================================================================
	// SQL Template style and transfer SQLException to DbProRuntimeException,
	// parameters carried by a Map<String, Object> instance

	/**
	 * Mixed-style(Inline+Template) execute SQL query, force return a String value.
	 * Note: This method does not close connection
	 */
	public String xQueryForString(Connection conn, String... templateSQL) {
		return String.valueOf(xQueryForObject(conn, templateSQL));
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
	 * Executes the Mixed-style(Inline+Template) given SELECT SQL query and returns
	 * a result object. property.
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
	public <T> T xQuery(ResultSetHandler rsh, String... templateSQL) {
		try {
			PreparedSQL sp = mixedToSqlAndParams(templateSQL);
			return (T) this.query(sp.getSql(), rsh, sp.getParamArray());
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
	 * statement.
	 * 
	 * @param templateSQL
	 *            the SQL template
	 * @return The number of rows updated.
	 */
	public int xUpdate(String... templateSQL) {
		try {
			PreparedSQL sp = mixedToSqlAndParams(templateSQL);
			return this.update(sp.getSql(), sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the Mixed-style(Inline+Template) given INSERT SQL statement.
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
	public <T> T xInsert(ResultSetHandler rsh, String... templateSQL) {
		try {
			PreparedSQL sp = mixedToSqlAndParams(templateSQL);
			return (T) insert(sp.getSql(), rsh, sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an Mixed-style(Inline+Template) SQL statement, including a stored
	 * procedure call, which does not return any result sets. Any parameters which
	 * are instances of {@link OutParameter} will be registered as OUT parameters.
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
			PreparedSQL sp = mixedToSqlAndParams(templateSQL);
			return this.execute(sp.getSql(), sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an Mixed-style(Inline+Template) SQL statement, including a stored
	 * procedure call, which returns one or more result sets. Any parameters which
	 * are instances of {@link OutParameter} will be registered as OUT parameters.
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
	public <T> List<T> xExecute(ResultSetHandler rsh, String... templateSQL) {
		try {
			PreparedSQL sp = mixedToSqlAndParams(templateSQL);
			return (List<T>) this.execute(sp.getSql(), rsh, sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	// ====================================================================
	// SQL Template style and transfer SQLException to DbProRuntimeException,
	// parameters carried by a Map<String, Object> instance

	/**
	 * Mixed-style(Inline+Template) execute SQL query, force return a String value
	 */
	public String xQueryForString(String... templateSQL) {
		return String.valueOf(xQueryForObject(templateSQL));
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

	// ============================================================================

	/**
	 * Cache parameters in ThreadLocal and return an empty String
	 */
	public static Param param(Object... parameters) {
		return new Param(Param.PARAM, parameters);
	}

	/**
	 * Cache parameters in ThreadLocal and return a "?" String
	 */
	public static Param question(Object... parameters) {
		return new Param(Param.QUESTION_PARAM, parameters);
	}

	/**
	 * Create "values(?,?,?...,?)" String according how many SQL parameters be
	 * cached in ThreadLocal
	 */
	public static Param valuesQuesions() {
		return new Param(Param.VALUES_QUESTIONS);
	}

	/**
	 * Prepare a PreparedSQL for iXxxx (In-line) style, iXxxx style SQL allow may
	 * Strings, parameters can written in-line of SQL, but parameters must in
	 * param() method, for example:
	 * 
	 * ctx.iQuery(new SimpleCacheHandler(), connection, "select u.** from users u
	 * where u.age>?", param(20)," and u.id=?", param("001"), MapListHandler.class);
	 * 
	 * Parameter 20 and "001" must be written as param(20) and param("001"). <br/>
	 * 
	 * In above example connection and handlers are optional, there is no order
	 * limitation for optional items, can appear at anywhere
	 * 
	 * @param items
	 *            SQL String or Parameters or Connector or ResultSetHandler or
	 *            ResultSetHandler Class
	 * @return PreparedSQL instance
	 */
	public static PreparedSQL iPrepareSQL(Object... items) {
		if (items == null || items.length == 0)
			throw new DbProRuntimeException("prepareSQL items can not be empty");
		PreparedSQL ps = new PreparedSQL();
		StringBuilder sqlSB = new StringBuilder();
		for (Object item : items) {
			if (item == null)
				throw new DbProRuntimeException("'null' can not added as part of SQL string");
			else if (item instanceof String)
				sqlSB.append((String) item);
			else if (item instanceof PreparedSQL) {
				return (PreparedSQL) item;
			} else if (item instanceof Param) {
				Param p = (Param) item;
				if (Param.PARAM.equals(p.getType())) {
					for (Object pm : p.value())
						ps.addParam(pm);
				} else if (Param.QUESTION_PARAM.equals(p.getType())) {
					int i = 0;
					for (Object pm : p.value()) {
						ps.addParam(pm);
						if (i > 0)
							sqlSB.append(",");
						sqlSB.append("?");
						i++;
					}
				} else if (Param.VALUES_QUESTIONS.equals(p.getType())) {
					sqlSB.append(" values(");
					for (int i = 0; i < ps.getParamSize(); i++) {
						if (i > 0)
							sqlSB.append(",");
						sqlSB.append("?");
					}
					sqlSB.append(")");
				}
			} else if (item instanceof Connection)
				ps.setConnection((Connection) item);
			else if (item instanceof SqlHandler)
				ps.addHandler((SqlHandler) item);
			else if (item instanceof ResultSetHandler)
				ps.setResultSetHandler((ResultSetHandler) item);
			else if (item instanceof Class) {
				boolean added = ps.addHandler(item);
				if (!added)
					throw new DbProRuntimeException("Class '" + item + "' can not be part of SQL");
			} else
				sqlSB.append(item); // Unknown object looked as SQL include null
		}
		ps.setSql(sqlSB.toString());
		return ps;
	}

	/**
	 * Prepare a PreparedSQL for eXxxx (Single SQL) style, eXxxx style only allow
	 * single String (The first appeared) as SQL, parameters no need inside of
	 * param() method, unknown objects (include null) will automatically looked as
	 * SQL parameters,for example:
	 * 
	 * ctx.sQuery(MapListHandler.class, "select * from users where age>? and id=?",
	 * 20 , "001" , connection, new PaginHandler(2,5) );
	 * 
	 * Parameter20 and "001" will automatically be looked as SQL parameter,
	 * 
	 * In above example connection and handlers are optional, there is no order
	 * limitation for optional items, can appear at anywhere
	 * 
	 * @param items
	 *            SQL String or Parameters or Connector or ResultSetHandler or
	 *            ResultSetHandler Class
	 * @return PreparedSQL instance
	 */
	public static PreparedSQL ePrepareSQL(Object... items) {
		if (items == null || items.length == 0)
			throw new DbProRuntimeException("prepareSQL items can not be empty");
		PreparedSQL ps = new PreparedSQL();
		StringBuilder sqlSB = new StringBuilder();
		boolean foundSQL = false;
		for (Object item : items) {
			if (item instanceof String) {
				if (foundSQL) {
					ps.addParam(item);
				} else {
					foundSQL = true;
					sqlSB.append(item);
				}
			} else if (item instanceof PreparedSQL) {
				return (PreparedSQL) item;
			} else if (item instanceof Param) {
				Param p = (Param) item;
				if (Param.PARAM.equals(p.getType())) {
					for (Object pm : p.value())
						ps.addParam(pm);
				} else if (Param.VALUES_QUESTIONS.equals(p.getType())) {
					sqlSB.append(" values(");
					for (int i = 0; i < ps.getParamSize(); i++) {
						if (i > 0)
							sqlSB.append(",");
						sqlSB.append("?");
					}
					sqlSB.append(")");
				}
			} else if (item instanceof Connection)
				ps.setConnection((Connection) item);
			else if (item instanceof SqlHandler)
				ps.addHandler((SqlHandler) item);
			else if (item instanceof ResultSetHandler)
				ps.setResultSetHandler((ResultSetHandler) item);
			else if (item instanceof Class) {
				boolean added = ps.addHandler(item);
				if (!added)
					throw new DbProRuntimeException("Class '" + item + "' can not be part of SQL");
			} else
				ps.addParam(item);// Unknown object looked as SQL parameter include null
		}
		ps.setSql(sqlSB.toString());
		return ps;
	}

	// ======================================================
	// =============== SQL methods below ====================
	// ======================================================

	/**
	 * Executes the in-line style query statement
	 * 
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return An object generated by the handler.
	 */
	public <T> T iQuery(Object... inlineSQL) {
		PreparedSQL ps = iPrepareSQL(inlineSQL);
		ps.setType(SqlType.QUERY);
		return (T) runPreparedSQL(ps);
	}

	/**
	 * Execute an In-line style query for an Object, only return the first row and
	 * first column's value if more than one column or more than 1 rows returned
	 * 
	 * @param inlineSQL
	 * @param params
	 * @return An Object or null value determined by SQL content
	 */
	public <T> T iQueryForObject(Object... inlineSQL) {
		PreparedSQL ps = iPrepareSQL(inlineSQL);
		ps.setType(SqlType.SCALAR);
		return (T) runPreparedSQL(ps);
	}

	/**
	 * In-line style execute query and force return a long value, runtime exception
	 * may throw if result can not be cast to long.
	 */
	public long iQueryForLongValue(Object... inlineSQL) {
		return ((Number) iQueryForObject(inlineSQL)).longValue();// NOSONAR
	}

	/**
	 * In-line style execute query and force return a String object.
	 */
	public String iQueryForString(Object... inlineSQL) {
		return String.valueOf(iQueryForObject(inlineSQL));
	}

	/**
	 * In-Line style execute query and force return a List<Map<String, Object>> type
	 * result.
	 */
	public List<Map<String, Object>> iQueryForMapList(Object... items) {
		PreparedSQL ps = iPrepareSQL(items);
		ps.addHandler(new MapListHandler());
		ps.setType(SqlType.QUERY);
		return (List<Map<String, Object>>) runPreparedSQL(ps);
	}

	/**
	 * Executes the in-line style INSERT, UPDATE, or DELETE statement
	 * 
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return The number of rows updated.
	 */
	public int iUpdate(Object... inlineSQL) {
		PreparedSQL ps = iPrepareSQL(inlineSQL);
		ps.setType(SqlType.UPDATE);
		return (Integer) runPreparedSQL(ps);
	}

	/**
	 * Executes the in-line style insert statement
	 * 
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return An object generated by the handler.
	 */
	public int iInsert(Object... inlineSQL) {
		PreparedSQL ps = iPrepareSQL(inlineSQL);
		ps.setType(SqlType.INSERT);
		return (Integer) runPreparedSQL(ps);
	}

	/**
	 * Executes the in-line style execute statement
	 * 
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return A list of objects generated by the handler, or number of rows updated
	 *         if no handler
	 */
	public <T> T iExecute(Object... inlineSQL) {
		PreparedSQL ps = iPrepareSQL(inlineSQL);
		ps.setType(SqlType.EXECUTE);
		return (T) runPreparedSQL(ps);
	}

	// ===============================eXxxx Style====================
	/**
	 * Executes the eXxxx style query statement
	 * 
	 * @param items
	 *            The items
	 * @return An object generated by the handler.
	 */
	public <T> T eQuery(Object... items) {
		PreparedSQL ps = ePrepareSQL(items);
		ps.setType(SqlType.QUERY);
		return (T) runPreparedSQL(ps);
	}

	/**
	 * Execute an eXxxx style query for an Object, only return the first row and
	 * first column's value if more than one column or more than 1 rows returned
	 * 
	 * @param items
	 *            The items
	 * @return An Object or null value determined by SQL content
	 */
	public <T> T eQueryForObject(Object... items) {
		PreparedSQL ps = ePrepareSQL(items);
		ps.setType(SqlType.SCALAR);
		return (T) runPreparedSQL(ps);
	}

	/**
	 * eXxxx style execute query and force return a long value, runtime exception
	 * may throw if result can not be cast to long.
	 */
	public long eQueryForLongValue(Object... items) {
		return ((Number) eQueryForObject(items)).longValue();// NOSONAR
	}

	/**
	 * eXxxx style execute query and force return a String object.
	 */
	public String eQueryForString(Object... items) {
		return String.valueOf(eQueryForObject(items));
	}

	/**
	 * eXxxx style execute query and force return a List<Map<String, Object>> type
	 * result.
	 */
	public List<Map<String, Object>> eQueryForMapList(Object... items) {
		PreparedSQL ps = ePrepareSQL(items);
		ps.addHandler(new MapListHandler());
		ps.setType(SqlType.QUERY);
		return (List<Map<String, Object>>) runPreparedSQL(ps);
	}

	/**
	 * Executes the eXxxx style INSERT, UPDATE, or DELETE statement
	 * 
	 * @param items
	 *            the items
	 * @return The number of rows updated.
	 */
	public int eUpdate(Object... items) {
		PreparedSQL ps = ePrepareSQL(items);
		ps.setType(SqlType.UPDATE);
		return (Integer) runPreparedSQL(ps);
	}

	/**
	 * Executes the eXxxx style insert statement
	 * 
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return An object generated by the handler.
	 */
	public int eInsert(Object... items) {
		PreparedSQL ps = ePrepareSQL(items);
		ps.setType(SqlType.INSERT);
		return (Integer) runPreparedSQL(ps);
	}

	/**
	 * Executes the eXxxx style execute statement
	 * 
	 * @param items
	 *            the items
	 * @return A list of objects generated by the handler, or number of rows updated
	 *         if no handler
	 */
	public <T> T eExecute(Object... items) {
		PreparedSQL ps = ePrepareSQL(items);
		ps.setType(SqlType.EXECUTE);
		return (T) runPreparedSQL(ps);
	}

	// ===============================Template Style====================
	/**
	 * A ThreadLocal variant for temporally store parameter key names which is a
	 * direct-replace type parameter in current Thread
	 */
	protected static ThreadLocal<Set<String>> directReplaceKeysCache = new ThreadLocal<Set<String>>() {
		@Override
		protected Set<String> initialValue() {
			return new HashSet<String>();
		}
	};

	// getter && setter ===========
	public SqlTemplateEngine getSqlTemplateEngine() {
		return sqlTemplateEngine;
	}

	/**
	 * A ThreadLocal variant for temporally store parameter Map in current Thread
	 */
	protected static ThreadLocal<Map<String, Object>> templateThreadlocalParamMapCache = new ThreadLocal<Map<String, Object>>() {
		@Override
		protected Map<String, Object> initialValue() {
			return new HashMap<String, Object>();
		}
	};

	/**
	 * Put a name-value pair into ThreadLocal parameter Map, return an empty String
	 * ""
	 */
	public static String put(String name, Object value) {
		templateThreadlocalParamMapCache.get().put(name, value);
		return "";
	}

	/**
	 * put a name-value into ThreadLocal parameter Map, return an empty String,
	 * Note: use replace() method the value will directly replace text in template
	 */
	public static String replace(String name, Object value) {
		templateThreadlocalParamMapCache.get().put(name, value);
		directReplaceKeysCache.get().add(name);
		return "";
	}

	/**
	 * Clear all template ThreadLocal parameters, put a name-value pair into
	 * ThreadLocal parameter Map, return an empty String ""
	 */
	public static String put0(String name, Object value) {
		clearBind();
		return put(name, value);
	}

	/**
	 * Clear all template ThreadLocal parameters, return an empty String ""
	 */
	public static String put0() {
		clearBind();
		return "";
	}

	/**
	 * Clear all template ThreadLocal parameters, then put a name-value into
	 * ThreadLocal parameter Map, return an empty String, Note: use replace() method
	 * the value will directly replace text in template
	 */
	public static String replace0(String name, Object value) {
		clearBind();
		return replace(name, value);
	}

	/**
	 * Clear all template ThreadLocal parameters
	 */
	public static void clearBind() {
		templateThreadlocalParamMapCache.get().clear();
		directReplaceKeysCache.get().clear();
	}

	/**
	 * Build a PreparedSQL instance by given template style SQL and parameters
	 * stored in ThreadLocal
	 * 
	 * @param sqlTemplate
	 * @return PreparedSQL instance
	 */
	protected PreparedSQL templateToSqlAndParams(Map<String, Object> paramMap, String... sqlTemplate) {
		try {
			String sql = null;
			if (sqlTemplate != null) {
				StringBuilder sb = new StringBuilder("");
				for (String str : sqlTemplate)
					sb.append(str);
				sql = sb.toString();
			}
			return sqlTemplateEngine.render(sql, paramMap, null);
		} finally {
			clearBind();
		}
	}

	// ======================================================
	// =============== SQL methods below ====================
	// ======================================================

	/**
	 * Template style execute SQL query, force return a Long value, runtime
	 * Exception may throw if result can not cast to long
	 */
	public long tQueryForLongValue(String templateSQL, Map<String, Object> paramMap) {
		return ((Number) tQueryForObject(templateSQL, paramMap)).longValue();// NOSONAR
	}

	/**
	 * Template style query and force return a List<Map<String, Object>> type result
	 */
	public List<Map<String, Object>> tQueryForMapList(String templateSQL, Map<String, Object> paramMap) {
		return this.tQuery(new MapListHandler(), templateSQL, paramMap);
	}

	/**
	 * Executes the template style given SELECT SQL query and returns a result
	 * object.
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
	public <T> T tQuery(ResultSetHandler rsh, String templateSQL, Map<String, Object> paramMap) {
		try {
			PreparedSQL sp = templateToSqlAndParams(paramMap, templateSQL);
			return (T) this.query(sp.getSql(), rsh, sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the template style given INSERT SQL statement.
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
	public <T> T tInsert(Connection conn, ResultSetHandler rsh, String templateSQL, Map<String, Object> paramMap) {
		try {
			PreparedSQL sp = templateToSqlAndParams(paramMap, templateSQL);
			return (T) insert(conn, sp.getSql(), rsh, sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the template style INSERT, UPDATE, or DELETE statement
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param templateSQL
	 *            the SQL template
	 * @return The number of rows updated.
	 */
	public int tUpdate(Connection conn, String templateSQL, Map<String, Object> paramMap) {
		try {
			PreparedSQL sp = templateToSqlAndParams(paramMap, templateSQL);
			return update(conn, sp.getSql(), sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Template style query and force return a List<Map<String, Object>> type
	 * result.
	 */
	public List<Map<String, Object>> tQueryForMapList(Connection conn, String templateSQL,
			Map<String, Object> paramMap) {
		return this.tQuery(conn, new MapListHandler(), templateSQL, paramMap);
	}

	/**
	 * Template style execute SQL query, force return a Long value, runtime
	 * Exception may throw if result can not cast to long.
	 */
	public long tQueryForLongValue(Connection conn, String templateSQL, Map<String, Object> paramMap) {
		return ((Number) tQueryForObject(conn, templateSQL, paramMap)).longValue();// NOSONAR
	}

	/**
	 * Executes the template style given SELECT SQL query and returns a result
	 * object.
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
	public <T> T tQuery(Connection conn, ResultSetHandler rsh, String templateSQL, Map<String, Object> paramMap) {
		try {
			PreparedSQL sp = templateToSqlAndParams(paramMap, templateSQL);
			return (T) this.query(conn, sp.getSql(), rsh, sp.getParamArray());
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
	public <T> T tQueryForObject(Connection conn, String templateSQL, Map<String, Object> paramMap) {
		return tQuery(conn, new ScalarHandler<T>(), templateSQL, paramMap);
	}

	/**
	 * Template style execute SQL query, force return a String value.
	 */
	public String tQueryForString(Connection conn, String templateSQL, Map<String, Object> paramMap) {
		return String.valueOf(tQueryForObject(conn, templateSQL, paramMap));
	}

	/**
	 * Execute an SQL template statement, including a stored procedure call, which
	 * does not return any result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters.
	 * 
	 * Use this method when invoking a stored procedure with OUT parameters that
	 * does not return any result sets.
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param templateSQL
	 *            the SQL template.
	 * @return The number of rows updated.
	 */
	public int tExecute(Connection conn, String templateSQL, Map<String, Object> paramMap) {
		try {
			PreparedSQL sp = templateToSqlAndParams(paramMap, templateSQL);
			return this.execute(conn, sp.getSql(), sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an SQL template statement, including a stored procedure call, which
	 * returns one or more result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters.
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
	public <T> List<T> tExecute(Connection conn, ResultSetHandler rsh, String templateSQL,
			Map<String, Object> paramMap) {
		try {
			PreparedSQL sp = templateToSqlAndParams(paramMap, templateSQL);
			return this.execute(conn, sp.getSql(), rsh, sp.getParamArray());
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
	public <T> T tQueryForObject(String templateSQL, Map<String, Object> paramMap) {
		return tQuery(new ScalarHandler<T>(), templateSQL, paramMap);
	}

	/** Template style execute SQL query, force return a String value */
	public String tQueryForString(String templateSQL, Map<String, Object> paramMap) {
		return String.valueOf(tQueryForObject(templateSQL, paramMap));
	}

	/**
	 * Executes the template style given INSERT, UPDATE, or DELETE SQL statement.
	 * 
	 * @param paramMap
	 *            The parameters stored in Map
	 * @param templateSQL
	 *            the SQL template
	 * @return The number of rows updated.
	 */
	public int tUpdate(String templateSQL, Map<String, Object> paramMap) {
		try {
			PreparedSQL sp = templateToSqlAndParams(paramMap, templateSQL);
			return this.update(sp.getSql(), sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Executes the template style given INSERT SQL statement.
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
	public <T> T tInsert(ResultSetHandler rsh, String templateSQL, Map<String, Object> paramMap) {
		try {
			PreparedSQL sp = templateToSqlAndParams(paramMap, templateSQL);
			return (T) insert(sp.getSql(), rsh, sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an SQL template statement, including a stored procedure call, which
	 * does not return any result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters.
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
	public int tExecute(String templateSQL, Map<String, Object> paramMap) {
		try {
			PreparedSQL sp = templateToSqlAndParams(paramMap, templateSQL);
			return this.execute(sp.getSql(), sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

	/**
	 * Execute an SQL template statement, including a stored procedure call, which
	 * returns one or more result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters.
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
	public <T> List<T> tExecute(ResultSetHandler rsh, String templateSQL, Map<String, Object> paramMap) {
		try {
			PreparedSQL sp = templateToSqlAndParams(paramMap, templateSQL);
			return this.execute(sp.getSql(), rsh, sp.getParamArray());
		} catch (SQLException e) {
			throw new DbProRuntimeException(e);
		}
	}

}
