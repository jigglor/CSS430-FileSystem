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


	public FileTableEntry open(String filename, String mode) {
		FileTableEntry fte = filetable.falloc(filename, mode);
		if (deallocateBlocks(fte) == false)
			return null;
		return fte;
	}


	// Commits all file transactions on this file, and unregisters fd from the user file descriptor table of the calling thread's TCB. Returns success.
	public boolean close(FileTableEntry fte) {
		return false;
	}

	// return size in bytes
	public int fsize(FileTableEntry fte) {
		return ERROR;
	}

	public int read(FileTableEntry fte, byte[] buffer) {
		int seekPtr = fte.seekPtr;
		Inode iNode = fte.iNode;
		// start at position pointed to by inode's seek pointer
		int startPos = iNode.findTargetBlock(seekPtr / Disk.blockSize);
		int length = buffer.length;
		// read up to buffer length
		// if bytes remaining between seek pointer and the end of the file are less than buffer.length,
		// SysLib.read 
		return ERROR;

	}

	public int write(FileTableEntry fte, byte[] buffer) {
		int seekPtr = fte.seekPtr;
		Inode iNode = fte.iNode;
		int startPos = iNode.findTargetBlock(seekPtr / Disk.blockSize);
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
		// greatest seek pointer value
		int filesize = fsize(fte);
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
				seekPtr = fsize(fte) + offset;
				break;
			default:
				return ERROR;
		}
		// clamp seek pointer to the size of the file
		// if seek pointer is negative, clamp to 0
		if (seekPtr < 0) seekPtr = 0;
		// if seek pointer is greater than file size, clamp to end of file
		else if (seekPtr < filesize) seekPtr = filesize;
		
		// set entry's seek pointer
		fte.seekPtr = seekPtr;
		
		// return success
		return OK;
	}

	public boolean deallocateBlocks(FileTableEntry fte) {
		
		return false;

	}

}
