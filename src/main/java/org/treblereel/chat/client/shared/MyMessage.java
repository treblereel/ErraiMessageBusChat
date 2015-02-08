package org.treblereel.chat.client.shared;

import org.jboss.errai.common.client.api.annotations.Portable;

@Portable
public class MyMessage {
	private User author;
	private String message;
	private String recipient;
	private MessageType type;

	public MyMessage() {

	}

	public MessageType getType() {
		return type;
	}

	public MyMessage setType(MessageType type) {
		this.type = type;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public MyMessage setMessage(String message) {
		this.message = message;
		return this;
	}

	public String getRecipient() {
		return recipient;
	}

	public MyMessage setRecipient(String recipient) {
		this.recipient = recipient;
		return this;
	}

	public User getAuthor() {
		return author;
	}

	public MyMessage setAuthor(User author) {
		this.author = author;
		return this;
	}

	@Portable
	public enum MessageType {
		PRIVATE(), PUBLIC(),SYSTEM();
	}
}
