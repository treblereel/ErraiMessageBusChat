package org.treblereel.chat.client.local.widget;

import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.RadioButton;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.constants.ButtonSize;
import org.gwtbootstrap3.client.ui.constants.ButtonType;
import org.gwtbootstrap3.client.ui.constants.Toggle;
import org.gwtbootstrap3.client.ui.html.Span;
import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.client.api.messaging.MessageBus;
import org.jboss.errai.bus.client.api.messaging.MessageCallback;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.bus.client.api.base.MessageBuilder;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.treblereel.chat.client.local.ChatClient;
import org.treblereel.chat.client.shared.User;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;

public class PropertyModalPanel extends Modal {
  private MessageBus bus = ErraiBus.get();

  private ButtonGroup buttonGroup = addButtons();
  private ChatClient chatClient;
  final Logger logger = LoggerFactory.getLogger(PropertyModalPanel.class.getName());
  private Span resultSpan = new Span();
  private Button saveButton;
  private HandlerRegistration saveButtonHandlerRegistration;
  private TextBox textBox;
  private String userColor = "PRIMARY";

  public PropertyModalPanel(ChatClient chatClient) {
    super();
    this.chatClient = chatClient;
    setTitle("Properties");
    setClosable(true);

    ModalBody usernameBody = new ModalBody();

    usernameBody.add(new Span("Change name:"));
    textBox = new TextBox();
    textBox.setMaxLength(24);
    textBox.setValue(chatClient.getCurrentUser().getUsername());
    usernameBody.add(textBox);
    add(usernameBody);

    ModalBody colorBody = new ModalBody();
    colorBody.add(new Span("Change message color:"));
    colorBody.add(buttonGroup);
    add(colorBody);

    ModalFooter modalFooter = new ModalFooter();
    modalFooter.add(resultSpan);
    resultSpan.getElement().getStyle().setFloat(com.google.gwt.dom.client.Style.Float.LEFT);

    saveButton = new Button("Save");
    saveButtonHandlerRegistration = saveButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        if (!textBox.getValue().isEmpty()) {
          sendUpdatedUserProperties(textBox.getValue());
        }
      }
    });
    modalFooter.add(saveButton);
    modalFooter.add(new Button("Cancel", new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        hide();
      }
    }));
    add(modalFooter);
    show();
  }

  private ButtonGroup addButtons() {
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setHeight("4em");
    buttonGroup.setJustified(true);
    buttonGroup.setDataToggle(Toggle.BUTTONS);
    for (final ButtonType t : ButtonType.values()) {
      RadioButton radioButton = new RadioButton(t.toString());
      radioButton.setType(t);
      radioButton.setSize(ButtonSize.SMALL);
      radioButton.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          userColor = t.name();
        }
      });
      buttonGroup.add(radioButton);
    }
    return buttonGroup;
  }

  private void changeSaveButtonToCloseState() {
    textBox.setEnabled(false);
    saveButtonHandlerRegistration.removeHandler();
    saveButton.setText("Close");
    saveButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        hide();
      }
    });
  }

  private void sendUpdatedUserProperties(final String value) {
    MessageBuilder.createMessage().toSubject("ChangeUsernameSubject").signalling()
        .with("username", value).with("color", userColor.toLowerCase())
        .errorsHandledBy(new ErrorCallback<Object>() {
          @Override
          public boolean error(Object message, Throwable throwable) {
            resultSpan.setText("Failed :" + message + " " + throwable.getMessage());
            changeSaveButtonToCloseState();
            return true;
          }
        }).repliesTo(new MessageCallback() {
          @Override
          public void callback(Message message) {
            String answer = message.get(String.class, "message");
            Boolean result = message.get(Boolean.class, "result");
            if (result) {
              changeSaveButtonToCloseState();
            }
            resultSpan.setText(result + ":  " + answer);
            chatClient.setCurrentUser(new User(value, userColor));
          }
        }).sendNowWith(bus);
  }
}
