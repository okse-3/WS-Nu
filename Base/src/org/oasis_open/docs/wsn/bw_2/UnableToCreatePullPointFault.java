
package org.oasis_open.docs.wsn.bw_2;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 2.7.10
 * 2014-03-03T11:43:03.961+01:00
 * Generated source version: 2.7.10
 */

@WebFault(name = "UnableToCreatePullPointFault", targetNamespace = "http://docs.oasis-open.org/wsn/b-2")
public class UnableToCreatePullPointFault extends Exception {
    
    private org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType unableToCreatePullPointFault;

    public UnableToCreatePullPointFault() {
        super();
    }
    
    public UnableToCreatePullPointFault(String message) {
        super(message);
    }
    
    public UnableToCreatePullPointFault(String message, Throwable cause) {
        super(message, cause);
    }

    public UnableToCreatePullPointFault(String message, org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType unableToCreatePullPointFault) {
        super(message);
        this.unableToCreatePullPointFault = unableToCreatePullPointFault;
    }

    public UnableToCreatePullPointFault(String message, org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType unableToCreatePullPointFault, Throwable cause) {
        super(message, cause);
        this.unableToCreatePullPointFault = unableToCreatePullPointFault;
    }

    public org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType getFaultInfo() {
        return this.unableToCreatePullPointFault;
    }
}