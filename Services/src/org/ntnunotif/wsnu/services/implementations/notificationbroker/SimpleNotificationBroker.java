package org.ntnunotif.wsnu.services.implementations.notificationbroker;

import org.ntnunotif.wsnu.base.internal.ForwardingHub;
import org.ntnunotif.wsnu.base.internal.UnpackingRequestInformationConnector;
import org.ntnunotif.wsnu.base.util.Information;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.ntnunotif.wsnu.base.util.Log;
import org.ntnunotif.wsnu.base.util.RequestInformation;
import org.ntnunotif.wsnu.services.eventhandling.ConsumerListener;
import org.ntnunotif.wsnu.services.eventhandling.NotificationEvent;
import org.ntnunotif.wsnu.services.general.ServiceUtilities;
import org.oasis_open.docs.wsn.b_2.*;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.br_2.RegisterPublisherResponse;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationFailedFault;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationRejectedFault;
import org.oasis_open.docs.wsn.bw_2.*;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;
import org.w3._2001._12.soap_envelope.Envelope;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

/** Simple broker that stores publishers and subscriptions in hashmaps.
 * Created by tormod on 26.03.14.
 */
public class SimpleNotificationBroker extends AbstractNotificationBroker {

    private HashMap<String, ServiceUtilities.EndpointTerminationTuple> _subscriptions;
    private HashMap<String, ServiceUtilities.EndpointTerminationTuple> _publishers;

    @Override
    public boolean keyExists(String key) {
        return _subscriptions.containsKey(key) || _publishers.containsKey(key);
    }

    @Override
    public List<String> getRecipients(Notify notify) {
        return new ArrayList(_subscriptions.values());
    }

    @Override
    @Oneway
    @WebMethod(operationName = "Notify")
    public void notify(@WebParam(partName = "Notify", name = "Notify", targetNamespace = "http://docs.oasis-open.org/wsn/b-2")
                           Notify notify, @Information RequestInformation requestInformation) {
        NotificationEvent event  = new NotificationEvent(notify);

        for(ConsumerListener listener : _listeners){
            listener.notify(event);
        }
    }

    /**
     * Register a publisher. This implementation does not take into account topics, and will never throw TopicNotSupportededFault.
     * @param registerPublisherRequest
     * @param requestInformation
     * @return
     * @throws InvalidTopicExpressionFault
     * @throws PublisherRegistrationFailedFault
     * @throws ResourceUnknownFault
     * @throws PublisherRegistrationRejectedFault
     * @throws UnacceptableInitialTerminationTimeFault
     * @throws TopicNotSupportedFault
     */
    @Override
    @WebResult(name = "RegisterPublisherResponse", targetNamespace = "http://docs.oasis-open.org/wsn/br-2", partName = "RegisterPublisherResponse")
    @WebMethod(operationName = "RegisterPublisher")
    public RegisterPublisherResponse registerPublisher(
            @WebParam(partName = "RegisterPublisherRequest",name = "RegisterPublisher", targetNamespace = "http://docs.oasis-open.org/wsn/br-2")
            RegisterPublisher registerPublisherRequest, @Information RequestInformation requestInformation)
    throws InvalidTopicExpressionFault, PublisherRegistrationFailedFault, ResourceUnknownFault, PublisherRegistrationRejectedFault,
           UnacceptableInitialTerminationTimeFault, TopicNotSupportedFault {
        String endpointReference = null;
        try {
            endpointReference = ServiceUtilities.parseW3CEndpoint(registerPublisherRequest.getPublisherReference().toString());
        } catch (SubscribeCreationFailedFault subscribeCreationFailedFault) {
            throw new PublisherRegistrationFailedFault();
        }

        long terminationTime = registerPublisherRequest.getInitialTerminationTime().toGregorianCalendar().getTimeInMillis();

        if(terminationTime < System.currentTimeMillis()){
            throw new UnacceptableInitialTerminationTimeFault("Invalid termination time. Can't be before current time");
        }

        String newSubscriptionKey = generateSubscriptionKey();
        String subscriptionEndpoint = generateSubscriptionURL(newSubscriptionKey);

        _publishers.put(newSubscriptionKey, new ServiceUtilities.EndpointTerminationTuple(endpointReference, terminationTime));

        RegisterPublisherResponse response = new RegisterPublisherResponse();

        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        builder.address(getEndpointReference() +""+ subscriptionEndpoint);

        response.setConsumerReference(builder.build());
        //response.setPublisherRegistrationReference();
        return response;
    }

