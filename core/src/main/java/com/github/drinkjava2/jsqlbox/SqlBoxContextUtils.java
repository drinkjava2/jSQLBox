/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.github.drinkjava2.jsqlbox;

import static com.github.drinkjava2.jdbpro.JDBPRO.param;
import static com.github.drinkjava2.jdbpro.JDBPRO.valuesQuestions;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.handlers.ArrayHandler;

import com.github.drinkjava2.jdbpro.SqlItem;
import com.github.drinkjava2.jdbpro.SqlOption;
import com.github.drinkjava2.jdialects.ClassCacheUtils;
import com.github.drinkjava2.jdialects.Dialect;
import com.github.drinkjava2.jdialects.TableModelUtils;
import com.github.drinkjava2.jdialects.Type;
import com.github.drinkjava2.jdialects.annotation.jpa.GenerationType;
import com.github.drinkjava2.jdialects.id.IdGenerator;
import com.github.drinkjava2.jdialects.id.IdentityIdGenerator;
import com.github.drinkjava2.jdialects.id.SnowflakeCreator;
import com.github.drinkjava2.jdialects.model.ColumnModel;
import com.github.drinkjava2.jdialects.model.TableModel;
import com.github.drinkjava2.jsqlbox.sharding.ShardingTool;

/**
 * SqlBoxContextUtils is utility class store static methods about SqlBoxContext
 * 
 * @author Yong Zhu
 * @since 1.0.0
 */
public abstract class SqlBoxContextUtils {// NOSONAR
	/**
	 * Read database Meta info into SqlBox[]
	 */
	public static TableModel[] loadMetaTableModels(SqlBoxContext ctx, Dialect dialect) {
		Connection con = null;
		SQLException sqlException = null;
		try {
			con = ctx.prepareConnection();
			return TableModelUtils.db2Models(con, dialect);
		} catch (SQLException e) {
			sqlException = e;
		} finally {
			try {
				ctx.close(con);
			} catch (SQLException e) {
				if (sqlException != null)
					sqlException.setNextException(e);
				else
					sqlException = e;
			}
		}
		throw new SqlBoxException(sqlException);
	}

	private static ColumnModel findMatchColumnForJavaField(String entityField, SqlBox box) {
		ColumnModel col = findMatchColumnForJavaField(entityField, box.getTableModel());
		if (col == null)
			throw new SqlBoxException("Can not find database column match entity field '" + entityField + "'");
		return col;
	}

	private static ColumnModel findMatchColumnForJavaField(String entityField, TableModel tableModel) {
		if (tableModel == null)
			return null;
		List<ColumnModel> columns = tableModel.getColumns();
		ColumnModel result = null;
		String underLineFieldName = SqlBoxStrUtils.camelToLowerCaseUnderline(entityField);
		for (ColumnModel col : columns) {
			if (entityField.equalsIgnoreCase(col.getEntityField())
					|| underLineFieldName.equalsIgnoreCase(col.getColumnName())) {
				if (result != null)
					throw new SqlBoxException("Field '" + entityField + "' found duplicated columns definition");
				result = col;
			}
		}
		return result;
	}
	

	public  static String handleShardTable(SqlBoxContext ctx, Object entityOrClass, Object shardKey1, Object shardKey2) {
		if (ctx.getShardingTools() == null || ctx.getShardingTools().length == 0)
			throw new SqlBoxException("No shardingTools be set."); 
		String table = null;
		for (ShardingTool sh : ctx.getShardingTools()) {
			String[] result = sh.handleShardTable(ctx, entityOrClass, shardKey1, shardKey2);
			if (result != null) {
				if (result.length == 0)
					throw new SqlBoxException("Can not find sharding table for target '" + entityOrClass + "'");
				if (result.length > 1)
					throw new SqlBoxException("Found more than 1 sharding tables for target '" + entityOrClass
							+ "', jSqlBox current version do not support auto-join, to solve this issue you need adjust your ShardTable search condition");
				table = result[0];
				break;
			}
		}
		return table;
	}
	

