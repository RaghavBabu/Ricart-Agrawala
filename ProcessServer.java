import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class ProcessServer
 * Each Process receives event from other process.
 * @author Raghav Babu
 * Date : 03/22/2016
 */
public class ProcessServer extends Thread {

	private InetSocketAddress boundPort = null;
	private static int port;
	private ServerSocket serverSocket;
	private int id;
	Process process;
	static Set<Integer> processIDs;

	public ProcessServer(int  id, Process process) {
		this.id = id;
		port = ProcessIPPortXmlParser.processIDToPortMap.get(id);
		this.process = process;
	}

	@Override
	public void run(){

		try {

			initServerSocket();
			processIDs = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());

			while(true) {

				Socket connectionSocket;
				ObjectInputStream ois;
				InputStream inputStream;

				connectionSocket = serverSocket.accept();
				inputStream = connectionSocket.getInputStream();
				ois = new ObjectInputStream(inputStream);

				Object obj = ois.readObject();	

				//if state received.
				if(obj instanceof ProcessState){

					ProcessState state = (ProcessState) obj;

					System.out.println("-----------State received from Process : "+state.processId+ " --------------");
					System.out.println(state);

					//acquires the lock.
					try {
						process.requestLock.acquire();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					//process request from other process, if its a reply, don't add to queue or send response again.
					if(state.status == CriticalSectionStatus.REQUEST){

						System.out.println("Local Process CS time stamp : "+Process.timeStamp+ " ,"
								+ "Remote Process CS time stamp : "+state.timeStamp);

						//handle case when both process wants CS and this process has lower timestamp. So adding to queue.
						if(Process.status == CriticalSectionStatus.HELD || (Process.status == CriticalSectionStatus.WANTED &&
								Process.criticalSectionReqdTimeStamp < state.timeStamp)){

							System.out.println("Request time stamp greater, so adding to queue");
							process.waitingQueue.add(state);
							System.out.println("Added to queue : "+process.waitingQueue);
							System.out.println("Current List : "+processIDs);
						}

						//handle case when both process wants CS and has same timestamp.
						else if(Process.status == CriticalSectionStatus.HELD || (Process.status == CriticalSectionStatus.WANTED &&
								Process.criticalSectionReqdTimeStamp == state.timeStamp)){

							if(Process.processId < state.processId){
								System.out.println("Request Timestamp same, but processId is lesser");
								process.waitingQueue.add(state);
								System.out.println("Added to queue : "+process.waitingQueue);
								System.out.println("Current List : "+processIDs);
							}

							else{
								System.out.println("Sending reply since CS is needed by both processes and same timestamp,"
										+ " but this process has higher process identifier : "+Process.processId);

								//check if already got a reply from that process to acquire critical section and id in processIDs list.
								if(!processIDs.contains(state.processId)){
									ProcessState msg = new ProcessState(Process.processId, Process.criticalSectionReqdTimeStamp, CriticalSectionStatus.REPLY);
									ProcessClient client = new ProcessClient(msg, state.processId);
									client.send();				
								}
								else{
									process.waitingQueue.add(state);
									System.out.println("Added to queue : "+process.waitingQueue);
								}

							}	
						}

						//handle case when both process wants CS and this process has higher timestamp. So it doesn't need CS now.
						else{
							System.out.println("Sending reply since CS not needed by this process : "+Process.processId);

							//check if already got a reply from that process to acquire critical section and id in processIDs list.
							if(!processIDs.contains(state.processId)){
								ProcessState msg = new ProcessState(Process.processId, Process.criticalSectionReqdTimeStamp, CriticalSectionStatus.REPLY);
								ProcessClient client = new ProcessClient(msg, state.processId);
								client.send();
							}
							else{
								process.waitingQueue.add(state);
								System.out.println("Added to queue : "+process.waitingQueue);
							}
							
						}
					}
					//reply from other processes, so add to list. If reply received from all other processes. Acquire CS
					else{
						processIDs.add(state.processId);
						System.out.println("Current List : "+processIDs);
					}

					//releases the lock.
					process.requestLock.release();

				}
				//if event received from another process.
				else{
					Event event = (Event) obj;

					// ping to other processes.
					if(event.eventType == EventType.PING){
						//System.out.println("Process : "+event.processId+" able to ping");
					}

					else if (event.eventType == EventType.TRANSFER){

						System.out.println("-----------Event received from Process : "+event.processId+ " --------------");
						Event e = new Event(EventType.RECEIVE, event.processId, event.amt, event.timeStamp);
						System.out.println("Received Event : "+e);
						Process.totalAmount += e.amt;
						Process.timeStamp = Math.max(Process.timeStamp, e.timeStamp) + 1;
					}

				}
			}
		}catch(Exception e){
			System.out.println("Exception while receiving event in process : "+id+" ");
			e.printStackTrace();
		}

	}

	/**
	 * method which initialized and bounds a server socket to a port.
	 * @return void.
	 */
	private void initServerSocket()
	{
		boundPort = new InetSocketAddress(port);
		try
		{
			serverSocket = new ServerSocket(port);

			if (serverSocket.isBound())
			{
				System.out.println("Server bound to data port " + serverSocket.getLocalPort() + " and is ready...");
			}
		}
		catch (Exception e)
		{
			System.out.println("Unable to initiate socket.");
		}

	}

}
