package edu.jhuapl.dorset.demos;

import java.io.IOException;

import javax.mail.MessagingException;

import edu.jhuapl.dorset.Application;
import edu.jhuapl.dorset.Request;
import edu.jhuapl.dorset.Response;
import edu.jhuapl.dorset.agents.Agent;
import edu.jhuapl.dorset.agents.DateTimeAgent;
import edu.jhuapl.dorset.routing.Router;
import edu.jhuapl.dorset.routing.SingleAgentRouter;

public class EmailClient implements Runnable {
	private Thread t;
	private String threadName;
	private final static int INBOX = 1;
	private final static int SEEN = 2;
	private final static int COMPLETE = 3;
	private final static int ERROR = 4;
	private final static int UNREAD = 5;
	private final static int PROCESSING = 6;
	private static EmailManager email;
	private static int emailsLeftInbox;
	
	EmailClient(String name) {
		threadName = name;
		System.out.println("Creating " + threadName);
	} 
	
	public static void main (String[] args) throws MessagingException, IOException {
		
		email = new EmailManager("", "");
		email.createFolders();
		
		email.openFolder(INBOX); 
		emailsLeftInbox = email.getCount(INBOX);
		System.out.println("Inbox: " + emailsLeftInbox);
		
		EmailClient T1= new EmailClient("Thread-1");
		T1.start();
		
		EmailClient T2 = new EmailClient("Thread-2");
		T2.start();
	}
	public static Agent determineAgent(String text) {
		return new DateTimeAgent();
	}
	public void start() {
		System.out.println("Starting " + threadName);
		if (t == null) {
			t = new Thread (this, threadName);
			t.start();
		}
	}
	public void run() {
		System.out.println("Running " + threadName);
		try {
			if (threadName.equals("Thread-1")) {
				while (emailsLeftInbox > 0 ) {
					if (!email.folderIsOpen(INBOX)) {
						email.openFolder(INBOX); 
					}
					if(!email.getEmail(INBOX, UNREAD)) {
						break;
					}
					email.markSeen(UNREAD); 
					email.addToQ(UNREAD);
					Thread.sleep(100);
					emailsLeftInbox = email.getCount(INBOX);
				}
			}
			else if (threadName.equals("Thread-2")) {
				Thread.sleep(50);
				while (!email.QisEmpty()) {
					if (!email.folderIsOpen(SEEN)) {
						email.openFolder(SEEN); 
					}
					email.removeFromQ(PROCESSING);
					System.out.println("email to be read: ");
					String emailText = email.readEmail(PROCESSING);
					
					if (emailText.equals("An error occured while processing your email.")) {
						System.out.println(email.replyToEmail(emailText, PROCESSING));
						System.out.println(email.copyEmail(INBOX, ERROR, PROCESSING));			
					}
					else {	
						Agent agent = determineAgent(emailText); 
						Router router = new SingleAgentRouter(agent);
						Application app = new Application(router);
						Request request = new Request(emailText);
						Response response = app.process(request);
						
						System.out.println("response: " + response.getText());
						System.out.println(email.replyToEmail(response.getText(), PROCESSING));
						System.out.println(email.copyEmail(INBOX, COMPLETE, PROCESSING));
					}
					
					System.out.println(email.deleteEmail(INBOX, PROCESSING));
					emailsLeftInbox = email.getCount(INBOX);
				}
			}
		}
		catch (InterruptedException | MessagingException | IOException e) {
			e.printStackTrace();
		}
		System.out.println("Thread " + threadName + " exiting");
	}
	
}