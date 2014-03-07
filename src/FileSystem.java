import java.util.*;

/* Joseph Schooley & Nguyen Tong, Professor Sung, UWB
   CSS430 Project - File System
   FileSystem Class
*/

public class FileSystem {
   private SuperBlock superBlock;
   private Directory directory;
   private FileTable fileTable;
   
   public FileSystem()  {
      
   }
   
   public FileSystem(int blocks) {
      superBlock = new SuperBlock(blocks);
      
      directory = new Directory(); 
      
      fileTable = new FileTable(directory);
   
   }
   
   public void sync()   {
      
   }
   
   public boolean format() {
      
   }
   
   public FileTableEntry open() {
      
   }
   
   public boolean close()  {
      
   }
   
   public int fsize() {
      
   }
   
   public int read ()   {
      
   }
   
   public int write()   {
      
   }
   
   
   public boolean delete() {
      
   }
   
   public int seek() {
      
   }
   
   public boolean deallocateBlocks()   {
      
   }
}
