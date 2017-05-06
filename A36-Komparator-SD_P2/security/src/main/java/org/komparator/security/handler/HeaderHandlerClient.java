package org.komparator.security.handler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

/**
 * This SOAPHandler shows how to set/get values from headers in inbound/outbound
 * SOAP messages.
 *
 * A header is created in an outbound message and is read on an inbound message.
 *
 * The value that is read from the header is placed in a SOAP message context
 * property that can be accessed by other handlers or by the application.
 */
public class HeaderHandlerClient implements SOAPHandler<SOAPMessageContext> {

	public static final String CONTEXT_PROPERTY = "my.property";
	private LocalDateTime now;
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	//
	// Handler interface implementation
	//

	/**
	 * Gets the header blocks that can be processed by this Handler instance. If
	 * null, processes all.
	 */
	@Override
	public Set<QName> getHeaders() {
		return null;
	}
	
	//This function inserts the timeStamp on header
	public SOAPHeaderElement insertTimestampHeader(SOAPEnvelope se, SOAPHeader sh) throws SOAPException{
		Name timestamp = se.createName("timestamp", "d", "http://timestamp");
		SOAPHeaderElement element = sh.addHeaderElement(timestamp);
		now = LocalDateTime.now();
        String formatDateTime = now.format(formatter);
		element.addTextNode(formatDateTime);
		return element;
		
	}
	
	//This function if the message is recent or not
	public void verifyFreshness(String valueString) throws InterruptedException{
		LocalDateTime valueDate = LocalDateTime.parse(valueString , formatter);
		now = LocalDateTime.now();
		if (now.isAfter(valueDate.plusSeconds(3))){
			throw new java.lang.RuntimeException();	
		}
	}

	/**
	 * The handleMessage method is invoked for normal processing of inbound and
	 * outbound messages.
	 */
	@Override
	public boolean handleMessage(SOAPMessageContext smc) {
		System.out.println("AddHeaderHandler: Handling message.");

		Boolean outboundElement = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

		try {
			if (outboundElement.booleanValue()) {
				System.out.println("Writing header in outbound SOAP message...");

				// get SOAP envelope
				SOAPMessage msg = smc.getMessage();
				SOAPPart sp = msg.getSOAPPart();
				SOAPEnvelope se = sp.getEnvelope();

				// add header
				SOAPHeader sh = se.getHeader();
				if (sh == null)
					sh = se.addHeader();

				SOAPHeaderElement element = insertTimestampHeader(se,sh);
				
				return true;

			} else {
				System.out.println("Reading header in inbound SOAP message...");

				// get SOAP envelope header
				SOAPMessage msg = smc.getMessage();
				SOAPPart sp = msg.getSOAPPart();
				SOAPEnvelope se = sp.getEnvelope();
				SOAPHeader sh = se.getHeader();

				// check header
				if (sh == null) {
					System.out.println("Header not found.");
					return true;
				}

				// get first header element
				Name timestamp = se.createName("timestamp", "d", "http://timestamp");
				Iterator it = sh.getChildElements(timestamp);
				// check header element
				if (!it.hasNext()) {
					System.out.println("Header element not found.");
					return true;
				}
				SOAPElement element = (SOAPElement) it.next();

				// get header element value
				String valueString = element.getValue();

				// print received header
				System.out.println("------------------------HEADER----------------------------");
				System.out.println("Header value is " + valueString);
				System.out.println("----------------------------------------------------");

			}
		} catch (SOAPException e) {
			System.out.println("Caught exception in handleMessage: ");
			System.out.println(e);
			System.out.println("Continue normal processing...");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	/** The handleFault method is invoked for fault message processing. */
	@Override
	public boolean handleFault(SOAPMessageContext smc) {
		System.out.println("Ignoring fault message...");
		return true;
	}

	/**
	 * Called at the conclusion of a message exchange pattern just prior to the
	 * JAX-WS runtime dispatching a message, fault or exception.
	 */
	@Override
	public void close(MessageContext messageContext) {
		// nothing to clean up
	}

}