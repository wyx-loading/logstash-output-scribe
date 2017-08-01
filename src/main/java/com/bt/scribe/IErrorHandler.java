package com.bt.scribe;

import scribe.thrift.LogEntry;

import java.util.List;

/**
 * @author wuyuxiang
 */
public interface IErrorHandler {
	void handleException(String detail, Throwable t);
	void recordSendFailMessages(List<LogEntry> logs);
}
