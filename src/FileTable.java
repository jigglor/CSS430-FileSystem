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
		dir = directory; // receive a reference to the Director
	} // from the file system

	
	public synchronized FileTableEntry falloc(String fileName, String mode) {
		Inode iNode;
		short iNumber;
		FileTableEntry fte;
		
		mode = mode.toLowerCase();
		
		// return null for invalid mode
		if (!mode.matches("^[ar]|w\\+?$")) return null;
		
		// get iNumber from file name in directory
		iNumber = dir.namei(fileName);
		
		// iNumber was not found in directory
		if (iNumber == -1) {
			// the file is created if it does not exist in the mode "w", "w+" or "a"
			if (mode == "r") return null;
			iNumber = dir.ialloc(fileName);
			iNode = new Inode();
		} else { // iNumber was found in directory
			// retrieve the iNode
			iNode = new Inode(iNumber);
		}
		
		// increment this iNode's count
		iNode.count++;
		
		// immediately write back this iNode to the disk
		iNode.toDisk(iNumber);
		
		// create FTE
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
		
		// TODO: check iNode flags
		
		// decrement this iNode's count
		iNode.count--;
		
		// save the corresponding iNode to the disk
		iNode.toDisk(iNumber);
		
		// notify waiting threads
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
