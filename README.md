
Simple chat built on top of Errai Message Bus and GwtBootstrap3

	Errai is a RedHat fork of the Google Web Toolkit with many amazing features, but the most famous Errai’s component is MessageBus which allows us to setup bi-directional communication between client and server. There are several ways how to manage this like GWT-RPC, RequestBuilder, RequestFactory or RestyGWT, but MessageBus allows us to transparently simplify server-push messaging over WebSockets without additional effort. If MessageBus fails to establish WebSockets connect, it performs Comet, if not, MessageBus uses LongPooling, which means, your clients will always be able to stay connected to your server, and accordingly, you will be able to send them events from the server side. And that’s just one of many key features of such a great framework.

	Firstly, you can try an online demo http://188.166.33.112/erraiMessageBusChat/ , please feel free to tell me if something is wrong :-) Secondly, source code is available on github https://github.com/treblereel/ErraiMessageBusChat. It’s very easy to run it locally, just check that you have Maven installed. To run application locally put in console:

>mvn clean gwt:run

or, you can build .war for your Wildfly instance :

> mvn -pjboss7 clean install

So far so good, let’s do a little dive into the code. All examples given below work on both client and server side without any changes, except only one thing, on client you can get MessageBus like that: 

private MessageBus bus = ErraiBus.get();

… on server via injection:

```java
 @Inject
 private MessageBus msgBus;
```

Here we are subscribing to subject “ChatClient” on client side, and callback will receive a message sent via bus.This message may contain instance of some class (here it’s MyMessage.class, which annotated @Portable and is available for client and server, it’s DTO object, of cause you can send any number of object inside your message).

```java
bus.subscribe("ChatClient", new MessageCallback() {
      public void callback(Message message) {
        MyMessage newMessage = message.get(MyMessage.class,"message");
            processMessage(newMessage);
      }
    });
```

On server side I use MessageBuilder, which helps me to construct my message. There I have two options: I can send  a message personally or do a broadcast delivery, here I do personal delivery via .with(MessageParts.SessionID, authorSessionId). So easy!

```java
MessageBuilder.createMessage().toSubject("ChatClient").signalling()
    .with(MessageParts.SessionID, authorSessionId)

.with("message", chatMessage)
.noErrorHandling().sendNowWith(msgBus);
```





By the way, I also can do conversations with the client, subject already provided, and I don’t want to dial with ErrorHandling here. One thing to note, MessageBus allows us to do something like direct client-to-client conversation,  all we need is to store information about users and their sessions (in my example I use SessionKeeper object to handle private messaging and session management), to route message through server.
PS. Reply() will be caught by repliesTo(new MessageCallback() as in the next example.

```java
public void callback(Message message) {
    QueueSession sess = message.getResource(QueueSession.class, Resources.Session.name());
    MessageBuilder.createConversation(message)
			  .subjectProvided()
			  .signalling()
     	  		  .with(MessageParts.SessionID,sess.getSessionId())
			  .with("result", true)
			  .noErrorHandling()
        		  .reply();

}
```

You can see a way more complex example below. In that case I need to know if something goes wrong so I use ErrorCallback() for that purpose, and I also want to get answer from recipient, MessageCallback() is very suitable for that.

```java
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
```
In conclusion, Errai is a very suitable and comfortable tool for purpose RIA, and MessageBus is just a part of it. Documentation sometimes is a little bit outdated, but developers are always ready to help on forum or the IRC channel.
