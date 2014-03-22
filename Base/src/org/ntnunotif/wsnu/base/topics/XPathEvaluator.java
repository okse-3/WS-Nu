package org.ntnunotif.wsnu.base.topics;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType;
import org.oasis_open.docs.wsn.b_2.TopicExpressionDialectUnknownFaultType;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
import org.oasis_open.docs.wsn.bw_2.InvalidTopicExpressionFault;
import org.oasis_open.docs.wsn.bw_2.MultipleTopicsSpecifiedFault;
import org.oasis_open.docs.wsn.bw_2.TopicExpressionDialectUnknownFault;
import org.oasis_open.docs.wsn.t_1.TopicNamespaceType;
import org.oasis_open.docs.wsn.t_1.TopicSetType;
import org.oasis_open.docs.wsn.t_1.TopicType;

import org.oasis_open.docs.wsrf.bf_2.BaseFaultType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.*;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by Inge on 10.03.14.
 */
public class XPathEvaluator implements TopicExpressionEvaluatorInterface {

    /**
     * The dialect this evaluator supports
     */
    public static final String dialectURI = "http://www.w3.org/TR/1999/REC-xpath-19991116";

    @Override
    public String getDialectURIAsString() {
        return dialectURI;
    }

    @Override
    public boolean evaluateTopicWithExpression(TopicExpressionType topicExpressionType, TopicType topicType)
            throws TopicExpressionDialectUnknownFault, InvalidTopicExpressionFault {
        throw new UnsupportedOperationException("Namespace evaluation is still not implemented");
        /*
        if (!topicExpressionType.getDialect().equals(dialectURI)) {
            // TODO Fill in exception
            throw new TopicExpressionDialectUnknownFault();
        }
        String expression = null;
        for (Object o : topicExpressionType.getContent()) {
            if (o instanceof String) {
                if (expression != null) {
                    // TODO respond to multiple strings in expression
                }
                expression = (String) o;
            }
        }
        if (expression == null) {
            // TODO Find exception for no expression in tag
        }
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        XPathExpression xPathExpression = null;
        try {
            xPathExpression = xPath.compile(expression);
        } catch (XPathExpressionException e) {
            // TODO fill in exception
            throw new InvalidTopicExpressionFault();
        }
        // TODO This is not yet implemented
        return false;
        */
    }

    @Override
    public TopicSetType getIntersection(TopicExpressionType topicExpressionType, TopicSetType topicSetType, NamespaceContext namespaceContext)
            throws TopicExpressionDialectUnknownFault, InvalidTopicExpressionFault {
        if (!topicExpressionType.getDialect().equals(dialectURI)) {
            TopicExpressionDialectUnknownFaultType faultType = new TopicExpressionDialectUnknownFaultType();
            faultType.setTimestamp(new XMLGregorianCalendarImpl(new GregorianCalendar(TimeZone.getTimeZone("UTC"))));
            BaseFaultType.Description description = new BaseFaultType.Description();
            description.setLang("en");
            description.setValue("Could not evaluate dialect! Given dialect was " + topicExpressionType.getDialect() +
                    " but only " + dialectURI + " is allowed!");
            faultType.getDescription().add(description);
            throw new TopicExpressionDialectUnknownFault(description.getValue(), faultType);
        }

        // Find expression string
        String expression = null;
        for (Object o : topicExpressionType.getContent()) {
            if (o instanceof String) {
                if (expression != null) {
                    InvalidTopicExpressionFaultType faultType = new InvalidTopicExpressionFaultType();
                    faultType.setTimestamp(new XMLGregorianCalendarImpl(new GregorianCalendar(TimeZone.getTimeZone("UTC"))));
                    BaseFaultType.Description description = new BaseFaultType.Description();
                    description.setLang("en");
                    description.setValue("The given content of the expression was not an XPath expression!");
                    faultType.getDescription().add(description);
                    throw new InvalidTopicExpressionFault(description.getValue(), faultType);
                }
                expression = (String) o;
            }
        }
        if (expression == null) {
            InvalidTopicExpressionFaultType faultType = new InvalidTopicExpressionFaultType();
            faultType.setTimestamp(new XMLGregorianCalendarImpl(new GregorianCalendar(TimeZone.getTimeZone("UTC"))));
            BaseFaultType.Description description = new BaseFaultType.Description();
            description.setLang("en");
            description.setValue("No expression was given, and thus can not be evaluated!");
            faultType.getDescription().add(description);
            throw new InvalidTopicExpressionFault(description.getValue(), faultType);
        }

        // Build XPath environment
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        // This should make expression solution more correct
        xPath.setNamespaceContext(namespaceContext);
        XPathExpression xPathExpression;
        try {
            xPathExpression = xPath.compile(expression);
        } catch (XPathExpressionException e) {
            InvalidTopicExpressionFaultType faultType = new InvalidTopicExpressionFaultType();
            faultType.setTimestamp(new XMLGregorianCalendarImpl(new GregorianCalendar(TimeZone.getTimeZone("UTC"))));
            BaseFaultType.Description description = new BaseFaultType.Description();
            description.setLang("en");
            description.setValue("Topic expression claimed to be an XPath expression, but was not!");
            faultType.getDescription().add(description);
            throw new InvalidTopicExpressionFault(description.getValue(), faultType);
        }


        // For every object in topicSet, try to evaluate it against expression and store result.
        int returnCount = 0;
        TopicSetType returnSet = new TopicSetType();
        for (Object o: topicSetType.getAny()) {
            try {
                NodeList nodeList = (NodeList) xPathExpression.evaluate(o, XPathConstants.NODESET);
                // If result contained topics, add them to return topic set.
                returnCount += nodeList.getLength();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    returnSet.getAny().add(node);
                }
            } catch (XPathExpressionException e) {
                InvalidTopicExpressionFaultType faultType = new InvalidTopicExpressionFaultType();
                faultType.setTimestamp(new XMLGregorianCalendarImpl(new GregorianCalendar(TimeZone.getTimeZone("UTC"))));
                BaseFaultType.Description description = new BaseFaultType.Description();
                description.setLang("en");
                description.setValue("Some part of expression failed to evaluate, this can not be a legal XPath expression!");
                faultType.getDescription().add(description);
                throw new InvalidTopicExpressionFault(description.getValue(), faultType);
            }
        }
        if (returnCount == 0)
            return null;
        return returnSet;
    }

    @Override
    public boolean isExpressionPermittedInNamespace(TopicExpressionType expression, TopicNamespaceType namespace)
            throws TopicExpressionDialectUnknownFault, InvalidTopicExpressionFault {
        throw new UnsupportedOperationException("Permittance in namespace is still not implemented");
    }

    @Override
    public List<QName> evaluateTopicExpressionToQName(TopicExpressionType topicExpressionType, NamespaceContext context)
            throws UnsupportedOperationException, InvalidTopicExpressionFault, MultipleTopicsSpecifiedFault {
        throw new UnsupportedOperationException("The XPath evaluator is unable to evaluate " +
                "an expression without context");
    }
}
