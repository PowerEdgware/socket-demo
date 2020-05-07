package com.study.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import com.study.util.Constants;

public class BIODemoServer {

	private static final BIODemoServer INSTANCE = new BIODemoServer();

	private BIODemoServer() {
	}

	ServerSocket serverSocket;
	int port;

	int backlog = 1;

	boolean accept = true;

	ExecutorService businessExecutor;

	ExecutorService acceptorExecutor;

	AtomicBoolean started = new AtomicBoolean(false);

	public static BIODemoServer getInstance() {
		return INSTANCE;
	}

	public void init(int port, int backlog) throws IOException {
		this.backlog = backlog;
		this.port = port;

		accept = Boolean.parseBoolean(System.getProperty("server.accept", "true"));

		serverSocket = new ServerSocket(port, backlog);

		shutdownHook = new Thread("shutdown-thread-" + serverSocket.getLocalSocketAddress()) {
			public void run() {
				BIODemoServer.this.stop();
			}
		};

		businessExecutor = Executors.newCachedThreadPool();
		acceptorExecutor = Executors.newFixedThreadPool(1);
		System.out.println("Server init ok.backlog=" + backlog + " port=" + port + " accept=" + accept);
	}

	public void start() throws IOException {
		if (started.get()) {
			return;
		}
		if (started.compareAndSet(false, true)) {
			acceptorExecutor.submit(this::doAccept);
			System.out.println("Server start ok bindIp=" + serverSocket.getLocalSocketAddress());

			addShutDownHook();
		}
	}

	public void stop() {
		if (!started.get()) {
			return;
		}
		if (started.compareAndSet(true, false)) {
			System.out.println("Server ready stop bindIp=" + serverSocket.getLocalSocketAddress());
			cleanUp();
		}

	}

	void doAccept() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				if (!accept) {
					System.out.println("server not accept." + accept);
					LockSupport.parkNanos(this, TimeUnit.SECONDS.toNanos(15));
					continue;
				}
				Socket socket = serverSocket.accept();
				
				System.out.println("server incomes socket=" + socket);

				businessExecutor.execute(new SocketRunner(socket, Constants.DEFAULT_TIME_OUT));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.stop();
	}

	void cleanUp() {
		acceptorExecutor.shutdownNow();
		businessExecutor.shutdownNow();
		if (serverSocket != null) {
			try {
				serverSocket.close();// SocketException
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Server stopped bindIp=" + serverSocket.getLocalSocketAddress());
		}
	}

	Thread shutdownHook;

	void addShutDownHook() {
		try {
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

class SocketRunner implements Runnable {
	Socket socket;
	int timeout;
	BufferedReader br;
	BufferedWriter bw;

	public SocketRunner(Socket s, int timeout) {
		assert s != null;
		this.socket = s;
		this.timeout = timeout;
		try {
			socket.setSoTimeout(this.timeout);

			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void run() {
		Thread current = Thread.currentThread();
		while (!Thread.currentThread().isInterrupted()) {
			try {
				String data = br.readLine();
				while (data != null) {
					System.out.println(current.getName() + "<=== Recv:" + data);

					bw.write(current.getName() + " Resp." + LocalDateTime.now());
					bw.newLine();
					bw.flush();

					data = br.readLine();
				}
				break;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println(current.getName() + " socket=" + socket + " closeing");

		quietyCLose();
	}

	void quietyCLose() {
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
