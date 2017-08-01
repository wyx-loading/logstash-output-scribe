package com.bt.scribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scribe.thrift.LogEntry;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wuyuxiang
 */
public class DefaultErrorHandler implements IErrorHandler {
	private final Logger logger = LoggerFactory.getLogger(DefaultErrorHandler.class);

	private final ConcurrentHashMap<String, Logger> categoryLoggerMap = new ConcurrentHashMap<>();

	private Logger getLogger(String category) {
		Logger lg = categoryLoggerMap.get(category);
		if(lg == null) {
			lg = categoryLoggerMap.computeIfAbsent(category, (key) -> {
				return LoggerFactory.getLogger(key);
			});
		}
		return lg;
	}

	public DefaultErrorHandler() {
	}

	@Override
	public void handleException(String detail, Throwable t) {
		logger.error(detail, t);
	}

	@Override
	public void recordSendFailMessages(List<LogEntry> logs) {
		for(LogEntry log : logs) {
			logger.info("{} {}", log.getCategory(), log.getMessage());
			getLogger(log.getCategory()).info(log.getMessage());
		}
	}
}
