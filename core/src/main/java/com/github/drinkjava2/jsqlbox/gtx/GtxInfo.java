/*
 * Copyright (C) 2016 Original Author
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package com.github.drinkjava2.jsqlbox.gtx;

import java.util.ArrayList;
import java.util.List;

import com.github.drinkjava2.jtransactions.TxInfo;

/**
 * This class store id, gtx log and gtx lock, will be saved to 3 tables in
 * GTX server
 * 
 * @author Yong Zhu
 * @since 2.0.7
 */
public class GtxInfo extends TxInfo {
	protected GtxId gtxId;

	protected List<GtxLog> gtxLogList = null;

	protected List<GtxLock> gtxLockList = null;

	public String getDebugInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append("id=" + gtxId).append("\r");
		sb.append("gtxLockList=" + gtxLockList).append("\r");
		sb.append("gtxLogList=" + gtxLogList).append("\r");
		return sb.toString();
	}

	public List<GtxLock> getGtxLockList() {
		if (gtxLockList == null)
			gtxLockList = new ArrayList<GtxLock>();
		return gtxLockList;
	}

	public List<GtxLog> getGtxLogList() {
		if (gtxLogList == null)
			gtxLogList = new ArrayList<GtxLog>();
		return gtxLogList;
	}

	// getter & setter=========
	public GtxId getGtxId() {
		return gtxId;
	}

	public void setGtxId(GtxId gtxId) {
		this.gtxId = gtxId;
	}

	public void setGtxLogList(List<GtxLog> gtxLogList) {
		this.gtxLogList = gtxLogList;
	}

	public void setGtxLockList(List<GtxLock> gtxLockList) {
		this.gtxLockList = gtxLockList;
	}

}