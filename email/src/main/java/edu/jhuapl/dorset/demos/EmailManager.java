package edu.jhuapl.dorset.demos;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class EmailManager {
	
	private String user;
	private String password;
	private String mailStoreType = "imap";
	private String host = "";
	private Properties prop = new Properties();
	 
	private Session session;
	private Store store;
	private Folder inboxFolder;
	private Folder seenFolder;
	private Folder errorFolder;
	private Folder completeFolder;
	private Message[] unreadMessages = new Message[1];
	private Message[] processingMessages = new Message[1];
	private ArrayDeque<Message> handler = new ArrayDeque<Message>();
	
	private final int INBOX = 1;
	private final int SEEN = 2;
	private final int COMPLETE = 3;
	private final int ERROR = 4;
	private final int UNREAD = 5;
	private final int PROCESSING = 6;
	private String response = "";
	
	public EmailManager(String user, String password) {
		this.user = user;
		this.password = password;
		
		prop.put("mail.store.protocol", mailStoreType);
		prop.put("mail.imap.host", host);
		prop.put("mail.imap.port", "143");
		prop.put("mail.smtp.auth", "true");
	    prop.put("mail.smtp.host", host);
	    prop.put("mail.smtp.port", "25");
	    
	    try {
		    session = Session.getDefaultInstance(prop);
		    store = session.getStore("imap");
		    store.connect(host, this.user, this.password);
		} 
	    catch (MessagingException e) {
	    	e.printStackTrace();
	    }
	}	
	public void createFolders() {
		try {
		    if(!store.getFolder("Error").exists()) {
		    	store.getFolder("Error").create(1);
		    }	
		    if(!store.getFolder("Seen").exists()) {
		    	store.getFolder("Seen").create(1);
		    }		    
		    if(!store.getFolder("Complete").exists()) {
		    	store.getFolder("Complete").create(1);
		    }
		    seenFolder = store.getFolder("Seen");
		    errorFolder = store.getFolder("Error");
		    completeFolder = store.getFolder("Complete");
		    inboxFolder = store.getFolder("INBOX");    
		} 
		catch (MessagingException e) {
	    	e.printStackTrace();
	    }
	}	
	public Folder determineFolder(int folderNum) {
		switch(folderNum) {
			case INBOX: 
				return inboxFolder;
			case SEEN:
				return seenFolder;
			case ERROR: 
				return errorFolder;
			case COMPLETE:
				return completeFolder;
			default:
				return null;
		}
	}	
	public Message[] determineMsg(int folderNum) {
		switch(folderNum) {
			case UNREAD: 
				return unreadMessages;
			case PROCESSING: 
				return processingMessages;
			default:
				return null;
		}
	}
	public void openFolder(int folder) throws MessagingException {
		determineFolder(folder).open(Folder.READ_WRITE);
	}	
	public boolean folderIsOpen(int folder) {
		return determineFolder(folder).isOpen();
	}	
	public void closeFolder(int folder) throws MessagingException {
		determineFolder(folder).close(false);
	}	
	public int getCount(int folder) throws MessagingException {
		return determineFolder(folder).getMessageCount();
	}	
	public boolean getEmail(int folder, int msg) throws MessagingException {
		determineMsg(msg)[0] = determineFolder(folder).getMessage(1);
		if (msg == UNREAD) {
			int n = 1;
			while (determineMsg(msg)[0].isSet(Flags.Flag.SEEN)) {
				determineMsg(msg)[0] = determineFolder(folder).getMessage(n);
				n++;
				if (n > determineFolder(folder).getMessageCount()) {
					return false;
				}
			}
		}
		return true;
	}
	public void markSeen(int msg) throws MessagingException {
		determineMsg(msg)[0].setFlag(Flags.Flag.SEEN, true);
	}
	public void addToQ(int msg) {
		 handler.addLast(determineMsg(msg)[0]);
	}
	public boolean QisEmpty() {
		return handler.isEmpty();
	}
	public void removeFromQ(int msg) {
		determineMsg(msg)[0] = handler.removeFirst();
	}
	public String readEmail(int msg) throws MessagingException, IOException {
		Message mess = determineMsg(msg)[0];
		System.out.println("---------------------------------");
		System.out.println("From: " + mess.getFrom()[0]);				
		System.out.println("Subject: " + mess.getSubject());
		
		String toReturn = "";
		response = "";
		toReturn += writePart(mess);
		System.out.println("Email reads:  \"" + toReturn + "\"");
		
		return toReturn;
	}
	public String writePart(Part p) {
		try {
			if (p.isMimeType("text/plain")) {
				//System.out.println((String) p.getContent());
				response += ((String) p.getContent());
			}
			else if (p.isMimeType("multipart/*")) {
				Multipart mp = (Multipart) p.getContent();
				int count = mp.getCount();
				for(int n = 0; n < count; n++){
					//System.out.println(writePart(mp.getBodyPart(n)));
					String a = (writePart(mp.getBodyPart(n)));
				}
			}
			else if (p.isMimeType("message/rfc822")) {
				//System.out.println(writePart((Part) p.getContent()));
				response += writePart((Part) p.getContent());
			}
			return response;
		}
		catch (MessagingException | IOException e) {
			e.printStackTrace();
			return "An error occured while processing your email.";
		}
		
	}
	public String copyEmail(int fromFolder, int toFolder, int msg) throws MessagingException {
		determineFolder(fromFolder).copyMessages(determineMsg(msg), determineFolder(toFolder));
		return "email moved from " + fromFolder + " to " + toFolder;
	}
	public String deleteEmail(int folder, int msg) throws MessagingException {
		determineMsg(msg)[0].setFlag(Flags.Flag.DELETED, true);
    	determineFolder(folder).expunge();
    	determineFolder(folder).close(false);
    	determineFolder(folder).open(Folder.READ_WRITE);
		return "email deleted from " + folder;
	}
	public String replyToEmail(String response, int msg) throws MessagingException{
		Message replymsg = new MimeMessage(session);
		String to = InternetAddress.toString(determineMsg(msg)[0].getReplyTo());
		
		replymsg = (MimeMessage) determineMsg(msg)[0].reply(false);	
		replymsg.setFrom(new InternetAddress(to));
		replymsg.setText(response);
		replymsg.setReplyTo(determineMsg(msg)[0].getReplyTo());
		
		Transport t = session.getTransport("smtp");
		try {
			t.connect(user, password);
			t.sendMessage(replymsg, replymsg.getAllRecipients());
		} finally {
			t.close();
		}
		return "email sent";
	}
	
}