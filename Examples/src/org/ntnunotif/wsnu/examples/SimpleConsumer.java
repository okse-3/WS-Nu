package org.ntnunotif.wsnu.examples;

import org.ntnunotif.wsnu.base.internal.ForwardingHub;
import org.ntnunotif.wsnu.base.internal.Hub;
import org.ntnunotif.wsnu.base.internal.UnpackingConnector;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.ntnunotif.wsnu.base.util.Log;
import org.ntnunotif.wsnu.services.implementations.notificationconsumer.NotificationConsumer;
import org.ntnunotif.wsnu.services.eventhandling.ConsumerListener;
import org.ntnunotif.wsnu.services.eventhandling.NotificationEvent;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.ObjectFactory;
import org.oasis_open.docs.wsn.b_2.Subscribe;

import javax.management.Notification;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static org.ntnunotif.wsnu.base.util.InternalMessage.*;

/**
 * Created by tormod on 3/17/14.
 */
public class SimpleConsumer implements ConsumerListener {

    private Hub hub;
    private NotificationConsumer consumer;

    public static void main(String[] args) throws Exception{
        SimpleConsumer simpleConsumer = new SimpleConsumer();
    }


    public void sendSubscriptionRequest(String address){
        ObjectFactory factory = new ObjectFactory();
        Subscribe subscribe = factory.createSubscribe();

        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        builder.address(consumer.getEndpointReference());

        W3CEndpointReference reference = builder.build();
        subscribe.setConsumerReference(reference);

        subscribe.setInitialTerminationTime(factory.createSubscribeInitialTerminationTime("P1Y"));

        InternalMessage message = new InternalMessage(STATUS_OK|STATUS_HAS_MESSAGE, subscribe);

        hub.acceptLocalMessage(message);

    }

    public SimpleConsumer() {
        NotificationConsumer consumer = new NotificationConsumer();
        hub = consumer.quickBuild();
        consumer.addConsumerListener(this);
        InputManager in = new InputManager();
        in.start();
    }

    private class InputManager{

        public InputManager(){

        }

        public void start(){
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Inputmanager started...\n");
            String in;
            try {
                while((in = reader.readLine()) != null){
                    if(in.matches("^exit")){
                        System.exit(0);
                    }else if(in.matches("^subscribe")){
                        String address = in.replaceAll("^subscribe *", "");
                        Log.d("SimpleConsumer", "Parsed endpointreference: " + address);

                        if(!address.matches(("^https?://"))){
                            Log.d("SimpleConsumer", "Inserted http://-tag");
                            address = "http://" + address;
                        }
                        SimpleConsumer.this.sendSubscriptionRequest(address);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }

    }

    @Override
    public void notify(NotificationEvent event) {
        /* This is a org.ntnunotif.wsnu.examples.SimpleConsumer, so we just take an event, display its contents, and leave */

        Notify notification = event.getRaw();

        List<Object> everything = notification.getAny();

        for (Object o : everything) {
            System.out.println(o.getClass());
            System.out.println(o);
        }
    }
}
