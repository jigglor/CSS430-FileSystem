/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
   CSS430 Final Project - File System
   TCB Class
 */
public class TCB {
	private Thread thread = null;
	private int tid = 0;
	private int pid = 0;
	private boolean terminated = false;
	private int sleepTime = 0;

	// User file descriptor table
	// each entry pointing to a file (structure) table entry
	public FileTableEntry[] fte = null;

	public TCB(Thread newThread, int myTid, int parentTid) {
		thread = newThread;
		tid = myTid;
		pid = parentTid;
		terminated = false;

		fte = new FileTableEntry[32];
		for (int i = 0; i < 32; i++)
			fte[i] = null;
		// fd[0], [1], [2] are kept null for input, output, err

		System.err.println("threadOS: a new thread (thread=" + thread + " tid="
				+ tid + " pid=" + pid + ")");
	}

	public synchronized Thread getThread() {
		return thread;
	}

	public synchronized int getTid() {
		return tid;
	}

	public synchronized int getPid() {
		return pid;
	}

	public synchronized boolean setTerminated() {
		terminated = true;
		return terminated;
	}

	public synchronized boolean getTerminated() {
		return terminated;
	}

	// added for the file system
	public synchronized int getFd(FileTableEntry entry) {
		if (entry == null)
			return -1;
		for (int i = 3; i < 32; i++) {
			if (fte[i] == null) {
				fte[i] = entry;
				return i;
			}
		}
		return -1;
	}

	// added for the file system
	public synchronized FileTableEntry returnFd(int fd) {
		if (fd >= 3 && fd < 32) {
			FileTableEntry oldEnt = fte[fd];
			fte[fd] = null;
			return oldEnt;
		} else
			return null;
	}

	// added for the file systme
	public synchronized FileTableEntry getFte(int fd) {
		if (fd >= 3 && fd < 32)
			return fte[fd];
		else
			return null;
	}
}
