package org.treblereel.chat.server;

import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.treblereel.chat.client.shared.MyMessage;
import org.treblereel.chat.client.shared.User;
import org.treblereel.chat.client.shared.MyMessage.MessageType;
import org.jboss.errai.bus.client.api.QueueSession;
import org.jboss.errai.bus.client.api.SubscribeListener;
import org.jboss.errai.bus.client.api.UnsubscribeListener;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.bus.client.api.messaging.MessageBus;
import org.jboss.errai.bus.client.api.messaging.MessageCallback;
import org.jboss.errai.bus.client.api.base.MessageBuilder;
import org.jboss.errai.bus.client.framework.SubscriptionEvent;
import org.jboss.errai.bus.server.annotations.Service;
import org.jboss.errai.common.client.protocols.MessageParts;
import org.jboss.errai.common.client.protocols.Resources;

@Service
public class ChatService implements MessageCallback {
  private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
  @Inject
  private MessageBus msgBus;

  private Random random = new Random();

  @Inject
  private SessionKeeper sessionKeeper;

  public void callback(Message message) {
    QueueSession sess = message.getResource(QueueSession.class, Resources.Session.name());
    MessageBuilder.createConversation(message).subjectProvided().signalling()
        .with(MessageParts.SessionID, sess.getSessionId()).with("result", true).noErrorHandling()
        .reply();

    MyMessage chatMessage = message.get(MyMessage.class, "message");
    if (chatMessage.getType().equals(MessageType.PUBLIC)) {
      sendBroadcastMessage(chatMessage);
    } else if(chatMessage.getType().equals(MessageType.PRIVATE)){
      sendPrivateMessage(chatMessage);
    }
  }

  @Inject
  private void init() {

    /**
     * On user connected
     * 
     */
    msgBus.addSubscribeListener(new SubscribeListener() {
      @Override
      public void onSubscribe(SubscriptionEvent event) {
        if (event.getSessionId() != null && event.getSubject().equals("SSEAgent")) {
          String session = event.getSessionId();
          String anonymous = "anonymous";
          if (sessionKeeper.isUserExist(session)) {
            if (!sessionKeeper.getSessionIdByUsername(anonymous).isEmpty()) {
              anonymous += "_" + random.nextInt(10000);
            }
          }
          if (!sessionKeeper.isUserExist(session)) {
            sessionKeeper.addSession(session, anonymous);
            // i do a little timeout because listener on client side may be not ready
            new Thread(new Runnable() {
              @Override
              public void run() {
                try {
                  Thread.sleep(300);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
                sendSystemMessage("new unknown user just joins the channel");
                updateUserListToSubscribers(sessionKeeper.getUsers());
              }
            }).start();
          }
        }
      }
    });



    /**
     * On Disconnect
     */

    msgBus.addUnsubscribeListener(new UnsubscribeListener() {
      @Override
      public void onUnsubscribe(SubscriptionEvent event) {
        String username = "unknown";
        if (event.getSessionId() != null)
          if (sessionKeeper.getUserBySessionId(event.getSessionId()) != null) {
            username = sessionKeeper.getUserBySessionId(event.getSessionId()).getUsername();
            if (!username.isEmpty()) {
              sendSystemMessage(username + " has left the channel");
            }
          }
        sessionKeeper.removeSession(event.getSessionId());
        updateUserListToSubscribers(sessionKeeper.getUsers());
        logger.info("user disconnected, session " + event.getSessionId());
      }
    });

    /**
     * alert on user name changed
     */

    msgBus.subscribe("ChangeUsernameSubject", new MessageCallback() {
      @Override
      public void callback(Message message) {
        logger.info("ChangeUsernameSubject " + message.get(String.class, "username"));
        QueueSession sess = message.getResource(QueueSession.class, Resources.Session.name());

        String oldname = sessionKeeper.getUserBySessionId(sess.getSessionId()).getUsername();
        String username = message.get(String.class, "username");

        String color = message.get(String.class, "color");
        Boolean result = true;
        String answer = "user properties saved";
        if (sessionKeeper.getSessionIdByUsername(username).equals(sess.getSessionId())
            || (sessionKeeper.getSessionIdByUsername(oldname).equals(sess.getSessionId()) && oldname
                .equals(username)) || sessionKeeper.getSessionIdByUsername(username).isEmpty()) {
          User oldUser = sessionKeeper.updateUser(sess.getSessionId(), username, color);
          if (!oldname.equals(username))
            sendSystemMessage("user has changed his name from " + oldUser.getUsername() + " to "
                + username);
          updateUserListToSubscribers(sessionKeeper.getUsers());
        } else {
          result = false;
          answer = "username already in use";
        }

        MessageBuilder.createConversation(message).subjectProvided().signalling()
            .with(MessageParts.SessionID, sess.getSessionId()).with("result", result)
            .with("message", answer).noErrorHandling().reply();
      }
    });

  }

  private void sendBroadcastMessage(MyMessage chatMessage) {
    MessageBuilder.createMessage().toSubject("ChatClient").signalling()
        .with("message", chatMessage).noErrorHandling().sendNowWith(msgBus);
  }

  private void sendPrivateMessage(MyMessage chatMessage) {
    String recipientSessionId = sessionKeeper.getSessionIdByUsername(chatMessage.getRecipient());
    chatMessage.setMessage(" to "+  chatMessage.getRecipient()  + " : " + chatMessage.getMessage());
    MessageBuilder.createMessage().toSubject("ChatClient").signalling()
        .with(MessageParts.SessionID, recipientSessionId).with("message", chatMessage)
        .noErrorHandling().sendNowWith(msgBus);
    
    /* We would like to info the user that their private message is *delivered*, actually, we should check
    /* that a recipient receives that massage, but it's just an example, isn't it?
    */
    
    String authorSessionId = sessionKeeper.getSessionIdByUsername(chatMessage.getAuthor().getUsername());
    MessageBuilder.createMessage().toSubject("ChatClient").signalling()
    .with(MessageParts.SessionID, authorSessionId).with("message", chatMessage)
    .noErrorHandling().sendNowWith(msgBus);
    logger.info("sendPrivateMessage done");
  }

  private void sendSystemMessage(String text) {
    sendBroadcastMessage(new MyMessage().setAuthor(new User("system", "default"))
        .setType(MyMessage.MessageType.SYSTEM).setMessage(text));
  }

  private void updateUserListToSubscribers(List<User> users) {
    MessageBuilder.createMessage().toSubject("ActiveUserList").signalling().with("users", users)
        .noErrorHandling().sendNowWith(msgBus);
  }

}
