package com.study;

import java.net.InetAddress;

public class Demo1 {

	public static void main(String[] args) throws Exception {
//		System.out.println(java.net.InetAddress.getLocalHost().getCanonicalHostName());
		System.out.println(InetAddress.getByName("allger-1").getHostAddress());
	}
}
