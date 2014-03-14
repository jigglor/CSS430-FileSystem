import java.util.*;

/* Joseph Schooley & Nguyen Tong, Professor Sung, UWB
 CSS430 Project - File System
 FileSystem Class
 */

public class FileSystem {
	private SuperBlock superblock;
	private Directory directory;
	private FileTable filetable;
	public final int ERROR = -1;
	public final int OK = 0;
	public final int SEEK_SET = 0;
	public final int SEEK_CUR = 1;
	public final int SEEK_END  = 2;

	public FileSystem() {

	}

	public FileSystem(int blocks) {
		superblock = new SuperBlock(blocks);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);

		// read the "/" file from disk
		FileTableEntry dirEnt = open("/", "r");
		int dirSize = fsize(dirEnt);
		if (dirSize > 0) {
			// the directory has some data
			byte[] dirData = new byte[dirSize];
			read(dirEnt, dirData);
			directory.bytes2directory(dirData);
		}
		close(dirEnt);
	}

	public void sync() {

	}
	

	public boolean format(int param) {
		return false;
	}

	//  The seek pointer is initialized to zero in the mode "r", "w", and "w+", whereas initialized at the end of the file in the mode "a".?
	public FileTableEntry open (String filename, String mode) {
		FileTableEntry fte = filetable.falloc(filename, mode);
		int EOF = seek(fte, 0, SEEK_END); // end of file
		int seekPtr = fte.seekPtr;
		fte.count++;
		switch (fte.mode) {
			case FileTableEntry.READONLY:
				break;
			case FileTableEntry.WRITEONLY:
				// delete all blocks
				if (deallocateBlocks(fte) == false) {
					SysLib.cerr("Could not deallocate all blocks.");
					return null;
				}
				// write from scratch
				break;
			case FileTableEntry.READWRITE:
				// keep all blocks
				if (seekPtr < EOF) { // if seek pointer is within EOF
					// corresponding bytes should be updated
					break;
				}
				// if seek pinter is at EOF, it behaves like APPEND
			case FileTableEntry.APPEND:
				// keep all blocks
				// set seek pointer to EOF
				seekPtr = EOF;
				// append new blocks
				break;
		}
		return fte;
	}


	// Commits all file transactions on this file, and unregisters fd from the user file descriptor table of the calling thread's TCB. Returns success.
	public boolean close(FileTableEntry fte) {
		fte.count--;
		return false;
	}

	// return size in bytes
	public int fsize(FileTableEntry fte) {
		return ERROR;
	}

	public int read(FileTableEntry fte, byte[] buffer) {
		// start at position pointed to by inode's seek pointer
		int seekPtr = fte.seekPtr;
		Inode iNode = fte.iNode;
		int length = buffer.length;
		// read up to buffer length
		// if bytes remaining between seek pointer and the end of the file are less than buffer.length,
		return ERROR;

	}

	public int write(FileTableEntry fte, byte[] buffer) {
		int seekPtr = fte.seekPtr;
		Inode iNode = fte.iNode;
		int length = buffer.length;
		return ERROR;
	}


	public boolean delete(String args) {
		return false;
	}

	public int seek(FileTableEntry fte, int offset, int whence) {
		int seekPtr;
		// current seek pointer
		int currSeekPtr = fte.seekPtr;
		// end of file
		int EOF = fsize(fte);
		switch (whence) {
			case SEEK_SET:
				// file's seek pointer is set to offset bytes from the beginning of the file
				seekPtr = offset;
				break;
			case SEEK_CUR:
				// file's seek pointer is set to its current value plus the offset
				seekPtr = currSeekPtr + offset;
				break;
				// file's seek pointer is set to the size of the file plus the offset
			case SEEK_END:
				seekPtr = EOF + offset;
				break;
			default:
				return ERROR;
		}
		// clamp seek pointer to the size of the file
		// if seek pointer is negative, clamp to 0
		if (seekPtr < 0) seekPtr = 0;
		// if seek pointer is greater than file size, clamp to end of file
		else if (seekPtr < EOF) seekPtr = EOF;
		
		// set entry's seek pointer
		fte.seekPtr = seekPtr;
		
		// return success
		return OK;
	}

	public boolean deallocateBlocks(FileTableEntry fte) {
		Inode iNode;
		if (fte == null) return false;
		if ((iNode = fte.iNode) == null) return false;
		for (int i = 0, inc = Disk.blockSize; i < iNode.length; i += inc) {
			superblock.returnBlock(iNode.findTargetBlock(i));
		}
		// TODO: could use iNode direct length but the directSize
		// would have to be public-- so I'm not sure if I should do that.
		// this method is incompleete. Need to check indirect and
		// write back to disk
		return false;
	}

}
