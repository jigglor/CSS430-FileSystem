import java.util.Vector;

/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
   CSS430 Final Project - File System
   Inode Class
 */

public class Inode {
	//private static Vector<Inode> Inodes; // maintains all iNode on memory
	
	private final static int iNodeSize = 32; // fix to 32 bytes
	private final static int directSize = 11; // # direct pointers

	// flag states
	public final static int UNUSED = 0;
	public final static int USED = 1;
	// return values
	public final static int OK = 0;
	public final static int ERROR = -1;

	public int length; // file size in bytes
	public short count; // # file-table entries pointing to this
	public short flag; // 0 = unused, 1 = used, ...
	public short[] direct = new short[directSize]; // direct pointers
	public short indirect; // a indirect pointer

	// a default constructor
	public Inode() {
		length = 0;
		count = 0;
		flag = UNUSED;
		for (int i = 0; i < directSize; i++) {
			direct[i] = ERROR;
		}
		indirect = ERROR;
	}

	// Inode constructor
	public Inode(short iNumber) {
		// read the corresponding disk block
		int noBlock = 1 + iNumber / 16;
		byte[] block = new byte[Disk.blockSize];
		SysLib.rawread(noBlock, block);
		int offset = (iNumber % 16) * 32;
		
		// locates the corresponding iNode information in that block
		length = SysLib.bytes2int(block, offset);
		offset += 4;
		count = SysLib.bytes2short(block, offset);
		offset += 2;
		flag = SysLib.bytes2short(block, offset);
		offset += 2;
		
		// initialize a new iNode with this information
		for (int i = 0; i < directSize; i++)	{
			direct[i] = SysLib.bytes2short(block, offset);
			offset += 2;
		}
		
		indirect = SysLib.bytes2short(block, offset);
		offset += 2;
	}
	

	public int toDisk(short iNumber) {
		// save to disk as the i-th inode
		byte[] buf = new byte[Disk.blockSize];
		int offset = (iNumber * iNodeSize) % Disk.blockSize;
		
		SysLib.int2bytes(length, buf, offset);
		offset += 4;
		SysLib.short2bytes(count, buf, offset);
		offset + 2;
		SysLib.short2bytes(flag, buf, offset);
		offset += 2;
		
		for (int i = 0; i < directSize; i++)	{
			SysLib.short2bytes(direct[i],buf, offset);
			offset += 2;
			
		}
		
		SysLib.short2bytes(indrect,buf, offset);
		offset += 2;
		
		int offset2 = 1 + iNumber / 16;
		byte[] buf2 = new byte[Disk.blockSize];
		SysLib.rawread(offset2, buf2);
		offset = iNumber % 16 * 32;
		
		SysLib.rawwrite(offset2, buf2);
	}
	
	public short getIndexBlockNumber()	{
		return indirect;
	}
	
	public boolean setIndexBlock(short indexBlockNumber)	{
		//check if all 12 pointers are null
		for (int i = 0; i < directSize; i++)	{
			if (direct[i] == -1)
				return ERROR;
		}
		
		if (indrect != -1)
			return ERROR;
		
		//set indirect pointer to the first block index
		indirect = indexBlockNumber;
		
		byte[] data = new byte[512];
		for (int i = 0; i < 256; i++)	
			SysLib.short2bytes((short)-1, data, i * 2);
			
		SysLib.rawwrite(indexBlockNumber, data);
	}
	
	public short findTargetBlock(int offset)	{
		
		
	}
	
	public int setTargetBlock(int iNumer, short i)	{
		
		
	}
	
	public byte[] deleteIndexBlock()	{
		
	}
}
