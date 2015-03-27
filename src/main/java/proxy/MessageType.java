package proxy;


import java.util.Map;
import java.util.stream.Stream;

import static java.util.function.Function.*;
import static java.util.stream.Collectors.*;

public enum MessageType {
	PUT("S01"),

	REPLACE("S02"),

	REMOVE("S03"),

	PUTIFABSENT("S04"),

	GET("G01"),

	SUBSCRIBE("G02"),

	GETANDSUBSCRIBE("G03"),

	UNSUBSCRIBE("G04"),

	ACK("A01"),

	HEARTBEAT("A02"),

	BEGIN("T01"),

	COMMIT("T02"),

	ROLLBACK("T03"),

	LOCK("L01");

	private final String code;

	private static final Map<String, MessageType> stringToEnum = Stream.of(values()).collect(toMap(MessageType::getCode, identity()));

	private MessageType(final String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static MessageType fromValue(String code) {
		return stringToEnum.get(code);
	}
}
