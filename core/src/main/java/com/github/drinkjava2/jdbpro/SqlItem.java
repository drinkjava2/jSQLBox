/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.drinkjava2.jdbpro;

/**
 * SqlItem store SQL SqlItemType type and value array
 * 
 * @author Yong Zhu
 * @since 1.7.0.3
 */
public class SqlItem {

	public static final SqlItem MASTER = new SqlItem(SqlItemType.USE_MASTER);
	public static final SqlItem SLAVE = new SqlItem(SqlItemType.USE_SLAVE);

	private SqlItemType type;
	private Object[] parameters;

	public SqlItem(String sqlPiece) {
		this.type = SqlItemType.SQL;
		this.parameters = new Object[] { sqlPiece };
	}

	public SqlItem(SqlItemType type, Object... parameters) {
		this.type = type;
		this.parameters = parameters;
	}

	public Object[] getParameters() {
		return parameters;
	}

	public SqlItemType getType() {
		return type;
	}

	public void setType(SqlItemType type) {
		this.type = type;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

	@Override
	public String toString() {
		throw new DbProRuntimeException(
				"SqlItem toString() method overrided, not allowed to change to String directly");
	}

}