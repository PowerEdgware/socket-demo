package com.study.client;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ClientStarter {

	public static void main(String[] args) throws Exception {

		BIODemoClient client = BIODemoClient.getClient();

		String host = InetAddress.getLocalHost().getHostAddress();
		int port = 8890;
		int poolsize = 1;
		host = "139.196.85.101";
		
		client.init(host, port, poolsize);

		client.connect();

		//Thread sender = startSender(client);
//		Thread.sleep(5*1000);
		String longMsg=acquireMsg(3952);
		System.out.println("longMsg len="+longMsg.length());
		try {
			String result = client.sendMsg(longMsg, 3 * 1000);
			System.out.println(result);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			System.in.read();
			//sender.interrupt();
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			client.closePool();
		}
		Thread.sleep(2*1000);
	}

	static String abc="abcdefgHiJKlMNoPqRsTuvWXyz01234567890";
	private static String acquireMsg(int i) {
		StringBuilder buf=new StringBuilder("Start_");
		for(;;) {
			buf.append(abc.charAt(ThreadLocalRandom.current().nextInt(abc.length())));
			if(buf.length()>=i)break;
		}
		return buf.toString();
	}

	 static Thread startSender(BIODemoClient client) {
		Thread sender=new Thread("sender-thread") {
			int i=0;
			String msg="helloserver-";
			@Override
			public void run() {
				while(!isInterrupted()) {
					try {
						msg=msg+msg;
						System.out.println("Sending msg="+msg+" at="+LocalDateTime.now());
						String result = client.sendMsg(msg+(i++), 2 * 1000);
						System.out.println(result+" for i="+i);
					} catch (Exception e) {
						//java.net.SocketTimeoutException
						//SocketException
						e.printStackTrace();
						if(e.getCause() instanceof SocketTimeoutException) {//读取数据超时
							
						}else if(e.getCause() instanceof SocketException) {
							//RST
							break;
						}
					}
					LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
				}
				System.out.println(getName()+" stopped at:"+LocalDateTime.now());
			}
		};
		sender.start();
		return sender;
	}
}
