package proxy;

import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.msgpack.MessagePack;
import org.msgpack.template.Template;
import org.msgpack.type.Value;
import org.msgpack.unpacker.BufferUnpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.nio.channels.AsynchronousChannelGroup.withThreadPool;
import static java.util.concurrent.Executors.*;
import static java.util.stream.Collectors.toList;
import static org.msgpack.template.Templates.*;
import static proxy.MessageType.*;

public class ProxyServiceController {

	private static final Logger logger = LoggerFactory.getLogger(ProxyServiceController.class);

	private static ExecutorService service = newSingleThreadExecutor(r -> {
		Thread th = new Thread(r);
		th.setDaemon(true);
		return th;
	});

	private static int heartbeatInterval = 30000;
	private static int ackThreadNo = 5;
	private static List<ExecutorService> ackExecutorServices;
	private static ScheduledExecutorService scheduledExecutorService = newScheduledThreadPool(1);
	private static Set<ConnectedClient> connectedClients =  new CopyOnWriteArraySet<>();

	public static void main(String[] args) throws IOException {
		ackExecutorServices = IntStream.range(0, ackThreadNo).mapToObj(i -> newSingleThreadExecutor()).collect(toList());

		DefaultCacheManager cacheManager = new DefaultCacheManager("infinispan.xml");
		Cache<Object, Object> abcd = cacheManager.getCache("abcd");

		MessagePack messagePack = new MessagePack();
		messagePack.register(MessageType.class, MessageTypeTemplate.getInstance());
		Template<Map<Integer, Value>> messageTemplate = tMap(TInteger, TValue);
		Template<List<Map<Value, Value>>> cacheDataTemplate = tList(tMap(TValue, TValue));

		int port = 9000;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
		SocketAddress local = new InetSocketAddress(port);
		AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open(withThreadPool(service)).bind(local);
		serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
				@Override
				public void completed(AsynchronousSocketChannel socketChannel, Void attachment) {
					serverSocketChannel.accept(null, this);

					InetSocketAddress remoteAddress;
					try {
						remoteAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
					} catch (IOException e) {
						logger.error(e.getMessage(), e);
						return;
					}
					String hostString = remoteAddress.getHostString();
					int fourth = remoteAddress.getAddress().getAddress()[3] & 0xff;
					ExecutorService ackExecutor = ackExecutorServices.get(fourth % ackThreadNo);

					ConnectedClient client = new ConnectedClient(socketChannel, hostString, ackExecutor);
					connectedClients.add(client);
					logger.info("Connected. {}", client.getHostString());

					client.setHeartbeatFuture(scheduledExecutorService.scheduleAtFixedRate(() -> {
						boolean active = client.getCommunicated().compareAndSet(true, false);
						if (!active) {
							// heartbeat send
							Map<Integer, Object> heartbeat = new HashMap<>();
							heartbeat.put(1, HEARTBEAT);
							heartbeat.put(2, connectedClients.size());
							heartbeat.put(3, heartbeatInterval);
							try {
								client.write(ByteBuffer.wrap(messagePack.write(heartbeat)));
							} catch (IOException e) {
								logger.error(e.getMessage(), e);
							}
						}
					}, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS));

					ByteBuffer dst = ByteBuffer.allocate(10240);
					socketChannel.read(dst, dst, new CompletionHandler<Integer, ByteBuffer>() {
						@Override
						public void completed(Integer result, ByteBuffer byteBuffer) {
							if (result == -1) {
								try {
									client.close();
								} catch (IOException e) {
									// ignore
								}
								connectedClients.remove(client);
								logger.info("Disconnected. {}", client.getHostString());
								return;
							}
							client.getCommunicated().set(true);
							byteBuffer.flip();
							BufferUnpacker bufferUnpacker = messagePack.createBufferUnpacker(byteBuffer);
							while(byteBuffer.hasRemaining()) {
								byteBuffer.mark();
								try {
									Map<Integer, Value> request = bufferUnpacker.read(messageTemplate);
									MessageType messageType = messagePack.convert(request.get(1), MessageTypeTemplate.getInstance());
									int requestId = request.get(2).asIntegerValue().getInt();

									switch (messageType) {
										case PUT:
										case REPLACE:
										case PUTIFABSENT:
										case REMOVE: {
											String cacheName = request.get(3).asRawValue().getString();
											Cache<Object, Object> cache = cacheManager.getCache(cacheName, false);
											if (cache == null) {
												logger.warn("unknown CacheName = {}", cacheName);
												continue;
											}
											byte[] cacheDataBytes;
											int compressOption = request.get(5).asIntegerValue().getInt();
											if (compressOption == 1) {
												int decompressSize = request.get(7).asIntegerValue().getInt();
												cacheDataBytes = new byte[decompressSize];
												LZ4SafeDecompressor lz4SafeDecompressor = LZ4Factory.safeInstance().safeDecompressor();
												lz4SafeDecompressor.decompress(request.get(4).asRawValue().getByteArray(), cacheDataBytes);
											} else {
												cacheDataBytes = request.get(4).asRawValue().getByteArray();
											}

											List<Map<Value, Value>> cacheData = messagePack.read(cacheDataBytes, cacheDataTemplate);
											cacheData.stream().flatMap(data -> data.entrySet().stream()).forEach(entry -> {
												Object key = entry.getKey().asRawValue().getString();
												Object value = entry.getValue().asRawValue().getString();
												switch (messageType) {
													case PUT:
														cache.put(key, value);
														break;
													case REPLACE:
														cache.replace(key, value);
														break;
													case PUTIFABSENT:
														cache.putIfAbsent(key, value);
														break;
													case REMOVE:
														cache.remove(key);
														break;
													default:
														break;
												}
											});

											Map<Integer, Object> ackData = new HashMap<>();
											ackData.put(1, requestId);
											ackData.put(5, 0);
											Map<Integer, Object> ack = createAck(ackData);
											client.write(ByteBuffer.wrap(messagePack.write(ack)));
											client.getCommunicated().set(true);
											break;
										}
										case GET:
										case SUBSCRIBE:
										case GETANDSUBSCRIBE: {
											String cacheName = request.get(3).asRawValue().getString();
											Cache<Object, Object> cache = cacheManager.getCache(cacheName, false);
											if (cache == null) {
												logger.warn("unknown CacheName = {}", cacheName);
												continue;
											}

											Optional<Value> queryName = Optional.ofNullable(request.get(4));
											Optional<Value> queryValues = Optional.ofNullable(request.get(5));
											Optional<Value> compressOption = Optional.ofNullable(request.get(6));
											Optional<Value> arraySize = Optional.ofNullable(request.get(8));
											Optional<Value> sortName = Optional.ofNullable(request.get(9));

											List<Object> results = cache.values().stream().sorted().collect(toList());
											int size = arraySize.isPresent() ? arraySize.get().asIntegerValue().getInt() : results.size();
											long maxSize = results.size() != 0 ? (results.size() + size - 1) / size : 1L;
											IntStream.iterate(0, i -> i + size).limit(maxSize).forEach(i -> {
												int to = i + size;
												if (results.size() < to) {
													to = results.size();
												}
												Map<Integer, Object> ackData = new HashMap<>();
												ackData.put(1, requestId);
												ackData.put(2, cacheName);
												ackData.put(3, results.subList(i, to));
												compressOption.ifPresent(v -> ackData.put(4, v.asIntegerValue().intValue()));
												ackData.put(5, 0);
												if (maxSize > 1) {
													ackData.put(7, maxSize);
													ackData.put(8, to == results.size() ? maxSize : to / size);
												}
												Map<Integer, Object> ack = createAck(ackData);
												try {
													client.write(ByteBuffer.wrap(messagePack.write(ack)));
													client.getCommunicated().set(true);
												} catch (IOException e) {
													e.printStackTrace();
												}
											});
											break;
										}
										case UNSUBSCRIBE:

											break;
										case BEGIN:
										case COMMIT:
										case ROLLBACK:

											break;
										case LOCK:

											break;
										default:
											//
											logger.warn("unknown messageType = {}", messageType);
											//continue;
									}
								} catch (EOFException e) {
									// ignore
								} catch (IOException e) {
									logger.error(e.getMessage(), e);
								}
							}
							byteBuffer.reset();
							byteBuffer.compact();
							socketChannel.read(byteBuffer, byteBuffer, this);
						}

						@Override
						public void failed(Throwable exc, ByteBuffer attachment) {
							logger.error(exc.getMessage(), exc);
						}

						public Map<Integer, Object> createAck(Map<Integer, Object> ackData){
							Map<Integer, Object> ack = new HashMap<>();
							ack.put(1, ACK);
							ack.put(2, Arrays.asList(ackData));
							return ack;
						}
					});
				}
				@Override
				public void failed(Throwable exc, Void attachment) {
					logger.error(exc.getMessage(), exc);
				}
			});

		while (System.in.read() > 0) {
			System.out.printf("size:%d%n", abcd.size());
		}
	}

//	public ByteBuffer createMessage(MessagePack messagePack, Map<Integer, Object> data) throws IOException {
//		return ByteBuffer.wrap(messagePack.write(data));
//	}
}
