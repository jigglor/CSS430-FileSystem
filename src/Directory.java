/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
   CSS430 Final Project - File System
   Directory Class
 */

public class Directory {
	private static int maxChars = 30; // max characters of each file name

	// Directory entries
	private int fsizes[]; // each element stores a different file size.
	private char fnames[][]; // each element stores a different file name.

	public Directory(int maxInumber) { // directory constructor
		String root = "/"; // entry(inode) 0 is "/"
		fsizes = new int[maxInumber]; // maxInumber = max files
		fnames = new char[maxInumber][maxChars];
		// initialize all file size to 0
		while (--maxInumber > 0) fsizes[maxInumber] = 0;
		fsizes[0] = 1; // fsize[0] is the size of "/".
		
		root.getChars(0, fsizes[0], fnames[0], 0); // fnames[0] includes "/"
	}

	public int bytes2directory(byte data[]) {
		return 0;
		// assumes data[] received directory information from disk
		// initializes the Directory instance with this data[]
	}

	public byte[] directory2bytes() {
		return null;
		// converts and return Directory information into a plain byte array
		// this byte array will be written back to disk
		// note: only meaningfull directory information should be converted
		// into bytes.
	}

	public short ialloc(String fileName) {
		return 0;
		// filename is the one of a file to be created.
		// allocates a new inode number for this filename
	}

	public boolean ifree(short iNumber) {
		return false;
		// deallocates this inumber (inode number)
		// the corresponding file will be deleted.
	}

	public short namei(String fileName) {
		// return 0 for root
		if (fileName == "/") return 0;
		return -1;
		// returns the inumber corresponding to this filename
	}
}
