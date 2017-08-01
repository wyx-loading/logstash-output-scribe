package com.bt.scribe;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import scribe.thrift.LogEntry;
import scribe.thrift.ResultCode;
import scribe.thrift.scribe.Client;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * @author wuyuxiang
 */
public class MultiScribeClient {
	private final String remoteHost;
	private final Integer remotePort;

	private volatile int maxMsgCountSendOnce = 50;
	private volatile int maxRetryTimes = 1;

	private TFramedTransport framedTransport;
	private Client client;

	public MultiScribeClient(String remoteHost, Integer remotePort) {
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;

		this.framedTransport = null;
		this.client = null;
	}

	private boolean isOpen() {
		return framedTransport != null && framedTransport.isOpen();
	}

	private SendResult open() {
		try {
			TSocket socket = new TSocket(new Socket(remoteHost, remotePort));
			framedTransport = new TFramedTransport(socket);
			TBinaryProtocol protocol = new TBinaryProtocol(framedTransport, false, false);
			client = new Client(protocol);
			return new SendResult(true);
		} catch (TTransportException | IOException e) {
			return new SendResult("Fail to open scribe client", e);
		}
	}

	public void close() {
		try {
			if(framedTransport != null) {
				framedTransport.close();
			}
		} catch (Throwable ignored) {
		}
		this.framedTransport = null;
		this.client = null;
	}

	private SendResult retrySendMessages(final List<LogEntry> logs) {
		SendResult result = new SendResult(true);
		int times = 0;
		boolean fatal = false;
		do {
			if(!isOpen()) {
				result.merge(open());
			}
			try {
				ResultCode resultCode = client.Log(logs);
				if(resultCode == ResultCode.OK) {
					result.setSuc(true);
					return result;
				}
				fatal = false;
			} catch (Throwable t) {
				result.addEx("retrySendMessages size: " + logs.size() + " fail", t);
				fatal = true;
				break;
			}

			times++;
		} while(times < maxRetryTimes);
		result.setSuc(false);
		result.addFailLogs(logs);
		if(fatal) {
			close();
		}
		return result;
	}

	private SendResult sliceLogs(List<LogEntry> logs) {
		int start = 0;
		int total = logs.size();
		SendResult result = new SendResult(true);
		while(start < total) {
			int end = Math.min(start + maxMsgCountSendOnce, total);
			result.merge(retrySendMessages(logs.subList(start, end)));
			start = end;
		}
		return result;
	}

	public void setMaxMsgCountSendOnce(int maxMsgCountSendOnce) {
		this.maxMsgCountSendOnce = maxMsgCountSendOnce;
	}

	public void setMaxRetryTimes(int maxRetryTimes) {
		this.maxRetryTimes = maxRetryTimes;
	}

	public SendResult sendMessages(List<LogEntry> logs) {
		return sliceLogs(logs);
	}

	public static class MultiScribeClientBuilder {
		private final String remoteHost;
		private final Integer remotePort;
		private Integer maxMsgCountSendOnce;
		private Integer maxRetryTimes;

		public MultiScribeClientBuilder(String remoteHost, Integer remotePort) {
			this.remoteHost = remoteHost;
			this.remotePort = remotePort;
		}

		public MultiScribeClientBuilder maxMsgCountSendOnce(int maxMsgCountSendOnce) {
			maxMsgCountSendOnce	= maxMsgCountSendOnce;
			return this;
		}

		public MultiScribeClientBuilder maxRetryTimes(int maxRetryTimes) {
			maxRetryTimes = maxRetryTimes;
			return this;
		}

		public MultiScribeClient build() {
			MultiScribeClient client = new MultiScribeClient(remoteHost, remotePort);
			if(maxMsgCountSendOnce != null) {
				client.setMaxMsgCountSendOnce(maxMsgCountSendOnce);
			}
			if(maxRetryTimes != null) {
				client.setMaxRetryTimes(maxRetryTimes);
			}
			return client;
		}
	}

}
