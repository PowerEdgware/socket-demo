package com.study.server;

import java.io.IOException;

public class ServerStarter {

	public static void main(String[] args) {
		
		BIODemoServer server=BIODemoServer.getInstance();
		
		int port=Integer.parseInt(System.getProperty("server.port", "8890"));
		int backlog=Integer.parseInt(System.getProperty("server.backlog", "10"));;
		try {
			server.init(port, backlog);
			
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
