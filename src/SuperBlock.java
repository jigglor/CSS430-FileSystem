/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
   CSS430 Final Project - File System
   Superblock Class
 */

public class SuperBlock {
	private final static int defaultInodeBlocks = 64;
	public int totalBlocks; // default 1000
	public int totalInodes; // default 64 (or 4 blocks including Inodes)
	public int freeList; // default 5 (block#0 = super, blocks#1,2,3,4 = inodes)
	
	/*Default SuperBlock Constructor
	  Create in format of 64 Inodes
	*/
	public SuperBlock() {
		this(defaultInodeBlocks);
	}
	
	/*SuperBlock Constructor
	*/
	public SuperBlock(int diskSize) {
		
		// allocate superblock
		byte[] block = new byte[Disk.blockSize];
		// read superblock from disk
		SysLib.rawread(0, block);
		totalBlocks = SysLib.bytes2int(block, 0);
		totalInodes = SysLib.bytes2int(block, 4);
		freeList = SysLib.bytes2int(block, 8);
		
		// success case
		if (totalBlocks == diskSize &&
			totalInodes > 0 &&
			freeList >= 2) {
			return;
		} else {
			format(defaultInodeBlocks);
		}
	}
	
	/*format()
	  Redo the formatting of Inodes and Superblock by the given
	  format number (inodeNo). For example, 32 will yield 2 blocks
	  of Inodes. 
	*/
	public void format(int iNodes) {
		totalBlocks = Disk.blockSize;
		totalInodes = iNodes;    
		
		for (int i = 0; i < totalInodes; i++) {
			Inode newInode = new Inode();
			//TODO: ADD? Since Inode constructor already 
			//implement flag as UNUSED, so no need?
			//newInode.flag = newInode.UNUSED;
			newInode.toDisk(i);
		}
		
		//set the free list depends on the number of Inodes
		//default 64 free list will points to 4
		freeList = (totalInodes * 32 / Disk.blockSize);
		
		//create new free blocks and write it into Disk
		for (int j = freeList; j < totalBlocks; j++) {
			byte[] block = new byte[Disk.blockSize];
			
			for (int i = 0; i < Disk.blockSize; i++)
				block[i] = 0;
				
			SysLib.int2bytes(j + 1, block, 0);
			SysLib.rawwrite(j, block);
		}
		//finalized and sync SuperBlock info to Disk
		//with everything formatted 
		sync();
	}
	
	/* sync()
	   write back totalBlocks, totalInodes, and freeList to Disk
	   in order to update the new specs in of the Superblock
	*/
	public void sync() {
		byte[] block = new byte[Disk.blockSize];
		SysLib.int2bytes(totalBlocks, block, 0);
		SysLib.int2bytes(totalInodes, block, 4);
		SysLib.int2bytes(totalInodes, block, 8);
		SysLib.rawwrite(0, block);
		SysLib.cout("Superblock synchronized");
	}
	
	/*getFreeBlock()
	// Dequeue the top block from the free list
	*/
	public short getFreeBlock() {
		short freeBlock;
		byte[] block;
		
		// return -1 if there are no more free blocks
		if (freeList < 0 || freeList > totalBlocks) return -1;
		// free block is given from free list
		freeBlock = (short) freeList;
		//create new empty block
		block = new byte[Disk.blockSize];
		//get the content of the freeList block
		//wipe out the to block from the freeList
		//with an empty block
		SysLib.rawread(freeList, block);
		SysLib.int2bytes(0, block, 0);
		SysLib.rawwrite(freeList, block);
		
		// free list becomes free block
		freeList = SysLib.bytes2int(block, 0);
		return freeBlock;
	}
	
	/*returnBlock()
	  Enqueue a given block to the end of the free list
	  Return true if the operation is successful
	*/
	public boolean returnBlock(int blockNumber) {
		byte[] block;
		// blockNumber cannot be superblock or out of range
		if (blockNumber < 0 || blockNumber > totalBlocks) return false;
		block = new byte[Disk.blockSize];
		//translate the new block to the end of
		// the freeList
		SysLib.int2bytes(freeList, block, 0);
		//write the content of the given block
		//to the new block
		SysLib.rawwrite(blockNumber, block);
		//set the next freeList to the given block
		freeList = blockNumber;
		return true;
	}
}
