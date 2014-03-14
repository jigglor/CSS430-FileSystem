import java.util.Vector;

/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
 CSS430 Final Project - File System
 FileTable Class
 */

public class FileTable {
	private Vector<FileTableEntry> table; // the actual entity of this file table
	private Directory dir; // the root directory

	public FileTable(Directory directory) { // constructor
		table = new Vector<FileTableEntry>(); // instantiate a file (structure) table
		dir = directory; //  reference to the directory from the file system
	}

	
	public synchronized FileTableEntry falloc(String fname, String mode) {
		int m = FileTableEntry.getMode(mode);
		short iNumber = -1;
		Inode iNode = null;
		FileTableEntry fte;
		// if mode is invalid, return null
		if (m == -1) return null;
		while (true) {
			// allocate / retrieve and register the corresponding inode using dir
			iNumber = fname.equals("/") ? 0 : dir.namei(fname);
			if (iNumber < 0) {	// file does not exist
				if (m == FileTableEntry.READONLY)	// do not allocate file if read only
					return null;
				// allocate Inode with default constructors
				iNumber = dir.ialloc(fname);
				iNode = new Inode();
			} else {
				iNode = new Inode(iNumber);
				if (iNode.flag == Inode.DELETE) {
					iNumber = -1; // to be deleted
					return null; // no more to open
				}
				if (m == FileTableEntry.READONLY) { // mode is "r"
					if (iNode.flag == Inode.READ) {
						break; // no need to wait
					} else if (iNode.flag == Inode.WRITE) { // wait for a write to exit
						try {
							wait();
						} catch (InterruptedException e) {}
					}
				} else { // mode is "w" "w+" "rw" or "a"
					// TODO: not sure if this should be for ALL modes
					if (iNode.flag == Inode.UNUSED || iNode.flag == Inode.USED) {
						// iNode is good
						iNode.flag = Inode.WRITE; // set flag to write
						break;
					} else if (iNode.flag == Inode.READ || iNode.flag == Inode.WRITE) { // wait for a read/write to exit
						try {
							wait();
						} catch (InterruptedException e) {}
					}
				}
			}
		}

		// increment this iNode's count
		iNode.count++;

		// immediately write back this iNode to the disk
		iNode.toDisk(iNumber);

		// allocate new file table entry for this file name
		fte = new FileTableEntry(iNode, iNumber, mode);

		// add FTE to table
		table.add(fte);

		// return a reference to this FTE
		return fte;
	}


	public synchronized boolean ffree(FileTableEntry fte) {
		Inode iNode = fte.iNode;
		short iNumber = fte.iNumber;
		
		// the FTE was not found in my table
		if (!table.removeElement(fte)) return false;
		
		// decrement this iNode's count
		if (iNode.count > 0) iNode.count--;
		
		// save the corresponding iNode to the disk
		iNode.toDisk(iNumber);
		
		// notify waiting threads
		if (iNode.flag == Inode.READ || iNode.flag == Inode.WRITE)
			notify();
		
		// free this FTE
		fte = null; // the FTE is now eligible for garbage collection
		
		// the FTE was found in my table
		return true;
	}

	public synchronized boolean fempty() {
		return table.isEmpty(); // return if table is empty
	}
}
