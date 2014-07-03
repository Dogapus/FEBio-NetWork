import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;

public class Server implements Runnable{
	private static boolean busy = false;
	private static String MasterName = "like";
	private int MasterNumber = 8;
	private String CurrentPath = System.getProperty("user.dir");
	private static String FileName = null;
	public static void main(String[] args) throws IOException {
		int SlaveNumber = 7;
		ServerSocket servsock = new ServerSocket(SlaveNumber);
		while (true) {
			Socket sock = servsock.accept();
			MasterName = sock.getInetAddress().getHostName();
			if (!busy) {
				byte[] Message = {0,0,127,127};
				OutputStream os = sock.getOutputStream();
				os.write(Message, 0, Message.length);
				os.flush();
				
				int FileNameLength;
				byte[] FileNameByte = new byte[1024];
				InputStream is = sock.getInputStream();
				FileNameLength = is.read();
				is.read(FileNameByte,0,FileNameLength);
				FileName = new String(FileNameByte,0,FileNameLength);
				FileOutputStream fos = new FileOutputStream(FileName);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				
				byte[] buff = new byte[1024];
				int len;
				while ((len = is.read(buff))!=-1) {
					bos.write(buff,0,len);
				}
				is.close();
				bos.close();
				os.close();
				sock.close();
				busy = true;
				(new Thread(new ReceiveServer())).start();
				System.out.println("Master name: "+MasterName);
			} else {
				byte[] Message = {127,127,0,0};
				OutputStream os = sock.getOutputStream();
				os.write(Message, 0, Message.length);
				os.flush();
				os.close();
				sock.close();
			}
		}
	}	
	public void run() {
		File myFile = new File(FileName);
		if(myFile.exists()) {
			Runtime rn = Runtime.getRuntime();
			Process task = null;
			try {
				System.out.println("FileName: "+FileName);
				task = rn.exec("cmd /c start C:\\Progra~1\\FEBio1p8\\febio.exe -i "+FileName);
				System.out.println("Working...");
			} catch (Exception e) {
				System.out.println("Error exec!");
			}
			while (isRunning("FEBio.exe")) {
				try{
					Thread.sleep(1000);
				} catch (InterruptedException e){
				}
			}
			System.out.println("Work Finished");
			myFile.delete();
			deleteFile(FileName.replaceAll(".feb",".xplt"));
			SendResult(FileName.replaceAll(".feb",".log"),MasterName,MasterNumber);
		} else {
			System.out.println("No .feb file received");
		}
		busy = false;
	}
	private void SendResult(String FileToBeSent,String HostName, int HostPortNumber){
		while(true){
			try {
				File myFile = new File(FileToBeSent);
				String myFileName = myFile.getName();
				Socket Sendsock = new Socket(HostName, HostPortNumber);
				byte[] Sendbytearray = new byte[(int) myFile.length()];
				BufferedInputStream Sendbis = new BufferedInputStream(new FileInputStream(myFile));
				Sendbis.read(Sendbytearray, 0, Sendbytearray.length);
				OutputStream Sendos = Sendsock.getOutputStream();
				Sendos.write(myFileName.getBytes().length);
				Sendos.write(myFileName.getBytes());
				Sendos.write(Sendbytearray, 0, Sendbytearray.length);
				Sendos.flush();
				Sendos.close();
				Sendbis.close();
				Sendsock.close();
				break;
			} catch (Exception e){
				System.out.println("Host busy. Reconnect after 5s...");
				try{
				Thread.sleep(1000);
				} catch (InterruptedException e2){
				}
			}
		}
		System.out.println("File has been sent");
		System.out.println("***********************");
		(new File(FileToBeSent)).delete();
		try{
			Thread.sleep(1000);
		} catch (InterruptedException e2){
		}
	}
	private boolean isRunning(String processName) {
    BufferedReader bufferedReader = null;
    try{
        Process proc = Runtime.getRuntime().exec("tasklist /FI \"IMAGENAME eq "+processName+ "\"");
        bufferedReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.contains(processName)) {
                return true;
            }
        }
        return false;
    } catch (Exception ex){
        ex.printStackTrace();
        return false;
    }
    finally{
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (Exception ex){
            }
        }
    } 
	} 
	private boolean deleteFile(String sPath) {  
		boolean flag = false;  
		File file = new File(sPath);  
		if (file.isFile() && file.exists()) {  
			file.delete();  
			flag = true;  
		}  
		return flag;  
	}
	private static void ReplaceStringInFile(File inFile, int lineno, String lineToBeInserted) throws Exception {
        File outFile = new File("$$$$$$$$$$$$$$.tmp");
        FileInputStream fis  = new FileInputStream(inFile);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));
        FileOutputStream fos = new FileOutputStream(outFile);
        PrintWriter out = new PrintWriter(fos);
        String thisLine = "";
        int i =1;
        while ((thisLine = in.readLine()) != null) {
			if(i == lineno) out.println(lineToBeInserted);
			else out.println(thisLine);
			i++;
		}
		out.flush();
		out.close();
		in.close();
		fis.close();
		fos.close();
		inFile.delete();
		outFile.renameTo(inFile);
	}	
}


