/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
   CSS430 Final Project - File System
   FileTableEntry Class
 */

public class FileTableEntry {
	public int seekPtr; // a file seek pointer
	public final Inode iNode; // a reference to its iNode
	public final short iNumber; // this iNode number
	public int count; // # threads sharing this entry
	public final int mode; // "r", "w", "w+", or "a"
	
	// modes
	public static final int READONLY = 0; // read only
	public static final int WRITEONLY = 1; // write only
	public static final int READWRITE = 2; // read and write
	public static final int APPEND = 3; // append
	
	public FileTableEntry(Inode i, short inumber, String m) {
		seekPtr = 0; // the seek pointer is set to the file top
		iNode = i;
		iNumber = inumber;
		count = 1; // at least on thread is using this entry
		mode = getMode(m); // once access mode is set, it never changes
		if (mode == APPEND) // if mode is append
			seekPtr = iNode.length; // seekPtr points to the end of file
	}
	

	public static int getMode(String mode) {
		switch (mode.toLowerCase()) {
			case "r" :
				return	READONLY;
			case "w" :
				return	WRITEONLY;
			case "rw" :
				return	READWRITE;
			case "a" :
				return	APPEND;
			default :
				return -1;
		}
	}
}
