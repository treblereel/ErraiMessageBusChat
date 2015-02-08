package org.treblereel.chat.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import org.treblereel.chat.client.shared.User;

@Singleton
public class SessionKeeper {
  private HashMap<String, User> sessions = new HashMap<String, User>();

  public void addSession(String sessionId, String username) {
    sessions.putIfAbsent(sessionId, new User(username, "SUCCESS"));
  }

  public String getUsernameBySessionId(String sessionId){
	  if(sessions.containsKey(sessionId))
		return sessions.get(sessionId).getUsername();
	  return "";
	}

  public String getSessionIdByUsername(String username) {
    for (Entry<String, User> kv : sessions.entrySet()) {
      if (kv.getValue().getUsername().equals(username))
        return kv.getKey();
    }
    return "";
  }

  public User updateUser(String sessionId, String username, String color) {
    return sessions.put(sessionId, new User(username, color));
  }

  public boolean isUserExist(String sessionId) {
    if (sessions.containsKey(sessionId)) {
      return true;
    }
    return false;
  }

  public void removeSession(String sessionId) {
    sessions.remove(sessionId);
  }

  public List<User> getUsers() {
    if (!sessions.isEmpty()) {
      List<User> users = new ArrayList<User>();
      for (Entry<String, User> kv : sessions.entrySet()) {
        users.add(kv.getValue());
      }
      return users;
    }
    return Collections.emptyList();
  }

  public HashMap<String, User> getSessions() {
    return sessions;
  }

}
