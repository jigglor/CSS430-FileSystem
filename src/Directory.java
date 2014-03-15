/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
   CSS430 Final Project - File System
   Directory Class
 */

public class Directory {
	private final static int maxChars = 30; // max characters of each file name

	// Directory entries
	private int fsizes[]; // each element stores a different file size (file name's length)
	private char fnames[][]; // each element stores a different file name
   
   /*Directory Constructor
   
   */
	public Directory(int maxInumber) { // directory constructor
		String root = "/"; // entry(inode) 0 is "/"
		fsizes = new int[maxInumber]; // maxInumber = max files
		fnames = new char[maxInumber][maxChars];
		// initialize all file size to 0
		while (--maxInumber > 0) fsizes[maxInumber] = 0;
		fsizes[0] = root.length(); // fsize[0] is the size of "/".
		
		root.getChars(0, fsizes[0], fnames[0], 0); // fnames[0] includes "/"
	}
   
   /*bytes2directory()
   
   */
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
   
   /*directory2bytes()
   // converts and return Directory information into a plain byte array
   // this byte array will be written back to disk
   // note: only meaningfull directory information should be converted
	// into bytes.
   */
	public byte[] directory2bytes() {
      //create the byte array to return
		byte[] dir = new byte[fsize.length * 4 
                           + fnames.length * maxChars * 2];
      int offset = 0;
      //convert the data in fsizes[i] to bytes and write
      //into dir byte array
      for (int i = 0; i < fsize.length; i++, offset += 4)
         SysLib.int2bytes(fsizes[i], dir, offset);
      
      //get the file name of this file then convert the 
      //String into bytes
      for (int i = 0; i < fnames.length; i++; offset += maxChars * 2) {
         String fname = new String(fnames[i], 0, fsize[i]);
         byte[] toWrite = fname.getBytes();
         
         //write fname to dir array
         for (int j = 0; j < toWrite.length; j++) {
            dir[offset] = fname[j];
            offset++;
         }
      }           
		return dir;
	}

	// returns 1, given "f1"
   /*ialloc()
   // filename is the one of a file to be created.
	// allocates a new inode number for this filename
   */
	public short ialloc(String fileName) {
		int j;
      for (int i = 1; i < fsizes.length; j = (short)(i + 1)) {
         if (fsize[i] = 0) {
            fsize[i] = Math.min(fileName.length(), maxChars);
            fileName.getChars(0, fsizes[i], fnames[i], 0);
            return i;
         }
      }
   return -1;
	}

	// returns true, given 1
   /*ifree()
   // deallocates this inumber (inode number)
   // the corresponding file will be deleted.
   */
	public boolean ifree(int iNumber) {
		if (fsize[iNumber] < 0) return false;
      fisizes[iNumber = 0;
      return true;
	}

	// returns 0, given "/"
   /*namei()
   
   */
	public short namei(String fileName) {
		int j;
      for (int i = 0; i < fsizes.length; i = (short)(i + 1)) {
         if(fsizes[i] == fileName.length())  {
            String fname = new String(fnames[i], 0, fsizes[i]);
            if(fileName.compareTo(fname))
               return i;
         }
      }
      return -1;
	}
}
