package com.bt.scribe;

import scribe.thrift.LogEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wuyuxiang
 */
public class SendResult {
	private boolean suc;
	private ArrayList<PairMsgThrowable> exMsgs;
	private ArrayList<LogEntry> failLogs;

	public SendResult merge(SendResult other) {
		this.suc = suc & other.suc;
		if(other.exMsgs != null) {
			addEx(other.exMsgs);
		}
		if(other.failLogs != null) {
			addFailLogs(other.failLogs);
		}
		return this;
	}

	public SendResult(String errorMessage, Throwable t) {
		this(false);
		addEx(errorMessage, t);
	}

	public SendResult(boolean suc) {
		this.suc = suc;
		this.exMsgs = null;
		this.failLogs = null;
	}

	public void addEx(String message, Throwable t) {
		if(this.exMsgs == null) {
			this.exMsgs = new ArrayList<>();
		}
		this.exMsgs.add(new PairMsgThrowable(message, t));
	}

	public void addEx(List<PairMsgThrowable> exes) {
		if(this.exMsgs == null) {
			this.exMsgs = new ArrayList<>(exes);
		}
		this.exMsgs.addAll(exes);
	}

	public void addFailLogs(List<LogEntry>logs) {
		if(this.failLogs == null) {
			this.failLogs = new ArrayList<>(logs.size());
		}
		this.failLogs.addAll(logs);
	}

	public boolean isSuc() {
		return suc;
	}

	public void setSuc(boolean suc) {
		this.suc = suc;
	}

	public ArrayList<PairMsgThrowable> getExMsgs() {
		return exMsgs;
	}

	public ArrayList<LogEntry> getFailLogs() {
		return failLogs;
	}
}
