import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * Class Process
 * Each Process running initiates an event.
 * 
 * @author Raghav Babu
 * Date : 04/2/2016
 */
public class Process{

	static int processId;
	static int timeStamp;
	static int totalAmount = 1000;
	static int criticalSectionReqdTimeStamp;
	public static  String PROCESS_DEST;
	
	//initially all process in released state. No process uses critical section.
	static CriticalSectionStatus status = CriticalSectionStatus.RELEASED;
   
	Object criticalSectionLock;
	Semaphore requestLock;
	
    int totalProcess;	
   	Queue<ProcessState> waitingQueue;
   	
	public Process(int totalProcess){
		
		//file to write time CS acquired for comparison.
		try {
			PROCESS_DEST = "process_"+InetAddress.getLocalHost().getHostName()+".txt";
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		this.totalProcess = totalProcess;
		this.waitingQueue = new PriorityBlockingQueue<ProcessState>();
		this.criticalSectionLock = new Object();
		this.requestLock = new Semaphore(1);
	}

	/*
	 * Main Function
	 * input arguments are pro.cessId and total number of process involved
	 */
	public static  void main(String args[]) {

		//command line args.
		processId = Integer.parseInt(args[0]);
		int totalProcess = Integer.parseInt(args[1]);

		Process process = new Process(totalProcess);

		//read XML file.
		ProcessIPPortXmlParser parser = new ProcessIPPortXmlParser();
		parser.parseXML();

		//start process server to receive events from other processes.
		ProcessServer server = new ProcessServer(processId, process);
		server.start();

		//process thread
		ProcessThread processThread = new ProcessThread(process);
		processThread.start();
		
		//process to check if it got reply from all other processes.
		ReplyExaminerThread replyThread = new ReplyExaminerThread(process);
		replyThread.start();

	}
	
	/**
	 * Check if marker received from all other processes.
	 * @param markers
	 * @return true or false.
	 */
	public boolean checkIfReplyReceivedFromAllProcesses(Set<Integer> markers) {

		//create incoming channels based on the number of processes.
		for(Entry<Integer, String> e : ProcessIPPortXmlParser.processIDToIpMap.entrySet() ){

			if(e.getKey().equals(Process.processId) || markers.contains(e.getKey())) {
				continue;
			}else{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Method to pick a random event.
	 * @return withdraw or depoist or transfer eventtype enum.
	 */
	public EventType pickRandomEventType() {

		int num = new Random().nextInt(EventType.values().length);
		return EventType.values()[num];

	}

}
