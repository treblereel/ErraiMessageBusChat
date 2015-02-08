package org.treblereel.chat.client.local;

import org.jboss.errai.ioc.client.Container;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

public class HelloWorldClientTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "org.treblereel.chat.App";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    
    // We need to bootstrap the IoC container manually because GWTTestCase
    // doesn't call onModuleLoad() for us.
    new Container().onModuleLoad();
  }
  
  public void testSendMessage() throws Exception {
	  
    ErraiIocTestHelper.afterBusInitialized(new Runnable() {
      @Override
      public void run() {
        final ChatClient client = ErraiIocTestHelper.instance.client;
        assertNotNull(client);
        
        // send a message using the bus (it is now initialized)
        System.out.println("Sent message");
        
        // wait a few seconds, then check that the server response caused a DOM update
        new Timer() {
          @Override
          public void run() {
            System.out.println("Checking for update");
            
          //  String labelText = client.getResponseLabel().getText();
            
//            assertTrue("Unexpected label contents after pressing button: \"" + labelText + "\"",
//                labelText.startsWith("Message from Server: Hello, World! The server's time is now"));
//            finishTest();
          }
        }.schedule(2000);

      }
    });
    delayTestFinish(20000);
  }
}
