package proxy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectedClient {

	private final AsynchronousSocketChannel socketChannel;

	private final String hostString;

	private final Executor ackExecutor;

	private final AtomicBoolean communicated = new AtomicBoolean();

	private ScheduledFuture heartbeatFuture;

	public ConnectedClient(AsynchronousSocketChannel socketChannel, String hostString, Executor ackExecutor){
		this.socketChannel = socketChannel;
		this.hostString = hostString;
		this.ackExecutor = ackExecutor;
	}

	public AtomicBoolean getCommunicated() {
		return communicated;
	}

	public String getHostString() {
		return hostString;
	}

	public void setHeartbeatFuture(ScheduledFuture heartbeatFuture) {
		this.heartbeatFuture = heartbeatFuture;
	}

	public void write(ByteBuffer buffer) {
		ackExecutor.execute(() -> {
			if (socketChannel.isOpen()) {
				try {
					socketChannel.write(buffer).get();
					//communicated.set(true);
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void close() throws IOException {
		heartbeatFuture.cancel(true);
		heartbeatFuture = null;
		socketChannel.close();
	}
}
