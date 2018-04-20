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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.OutParameter;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

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
public class DbPro extends ImprovedQueryRunner implements NormalJdbcTool {// NOSONAR
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
		this.sqlHandlers = config.getHandlers();
	}

	public DbPro(DataSource ds, DbProConfig config) {
		super(ds);
		this.connectionManager = config.getConnectionManager();
		this.sqlTemplateEngine = config.getTemplateEngine();
		this.allowShowSQL = config.getAllowSqlSql();
		this.logger = config.getLogger();
		this.batchSize = config.getBatchSize();
		this.sqlHandlers = config.getHandlers();
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

	public void ________INLINE_Methods________() {// NOSONAR
	}

	/** ThreadLocal variant cache INLINE methods */
	private static ThreadLocal<ArrayList<Object>> INLINE_PARAM_CACHE = new ThreadLocal<ArrayList<Object>>() {// NOSONAR
		@Override
		protected ArrayList<Object> initialValue() {
			return new ArrayList<Object>();
		}
	};

	/**
	 * Clear all ThreadLocal parameters first, then cache parameters in ThreadLocal
	 * and return an empty String, a non parameter param0() call equal to
	 * clearAllInlineThreadlocalParams()
	 */
	public static String PARA0(Object... params) {// NOSONAR
		INLINE_PARAM_CACHE.get().clear();
		return PARA(params);
	}

	/** Cache parameters in ThreadLocal and return an empty String */
	public static String PARA(Object... params) {// NOSONAR
		for (Object o : params)
			INLINE_PARAM_CACHE.get().add(o);
		return "";
	}

	/**
	 * Build a PreparedSQL instance by given in-line style SQL and parameters stored
	 * in ThreadLocal
	 * 
	 * @param inlineSQL
	 * @return PreparedSQL instance
	 */
	public static Object[] PARAMS() {// NOSONAR
		try {
			return INLINE_PARAM_CACHE.get().toArray();
		} finally {
			INLINE_PARAM_CACHE.get().clear();
		}
	}

	/**
	 * Clear all ThreadLocal parameters first, then cache parameters in ThreadLocal,
	 * then return a "?" String
	 */
	public static String QUES0(Object... params) {// NOSONAR
		INLINE_PARAM_CACHE.get().clear();
		return QUES(params);
	}

	/** Cache parameters in ThreadLocal and return a "?" String */
	public static String QUES(Object... params) {// NOSONAR
		for (Object o : params)
			INLINE_PARAM_CACHE.get().add(o);
		return "?";
	}

	/**
	 * Create "values(?,?,?...,?)" String according how many SQL parameters be
	 * cached in ThreadLocal
	 */
	public static String VALUESQUES() {// NOSONAR
		int howManyQuestionMarks = INLINE_PARAM_CACHE.get().size();
		StringBuilder sb = new StringBuilder("values(");
		for (int i = 0; i < howManyQuestionMarks; i++) {
			if (i != howManyQuestionMarks - 1)
				sb.append("?,");
			else
				sb.append("?");
		}
		sb.append(")");
		return sb.toString();
	}

	public void ________prepareMethods________() {// NOSONAR
	}

	/**
	 * Prepare a PreparedSQL for pXxxx (Single SQL) style, pXxxx style only allow
	 * single String (The first appeared) as SQL, unknown objects (include null)
	 * will automatically looked as SQL parameters, more detail see doPrepare method
	 */
	public static PreparedSQL pPrepare(Object... items) {
		return doPrepare(false, items);
	}

	/**
	 * Prepare a PreparedSQL for iXxxx (Single SQL) style, unknown objects (include
	 * null) will automatically looked as SQL pieces, more detail see doPrepare
	 * method
	 */
	public static PreparedSQL iPrepare(Object... items) {
		return doPrepare(true, items);
	}

	private PreparedSQL pPrepareAndInsertHandlers(Object... items) {
		PreparedSQL ps = doPrepare(false, items);
		ps.addGlobalAndThreadedHandlers(this);
		return ps;
	}

	private PreparedSQL iPrepareAndInsertHandlers(Object... items) {
		PreparedSQL ps = doPrepare(true, items);
		ps.addGlobalAndThreadedHandlers(this);
		return ps;
	}

	/**
	 * Prepare a PreparedSQL for iXxxx (In-line) style or pXxxx style, For iXxxx
	 * style, unknown items be treated as String, SQL parameters must written in
	 * param() method, for example:
	 * 
	 * ctx.iQuery(new SimpleCacheHandler(), connection, "select u.** from users u
	 * where u.age>?", param(20)," and u.id=?", param("001"), MapListHandler.class);
	 * 
	 * 
	 * pXxxx style only allow first appeared String as SQL, left unknown items will
	 * be treated as SQL parameters, for example:
	 * 
	 * ctx.pQuery(MapListHandler.class, "select * from users where age>? and id=?",
	 * 20 , "001" , connection, new PaginHandler(2,5), sql(" and name=?"), "Tom" );
	 * 
	 * In above examples connection and sqlHandlers are optional items, these
	 * optional items can appear at anywhere
	 * 
	 * @param items
	 *            SQL String / SQL Parameters / Connection / ResultSetHandler class
	 *            or instance / SqlHandler class or instance
	 * @return a PreparedSQL instance
	 */
	private static PreparedSQL doPrepare(boolean iXxxStyle, Object... items) {// NOSONAR
		// TODO add templateEngine
		if (items == null || items.length == 0)
			throw new DbProRuntimeException("prepareSQL items can not be empty");
		PreparedSQL result = new PreparedSQL();
		StringBuilder sql = new StringBuilder();
		boolean foundSQL = false;
		for (Object item : items) {
			if (item == null) {
				if (iXxxStyle)
					throw new DbProRuntimeException("In iXxxx style,  null value can not append as SQL piece");
				else
					result.addParam(null);
			} else if (item instanceof String) {
				if (iXxxStyle)
					sql.append(item);
				else if (foundSQL)
					result.addParam(item);
				else {
					sql.append(item);
					foundSQL = true;
				}
			} else if (item instanceof PreparedSQL) {
				PreparedSQL psItem = (PreparedSQL) item;
				if (psItem.getSql() != null) {
					sql.append(psItem.getSql());
					foundSQL = true;
				}
				if (psItem.getParams() != null)
					for (Object obj : psItem.getParams())
						result.addParam(obj);
			} else if (item instanceof Map<?, ?>) {
				result.addTemplateMap((Map<String, Object>) item);
			} else if (item instanceof SqlParam) {
				SqlParam spm = (SqlParam) item;
				if (SqlParamType.PARAM.equals(spm.getType())) {
					for (Object pm : spm.getParameters())
						result.addParam(pm);
				} else if (SqlParamType.PUT.equals(spm.getType())) {
					result.addTemplateParam(spm);
				} else if (SqlParamType.SQL.equals(spm.getType())) {
					for (Object pm : spm.getParameters())
						sql.append(pm);
				} else if (SqlParamType.QUESTION_PARAM.equals(spm.getType())) {
					int i = 0;
					for (Object pm : spm.getParameters()) {
						result.addParam(pm);
						if (i > 0)
							sql.append(",");
						sql.append("?");
						i++;
					}
				} else if (SqlParamType.NOT_NULL.equals(spm.getType())) {
					if (spm.getParameters()[1] != null) {
						sql.append(spm.getParameters()[0]);
						result.addParam(spm.getParameters()[1]);
					}
				} else if (SqlParamType.VALUES_QUESTIONS.equals(spm.getType())) {
					sql.append(" values(");
					for (int i = 0; i < result.getParamSize(); i++) {
						if (i > 0)
							sql.append(",");
						sql.append("?");
					}
					sql.append(")");
				} else
					// should never run here
					throw new DbProRuntimeException("What the heck the param type is");
			} else if (item instanceof Connection)
				result.setConnection((Connection) item);
			else if (item instanceof SqlHandler)
				result.addHandler((SqlHandler) item);
			else if (item instanceof ResultSetHandler)
				result.setResultSetHandler((ResultSetHandler) item);
			else if (item instanceof Class) {
				boolean added = result.addHandler(item);
				if (!added)
					throw new DbProRuntimeException(
							"Class '" + item + "' can not use public zero parameter constructor to build instance");
			} else if (item instanceof CustomSqlItem) {
				((CustomSqlItem) item).dealItem(result, sql);
			} else {
				if (iXxxStyle)
					sql.append(item); // iXxxx style, unknown object is SQL piece
				else
					result.addParam(item); // pXxxx style, unknown object is parameter
			}
		}
		result.setSql(sql.toString());
		return result;
	}

	// ============================================================================

	/** Return a SqlParamType.PARAM type SqlParam instance */
	public static SqlParam param(Object... parameters) {
		return new SqlParam(SqlParamType.PARAM, parameters);
	}

	/**
	 * Cache parameters in ThreadLocal and return an empty String
	 */
	public static SqlParam sql(Object... parameters) {
		return new SqlParam(SqlParamType.SQL, parameters);
	}

	/**
	 * Cache parameters in ThreadLocal and return a "?" String
	 */
	public static SqlParam question(Object... parameters) {
		return new SqlParam(SqlParamType.QUESTION_PARAM, parameters);
	}

	/**
	 * If param is not null, then append sqlPiece
	 * 
	 * @param sqlPiece
	 *            The SQL piece will be appended
	 * @param param
	 *            The param
	 * @return a SqlParam instance will be used by iPrepare method
	 */
	public static SqlParam notNull(String sqlPiece, Object param) {
		return new SqlParam(SqlParamType.NOT_NULL, sqlPiece, param);
	}

	/**
	 * Create "values(?,?,?...,?)" String according how many SQL parameters be
	 * cached in ThreadLocal
	 */
	public static SqlParam valuesQuestions() {
		return new SqlParam(SqlParamType.VALUES_QUESTIONS);
	}

	/**
	 * For tXxxx style templateEngine use, return a SqlParamType.PUT type SqlParam
	 * instance,
	 * 
	 * Usage: put("key1",value1,"key2",value2...);
	 */
	public static SqlParam put(Object... parameters) {
		return new SqlParam(SqlParamType.PUT, parameters);
	}

	public void ________iXxxxStyles________() {// NOSONAR
	}

	// ============================================================================

	/**
	 * Executes the in-line style query statement
	 * 
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return An object generated by the handler.
	 */
	public <T> T iQuery(Object... inlineSQL) {
		PreparedSQL ps = iPrepareAndInsertHandlers(inlineSQL);
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
		PreparedSQL ps = iPrepareAndInsertHandlers(inlineSQL);
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
		PreparedSQL ps = iPrepareAndInsertHandlers(items);
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
		PreparedSQL ps = iPrepareAndInsertHandlers(inlineSQL);
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
		PreparedSQL ps = iPrepareAndInsertHandlers(inlineSQL);
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
		PreparedSQL ps = iPrepareAndInsertHandlers(inlineSQL);
		ps.setType(SqlType.EXECUTE);
		return (T) runPreparedSQL(ps);
	}

	public void ________pXxxxStyles________() {// NOSONAR
	}

	/**
	 * Executes the pXxxx style query statement
	 * 
	 * @param items
	 *            The items
	 * @return An object generated by the handler.
	 */
	public <T> T pQuery(Object... items) {
		PreparedSQL ps = pPrepareAndInsertHandlers(items);
		ps.setType(SqlType.QUERY);
		return (T) runPreparedSQL(ps);
	}

	/**
	 * Execute an pXxxx style query for an Object, only return the first row and
	 * first column's value if more than one column or more than 1 rows returned
	 * 
	 * @param items
	 *            The items
	 * @return An Object or null value determined by SQL content
	 */
	public <T> T pQueryForObject(Object... items) {
		PreparedSQL ps = pPrepareAndInsertHandlers(items);
		ps.setType(SqlType.SCALAR);
		return (T) runPreparedSQL(ps);
	}

	/**
	 * pXxxx style execute query and force return a long value, runtime exception
	 * may throw if result can not be cast to long.
	 */
	public long pQueryForLongValue(Object... items) {
		return ((Number) pQueryForObject(items)).longValue();// NOSONAR
	}

	/**
	 * pXxxx style execute query and force return a String object.
	 */
	public String pQueryForString(Object... items) {
		return String.valueOf(pQueryForObject(items));
	}

	/**
	 * pXxxx style execute query and force return a List<Map<String, Object>> type
	 * result.
	 */
	public List<Map<String, Object>> pQueryForMapList(Object... items) {
		PreparedSQL ps = pPrepareAndInsertHandlers(items);
		ps.addHandler(new MapListHandler());
		ps.setType(SqlType.QUERY);
		return (List<Map<String, Object>>) runPreparedSQL(ps);
	}

	/**
	 * Executes the pXxxx style INSERT, UPDATE, or DELETE statement
	 * 
	 * @param items
	 *            the items
	 * @return The number of rows updated.
	 */
	public int pUpdate(Object... items) {
		PreparedSQL ps = pPrepareAndInsertHandlers(items);
		ps.setType(SqlType.UPDATE);
		return (Integer) runPreparedSQL(ps);
	}

	/**
	 * Executes the pXxxx style insert statement
	 * 
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return An object generated by the handler.
	 */
	public int pInsert(Object... items) {
		PreparedSQL ps = pPrepareAndInsertHandlers(items);
		ps.setType(SqlType.INSERT);
		return (Integer) runPreparedSQL(ps);
	}

	/**
	 * Executes the pXxxx style execute statement
	 * 
	 * @param items
	 *            the items
	 * @return A list of objects generated by the handler, or number of rows updated
	 *         if no handler
	 */
	public <T> T pExecute(Object... items) {
		PreparedSQL ps = pPrepareAndInsertHandlers(items);
		ps.setType(SqlType.EXECUTE);
		return (T) runPreparedSQL(ps);
	}

	public void ________tXxxxStyles________() {// NOSONAR
	}

	/**
	 * Executes the pXxxx style query statement
	 * 
	 * @param items
	 *            The items
	 * @return An object generated by the handler.
	 */
	public <T> T tQuery(Object... items) {
		PreparedSQL ps = iPrepareAndInsertHandlers(items);
		ps.setUseTemplate(true);
		ps.setType(SqlType.QUERY);
		return (T) runPreparedSQL(ps);
	}

	/**
	 * Execute an pXxxx style query for an Object, only return the first row and
	 * first column's value if more than one column or more than 1 rows returned
	 * 
	 * @param items
	 *            The items
	 * @return An Object or null value determined by SQL content
	 */
	public <T> T tQueryForObject(Object... items) {
		PreparedSQL ps = iPrepareAndInsertHandlers(items);
		ps.setUseTemplate(true);
		ps.setType(SqlType.SCALAR);
		return (T) runPreparedSQL(ps);
	}

	/**
	 * pXxxx style execute query and force return a long value, runtime exception
	 * may throw if result can not be cast to long.
	 */
	public long tQueryForLongValue(Object... items) {
		return ((Number) tQueryForObject(items)).longValue();// NOSONAR
	}

	/**
	 * pXxxx style execute query and force return a String object.
	 */
	public String tQueryForString(Object... items) {
		return String.valueOf(tQueryForObject(items));
	}

	/**
	 * pXxxx style execute query and force return a List<Map<String, Object>> type
	 * result.
	 */
	public List<Map<String, Object>> tQueryForMapList(Object... items) {
		PreparedSQL ps = iPrepareAndInsertHandlers(items);
		ps.setUseTemplate(true);
		ps.addHandler(new MapListHandler());
		ps.setType(SqlType.QUERY);
		return (List<Map<String, Object>>) runPreparedSQL(ps);
	}

	/**
	 * Executes the pXxxx style INSERT, UPDATE, or DELETE statement
	 * 
	 * @param items
	 *            the items
	 * @return The number of rows updated.
	 */
	public int tUpdate(Object... items) {
		PreparedSQL ps = iPrepareAndInsertHandlers(items);
		ps.setUseTemplate(true);
		ps.setType(SqlType.UPDATE);
		return (Integer) runPreparedSQL(ps);
	}

	/**
	 * Executes the pXxxx style insert statement
	 * 
	 * @param inlineSQL
	 *            the in-line style SQL
	 * @return An object generated by the handler.
	 */
	public int tInsert(Object... items) {
		PreparedSQL ps = iPrepareAndInsertHandlers(items);
		ps.setUseTemplate(true);
		ps.setType(SqlType.INSERT);
		return (Integer) runPreparedSQL(ps);
	}

	/**
	 * Executes the pXxxx style execute statement
	 * 
	 * @param items
	 *            the items
	 * @return A list of objects generated by the handler, or number of rows updated
	 *         if no handler
	 */
	public <T> T tExecute(Object... items) {
		PreparedSQL ps = iPrepareAndInsertHandlers(items);
		ps.setUseTemplate(true);
		ps.setType(SqlType.EXECUTE);
		return (T) runPreparedSQL(ps);
	}

	/**
	 * nXxxx style series methods are design to replace QueryRunner's xxxx method,
	 * the difference is nXxxx methods do not throw SqlException
	 */
	public void ________nXxxxStyles________() {// NOSONAR

	}

	/**
	 * Executes the given SELECT SQL query and returns a result object.
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
		PreparedSQL ps = new PreparedSQL(SqlType.QUERY, conn, rsh, sql, params);
		return (T) runPreparedSQL(ps);
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
	public <T> T nQueryForObject(Connection conn, String sql, Object... params) {
		PreparedSQL ps = new PreparedSQL(SqlType.QUERY, conn, SingleTonHandlers.scalarHandler, sql, params);
		return (T) runPreparedSQL(ps);
	}

	/**
	 * Execute query and force return a String object, no need catch SQLException.
	 * 
	 */
	public String nQueryForString(Connection conn, String sql, Object... params) {
		return nQueryForObject(conn, sql, params);
	}

	/**
	 * Execute query and force return a Long object, no need catch SQLException,
	 * runtime exception may throw if result can not be cast to long.
	 */
	public long nQueryForLongValue(Connection conn, String sql, Object... params) {
		return ((Number) nQueryForObject(conn, sql, params)).longValue();// NOSONAR
	}

	/**
	 * Execute query and force return a List<Map<String, Object>> type result, no
	 * need catch SQLException.
	 */
	public List<Map<String, Object>> nQueryForMapList(Connection conn, String sql, Object... params) {
		PreparedSQL ps = new PreparedSQL(SqlType.QUERY, conn, SingleTonHandlers.mapListHandler, sql, params);
		return (List<Map<String, Object>>) runPreparedSQL(ps);
	}

	/**
	 * Executes the given INSERT, UPDATE, or DELETE SQL statement.
	 * 
	 * @param sql
	 *            the SQL
	 * @param params
	 *            the parameters if have
	 * @return The number of rows updated.
	 */
	public int nUpdate(Connection conn, String sql, Object... params) {
		PreparedSQL ps = new PreparedSQL(SqlType.UPDATE, conn, null, sql, params);
		return (Integer) runPreparedSQL(ps);
	}

	/**
	 * Executes the given INSERT SQL statement. Note: This method does not close
	 * connection.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The resultSetHandler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param sql
	 *            the SQL
	 * @param params
	 *            the parameters if have
	 * @return An object generated by the handler.
	 * 
	 */
	public <T> T nInsert(Connection conn, ResultSetHandler<T> rsh, String sql, Object... params) {
		PreparedSQL ps = new PreparedSQL(SqlType.INSERT, conn, rsh, sql, params);
		return (T) runPreparedSQL(ps);
	}

	/**
	 * Execute an statement, including a stored procedure call, which does not
	 * return any result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters.
	 * <p>
	 * Use this method when invoking a stored procedure with OUT parameters that
	 * does not return any result sets.
	 * 
	 * @param sql
	 *            the SQL
	 * @return The number of rows updated.
	 */
	public int nExecute(Connection conn, String sql, Object... params) {
		PreparedSQL ps = new PreparedSQL(SqlType.EXECUTE, conn, null, sql, params);
		return (Integer) runPreparedSQL(ps);
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
		PreparedSQL ps = new PreparedSQL(SqlType.EXECUTE, conn, rsh, sql, params);
		return (List<T>) runPreparedSQL(ps);
	}

	/**
	 * Executes the given SELECT SQL query and returns a result object.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param sql
	 *            the SQL
	 * @param rsh
	 *            The resultSetHandler used to create the result object from the
	 *            <code>ResultSet</code>.
	 * @param params
	 *            the parameters if have
	 * @return An object generated by the handler.
	 * 
	 */
	public <T> T nQuery(ResultSetHandler<T> rsh, String sql, Object... params) {
		PreparedSQL ps = new PreparedSQL(SqlType.QUERY, null, rsh, sql, params);
		return (T) runPreparedSQL(ps);
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
		PreparedSQL ps = new PreparedSQL(SqlType.QUERY, null, SingleTonHandlers.scalarHandler, sql, params);
		return (T) runPreparedSQL(ps);
	}

	// ============================================================================

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
		return ((Number) nQueryForObject(sql, params)).longValue();// NOSONAR
	}

	/**
	 * Execute query and force return a List<Map<String, Object>> type result, no
	 * need catch SQLException
	 */
	public List<Map<String, Object>> nQueryForMapList(String sql, Object... params) {
		PreparedSQL ps = new PreparedSQL(SqlType.QUERY, null, SingleTonHandlers.mapListHandler, sql, params);
		return (List<Map<String, Object>>) runPreparedSQL(ps);
	}

	/**
	 * Executes the given INSERT, UPDATE, or DELETE SQL statement.
	 * 
	 * @param sql
	 *            the SQL
	 * @param params
	 *            the parameters if have
	 * @return The number of rows updated.
	 */
	@Override
	public int nUpdate(String sql, Object... params) {
		PreparedSQL ps = new PreparedSQL(SqlType.UPDATE, null, null, sql, params);
		return (Integer) runPreparedSQL(ps);
	}

	/**
	 * Executes the given INSERT SQL statement.
	 * 
	 * @param <T>
	 *            The type of object that the handler returns
	 * @param rsh
	 *            The resultSetHandler used to create the result object from the
	 *            <code>ResultSet</code> of auto-generated keys.
	 * @param sql
	 *            the SQL
	 * @param params
	 *            the parameters if have
	 * @return An object generated by the handler.
	 * 
	 */
	public <T> T nInsert(ResultSetHandler rsh, String sql, Object... params) {
		PreparedSQL ps = new PreparedSQL(SqlType.INSERT, null, rsh, sql, params);
		return (T) runPreparedSQL(ps);
	}

	/**
	 * Execute an statement, including a stored procedure call, which does not
	 * return any result sets. Any parameters which are instances of
	 * {@link OutParameter} will be registered as OUT parameters.
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
		PreparedSQL ps = new PreparedSQL(SqlType.EXECUTE, null, null, sql, params);
		Object o = runPreparedSQL(ps);
		return (Integer) o;
	}

	/**
	 * Execute an statement, including a stored procedure call, which returns one or
	 * more result sets. Any parameters which are instances of {@link OutParameter}
	 * will be registered as OUT parameters.
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
	public <T> List<T> nExecute(ResultSetHandler rsh, String sql, Object... params) {
		PreparedSQL ps = new PreparedSQL(SqlType.EXECUTE, null, rsh, sql, params);
		return (List<T>) runPreparedSQL(ps);
	}

}