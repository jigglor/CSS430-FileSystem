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

	public FileSystem(int blocks) {
		byte[] dirData;
		
		superblock = new SuperBlock(blocks);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);
		// read the "/" file from disk
		FileTableEntry dirEnt = open("/", "r");
		int dirSize = fsize(dirEnt);
		if (dirSize > 0) {
			// the directory has some data
			dirData = new byte[dirSize];
			read(dirEnt, dirData);
			directory.bytes2directory(dirData);
		}
		close(dirEnt);
	}

	
	public void sync() {
		// write the "/" file from disk
		FileTableEntry dirEnt = open("/", "w");
		
		// get directory data in bytes
		byte[] dirData = directory.directory2bytes();
		
		// write the file table entry
		write(dirEnt, dirData);
		close(dirEnt);
		
		// tell super block to write to the disk
		superblock.sync();
	}
	

	public boolean format(int files) {
		// file table contents must be closed
		if (!filetable.fempty()) {
			// TODO: omit or wait with while loop if this causes errors
			Kernel.report("Cannot format superblock while file are in use");
			return false;
		}
		superblock.format(files);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);
		return true;
	}

	public FileTableEntry open (String filename, String mode) {
		FileTableEntry fte;
		Inode iNode;
		// must provide valid filename and mode
		if (filename == "" || mode == "")
			return null;
		// if file table entry is null
		if ((fte = filetable.falloc(filename, mode)) == null ||
			fte.mode == -1 || // or mode is invalid
			(iNode = fte.iNode) == null || // or iNode is null
			iNode.flag == Inode.DELETE) { // or iNode flag is 'to be deleted'
			filetable.ffree(fte); // relieve entry from memory
			return null;
		}
		if (fte.mode == FileTableEntry.WRITEONLY &&
			// if mode is "w" delete all blocks and write from scratch
			!deallocateBlocks(fte)) {
			// on failure, relieve entry from memory
			filetable.ffree(fte);
			Kernel.report("Could not deallocate all blocks");
			return null;
		}
		return fte;
	}


	// Commits all file transactions on this file,
	// and unregisters fd from the user file descriptor table
	// of the calling thread's TCB. Returns success.
	public boolean close(FileTableEntry fte) {
		Inode iNode;
		if (fte == null) return false;
		if ((iNode = fte.iNode) == null) return false;
		while (!filetable.ffree(fte));
		iNode.toDisk(fte.iNumber);
		return true;
	}

	// return size in bytes
	public int fsize(FileTableEntry fte) {
		Inode iNode;
		// return -1 if fte is null
		if (fte == null) return ERROR;
		// return -1 if iNode is null
		if ((iNode = fte.iNode) == null) return ERROR;
		// return iNode.length (already in bytes, no need for conversion)
		return iNode.length;
	}

	public int read(FileTableEntry fte, byte[] buffer) {
		int seekPtr, length, EOF, block, start;
		Inode iNode;
		byte[] data;
		// file table entry cannot be null
		if (fte == null) return ERROR;
		// mode must be read only
		if (fte.mode != FileTableEntry.READONLY) return ERROR;
		// iNode cannot be null
		if ((iNode = fte.iNode) == null) return ERROR;
		// start at position pointed to by inode's seek pointer
		start = seekPtr = fte.seekPtr;
		// read up to buffer length
		length = buffer.length;
		// length must be less than the end of the file
		if (length > (EOF = fsize(fte))) length = EOF;
		// multiple threads cannot read at the same time
		synchronized (fte) {
			// TODO: check if iterator is sufficient
			for (int i = 0; seekPtr < length; seekPtr += i) {
				// block must exist
				if ((block = iNode.findTargetBlock(seekPtr)) == ERROR)
					return ERROR;
				data = new byte[Disk.blockSize];
				// read block into data
				SysLib.rawread(block, data);
				// advance to next block data
				i = seekPtr % Disk.blockSize;
				// copy data to buffer
				System.arraycopy(data, fte.seekPtr % Disk.blockSize, buffer, seekPtr, i);
			}
			// set new seek pointer
			fte.seekPtr = seekPtr;
		}
		return seekPtr - start;
	}

	public int write(FileTableEntry fte, byte[] buffer) {
		int seekPtr, length, EOF, start;
		short block;
		Inode iNode;
		byte[] data;
		// file table entry cannot be null
		if (fte == null) return ERROR;
		// mode cannot be read only
		if (fte.mode == FileTableEntry.READONLY) return ERROR;
		// iNode cannot be null
		if ((iNode = fte.iNode) == null) return ERROR;
		// start at position pointed to by inode's seek pointer
		start = seekPtr = fte.seekPtr;
		// write up to buffer length
		length = buffer.length;
		// length must be less than the end of the file
		if (length > (EOF = fsize(fte))) length = EOF;
		// multiple threads cannot read at the same time
		synchronized (fte) {
			// TODO: check if iterator is sufficient
			for (int i = 0; seekPtr < length; seekPtr += i) {
				/* Attempted shorthand at ERROR checking below...
				// if block does not exist
				if ((block = iNode.findTargetBlock(seekPtr)) == ERROR &&
						// try to get free block
						((block = superblock.getFreeBlock()) == ERROR ||
						// allocate free block
						 iNode.setTargetBlock(seekPtr, block) == ERROR ||
						 // try to allocate indirect block
						 iNode.setIndexBlock(block) == false ||
						 // try to get free block
						 (block = superblock.getFreeBlock()) == ERROR ||
						 // try to allocate free block
						 iNode.setTargetBlock(seekPtr, block) == ERROR))
					return ERROR; // return ERROR on any failure
					*/
				// if block does not exist...
				if ((block = iNode.findTargetBlock(seekPtr)) == ERROR) {
					// ...we need to allocate one
					if ((block = superblock.getFreeBlock()) == ERROR)
						return ERROR; // out of memory
					// try to allocate block
					if (iNode.setTargetBlock(seekPtr, block) == ERROR) {
						// failure. try to allocate indirect block
						if (iNode.setIndexBlock(block) == false)
							return ERROR; // could not allocate indirect block
						// try to find a block again
						if ((block = superblock.getFreeBlock()) == ERROR)
							return ERROR; // nope. "I just can't do it captain!"
						if (iNode.setTargetBlock(seekPtr, block) == ERROR)
							return ERROR; // last allocation attempt failed
					}
				}
				data = new byte[Disk.blockSize];
				SysLib.rawread(block, data);
				// advance to next block data
				i = seekPtr % Disk.blockSize;
				// copy data to buffer
				System.arraycopy(data, fte.seekPtr % Disk.blockSize, buffer, seekPtr, i);
				// write data to disk
				SysLib.rawwrite(block, data);
			}
			// set new seek pointer
			fte.seekPtr = seekPtr;
			
			// save iNode to disk
			iNode.toDisk(fte.iNumber);
		}
		return seekPtr - start;
	}


	public boolean delete(String fname) {
		int iNumber;
		if (fname == "") // if blank file name, return false
			return false;
		// get the iNumber for this filename
		if ((iNumber = directory.namei(fname)) == -1)
			return false; 	// if it does not exist, return false
		// deallocate file, return success or failure
		return directory.ifree(iNumber);
	}

	public int seek(FileTableEntry fte, int offset, int whence) {
		// seek pointer, current seek pointer, end of file
		int seekPtr, currSeekPtr, EOF;
		if (fte == null) return ERROR;
		currSeekPtr = fte.seekPtr;
		EOF = fsize(fte);
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
		else if (seekPtr > EOF) seekPtr = EOF;
		
		// set entry's seek pointer
		fte.seekPtr = seekPtr;
		
		// return success
		return OK;
	}

	public boolean deallocateBlocks(FileTableEntry fte) {
		Inode iNode;
		byte[] data;
		int block;
		// return false if fte is null
		if (fte == null) return false;
		// return false if iNode is null
		if ((iNode = fte.iNode) == null) return false;
		// return false if iNode is being used
		if (iNode.count > 1) return false;
		
		// deallocate direct blocks
		/* increment index by block size until iNode's length is reached,
		 * since length is a byte length. could have used iNode.direct.size
		 * and incremented by 1 through the array, but this would defeat
		 * the purpose of directSize being a private int-- so instead, we
		 * use the built-in methods.
		*/
		for (int i = 0, l = iNode.length, inc = Disk.blockSize; i < l; i += inc) {
			// skip unallocated block
			if ((block = iNode.findTargetBlock(i)) == ERROR)
				continue;
			// deallocate block
			superblock.returnBlock(block);
			iNode.setTargetBlock(block, -1);
		}
		
		// deallocate indirect blocks
		if ((data = iNode.deleteIndexBlock()) != null) {
			for (int i = 0, l = Disk.blockSize / 2; i < l; i += 2) {
				// skip unallocated block
				if ((block = SysLib.bytes2short(data, i)) == ERROR)
					continue;
				// deallocate block
				superblock.returnBlock(block);
			}
		}
		// write iNode to disk
		iNode.toDisk(fte.iNumber);
		return true;
	}

}
