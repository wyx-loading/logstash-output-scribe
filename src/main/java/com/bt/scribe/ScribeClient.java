package com.bt.scribe;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import scribe.thrift.LogEntry;
import scribe.thrift.ResultCode;
import scribe.thrift.scribe.Client;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author wuyuxiang
 */
@Deprecated
public class ScribeClient {
	private static int MAX_SEND_TOGETHER = 50;
	private static boolean debugDots = false;

	public static void setMaxSendTogether(int maxNum) {
		MAX_SEND_TOGETHER = maxNum;
	}

	public static void setDebugDots(boolean debug) {
		debugDots = debug;
	}

	private final String remoteHost;
	private final Integer remotePort;

	private final IErrorHandler errorHandler;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final ConcurrentLinkedQueue<LogEntry> queue = new ConcurrentLinkedQueue<>();

	private TFramedTransport framedTransport;
	private Client client;

	private final Runnable sendMessageAsync = new Runnable() {
		@Override
		public void run() {
			LogEntry log = queue.poll();
			while(log != null) {
				int queueSize = queue.size();
				ArrayList<LogEntry> toSend = new ArrayList<>();
				int count = 0;
				StringBuilder sb = null;
				if(debugDots) {
					sb = new StringBuilder();
				}
				while(log != null && count < MAX_SEND_TOGETHER) {
					toSend.add(log);
					log = queue.poll();
					count++;
					if(debugDots) {
						sb.append(".");
					}
				}

				try {
					if(!isOpen()) {
						open();
					}

					ResultCode result = client.Log(toSend);
					if(result == ResultCode.OK) {
						if(debugDots) {
							System.out.print(sb.toString());
						}
					} else {
						// TODO
					}
				} catch (TException e) {
					errorHandler.handleException("Fail to send " + toSend.size() + " messages, buffer size: " + queue.size(), e);
					queue.addAll(toSend);
					if(log == null) {
						log = queue.poll();
					}
				}
			}
		}
	};

	public ScribeClient(String remoteHost, Integer remotePort) {
		this(remoteHost, remotePort, new DefaultErrorHandler());
	}

	public ScribeClient(String remoteHost, Integer remotePort, IErrorHandler errorHandler) {
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		this.errorHandler = errorHandler;
	}

	private boolean isOpen() {
		return framedTransport != null && framedTransport.isOpen();
	}

	private boolean open() {
		try {
			TSocket socket = new TSocket(new Socket(remoteHost, remotePort));
			framedTransport = new TFramedTransport(socket);
			TBinaryProtocol protocol = new TBinaryProtocol(framedTransport, false, false);
			client = new Client(protocol);
			return true;
		} catch (TTransportException | IOException e) {
			errorHandler.handleException("Fail to open ScribeClient", e);
			return false;
		}
	}

	public void close() {
		if(framedTransport != null) {
			framedTransport.close();
		}
	}

	public void sendMessage(String category, String message) {
		queue.offer(new LogEntry(category, message));
		executor.execute(sendMessageAsync);
	}

	public void sendMessages(List<LogEntry> entrys) {
		queue.addAll(entrys);
		executor.execute(sendMessageAsync);
	}
}
