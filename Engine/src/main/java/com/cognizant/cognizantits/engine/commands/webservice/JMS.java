package com.cognizant.cognizantits.engine.commands.webservice;

import com.cognizant.cognizantits.engine.commands.General;
import com.cognizant.cognizantits.engine.core.CommandControl;
import com.cognizant.cognizantits.engine.support.Status;
import com.cognizant.cognizantits.engine.support.methodInf.Action;
import com.cognizant.cognizantits.engine.support.methodInf.InputType;
import com.cognizant.cognizantits.engine.support.methodInf.ObjectType;
import com.tibco.tibjms.TibjmsQueueConnectionFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class JMS extends General {
	public JMS(CommandControl cc) {
		super(cc);
		key = userData.getScenario() + userData.getTestCase();
	}

	static private Map<String, String> username = new HashMap<>();
	static private Map<String, String> password = new HashMap<>();
	static private Map<String, String> serverURL = new HashMap<>();
	static private Map<String, String> response = new HashMap<>();
	static private Map<String, String> queuename = new HashMap<>();
	static private Map<String, String> soapAction = new HashMap<>();
	static private Map<String, String> responsefilepath = new HashMap<>();
	static private Map<String, Long> starttime = new HashMap<>();
	static private Map<String, Long> endtime = new HashMap<>();
	static private Map<String, String> scenarioidentifier = new HashMap<>();
	static private Map<String, String> LienID = new HashMap<>();
	static private Map<String, String> currentLienID = new HashMap<>();

	private String key;

	@Action(object = ObjectType.WEBSERVICE, desc = "Set the username", input = InputType.YES)
	public void setUserName() {
		username.put(key, Data);
		Report.updateTestLog(Action, "Username is set as [" + Data + "]", Status.DONE);
	}

	@Action(object = ObjectType.WEBSERVICE, desc = "Set the password", input = InputType.YES)
	public void setPassword() {
		password.put(key, Data);
		Report.updateTestLog(Action, "Password is set", Status.DONE);
	}

	@Action(object = ObjectType.WEBSERVICE, desc = "Set the server URL", input = InputType.YES)
	public void setServerURL() {
		serverURL.put(key, Data);
		Report.updateTestLog(Action, "Server URL is set as [" + Data + "]", Status.DONE);
	}

	@Action(object = ObjectType.WEBSERVICE, desc = "Store Response file", input = InputType.YES, condition = InputType.YES)
	public void setResponseFilePath() {
		responsefilepath.put(key, Data);
		scenarioidentifier.put(key, Condition);

		// Report.updateTestLog(Action, "Response File path is set as " + Data,
		// Status.DONE);
	}

	@Action(object = ObjectType.WEBSERVICE, desc = "Set the name of the queue", input = InputType.YES)
	public void setQueue() {
		queuename.put(key, Data);
		Report.updateTestLog(Action, "JMS queue is set as [" + Data + "]", Status.DONE);
	}

	@Action(object = ObjectType.WEBSERVICE, desc = "Set the Soap Action", input = InputType.YES)
	public void setSoapAction() {
		soapAction.put(key, Data);
		Report.updateTestLog(Action, "soapAction is set as [" + Data + "]", Status.DONE);
	}

	@Action(object = ObjectType.WEBSERVICE, desc = "Push JMS  Message into JMS Queue", input = InputType.YES, condition = InputType.YES)
	public void PushJMSmessage() {
		PushMessage(Condition, queuename.get(key), Data, soapAction.get(key));
	}


	@Action(object = ObjectType.WEBSERVICE, desc = "Store JMS  Response in Data Sheet", input = InputType.YES)
	public void storeJMSresponseinDataSheet() {
		try {
			String strObj = Input;
			if (strObj.matches(".*:.*")) {
				try {
					System.out.println("Updating value in SubIteration " + userData.getSubIteration());
					String sheetName = strObj.split(":", 2)[0];
					String columnName = strObj.split(":", 2)[1];
					userData.putData(sheetName, columnName, response.get(key));
					Report.updateTestLog(Action, "Response text is stored in " + strObj, Status.DONE);
				} catch (Exception ex) {
					Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
					Report.updateTestLog(Action, "Error Storing response in datasheet :" + "\n" + ex.getMessage(),
							Status.DEBUG);
				}
			} else {
				Report.updateTestLog(Action,
						"Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
						Status.DEBUG);
			}
		} catch (Exception ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
			Report.updateTestLog(Action, "Error storing response in datasheet :" + "\n" + ex.getMessage(),
					Status.DEBUG);
		}
	}

	public void PushMessage(String Sheetname, String Queue, String filepath, String SoapAction) {
		try {
			TibjmsQueueConnectionFactory tibjmsQueueConnectionFactory = new TibjmsQueueConnectionFactory(
					serverURL.get(key));

			QueueConnection connection = tibjmsQueueConnectionFactory.createQueueConnection(username.get(key),
					password.get(key));
			QueueSession session = connection.createQueueSession(false, 1);
			Queue queue = session.createQueue(Queue);
			QueueSender sender = session.createSender(queue);
			connection.start();
			TextMessage msg = session.createTextMessage();

			System.out.println("===============================Payload=================================");
			msg.setText(CreatePayload(filepath, Queue, Sheetname));
			msg.setStringProperty("soapAction", SoapAction);
			System.out.println(msg.getText());
			System.out.println("=======================================================================");
			System.out.println("Request sent successfully");
			TemporaryQueue temporaryQueue = session.createTemporaryQueue();
			MessageConsumer consumer = session.createConsumer((Destination) temporaryQueue);
			msg.setJMSReplyTo((Destination) temporaryQueue);
			starttime.put(key, System.currentTimeMillis());
			sender.send((Message) msg);
			TextMessage textMsg = (TextMessage) consumer.receive(50000L);
			if (textMsg.getText().contains("Paradigm>Response<") || textMsg.getText().contains("Paradigm>Reply<")) {
				endtime.put(key, System.currentTimeMillis());
				long responsetime = endtime.get(key) - starttime.get(key);
				String responsetimeFormatted = String.format("%,.2f", responsetime / 1000.0);
				System.out.println("Response received successfully");
				System.out.println("=======================================================================");
				Report.updateTestLog(Action,
						"Request sent and response received in " + responsetimeFormatted + " seconds", Status.PASSNS);
				response.put(key, textMsg.getText());
				if (new File(responsefilepath.get(key)).isFile()) {
					BufferedWriter writer = new BufferedWriter(new FileWriter(responsefilepath.get(key), true));
					writer.append(scenarioidentifier.get(key) + "," + responsetimeFormatted.replace(",", ".") + "\n");
					writer.close();
				}
				connection.close();
			} else {
				System.out.println("Request was unsuccessful");
				response.put(key, textMsg.getText());
				System.out.println("=======================================================================");
				if (textMsg.getText().contains("TIBCO JMS connection timeout exception"))
					Report.updateTestLog(Action, "TIBCO JMS connection timed out", Status.FAILNS);
				else
					Report.updateTestLog(Action, "Request unsuccessful", Status.FAILNS);
				connection.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Report.updateTestLog(Action, "Something went wrong : \n" + e.getMessage(), Status.DEBUG);
		}
	}

	

	private String CreatePayload(String pathtofile, String queuename, String SheetName) throws FileNotFoundException {
		File f = new File(pathtofile);
		Scanner sc = new Scanner(f);
		String filecontent = "";
		while (sc.hasNext())
			filecontent = String.valueOf(filecontent) + sc.nextLine() + "\n";
		Pattern Variables = Pattern.compile("\\%(.*?)\\%");
		Matcher m = Variables.matcher(filecontent);
		while (m.find()) {
			String s = m.group(1);
			if (s.equalsIgnoreCase("queuename")) {
				filecontent = filecontent.replaceAll("%queuename%", queuename);
				continue;
			}
			String data = userData.getData(SheetName, s);
			filecontent = filecontent.replaceAll("%" + s + "%", data);
		}		
		System.out.println("Request message created successfully");
		System.out.println("\n" + "==============================================================================");
		return filecontent;
	}

	@Action(object = ObjectType.WEBSERVICE, desc = "Assert Response Tag", input = InputType.YES, condition = InputType.YES)
	public void assertJMSResponseTag() {
		String value = "";
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new ByteArrayInputStream(response.get(key).getBytes()));
			XPath xpath = XPathFactory.newInstance().newXPath();
			xpath.setNamespaceContext(new NamespaceContext() {
				public String getNamespaceURI(String prefix) {
					return returnNamespaceURI(prefix, response.get(key));
				}

				public Iterator<?> getPrefixes(String prefix) {
					return null;
				}

				public String getPrefix(String prefix) {
					return null;
				}
			});
			XPathExpression expr = xpath.compile(Condition);
			Object result = expr.evaluate(doc, XPathConstants.NODESET);
			NodeList nodes = (NodeList) result;
			value = nodes.item(0).getNodeValue().trim();
			if (value.contains(Data)) {
				Report.updateTestLog(Action, "Response [" + value + "] is as expected", Status.PASSNS);
			} else {
				Report.updateTestLog(Action, "Response data is : [" + value + "] but should be " + Data, Status.FAILNS);
			}
		} catch (Exception ex) {
			Logger.getLogger(getClass().getName()).log(Level.OFF, (String) null, ex);
			Report.updateTestLog(Action,
					"Something went wrong in asserting the value [" + Data + "]" + "\n" + ex.getMessage(),
					Status.FAILNS);
		}
	}


	@Action(object = ObjectType.WEBSERVICE, desc = "Store Response Tag", input = InputType.YES, condition = InputType.YES)
	public void storeJMSResponseTag() {
		String value = "";
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new ByteArrayInputStream(response.get(key).getBytes()));
			XPath xpath = XPathFactory.newInstance().newXPath();
			xpath.setNamespaceContext(new NamespaceContext() {
				public String getNamespaceURI(String prefix) {
					return returnNamespaceURI(prefix, response.get(key));
				}

				public Iterator<?> getPrefixes(String prefix) {
					return null;
				}

				public String getPrefix(String prefix) {
					return null;
				}
			});
			XPathExpression expr = xpath.compile(Condition);
			Object result = expr.evaluate(doc, XPathConstants.NODESET);
			NodeList nodes = (NodeList) result;
			value = nodes.item(0).getNodeValue().trim();
			String strObj = Input;
			if (strObj.matches(".*:.*")) {
				String sheetName = strObj.split(":", 2)[0];
				String columnName = strObj.split(":", 2)[1];
				userData.putData(sheetName, columnName, value);
				Report.updateTestLog(Action, "Value [" + value + "] is stored in " + strObj, Status.DONE);
				// Report.updateTestLog(Action, "EBT-ID [" + value + "] is generated against
				// LienID ["+ currentLienID.get(key) +"]", Status.DONE);
			}
		} catch (Exception ex) {
			Logger.getLogger(getClass().getName()).log(Level.OFF, (String) null, ex);
			if (ex.getMessage().contains("String index out of range: -1"))
				Report.updateTestLog(Action, "No Response Received", Status.FAILNS);
			else
				Report.updateTestLog(Action,
						"Something went wrong in storing the tag [" + value + "]" + "\n" + ex.getMessage(),
						Status.FAILNS);
		}
	}

	private String returnNamespaceURI(String prefix, String Response) {
		if ("ns1".equals(prefix)) {
			if (Condition.contains("Status") || Condition.contains("Error"))
				return "http://www.ing.com/xsd/CommonObjects_001";
			else {
				String ns1_1 = Response.substring(Response.indexOf("ns1:Document xmlns:ns1=\"") + 24);
				String ns1 = ns1_1.substring(0, ns1_1.indexOf("\">"));
				// System.out.println("ns1 : " + ns1);
				return ns1;
			}
		}
		if ("ns0".equals(prefix)) {
			// String ns0_1 = Response.substring(Response.indexOf("xmlns:ns0=\"",
			// Response.indexOf("xmlns:ns0=\"")+1));
			// String ns0 = ns0_1.substring(11, ns0_1.indexOf("\">"));
			String ns0_1 = Response.substring(Response.indexOf("_Reply xmlns:ns0=\""));
			String ns0 = ns0_1.substring(18, ns0_1.indexOf("\">"));
			// System.out.println("ns0 :" + ns0);
			return ns0;
		}

		return null;
	}
}
