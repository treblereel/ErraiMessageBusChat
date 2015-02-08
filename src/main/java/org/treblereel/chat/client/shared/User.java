package org.treblereel.chat.client.shared;


import org.jboss.errai.common.client.api.annotations.Portable;

@Portable
public class User{

	private String username;
	private String color;

	public User() {
	}

	public User(String name, String color) {
		setUsername(name);
		setColor(color);
	}


	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
