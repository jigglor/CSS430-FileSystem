/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
   CSS430 Final Project - File System
   Directory Class
 */

public class Directory {
	private final static int maxChars = 30; // max characters of each file name

	// Directory entries
	private int fsizes[]; // each element stores a different file size (file name's length)
	private char fnames[][]; // each element stores a different file name

	public Directory(int maxInumber) { // directory constructor
		String root = "/"; // entry(inode) 0 is "/"
		fsizes = new int[maxInumber]; // maxInumber = max files
		fnames = new char[maxInumber][maxChars];
		// initialize all file size to 0
		while (--maxInumber > 0) fsizes[maxInumber] = 0;
		fsizes[0] = root.length(); // fsize[0] is the size of "/".
		
		root.getChars(0, fsizes[0], fnames[0], 0); // fnames[0] includes "/"
	}

	public void bytes2directory(byte data[]) {
		int offset = 0;
		for (int i = 0; i < fsizes.length; i++, offset += 4) {
			fsizes[i] = SysLib.bytes2int(data, offset);
		}
		for (int i = 0; i < fnames.length; i++, offset += maxChars * 2) {
			String fname = new String(data, offset, maxChars *2);
			fname.getChars(0, fsizes[i], fnames[i], 0);
		}
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

	// returns 1, given "f1"
	public short ialloc(String fileName) {
		return 0;
		// filename is the one of a file to be created.
		// allocates a new inode number for this filename
	}

	// returns true, given 1
	public boolean ifree(short iNumber) {
		return false;
		// deallocates this inumber (inode number)
		// the corresponding file will be deleted.
	}

	// returns 0, given "/"
	public short namei(String fileName) {
		// return 0 for root
		if (fileName == "/") return 0;
		return -1;
		// returns the inumber corresponding to this filename
	}
}
