import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
public class Client implements Runnable{
	private static String[] SlaveName = null;
	 private static int[] SlaveNumber = null;
	private static String CurrentPath = System.getProperty("user.dir");
    	public static void main(String[] args) throws Exception {
		int PCnum = 1;
		Scanner in = new Scanner(System.in);
		System.out.println("Please enter the number of workstations:");
		PCnum = in.nextInt();
		SlaveName = new String[PCnum];
		SlaveNumber = new int[PCnum];
		for(int i=0;i<PCnum;i++) {
			System.out.println("Please enter the name of workstation "+(i+1)+":");
			SlaveName[i] = in.next();
			SlaveNumber[i] = 7;
		}
		int MasterNumber = 8;	
		(new Thread(new Client())).start();
		ServerSocket servsock = new ServerSocket(MasterNumber);
		while (true) {
			Socket sock = servsock.accept();
			int FileNameLength;
			byte[] FileNameByte = new byte[1024];
			InputStream is = sock.getInputStream();
			FileNameLength = is.read(); 
			is.read(FileNameByte,0,FileNameLength);
			String FileName = new String(FileNameByte,0,FileNameLength);
			FileOutputStream fos = new FileOutputStream(CurrentPath+"\\Result\\"+FileName);
			System.out.println("File created");
			BufferedOutputStream bos = new BufferedOutputStream(fos);
				
			byte[] buff = new byte[1024];
			int len;
			while ((len = is.read(buff))!=-1) {
				bos.write(buff,0,len);
			}
			is.close();
			bos.close();
			fos.close();
			sock.close();
			System.out.println("Result received");
			System.out.println("****************");
		}
	}
	public void run() {
		while (true) {
			File file=new File(CurrentPath+"\\Task");
			String files[];
			files=file.list();
			int num = files.length;
			if (num>0) {
				System.out.println("File name: "+files[0]);
				for (int j=0;j<SlaveName.length;j++){
					if (SendFile(CurrentPath+"\\Task\\"+files[0],SlaveName[j],SlaveNumber[j])) {
						deleteFile(CurrentPath+"\\Task\\"+files[0]);
						System.out.println("File has been sent.");
						System.out.println("****************************");
						break;
					}
				}
			}
			try{
				Thread.sleep(5000);
			} catch (InterruptedException e){
			}
		}
	}    
	private boolean SendFile(String Path, String hostName, int portNumber) {
		System.out.println("Trying to send to "+ hostName);
		try {
		Socket sock = new Socket(hostName, portNumber);
		int Message;
		int count = 0;
		int Available = 0;
		int Unavailable = 0;
		boolean Busy;
		InputStream is = sock.getInputStream();
		while(true) {
			Message = is.read();
			switch (count) {
				case 0:
					if (Message == 0) Available++;
					else if(Message ==127) Unavailable++;
					break;
				case 1:
					if (Message == 0) Available++;
					else if(Message == 127) Unavailable++;
					break;
				case 2:
					if (Message == 127) Available++;
					else if(Message == 0) Unavailable++;
					break;
				case 3:
					if (Message == 127) Available++;
					else if(Message == 0) Unavailable++;
					break;
				default: 
					break;
			}
			count++;
			if ((int)Available == 4) {
				System.out.println("Available");
				Busy = false;
				break;
			} else if ((int)Unavailable == 4) {
				System.out.println("Busy");
				Busy = true;
				break;
			} else if (count==4){
				System.out.println("Message Not Confirmed");
				sock.close();
				return false;
			}
		} 
		System.out.println("Message Confirmed");
		if (!Busy) {
			File myFile = new File(Path);
			String myFileName = myFile.getName();
			byte[] mybytearray = new byte[(int) myFile.length()];
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
			bis.read(mybytearray, 0, mybytearray.length);
			OutputStream os = sock.getOutputStream();
			os.write(myFileName.getBytes().length);
			os.write(myFileName.getBytes());
			os.write(mybytearray, 0, mybytearray.length);
			os.flush();
			os.close();
			bis.close();
			sock.close();
			return true;
		}
		sock.close();
		} catch (IOException e) {
            System.out.println("Cannot connect to the workstation");
        }
		return false;		
	}
	public boolean deleteFile(String sPath) {  
		boolean flag = false;  
		File file = new File(sPath);  
		if (file.isFile() && file.exists()) {  
			file.delete();  
			flag = true;  
		}  
		return flag;  
	}  
}