	/**
	 * Insert entityBean into database, and change ID fields to values generated
	 * by IdGenerator (identity or sequence or UUID...)
	 */
	public static int insert(SqlBoxContext ctx, Object entityBean, Object... optionalSqlItems) {// NOSONAR
		SqlBox box = SqlBoxUtils.findAndBindSqlBox(ctx, entityBean);
		checkBeanAndBoxExist(entityBean, box);
		TableModel tableModel = box.getTableModel();
		List<Object> iXxx = new ArrayList<Object>();

		String identityFieldName = null;
		Type identityType = null;
		Map<String, Method> readMethods = ClassCacheUtils.getClassReadMethods(entityBean.getClass());

		iXxx.add(0, " (");
		boolean foundColumnToInsert = false;
		SqlItem shardTableItem = null;
		for (String fieldName : readMethods.keySet()) {
			ColumnModel col = findMatchColumnForJavaField(fieldName, box);
			if (!col.getTransientable() && col.getInsertable()) {
				if (col.getIdGenerationType() != null) {
					if (col.getIdGenerator() == null)
						throw new SqlBoxException("No IdGenerator found for column '" + col.getColumnName() + "'");
					IdGenerator idGen = col.getIdGenerator();
					if (GenerationType.IDENTITY.equals(idGen.getGenerationType())) {// Identity
						if (identityFieldName != null)
							throw new SqlBoxException(
									"More than 1 identity field found for table '" + tableModel.getTableName() + "'");
						identityType = col.getColumnType();
						identityFieldName = fieldName;
					} else if (GenerationType.SNOWFLAKE.equals(idGen.getGenerationType())) {// Snow
						iXxx.add(col.getColumnName());
						SnowflakeCreator snow = ctx.getSnowflakeCreator();
						if (snow == null)
							throw new SqlBoxException(
									"Current SqlBoxContext no SnowflakeCreator found when try to create a Snowflake value");
						Object id = snow.nextId();
						iXxx.add(param(id));
						iXxx.add(", ");
						foundColumnToInsert = true;
						ClassCacheUtils.writeValueToBeanField(entityBean, fieldName, id);
					} else {// Normal Id Generator
						iXxx.add(col.getColumnName());
						Object id = idGen.getNextID(ctx, ctx.getDialect(), col.getColumnType());
						iXxx.add(param(id));
						iXxx.add(", ");
						foundColumnToInsert = true;
						ClassCacheUtils.writeValueToBeanField(entityBean, fieldName, id);
					}
				} else {
					Object value = ClassCacheUtils.readValueFromBeanField(entityBean, fieldName);
					iXxx.add(col.getColumnName());
					iXxx.add(new SqlItem(SqlOption.PARAM, value));
					iXxx.add(", ");
					foundColumnToInsert = true;
				} 
				
				if (col.getShardDatabase() != null) //Sharding database?
					iXxx.add(ctx.shardDatabase(tableModel, ClassCacheUtils.readValueFromBeanField(entityBean, fieldName)));

				// move shardTable at front
				if (col.getShardTable() != null)
					shardTableItem = ctx.shardTable(tableModel,
							ClassCacheUtils.readValueFromBeanField(entityBean, fieldName));
			}
		}
		if (foundColumnToInsert)
			iXxx.remove(iXxx.size() - 1);// delete the last ", " character

		if (shardTableItem != null) //Sharding table?
			iXxx.add(0, shardTableItem);
		else
			iXxx.add(0, tableModel.getTableName());
		
		iXxx.add(0, "insert into ");// insert into xxx (
		iXxx.add(") "); // insert into xxx ()
		iXxx.add(valuesQuestions()); // insert into xxx () values(?,?)

		if (optionalSqlItems != null) // optional SqlItems put at end
			for (Object item : optionalSqlItems)
				iXxx.add(0, item);

		int result = ctx.iUpdate(iXxx.toArray(new Object[iXxx.size()]));
		if (ctx.isBatchEnabled())
			return result; // in batch mode, no need fetch Identity value
		if (identityFieldName != null) {// write identity id to Bean field
			Object identityId = IdentityIdGenerator.INSTANCE.getNextID(ctx, ctx.getDialect(), identityType);
			ClassCacheUtils.writeValueToBeanField(entityBean, identityFieldName, identityId);
		}
		return result;
	}

