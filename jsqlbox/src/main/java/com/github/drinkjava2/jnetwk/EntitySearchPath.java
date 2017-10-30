/*
 * Copyright (C) 2016 Yong Zhu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.github.drinkjava2.jnetwk;

/**
 * SearchPath is a POJO store a search Path who to search nodes in EntityNet
 * 
 * @author Yong Zhu (Yong9981@gmail.com)
 * @since 1.0.0
 */
public class EntitySearchPath {
	public static final EntitySearchPath GUESS_PATH = new EntitySearchPath("GUESS_PATH");
	public static final EntitySearchPath PARENT_PATH = new EntitySearchPath("PARENT_PATH");
	public static final EntitySearchPath CHILD_PATH = new EntitySearchPath("CHILD_PATH");
	public static final EntitySearchPath TOP_PARENT_PATH = new EntitySearchPath("TOP_PARENT_PATH");
	public static final EntitySearchPath ALL_CHILD_PATH = new EntitySearchPath("ALL_CHILD_PATH");

	public EntitySearchPath() {

	}

	public EntitySearchPath(Object... paths) {

	}

}