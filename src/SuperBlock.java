/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
   CSS430 Final Project - File System
   Superblock Class
 */

public class SuperBlock {
	private final int defaultInodeBlocks = 64;
	public int totalBlocks; // default 1000
	public int totalInodes; // default 64 (or 4 blocks including Inodes)
	public int freeList; // default 5 (block#0 = super, blocks#1,2,3,4 = inodes)

	public SuperBlock(int diskSize) {
		
		// allocate superblock
		byte[] block = new byte[Disk.blockSize];
		// read superblock from disk
		SysLib.rawread(0, block);
		totalBlocks = SysLib.bytes2int(block, 0);
		totalInodes = SysLib.bytes2int(block, 4);
		freeList = SysLib.bytes2int(block, 8);
		
		// success
		if (totalBlocks == diskSize && totalInodes > 0 && freeList >= 2) {
			return;
		} else {
			format(defaultInodeBlocks);
		}
	}

	private void format(int diskSize) {
		int blocks = diskSize/16;
		totalBlocks = diskSize;
		Inode inode;
		for (int i = 0; i < diskSize; i++) {
			inode = new Inode();
			inode.flag = Inode.UNUSED;
			inode.toDisk(i);
		}
	}
	
	// write back totalBlocks, totalInodes, and freeList to disk
	public void sync() {
		
	}
	
	// Dequeue the top block from the free list
	public void getFreeBlock() {
		
	}
	
	// Enqueue a given block to the end of the free list
	public void returnBlock(int blockNumber) {
		
	}
}