	/** Update entityBean according primary key */
	public static int update(SqlBoxContext ctx, Object entityBean, Object... optionalSqlItems) {
		SqlBox box = SqlBoxUtils.findAndBindSqlBox(ctx, entityBean);
		checkBeanAndBoxExist(entityBean, box);
		TableModel tableModel = box.getTableModel();

		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(tableModel.getTableName()).append(" set ");

		List<Object> normalParams = new ArrayList<Object>();
		List<Object> pkeyParams = new ArrayList<Object>();
		List<ColumnModel> pkeyColumns = new ArrayList<ColumnModel>();

		Map<String, Method> readMethods = ClassCacheUtils.getClassReadMethods(entityBean.getClass());
		for (String fieldName : readMethods.keySet()) {
			ColumnModel col = findMatchColumnForJavaField(fieldName, box);
			if (!col.getTransientable() && col.getUpdatable()) {
				Object value = ClassCacheUtils.readValueFromBeanField(entityBean, fieldName);
				if (!col.getPkey()) {
					normalParams.add(value);
					sb.append(col.getColumnName()).append("=?, ");
				} else {
					pkeyParams.add(value);
					pkeyColumns.add(col);
				}
			}
		}
		if (!normalParams.isEmpty())
			sb.setLength(sb.length() - 2);// delete the last ", " characters
		if (pkeyColumns.isEmpty())
			throw new SqlBoxException("No primary column setting found for entityBean");
		sb.append(" where ");// NOSONAR
		for (ColumnModel col : pkeyColumns)
			sb.append(col.getColumnName()).append("=? and ");
		sb.setLength(sb.length() - 5);// delete the last " and " characters
		for (Object pkeyParam : pkeyParams)
			normalParams.add(pkeyParam);// join PKey values

		List<Object> sqlItemList = SqlItem.toParamSqlItemList(normalParams);
		if (optionalSqlItems != null)
			for (Object item : optionalSqlItems)
				sqlItemList.add(item);
		return box.context.iUpdate(sb.toString(), sqlItemList.toArray(new Object[sqlItemList.size()]));
	}

	/**
	 * Delete entityBean in database according primary key value
	 */
	public static void delete(SqlBoxContext ctx, Object entityBean, Object... optionalSqlItems) {
		SqlBox box = SqlBoxUtils.findAndBindSqlBox(ctx, entityBean);
		checkBeanAndBoxExist(entityBean, box);
		TableModel tableModel = box.getTableModel();

		List<Object> pkeyParameters = new ArrayList<Object>();
		StringBuilder sb = new StringBuilder();
		sb.append("delete from ").append(tableModel.getTableName()).append(" where ");
		Map<String, Method> readMethods = ClassCacheUtils.getClassReadMethods(entityBean.getClass());
		for (String fieldName : readMethods.keySet()) {
			ColumnModel col = findMatchColumnForJavaField(fieldName, box);
			if (!col.getTransientable() && col.getPkey()) {
				Object value = ClassCacheUtils.readValueFromBeanField(entityBean, fieldName);
				sb.append(col.getColumnName()).append("=?, ");
				pkeyParameters.add(value);
			}
		}
		sb.setLength(sb.length() - 2);// delete the last "," character
		if (pkeyParameters.isEmpty())
			throw new SqlBoxException("No primary key set for entityBean");

		List<Object> sqlItemList = SqlItem.toParamSqlItemList(pkeyParameters);
		if (optionalSqlItems != null)
			for (Object item : optionalSqlItems)
				sqlItemList.add(item);
		int rowAffected = box.context.iUpdate(sb.toString(), sqlItemList.toArray(new Object[sqlItemList.size()]));

		if (ctx.isBatchEnabled())
			return;
		if (rowAffected <= 0)
			throw new SqlBoxException("No row be deleted for entityBean");
		if (rowAffected > 1)
			throw new SqlBoxException("Multiple rows affected when delete entityBean");
	}

