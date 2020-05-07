package com.study.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class BIODemoClient {

	String host;
	int port;
	int poolsize = 1;
	
	static int defalut_time_out=10*1000;

	Queue<SocketHolder> socketPool;

	ReentrantLock poolLock = new ReentrantLock();

	Condition poolEmpty = poolLock.newCondition();

	private static volatile BIODemoClient clientInstance;

	private BIODemoClient() {
	}

	public static BIODemoClient getClient() {
		if (clientInstance == null) {
			synchronized (BIODemoClient.class) {
				if (clientInstance == null) {
					clientInstance = new BIODemoClient();
				}
			}
		}
		return clientInstance;
	}

	public void init(String host, int port, int poolsize) {
		this.host = host;
		this.port = port;
		this.poolsize = poolsize;
		this.socketPool = new LinkedList<SocketHolder>();
	}

	public void connect() {
		for (; poolsize-- > 0;) {
			try {
				Socket socket = new Socket(host, port);
				System.out.println(socket+" connects success");
				socketPool.offer(new SocketHolder(socket,defalut_time_out));
			} catch (IOException e) {
				e.printStackTrace();
				LockSupport.parkNanos(clientInstance, TimeUnit.MILLISECONDS.toNanos(500));
			}
		}
	}
	
	public void closePool() {
		socketPool.forEach(SocketHolder::quietyCLose);
	}
	
	
	private SocketHolder getConnection() throws InterruptedException {
		ReentrantLock lock=this.poolLock;
		lock.lock();
		try {
			while(socketPool.isEmpty()) {
				poolEmpty.await();
			}
			return socketPool.poll();
		} finally {
			lock.unlock();
		}
	}
	
	private void returnConnection(SocketHolder s) {
		if(s==null) return;
		ReentrantLock lock=this.poolLock;
		lock.lock();
		try {
			 socketPool.offer(s);
			 poolEmpty.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public String sendMsg(String msg,int milis ) {
		SocketHolder conn=null;
			try {
				conn=getConnection();
				String result=conn.sendAndGet(msg,milis);
				return result;
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}finally {
				returnConnection(conn);
			}
	}
}

class SocketHolder{
	Socket socket;
	int timeout;//in ms
	
	BufferedReader br;
	BufferedWriter bw;

	public SocketHolder(Socket s,int timeout) {
		assert s != null;
		this.socket = s;
		this.timeout=timeout;
		try {
			this.socket.setKeepAlive(true);
			this.socket.setSoTimeout(timeout);
			
			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String sendAndGet(String msg,int ms) {
		try {
			this.socket.setSoTimeout(ms);
			bw.write(msg);
			bw.newLine();
			bw.flush();
			
			return br.readLine();//wait for timeout ms
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void quietyCLose() {
		if (this.socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (br != null) {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (bw != null) {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
