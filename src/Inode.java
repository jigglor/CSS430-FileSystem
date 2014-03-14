import java.util.Vector;

/* Joseph Schooley & Nguyen Tong, CSS 430, Professor Sung
 CSS430 Final Project - File System
 Inode Class
 */

public class Inode {
	public static Vector<Inode> iNodeList; // maintains all iNode on memory
	private final static int iNodeSize = 32; // fix to 32 bytes
	private final static int directSize = 11; // # direct pointers

	// flag states
	public final static int UNUSED = 0;
	public final static int USED = 1;
	public final static int READ = 2;
	public final static int WRITE = 3;
	public final static int DELETE = 4;
	// return values
	public final static int OK = 0;
	public final static int ERROR = -1;

	public int length; // file size in bytes
	public short count; // # file-table entries pointing to this
	public short flag; // 0 = unused, 1 = used, 2 = read, 3 = write, 4 = delete
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
		iNodeList.add(this);
	}

	// Inode constructor
	public Inode(short iNumber) {
		// read the corresponding disk block
		int blockNumber = 1 + iNumber / 16;
		byte[] data = new byte[Disk.blockSize];
		SysLib.rawread(blockNumber, data);
		int offset = (iNumber % 16) * 32;

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
		iNodeList.add(this);
	}

	public void toDisk(int iNumber) {
		// save to disk as the i-th inode
		byte[] buf = new byte[Disk.blockSize];
		int offset = (iNumber * iNodeSize) % Disk.blockSize;

		SysLib.int2bytes(length, buf, offset);
		offset += 4;
		SysLib.short2bytes(count, buf, offset);
		offset += 2;
		SysLib.short2bytes(flag, buf, offset);
		offset += 2;

		for (int i = 0; i < directSize; i++, offset += 2) {
			SysLib.short2bytes(direct[i], buf, offset);
		}

		SysLib.short2bytes(indirect, buf, offset);
		offset += 2;

		int offset2 = 1 + iNumber / 16;
		byte[] buf2 = new byte[Disk.blockSize];
		SysLib.rawread(offset2, buf2);
		offset = iNumber % 16 * 32;

		SysLib.rawwrite(offset2, buf2);
	}

	public short getIndexBlockNumber() {
		return indirect;
	}

	public boolean setIndexBlock(short indexBlockNumber) {
		byte[] data;
		
		if (indirect != -1)
			return false;
		
		// check if all 12 pointers are null
		for (int i = 0; i < directSize; i++)
			if (direct[i] == -1)
				return false;

		// set indirect pointer to the first block index
		indirect = indexBlockNumber;

		data = new byte[Disk.blockSize];
		for (int i = 0, l = Disk.blockSize/2; i < l; i += 2)
			SysLib.short2bytes((short) -1, data, i);

		SysLib.rawwrite(indexBlockNumber, data);
		return true;
	}

	public short findTargetBlock(int offset) {
		byte[] data;
		return ERROR;
	}

	public int setTargetBlock(int iNumber, int freeBlock) {
		return OK;
	}

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
