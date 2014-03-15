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
		int seekPtr, length, block, offset, available, remaining, rLength, index;
		Inode iNode;
		byte[] data;
		// file table entry cannot be null
		if (fte == null) return ERROR;
		// mode must be read
		if (fte.mode == FileTableEntry.WRITEONLY ||
			fte.mode == FileTableEntry.APPEND) return ERROR;
		// iNode cannot be null
		if ((iNode = fte.iNode) == null) return ERROR;
		// start at position pointed to by iNode's seek pointer
		seekPtr = fte.seekPtr;
		// read up to buffer length
		length = buffer.length;
		// multiple threads cannot read at the same time
		synchronized (fte) {
			index = 0;
			while (index < length) {
				// block must exist
				if ((block = iNode.findTargetBlock(seekPtr)) == ERROR) {
					Kernel.report("Read failure: Failed to find target block " + seekPtr + "\n");
					return ERROR;
				}
				
				// byte offset-- 0 is a new block
				offset = seekPtr % Disk.blockSize;
				// bytes available
				available = Disk.blockSize - offset;
				// bytes remaining
				remaining = length - index;
				// bytes to read-- cannot be greater than available
				rLength = Math.min(available, remaining);
				
				data = new byte[Disk.blockSize];
				
				// read block from disk to data
				SysLib.rawread(block, data);
				
				// copy data to buffer
				// source, source position, destination, destination position, length to copy
				System.arraycopy(data, offset, buffer, seekPtr, rLength);
				
				index += rLength;
				seekPtr += rLength;
			}
			// set new seek pointer
			fte.seekPtr = seekPtr;
		}
		return length;
	}

	public int write(FileTableEntry fte, byte[] buffer) {
		int seekPtr, length, offset, remaining, available, wLength, index;
		short block;
		Inode iNode;
		byte[] data;
		// file table entry cannot be null
		if (fte == null) return ERROR;
		// mode cannot be read only
		if (fte.mode == FileTableEntry.READONLY) return ERROR;
		// iNode cannot be null
		if ((iNode = fte.iNode) == null) return ERROR;
		// iNode must not be in use
		if (iNode.flag == Inode.READ ||
			iNode.flag == Inode.WRITE ||
			iNode.flag == Inode.DELETE)
			return ERROR;
		// start at position pointed to by inode's seek pointer
		seekPtr = fte.seekPtr;
		// write up to buffer length
		length = buffer.length;
		// on error, set iNode flag to UNUSED because it's probably garbage now
		synchronized (fte) {
			// multiple threads cannot write at the same time
			iNode.flag = Inode.WRITE; // set flag to write
			index = 0;
			while (index < length) {
				// get next block from iNode
				if ((block = iNode.findTargetBlock(seekPtr)) == ERROR) {
					// if ERROR, file is out of memory, so get a new block
					if ((block = superblock.getFreeBlock()) == ERROR) {
						Kernel.report("Write failure: Out of memory!\n");
						iNode.flag = Inode.UNUSED;
						return ERROR; // no more free blocks
					}
					// read the file to the block
					if (iNode.setTargetBlock(seekPtr, block) == ERROR) {
						// out of bounds, try to get a new indirect block
						if (iNode.setIndexBlock(block) == false) {
							Kernel.report("Write failure: Failed to set index block " + block + "\n");
							iNode.flag = Inode.UNUSED;
							return ERROR;
						}
						// index block set, get a new block
						if ((block = superblock.getFreeBlock()) == ERROR) {
							Kernel.report("Write failure: Out of memory!\n");
							iNode.flag = Inode.UNUSED;
							return ERROR; // no more free blocks
						}
						if (iNode.setTargetBlock(seekPtr, block) == ERROR) {
							Kernel.report("Write failure: Failed to set target block " + block + "\n");
							iNode.flag = Inode.UNUSED;
							return ERROR;
						}
					}
				}
				
				// byte offset-- 0 is a new block
				offset = seekPtr % Disk.blockSize;
				// bytes available
				available = Disk.blockSize - offset;
				// bytes remaining
				remaining = length - index;
				// bytes to write-- cannot be greater than available
				wLength = Math.min(available, remaining);
				
				data = new byte[Disk.blockSize];
				
				// read block from disk to data
				SysLib.rawread(block, data);
				
				// copy data to buffer
				// source, source position, destination, destination position, length to copy
				System.arraycopy(buffer, seekPtr, data, offset, wLength);
				// write data to disk
				SysLib.rawwrite(block, data);
				
				index += wLength;
				seekPtr += wLength;
			}
			// set new seek pointer
			fte.seekPtr = seekPtr;
			// save iNode to disk
			iNode.toDisk(fte.iNumber);
		}
		// if error was not returned, all bytes wrote successfully-- return length
		// TODO: return successful amount of bytes instead of -1?
		return length;
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
		// seek pointer, end of file
		int seekPtr, EOF;
		if (fte == null) return ERROR;
		seekPtr = fte.seekPtr;
		EOF = fsize(fte);
		switch (whence) {
			case SEEK_SET:
				// file's seek pointer is set to offset bytes from the beginning of the file
				seekPtr = offset;
				break;
			case SEEK_CUR:
				// file's seek pointer is set to its current value plus the offset
				seekPtr += offset;
				break;
				// file's seek pointer is set to the size of the file plus the offset
			case SEEK_END:
				seekPtr = EOF + offset;
				break;
			default:
				Kernel.report("Seek error: Whence " + whence + " is unrecognized");
				//return ERROR;
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
			iNode.setTargetBlock(block, (short) -1);
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