	@SuppressWarnings("unchecked")
	public static <T> T load(SqlBoxContext ctx, Class<?> entityClass, Object pkeyValue, Object... optionalSqlItems) {// NOSONAR
		SqlBoxException.assureNotNull(entityClass, "entityClass can not be null");
		SqlBoxException.assureNotNull(pkeyValue, "pkey can not be null");

		Map<String, Object> pkValueMap = null;
		if (pkeyValue instanceof Map)
			pkValueMap = (Map<String, Object>) pkeyValue;// NOSONAR
		else {
			pkValueMap = new HashMap<String, Object>();
			pkValueMap.put("I'm the only PKey(by jSqlBox)", pkeyValue);
		}

		Object entity = null;
		try {
			entity = entityClass.newInstance();
		} catch (Exception e) {
			throw new SqlBoxException(e);
		}

		SqlBox box = SqlBoxUtils.findAndBindSqlBox(ctx, entity);
		TableModel model = box.getTableModel();

		StringBuilder sb = new StringBuilder("select ");
		List<Object> pkParams = new ArrayList<Object>();

		List<ColumnModel> pkeyColumns = new ArrayList<ColumnModel>();
		List<ColumnModel> allColumns = new ArrayList<ColumnModel>();
		List<String> allFieldNames = new ArrayList<String>();

		Map<String, Method> writeMethods = ClassCacheUtils.getClassWriteMethods(entityClass);

		for (String fieldName : writeMethods.keySet()) {
			ColumnModel col = findMatchColumnForJavaField(fieldName, box);
			if (!col.getTransientable()) {
				allColumns.add(col);
				allFieldNames.add(fieldName);
				sb.append(col.getColumnName()).append(", ");
				if (col.getPkey()) {
					pkeyColumns.add(col);
					if (pkValueMap.size() == 1)
						pkParams.add(pkValueMap.entrySet().iterator().next().getValue());
					else
						pkParams.add(pkValueMap.get(fieldName));
				}
			}
		}
		if (pkeyColumns.isEmpty())
			throw new SqlBoxException("No primary key set for entityBean");
		sb.setLength(sb.length() - 2);// delete the last ", "
		if (pkParams.size() != pkValueMap.size())
			throw new SqlBoxException("Wrong number of primary key parameters: expected " + pkParams.size()
					+ ", was given " + pkValueMap.size());

		sb.append(" from ").append(model.getTableName()).append(" where ");
		for (ColumnModel col : pkeyColumns)
			sb.append(col.getColumnName()).append("=? and ");
		sb.setLength(sb.length() - 5);// delete the last " and "

		try {

			List<Object> sqlItemList = SqlItem.toParamSqlItemList(pkParams);
			if (optionalSqlItems != null)
				for (Object item : optionalSqlItems)
					sqlItemList.add(item);

			Object[] values = ctx.iQuery(new ArrayHandler(), sb.toString(),
					sqlItemList.toArray(new Object[sqlItemList.size()]));
			for (int i = 0; i < values.length; i++) {
				Method writeMethod = writeMethods.get(allFieldNames.get(i));
				writeMethod.invoke(entity, values[i]);
			}
		} catch (Exception e) {
			throw new SqlBoxException(e);
		}
		return (T) entity;
	}

	private static void checkBeanAndBoxExist(Object entityBean, SqlBox box) {
		SqlBoxException.assureNotNull(entityBean, "Assert error, entityBean can not be null");
		SqlBoxException.assureNotNull(box, "Assert error, box of entityBean can not be null");
		SqlBoxException.assureNotNull(box.getContext(), "Assert error, box's SqlBoxContext can not be null");
		SqlBoxException.assureNotNull(box.getTableModel(), "Assert error, box's TableModel can not be null");
		SqlBoxException.assureNotEmpty(box.getTableModel().getTableName(),
				"Assert error, box's tableName can not be null");
	}

	/**
	 * @param ctx
	 * @param obj
	 * @return A TableModel instance related this object
	 */
	public static TableModel getTableModelFromEntityOrClass(SqlBoxContext ctx, Object entityOrClass) {
		if (entityOrClass == null)
			throw new SqlBoxException("Can build TableModel configuration for null netConfig");
		if (entityOrClass instanceof TableModel)
			return (TableModel) entityOrClass;
		else if (entityOrClass instanceof SqlBox)
			return ((SqlBox) entityOrClass).getTableModel();
		else if (entityOrClass instanceof Class)
			return SqlBoxUtils.createSqlBox(ctx, (Class<?>) entityOrClass).getTableModel();
		else
			return SqlBoxUtils.findAndBindSqlBox(ctx, entityOrClass).getTableModel();
	}

}