package org.treblereel.chat.client.local;

import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.Label;
import org.gwtbootstrap3.client.ui.ListGroup;
import org.gwtbootstrap3.client.ui.ListGroupItem;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.IconSize;
import org.gwtbootstrap3.client.ui.constants.IconType;
import org.gwtbootstrap3.client.ui.constants.LabelType;
import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.base.MessageBuilder;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.bus.client.api.messaging.MessageCallback;
import org.jboss.errai.bus.client.api.messaging.MessageBus;
import org.jboss.errai.bus.server.annotations.Service;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.ioc.client.api.EntryPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.treblereel.chat.client.local.widget.PropertyModalPanel;
import org.treblereel.chat.client.shared.MyMessage;
import org.treblereel.chat.client.shared.User;
import org.treblereel.chat.client.shared.MyMessage.MessageType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

@EntryPoint
@Service
@ApplicationScoped
public class ChatClient extends Composite {

  @UiTemplate("ChatClient.ui.xml")
  interface ChatClientUiBinder extends UiBinder<Widget, ChatClient> {
  }
  private static final String BROADCAST_MESSAGE = "Broadcast message";
  private static final String PRIVATE_MESSAGE = "Private message to";
  private static ChatClientUiBinder uiBinder = GWT.create(ChatClientUiBinder.class);
  private MessageBus bus = ErraiBus.get();
  @UiField
  Button cancelButton;
  @UiField
  ListGroup chatList;
  private MyMessage.MessageType conversationType = MyMessage.MessageType.PUBLIC;



  private User currentUser = new User("anonymous", "primary");
  final Logger logger = LoggerFactory.getLogger(ChatClient.class.getName());
  @UiField
  TextBox messageField;
  @UiField
  Label messageTypeLabel;
  private RootLayoutPanel panel;
  private Random random = new Random();
  private String recipient = "";

  @UiField
  Button sendMessageButton;

  @UiField
  Label usernameLabel;;

  @UiField
  ListGroup usersList;

  private MyMessage assembleMessage() {
    return new MyMessage().setAuthor(this.currentUser).setMessage(messageField.getValue())
        .setType(conversationType).setRecipient(recipient);
  }

  @PostConstruct
  public void buildUI() {
    initWidget(uiBinder.createAndBindUi(this));
    panel = RootLayoutPanel.get();
    panel.add(this);
    initSubscribers();
  }

  @UiHandler("cancelButton")
  public void clearMessageClickHandler(final ClickEvent event) {
    restoreOnCancelOrDelivery();
  }

  public User getCurrentUser() {
    return currentUser;
  }

  @SuppressWarnings({"unchecked"})
  private void initSubscribers() {
    bus.subscribe("ActiveUserList", new MessageCallback() {
      public void callback(Message message) {
        usersList.getElement().removeAllChildren();
        List<User> users = (List<User>) message.get(List.class, "users");
        for (User u : users) {
          ListGroupItem listGroupItem = new ListGroupItem();
          listGroupItem.setId("activeUserList_" + random.nextInt());
          listGroupItem.setText(u.getUsername());
          listGroupItem.getElement().setAttribute("username", u.getUsername());
          listGroupItem.getElement().addClassName("list-group-item-" + u.getColor().toLowerCase());
          listGroupItem.sinkEvents(Event.ONCLICK);
          listGroupItem.addHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              String username = event.getRelativeElement().getAttribute("username");
              if (username.equals("anonymous")) {
                Window.alert("Sorry, we doesn't provide sending private messanges to anonymous");
              } else {
                sendPrivateMessage(username);
              }
            }
          }, ClickEvent.getType());
          usersList.add(listGroupItem);
        }
      }
    });

    /**
     * Perform : receive new Message
     * 
     * @param message
     */
    bus.subscribe("ChatClient", new MessageCallback() {
      public void callback(Message message) {
        MyMessage newMessage = message.get(MyMessage.class, "message");
        logger.info("chatMessage " + newMessage.getMessage() + " " + newMessage.getRecipient()
            + " " + newMessage.getAuthor().getColor() + " " + newMessage.getType() + " "
            + newMessage.getAuthor().getColor());
        processMessage(newMessage);
      }
    });
  }

  private void processMessage(MyMessage newMessage) {
    ListGroupItem message = new ListGroupItem();
    Icon icon = new Icon();
    icon.setSize(IconSize.LARGE);
    if (newMessage.getType().equals(MyMessage.MessageType.SYSTEM)) {
      icon.setType(IconType.COG);
    } else if (newMessage.getType().equals(MyMessage.MessageType.PUBLIC)) {
      icon.setType(IconType.USER);
    } else {
      icon.setType(IconType.USER_SECRET);
    }

    message.insert(icon, 0);
    message.setId("message_" + random.nextInt());
    message.setText(" " + DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " "
        + newMessage.getAuthor().getUsername() + " says : " + newMessage.getMessage());
    message.getElement().addClassName(
        "list-group-item-" + newMessage.getAuthor().getColor().toLowerCase());
    chatList.add(message);
  }

  @UiHandler("propertyButton")
  public void propertyButtonhandleClick(final ClickEvent event) {
    PropertyModalPanel p = new PropertyModalPanel(this);
    p.show();
  }

  private void restoreOnCancelOrDelivery() {
    if (conversationType.equals(MyMessage.MessageType.PRIVATE)) {
      conversationType = MyMessage.MessageType.PUBLIC;
      messageTypeLabel.setText(BROADCAST_MESSAGE);
      recipient = "";
    }
    messageField.clear();
  }

  void sendMessage() {
    MessageBuilder.createMessage().toSubject("ChatService").with("message", assembleMessage())
        .errorsHandledBy(new ErrorCallback() {
          @Override
          public boolean error(Object message, Throwable throwable) {
            Window.alert("ChatService can't send message, try late");
            return false;
          }
        }).repliesTo(new MessageCallback() {
          public void callback(Message message) {
            Boolean result = message.get(Boolean.class, "result");
            if (result) {
              restoreOnCancelOrDelivery();
            }
          }
        }).sendNowWith(bus);
  }

  @UiHandler("sendMessageButton")
  public void sendMessageButtonHandleClick(final ClickEvent event) {
    if (!sendMessageButton.getText().isEmpty())
      sendMessage();
  }

  @UiHandler("messageField")
  public void sendMessageClickHandler(KeyDownEvent event) {
    if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER && !sendMessageButton.getText().isEmpty()) {
      sendMessage();
    }
  }

  private void sendPrivateMessage(String recipient) {
    messageTypeLabel.setText(PRIVATE_MESSAGE + " " + recipient);
    conversationType = MyMessage.MessageType.PRIVATE;
    this.recipient = recipient;
  }

  public void setCurrentUser(User user) {
    if (!user.getUsername().isEmpty()) {
      currentUser.setUsername(user.getUsername());
      usernameLabel.setText(user.getUsername());
    }
    if (!user.getColor().isEmpty()) {
      this.currentUser.setColor(user.getColor());
      usernameLabel.setType(LabelType.fromStyleName("label-" + user.getColor().toLowerCase()));
    }
  }


}
