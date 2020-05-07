package com.study.client;

import java.net.InetAddress;

public class ClientStarter {

	public static void main(String[] args) throws Exception {

		BIODemoClient client = BIODemoClient.getClient();

		String host = InetAddress.getLocalHost().getHostAddress();
		int port = 8890;
		int poolsize = 10;
		host = "139.196.85.101";
		
		client.init(host, port, poolsize);

		client.connect();

//		new Thread() {
//			@Override
//			public void run() {
//				while(!isInterrupted()) {
//				String result = client.sendMsg("hello server", 100 * 1000);
//				
//				System.out.println(result);
//				}
//			}
//		}.start();
		Thread.sleep(50*1000);
		
		String result = client.sendMsg("hello server", 30 * 1000);
		
		System.out.println(result);

		try {
			System.in.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
		client.closePool();
	}
}
