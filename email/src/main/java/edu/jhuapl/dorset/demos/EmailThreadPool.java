package edu.jhuapl.dorset.demos;

	import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.MessagingException;

public class EmailThreadPool {
		public static void main (String[] args) throws MessagingException {
			ExecutorService executor = Executors.newFixedThreadPool(2);
			Runnable worker;
			
			//instead of doing a for loop-- run until inbox is empty?
			//also could create polling for emails eventually
			for (int i = 0; i < 10; i++) {
				int thread;
				if (i%2 == 0) {
					thread = 1;
				}
				else {
					thread = 2;
				}
				 worker = new EmailClient("Thread-" + thread);
				executor.execute(worker);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			executor.shutdown();
			while(!executor.isTerminated()) { }
			
			System.out.println("Finished all threads");
			//call close folders and store
			EmailClient client = new EmailClient("closing");
			client.closeAll();
		}
	}
