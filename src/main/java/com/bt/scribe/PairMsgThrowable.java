package com.bt.scribe;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author wuyuxiang
 */
public class PairMsgThrowable {
	private final String errorMsg;
	private final Throwable throwable;

	public PairMsgThrowable(String errorMsg, Throwable throwable) {
		this.errorMsg = errorMsg;
		this.throwable= throwable;
	}

	public String getStackTrace() {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public Throwable getThrowable() {
		return throwable;
	}
}
