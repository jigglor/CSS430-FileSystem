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
		int block, offset;
		byte[] data;
		
		if (iNumber < 0) return;

		block = getOffset(iNumber);
		offset = (iNumber % 16) * iNodeSize;
		data = new byte[Disk.blockSize];
		
		SysLib.rawread(block, data);

		// locates the corresponding iNode information in that block
		length = SysLib.bytes2int(data, offset);
		offset += 4;
		count = SysLib.bytes2short(data, offset);
		offset += 2;
		flag = SysLib.bytes2short(data, offset);
		offset += 2;

		// initialize a new iNode with this information
		for (int i = 0; i < directSize; i++) {
			direct[i] = SysLib.bytes2short(data, offset);
			offset += 2;
		}

		indirect = SysLib.bytes2short(data, offset);
		offset += 2;
		//iNodeList.add(this);
	}
	
	/*toDisk()
	  save to disk as the i-th inode
	*/
	public void toDisk(short iNumber) {
		//initialize the Inode to be added back to Disk
		int offset, block;
		byte[] newInode, data;
		if (iNumber < 0) return;
		newInode = new byte[Disk.blockSize];
		offset = (iNumber % 16) * iNodeSize;
		block = getOffset(iNumber);
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
		/*offset += 2;
		
		//get the Disk block at the iNumber-th place
		data = new byte[Disk.blockSize];
		SysLib.rawread(block, data);
		offset = (short) ((iNumber % 16) * iNodeSize);
		
		//put the iNode into the block
		for (int i = 0; i < iNodeSize; i++) {
			data[offset] = newInode[i];
			offset++;
		}
		//write back to Disk*/
		SysLib.rawwrite(block, newInode);
	}
	
	/*getOffset()
	  return the current offset of the block
	*/
	public int getOffset(int iNumber) {
		if (iNumber < 0) return ERROR;
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
		byte[] data;
		if (iNumber < 0) return false;
		// check if all 12 pointers are null
		if (indirect != -1)
			return false;
		for (int i = 0; i < directSize; i++)
			if (direct[i] == -1)
				return false;
		// set indirect pointer to the first block index
		indirect = iNumber;
		
		data = new byte[Disk.blockSize];
		//set all indirect blocks to -1
		for (int i = 0, l = Disk.blockSize/2; i < l; i += 2)
			SysLib.short2bytes((short) -1, data, i);
		SysLib.rawwrite(iNumber, data);
		return true;
	}
	
	/*findTargetBlock()
	  find and return the location of the block
	*/
	public short findTargetBlock(int offset) {
		int block, iBlock; // offset block, indirect offset
		byte[] data;
		block = offset / Disk.blockSize;  
		
		//target block is in one of the direct blocks
		//return the block
		if (block < directSize)
			return direct[block];
		//this block is not registered
		if (indirect < 0)
			return ERROR;
			
		data = new byte[Disk.blockSize];
		SysLib.rawread(indirect, data);
		
		//get the target block in indirect block
		//and return 
		iBlock = block - directSize;
		short loc = SysLib.bytes2short(data, iBlock * 2);
		return loc;
	}
	
	/*setTargetBlock()
	  register the block into one of the direct or indirect blocks
	*/
	public int setTargetBlock(int iNumber, short fBlock) {
		int block, iBlock; // iBlock is indirect block
		byte[] data;
		if (iNumber < 0) return ERROR;
		
		block = iNumber / Disk.blockSize;
		
		//to sat into one of the direct blocks
		if (block < directSize) {
			//existed block
			if (direct[block] >= 0)
				return ERROR;
			//before is unused
			if ((block > 0) && (direct[block - 1] == -1))
				return ERROR;
			//else register the target block
			direct[block] = (short) iNumber;
			return OK;
		}
		//indirect blocks are unused
		if (indirect < 0)
			return ERROR;
			
		data = new byte[Disk.blockSize];
		SysLib.rawread(indirect, data);
		//iBlock = block - iNodeSize;
		iBlock = block - directSize;
		
		if (SysLib.bytes2short(data, iBlock * 2) > 0)	{
			SysLib.cerr("indexBlock, indirectNo = " + block
			+ " contents = " + 
			SysLib.bytes2short(data, iBlock * 2) + "\n");
			return ERROR;
		}
		
		//register the block into one of the indirect blocks
		SysLib.short2bytes(fBlock, data, iBlock * 2);
		SysLib.rawwrite(indirect, data);
		return OK;
	}
	
	/*deleteIndexBlock()
	  delete one of the registered block in indirect blocks
	*/
	public byte[] deleteIndexBlock() {
		byte[] data;
		
		// nothing to delete
		if (indirect == ERROR) return null;
		
		data = new byte[Disk.blockSize];
		SysLib.rawread(indirect, data);
		
		// indicate deletion
		indirect = -1;
		return data;
	}
}
