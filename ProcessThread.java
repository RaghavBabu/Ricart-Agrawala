import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Map.Entry;


/**
 * Class ProcessThread
 * Each ProcessThread running initiates a withdraw,deposit event and try to acquire critical section to transfer amount
 * to another  random process.
 * @author Raghav Babu
 * Date : 04/2/2016
 */
public class ProcessThread extends Thread {

	Process process;
	File file;
	FileWriter fw;

	public ProcessThread(Process process) {
		this.process = process;
		 this.file = new File(Process.PROCESS_DEST);
		 
		 try {
				this.fw = new FileWriter(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
	}

	@Override
	public void run() {

		boolean entryFlag = true;
		boolean markerProceed = false;

		while(true) {

			ProcessState msg = null;

			//ping all running processing so that it can try to acquire critical section.
			if(entryFlag){

				Event eve = new Event(EventType.PING, Process.processId);

				for(Entry<Integer, String> e : ProcessIPPortXmlParser.processIDToIpMap.entrySet() ){

					if(!e.getKey().equals(Process.processId)) {
						ProcessClient dummySend = new ProcessClient(eve, e.getKey());

						if(dummySend.send()){
							markerProceed = true;
						}else{
							markerProceed = false;
						}
					}
				}
			}

			if(!markerProceed){
				//System.out.println("Waiting for all process to start running before initiation");
				continue;
			}
			else{

				if(entryFlag){
					System.out.println("All the processes are up and running, so Ricart-Agarwala algorithm can be initiated.");

					//setting entry flag as false, so that ping process need not be done again.
					entryFlag = false;
				}

				Event event;
			
				Random rand = new Random();
				int randomCount = rand.nextInt(10) + 1;
				int randomAmt;

				//for random count times, withdraw and deposit will be happening within the process.
				while(randomCount != 0){
					
					//choosing random send event. deposit, withdraw or send.
					EventType randomEventType = process.pickRandomEventType();

					//event to withdraw amount.
					if(randomEventType == EventType.WITHDRAW) {

						randomAmt = rand.nextInt(100) + 1;
						Process.totalAmount -= randomAmt;
						Process.timeStamp += 1;

						event = new Event(randomEventType, Process.processId,randomAmt, Process.timeStamp);
						System.out.println(event);

						System.out.println("Current Total Balance = "+Process.totalAmount);
						System.out.println("---------------------------------------");

						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}

					//event to deposit amount.
					else if(randomEventType == EventType.DEPOSIT){

						randomAmt = rand.nextInt(100) + 1;
						Process.totalAmount += randomAmt;
						Process.timeStamp += 1;

						event = new Event(randomEventType, Process.processId,randomAmt, Process.timeStamp);

						System.out.println(event);
						System.out.println("Current Total Balance = "+Process.totalAmount);
						System.out.println("---------------------------------------");

						try {
							Thread.sleep(2000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					randomCount -= 1;
				}


				//Change status to wanted if it is in released state.
				if(Process.timeStamp % 2 == 0){

					System.out.println("Process in Released state and so making it to acquire CS.");
					//when setting status to be wanted, it shouldn't be already in waiting or inside critical section.
					if(Process.status == CriticalSectionStatus.RELEASED){
						Process.status = CriticalSectionStatus.WANTED;
					}

				}

				//if event is transfer, choose another process to send to.
				if(Process.status == CriticalSectionStatus.WANTED) {

					System.out.println("Process "+Process.processId+" wants critical section");

					//acquire lock to send requests. request processing deferred at this time.
					try {
						process.requestLock.acquire();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					Process.criticalSectionReqdTimeStamp = Process.timeStamp;

					msg = new ProcessState(Process.processId, Process.criticalSectionReqdTimeStamp, CriticalSectionStatus.REQUEST);

					System.out.println("Sending message to all other processes");
					for(Entry<Integer, String> e : ProcessIPPortXmlParser.processIDToIpMap.entrySet() ){

						if(!e.getKey().equals(Process.processId)) {

							//prompt the client to send it to all available destination processes.
							ProcessClient client = new ProcessClient(msg, e.getKey());
							client.send();
						}	
					}	
					
					process.requestLock.release();
					
					//process should acquire critical section to transfer amount to another process.
					synchronized (process.criticalSectionLock) {

						System.out.println("Process "+Process.processId+" waiting for critical section with timestamp : "
								+Process.timeStamp);

						try {
							process.criticalSectionLock.wait();
							//clearing the old request list after acquiring CS.
							ProcessServer.processIDs.clear();
							
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						Process.status = CriticalSectionStatus.HELD;
					}
					
						//note the time when it acquired critical section.
						Date date = new Date();
						DateFormat df = new SimpleDateFormat("HH:mm:ss");
						System.out.println("*************Process "+Process.processId+" acquired the critical section at : "+df.format(date)+" ****************");
						
						//writing to file CS acquired time.
						writeFile(df.format(date));
						
						System.out.println("Current Process status of process : "+Process.processId+ " is : "+Process.status);

						//choosing a random time to hold the process before releasing.
						int randomTime = new Random().nextInt(10) + 1;

						//int randomTime = 10;
						randomAmt = rand.nextInt(100) + 1;

						try {

							//If acquired critical section, transfer amount to another process.

							Process.timeStamp += 1;
							//choosing a random process to send to.
							int randomToProcess = chooseRandomProcessId();  

							//choosing a different process id other than its own to send money.
							while(randomToProcess == Process.processId){
								randomToProcess = chooseRandomProcessId();
							}

							event = new Event(EventType.TRANSFER, Process.processId, randomAmt,Process.timeStamp);
							ProcessClient client = new ProcessClient(event, randomToProcess);
							client.send();


							//sleeping for some time inside critical section.
							System.out.println("Holding critical section for random time : "+randomTime);

							Thread.currentThread();
							Thread.sleep(randomTime * 1000);

						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					    date = new Date();
					    df = new SimpleDateFormat("HH:mm:ss");
						System.out.println("**********Process released critical section at : "+df.format(date)+" *************");

						writeFile(df.format(date));
						writeFile("------------------------------------");
						
						//releasing CS after random time.
						Process.status = CriticalSectionStatus.RELEASED;
						
						//process released from CS.
						msg = new ProcessState(Process.processId,Process. timeStamp,CriticalSectionStatus.REPLY);
						
						//sending reply to all processes int the queue.
						while(!process.waitingQueue.isEmpty()) {
							
							ProcessState currentState = process.waitingQueue.poll();
							
							ProcessClient client = new ProcessClient(msg, currentState.processId);
							client.send();
						}		

				}

			}
		}

	}
	
	
	/**
	 * write file to directory.
	 */
	public void writeFile(String str){

			try {
				BufferedWriter bw = new BufferedWriter(fw);

				bw.write(str+"\n");
				bw.flush();

			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	

	/**
	 * Generate a random process id to send to.
	 * @return integer 
	 */
	private int chooseRandomProcessId() {

		int random = (int) ((Math.random() * 10) % process.totalProcess) + 1;
		return random;
	}

}
