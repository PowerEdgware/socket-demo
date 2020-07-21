package com.study;

import java.net.Inet4Address;
import java.net.InetAddress;

public class Demo1 {

	public static void main(String[] args) throws Exception {
//		System.out.println(java.net.InetAddress.getLocalHost().getCanonicalHostName());
		//System.out.println(InetAddress.getByName("allger-1").getHostAddress());
		String host = "139.196.85.101";
		InetAddress address=Inet4Address.getByName(host);
		//send ICPM查询消息
		boolean reach=address.isReachable(30*1000);
		System.out.println(reach);
	}
}
