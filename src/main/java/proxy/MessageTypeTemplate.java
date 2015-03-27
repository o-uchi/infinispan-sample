package proxy;

import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

public class MessageTypeTemplate extends AbstractTemplate<MessageType> {
	private MessageTypeTemplate(){
	}

	@Override
	public void write(Packer pk, MessageType v, boolean required) throws IOException {
		if (v == null) {
			if (required) {
				throw new MessageTypeException("Attempted to write null");
			}
			pk.writeNil();
			return;
		}
		pk.write(v.getCode());
	}

	@Override
	public MessageType read(Unpacker u, MessageType to, boolean required) throws IOException {
		if (!required && u.trySkipNil()) {
			return null;
		}
		String temp = u.readString();
		return MessageType.fromValue(temp);
	}

	public static MessageTypeTemplate getInstance() {
		return instance;
	}

	static final MessageTypeTemplate instance = new MessageTypeTemplate();
}
