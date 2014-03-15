import java.util.Vector;

/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
 CSS430 Final Project - File System
 Inode Class
 */

public class Inode {
	//public static Vector<Inode> iNodeList; // maintains all iNode on memory
	private final static int iNodeSize = 32; // fix to 32 bytes
	private final static int directSize = 11; // # direct pointers

	// flag states
	public final static short UNUSED = 0;
	public final static short USED = 1;
	public final static short READ = 2;
	public final static short WRITE = 3;
	public final static short DELETE = 4;
	// return values
	public final static int OK = 0;
	public final static int ERROR = -1;

	public int length; // file size in bytes
	public short count; // # file-table entries pointing to this
	public short flag; // 0 = unused, 1 = used, 2 = read, 3 = write, 4 = delete
	public short[] direct = new short[directSize]; // direct pointers
	public short indirect; // a indirect pointer

	/*Inode Default Constructor
	  Initialize all global variables 
	*/
	public Inode() {
		length = 0;
		count = 0;
		flag = UNUSED;
		for (int i = 0; i < directSize; i++) {
			direct[i] = ERROR;
		}
		indirect = ERROR;
		//iNodeList.add(this);
	}

	/*Inode Constructor
	
	*/
	public Inode(short iNumber) {
		// read the corresponding disk block
		int blockNumber = getOffset(iNumber);
		byte[] block = new byte[Disk.blockSize];
		SysLib.rawread(blockNumber, block);
		int offset = (iNumber % 16) * iNodeSize;

		// locates the corresponding iNode information in that block
		length = SysLib.bytes2int(block, offset);
		offset += 4;
		count = SysLib.bytes2short(block, offset);
		offset += 2;
		flag = SysLib.bytes2short(block, offset);
		offset += 2;

		// initialize a new iNode with this information
		for (int i = 0; i < directSize; i++) {
			direct[i] = SysLib.bytes2short(block, offset);
			offset += 2;
		}

		indirect = SysLib.bytes2short(block, offset);
		offset += 2;
		//iNodeList.add(this);
	}
	
	/*toDisk()
	  save to disk as the i-th inode
	*/
	public void toDisk(int iNumber) {
		//initialize the Inode to be added back to Disk
		byte[] newInode;
		short offset;
		if (iNumber < 0) return;
		newInode = new byte[Disk.blockSize];
		offset = (short) ((iNumber % 16) * iNodeSize);
		SysLib.int2bytes(length, newInode, offset);
		offset += 4;
		SysLib.short2bytes(count, newInode, offset);
		offset += 2;
		SysLib.short2bytes(flag, newInode, offset);
		offset += 2;

		for (int i = 0; i < directSize; i++, offset += 2) {
			SysLib.short2bytes(direct[i], newInode, offset);
		}

		SysLib.short2bytes(indirect, newInode, offset);
		offset += 2;
		
		//get the Disk block at the iNumber-th place
		int diskOffset = getOffset(iNumber);
		byte[] block = new byte[Disk.blockSize];
		SysLib.rawread(diskOffset, block);
		offset = (short) ((iNumber % 16) * iNodeSize);
		
		//put the iNode into the block
		for (int i = 0; i < iNodeSize; i++) {
			block[offset] = newInode[i];
			offset++;
		}
		//write back to Disk
		SysLib.rawwrite(diskOffset, block);
	}
	
	/*getOffset()
	  return the current offset of the block
	*/
	public int getOffset(int iNumber) {
		return 1 + iNumber / 16;
	}
	
	/*getIndexBlockNumber()
	  return the Index Block that's registered to this 
	  iNode
	*/
	public short getIndexBlockNumber() {
		return indirect;
	}
	
	/*setIndexBlock()
	  create an Index Block to be used by
	  indirect block
	*/
	public boolean setIndexBlock(short iNumber) {
		byte[] block;
		
		// check if all 12 pointers are null
		if (indirect != -1)
			return false;
		
		for (int i = 0; i < directSize; i++)
			if (direct[i] == -1)
				return false;

		// set indirect pointer to the first block index
		indirect = iNumber;
		
		block = new byte[Disk.blockSize];
		//set all indirect blocks to -1
		for (int i = 0, l = Disk.blockSize/2; i < l; i += 2)
			SysLib.short2bytes((short) -1, block, i);

		SysLib.rawwrite(iNumber, block);
		return true;
	}
	
	/*findTargetBlock()
	  find and return the location of the block
	*/
	public short findTargetBlock(int offset) {
		byte[] block;
		int i = offset / Disk.blockSize;  
		
		//target block is in one of the direct blocks
		//return the block
		if (i < 11)
			return direct[i];
			
		//this block is not registered
		if (indirect < 0)
			return ERROR;
			
		block = new byte[Disk.blockSize];
		SysLib.rawread(indirect, block);
		
		//get the target block in indirect block
		//and return 
		int indirectOffset = i - directSize;
		return SysLib.bytes2short(block, indirectOffset * 2);
	}
	
	/*setTargetBlock()
	  register the block into one of the direct or indirect blocks
	*/
	public int setTargetBlock(int iNumber, short freeBlock) {
		byte[] block;
		int i = iNumber / Disk.blockSize;
		
		//to sat into one of the direct blocks
		if (i < 11) {
			
			//existed block
			if (direct[i] >= 0)
				return ERROR;
			
			//before is unused
			if ((i > 0) && (direct[i - 1] == -1))
				return ERROR;
			
			//else register the target block
			direct[i] = (short) iNumber;
			return OK;
		}
		
		//indirect blocks are unused
		if (indirect < 0)
			return ERROR;
			
		block = new byte[Disk.blockSize];
		SysLib.rawread(indirect, block);
		int indirectOffset = i - iNodeSize;
		
		if (SysLib.bytes2short(block, indirectOffset * 2) > 0)	{
			SysLib.cerr("indexBlock, indirectNo = " + i
			+ " contents = " + 
			SysLib.bytes2short(block, indirectOffset * 2) + "\n");
			
			return ERROR;
		}
		
		//register the block into one of the indirect blocks
		SysLib.short2bytes(freeBlock, block, indirectOffset * 2);
		SysLib.rawwrite(indirect, block);
		
		return OK;
	}
	
	/*deleteIndexBlock()
	  delete one of the registered block in indirect blocks
	*/
	public byte[] deleteIndexBlock() {
		byte[] block;
		
		// nothing to delete
		if (indirect == ERROR) return null;
		
		block = new byte[Disk.blockSize];
		SysLib.rawread(indirect, block);
		
		// indicate deletion
		indirect = -1;
		return block;
	}
}
