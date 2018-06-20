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
package com.github.drinkjava2.jsqlbox.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.drinkjava2.jdbpro.DefaultOrderSqlHandler;
import com.github.drinkjava2.jdbpro.ImprovedQueryRunner;
import com.github.drinkjava2.jdbpro.PreparedSQL;
import com.github.drinkjava2.jdbpro.SingleTonHandlers;
import com.github.drinkjava2.jdialects.model.TableModel;
import com.github.drinkjava2.jsqlbox.SqlBoxContext;
import com.github.drinkjava2.jsqlbox.SqlBoxContextUtils;
import com.github.drinkjava2.jsqlbox.SqlBoxException;

/**
 * EntityListHandler is the SqlHandler used explain the Entity query SQL (For
 * example 'select u.** from users u') and return a List<entityObject> instance
 * 
 * @author Yong Zhu
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class EntityListHandler extends DefaultOrderSqlHandler {
	protected Object config = null;

	public EntityListHandler() {
	}

	public EntityListHandler(Object config) {
		this.config = config;
	}

	@Override
	public Object handle(ImprovedQueryRunner runner, PreparedSQL ps) {
		Object cfg = null;
		if (config != null) {
			cfg = config;
			if (ps.getModels() != null && ps.getModels().size() > 0)
				throw new SqlBoxException(
						"EntityListHandler already have config parameter, no need extra TableModel sqlItem parameter");
		} else {
			List<Object> tableModels = ps.getModels();
			if (tableModels == null || tableModels.isEmpty())
				throw new SqlBoxException("TableModel setting needed for EntityListHandler");
			if (tableModels.size() > 1)
				throw new SqlBoxException("TableModel setting should only have 1 for EntityListHandler");
			cfg = (TableModel) tableModels.get(0);
		}

		ps.setResultSetHandler(SingleTonHandlers.mapListHandler);
		List<Map<String, Object>> maps = (List<Map<String, Object>>) runner.runPreparedSQL(ps);
		List<Object> entityList = new ArrayList<Object>();
		for (Map<String, Object> row : maps) {
			Object entity = SqlBoxContextUtils.mapToEntityBean((SqlBoxContext) runner, cfg, row);
			entityList.add(entity);
		}
		return entityList;
	}

}