    /**
     * The oasis wsn subscribe function. This implementation will at all times be the same as the implementation in {@link org.ntnunotif.wsnu.services.implementations.notificationproducer.SimpleNotificationProducer}
     * @param subscribeRequest
     * @param requestInformation
     * @return
     * @throws NotifyMessageNotSupportedFault
     * @throws UnrecognizedPolicyRequestFault
     * @throws TopicExpressionDialectUnknownFault
     * @throws ResourceUnknownFault
     * @throws InvalidTopicExpressionFault
     * @throws UnsupportedPolicyRequestFault
     * @throws InvalidFilterFault
     * @throws InvalidProducerPropertiesExpressionFault
     * @throws UnacceptableInitialTerminationTimeFault
     * @throws SubscribeCreationFailedFault
     * @throws TopicNotSupportedFault
     * @throws InvalidMessageContentExpressionFault
     */
    @Override
    @WebResult(name = "SubscribeResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-2", partName = "SubscribeResponse")
    @WebMethod(operationName = "Subscribe")
    public SubscribeResponse subscribe(@WebParam(partName = "SubscribeRequest", name = "Subscribe", targetNamespace = "http://docs.oasis-open.org/wsn/b-2") Subscribe subscribeRequest, @Information RequestInformation requestInformation) throws NotifyMessageNotSupportedFault, UnrecognizedPolicyRequestFault, TopicExpressionDialectUnknownFault, ResourceUnknownFault, InvalidTopicExpressionFault, UnsupportedPolicyRequestFault, InvalidFilterFault, InvalidProducerPropertiesExpressionFault, UnacceptableInitialTerminationTimeFault, SubscribeCreationFailedFault, TopicNotSupportedFault, InvalidMessageContentExpressionFault {
        Log.d("SimpleNotificationProducer", "Got new subscription request");

        W3CEndpointReference consumerEndpoint = subscribeRequest.getConsumerReference();

        if(consumerEndpoint == null){
            throw new SubscribeCreationFailedFault("Missing EndpointReference");
        }

        //TODO: This is not particularly pretty, make WebService have a W3Cendpointreference variable instead of String?
        String endpointReference = ServiceUtilities.parseW3CEndpoint(consumerEndpoint.toString());

        FilterType filter = subscribeRequest.getFilter();

        if(filter != null){
            throw new InvalidFilterFault("Filters not supported for this NotificationProducer");
        }

        long terminationTime = 0;
        if(subscribeRequest.getInitialTerminationTime() != null){
            try {
                terminationTime = ServiceUtilities.interpretTerminationTime(subscribeRequest.getInitialTerminationTime().getValue());

                if(terminationTime < System.currentTimeMillis()){
                    throw new UnacceptableInitialTerminationTimeFault();
                }

            } catch (UnacceptableTerminationTimeFault unacceptableTerminationTimeFault) {
                throw new UnacceptableInitialTerminationTimeFault();
            }
        }else{
            /* Set it to terminate in one day */
            terminationTime = System.currentTimeMillis() + 86400*1000;
        }

        /* Generate the response */
        SubscribeResponse response = new SubscribeResponse();

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(terminationTime);

        try {
            XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar();
            response.setTerminationTime(calendar);
        } catch (DatatypeConfigurationException e) {
            Log.d("SimpleNotificationProducer", "Subscription request org.generated UnacceptableIntialTerminationTimeFault");
            throw new UnacceptableInitialTerminationTimeFault();
        }

        /* Generate new subscription hash */
        String newSubscriptionKey = generateSubscriptionKey();
        String subscriptionEndpoint = generateSubscriptionURL(newSubscriptionKey);

        /* Build endpoint reference */
        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        builder.address(getEndpointReference() +""+ subscriptionEndpoint);

        response.setSubscriptionReference(builder.build());

        /* Set up the subscription */
        _subscriptions.put(newSubscriptionKey, new ServiceUtilities.EndpointTerminationTuple(endpointReference, terminationTime));
        Log.d("SimpleNotificationProducer", "Added new subscription");

        return response;
    }

    @Override
    @WebResult(name = "GetCurrentMessageResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-2", partName = "GetCurrentMessageResponse")
    @WebMethod(operationName = "GetCurrentMessage")
    public GetCurrentMessageResponse getCurrentMessage(@WebParam(partName = "GetCurrentMessageRequest", name = "GetCurrentMessage", targetNamespace = "http://docs.oasis-open.org/wsn/b-2") GetCurrentMessage getCurrentMessageRequest) throws InvalidTopicExpressionFault, TopicExpressionDialectUnknownFault, MultipleTopicsSpecifiedFault, ResourceUnknownFault, NoCurrentMessageOnTopicFault, TopicNotSupportedFault {
        GetCurrentMessageResponse response = factory.createGetCurrentMessageResponse();
        response.getAny().add(currentMessage);
        return response;
    }

    @Override
    public Object acceptSoapMessage(@WebParam Envelope envelope, @Information RequestInformation requestInformation) {
        return null;
    }

    @Override
    public InternalMessage acceptRequest(@Information RequestInformation requestInformation) {
        return null;
    }

    @Override
    public ForwardingHub quickBuild() {
        try{
            ForwardingHub hub = new ForwardingHub();
            UnpackingRequestInformationConnector connector = new UnpackingRequestInformationConnector(this);
            hub.registerService(connector);
            this.registerConnection(connector);
            _hub = hub;
            return hub;
        }catch(Exception e){
            throw new RuntimeException("Unable to quickbuild: " + e.getMessage());
        }
    }
}
