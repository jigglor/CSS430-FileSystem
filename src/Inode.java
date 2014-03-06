import java.util.Vector;

/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
   CSS430 Final Project - File System
   Inode Class
 */

public class Inode {
	private static Vector<Inode> Inodes; // maintains all iNode on memory
	
	private final static int iNodeSize = 32; // fix to 32 bytes
	private final static int directSize = 11; // # direct pointers

	// flag states
	public final static int UNUSED = 0;
	public final static int USED = 1;
	// return values
	public final static int OK = 0;
	public final static int ERROR = -1;

	public int length; // file size in bytes
	public short count; // # file-table entries pointing to this
	public short flag; // 0 = unused, 1 = used, ...
	public short direct[] = new short[directSize]; // direct pointers
	public short indirect; // a indirect pointer

	// a default constructor
	Inode() {
		length = 0;
		count = 0;
		flag = USED;
		for (int i = 0; i < directSize; i++) {
			direct[i] = ERROR;
		}
		indirect = ERROR;
	}

	// retrieves an existing iNode from the disk onto the memory
	Inode(short iNumber) {
		// read the corresponding disk block
		// locates the corresponding iNode information in that block
		// initialize a new iNode with this information
	}

	int toDisk(short iNumber) {
		// save to disk as the i-th inode
		return iNumber;
	}
}
