package org.ntnunotif.wsnu.services.implementations.publisherregistrationmanager;

import org.ntnunotif.wsnu.base.internal.Hub;
import org.ntnunotif.wsnu.base.util.Log;
import org.ntnunotif.wsnu.base.util.RequestInformation;
import org.ntnunotif.wsnu.services.eventhandling.PublisherRegistrationEvent;
import org.ntnunotif.wsnu.services.general.ServiceUtilities;
import org.oasis_open.docs.wsn.br_2.DestroyRegistration;
import org.oasis_open.docs.wsn.br_2.DestroyRegistrationResponse;
import org.oasis_open.docs.wsn.brw_2.ResourceNotDestroyedFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tormod on 23.04.14.
 */
@WebService(targetNamespace = "http://docs.oasis-open.org/wsn/brw-2", name = "PublisherRegistrationManager")
@XmlSeeAlso({org.oasis_open.docs.wsn.t_1.ObjectFactory.class, org.oasis_open.docs.wsn.br_2.ObjectFactory.class, org.oasis_open.docs.wsrf.r_2.ObjectFactory.class, org.oasis_open.docs.wsrf.bf_2.ObjectFactory.class, org.oasis_open.docs.wsn.b_2.ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class SimplePublisherRegistrationManager extends AbstractPublisherRegistrationManager {

    private HashMap<String, Long> _publishers = new HashMap<>();

    /**
     * Empty Constructor
     */
    public SimplePublisherRegistrationManager() {
    }

    /**
     * Constructor taking a Hub argument, and passing it up to its {@link org.ntnunotif.wsnu.services.general.WebService}
     * super.
     * @param hub
     */
    public SimplePublisherRegistrationManager(Hub hub) {
        super(hub);
    }

    /**
     * Registers a publisher. This is a method that should be called when a {@link org.ntnunotif.wsnu.services.implementations.notificationbroker.AbstractNotificationBroker}
     * implementation receives a subscription.
     * @param endpointReference
     * @param subscriptionEnd
     */
    @Override
    @WebMethod(exclude = true)
    public void addPublisher(String endpointReference, long subscriptionEnd) {
        _publishers.put(endpointReference, subscriptionEnd);
    }

    /**
     * Removes a publisher. This method is mainly here to allow extensibility, i.e. if it is ever needed for this method
     * to be called externally. Per the current specification (2006 draft), this is not needed.
     * @param endpointReference
     */
    @Override
    @WebMethod(exclude = true)
    public void removePublisher(String endpointReference) {
        _publishers.remove(endpointReference);
    }

    /**
     * This method implements the {@link org.oasis_open.docs.wsn.brw_2.PublisherRegistrationManager}'s DestroyRegistration.
     *
     * The method conforms to the standard. Thus, any specifics can be found at
     * <href>http://docs.oasis-open.org/wsn/wsn-ws_brokered_notification-1.3-spec-os.htm#_Toc133294203</href>.
     *
     * Note that the subscription-reference is contained in the request-url.
     *
     * @param destroyRegistrationRequest The parsed object.
     * @return The DestryoRegistrationtResponse if everything went fine.
     * @throws ResourceNotDestroyedFault This is thrown if either the publisher-reference is ill-formatted,
     * or does not represent an existing publisher registration
     * @throws ResourceUnknownFault As of 0.3 this is never thrown as WS-Resources is not implemented
     */
    @Override
    @WebResult(name = "DestroyRegistrationResponse", targetNamespace = "http://docs.oasis-open.org/wsn/br-2", partName = "DestroyRegistrationResponse")
    @WebMethod(operationName = "DestroyRegistration")
    public DestroyRegistrationResponse destroyRegistration
    (
        @WebParam(partName = "DestroyRegistrationRequest", name = "DestroyRegistration", targetNamespace = "http://docs.oasis-open.org/wsn/br-2")
        DestroyRegistration destroyRegistrationRequest
    ) throws ResourceNotDestroyedFault, ResourceUnknownFault {
        Log.d("SimplePublishersRegistrationManager", "Received DestroyRegistration request");
        RequestInformation requestInformation = _connection.getRequestInformation();

        for (Map.Entry<String, String[]> entry : requestInformation.getParameters().entrySet()) {
            if(!entry.getKey().equals("publisherregistration")){
                continue;
            }

            if(entry.getValue().length > 1){
                String subRef = entry.getValue()[0];
                if(!_publishers.containsKey(subRef)){
                    _publishers.remove(subRef);
                    return new DestroyRegistrationResponse();
                }
                ServiceUtilities.throwResourceNotDestroyed("en", "Ill-formated subscription-parameter");
            } else if(entry.getValue().length == 0){
                ServiceUtilities.throwResourceNotDestroyed("en", "Subscription-parameter is missing value");
            }

            String subRef = entry.getValue()[0];

            /* The subscriptions is not recognized */
            if(!_publishers.containsKey(subRef)){
                Log.d("SimplePublishersRegistrationManager", "Subscription not found");
                Log.d("SimplePublishersRegistrationManager", "Expected: " + subRef);
                ServiceUtilities.throwResourceNotDestroyed("en", "Subscription not found.");
            }

            Log.d("SimplePublishersRegistrationManager", "Removed subscription");

            removePublisher(subRef);
            firePublisherRegistrationChanged(subRef, PublisherRegistrationEvent.Type.DESTROYED);

            return new DestroyRegistrationResponse();
        }
        ServiceUtilities.throwResourceNotDestroyed("en", "The registration was not found as any parameter" +
                " in the request-uri. Please send a request on the form: " +
                "\"http://urlofthis.domain/webservice/?publisherregistration=registrationkey");
        return null;
    }

    /**
     * Updates the publisher registrations. Removes any registration that is outdated.
     */
    @Override
    @WebMethod(exclude = true)
    public void update() {
        long timeNow = System.currentTimeMillis();
        Log.d("SimplePublisherRegistrationtManager", "Updating");
        synchronized(_publishers){
            ArrayList<String> toBeRemoved = new ArrayList<>();
            for(Map.Entry<String, Long> entry : _publishers.entrySet()){

                /* The subscription is expired */
                if(entry.getValue().longValue() > timeNow){
                    toBeRemoved.add(entry.getKey());
                }
            }

            for (String s : toBeRemoved) {
                _publishers.remove(s);
            }
        }
    }
}


