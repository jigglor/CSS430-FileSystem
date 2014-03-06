/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
   CSS430 Final Project - File System
   Superblock Class
 */

public class SuperBlock {
	public final int totalBlocks;
	public final int freeList;
	public final int inodeBlocks;

	public SuperBlock(int blocks) {
		
		// allocate superblock
		byte[] block = new byte[Disk.blockSize];
		
		// read superblock from disk
		SysLib.rawread(0, block);
		
		totalBlocks = SysLib.bytes2int(block, 0);
		inodeBlocks = SysLib.bytes2int(block, 4);
		freeList = SysLib.bytes2int(block, 8);
		
		// success
		if (totalBlocks == blocks) {
			// TODO: more bounds checking
		} else {
			// TODO: format
		}
	}
}
