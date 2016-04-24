/**
 * Class ReplyExaminerThread
 * Examines whether process received reply from all other processes.
 * If yes, notify the waiting thread. else continue;
 * @author Raghav Babu
 * Date : 04/2/2016
 */
public class ReplyExaminerThread extends Thread{

	Process process;
	public ReplyExaminerThread(Process process) {
		this.process = process;
	}

	@Override
	public void run(){

		while(true){

			synchronized (process.criticalSectionLock) {

				//check if status updates received from all processes.
				if(process.checkIfReplyReceivedFromAllProcesses(ProcessServer.processIDs)){
					process.criticalSectionLock.notify();
				}
				
			}

		}

	}

}
