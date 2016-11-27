package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileWriteUtil {
	public static void WriteDocument(String fileName, String content) {
		FileWriter fw = null;
		BufferedWriter bw = null;
		PrintWriter pw = null;
		try {
			fw = new FileWriter(new File(fileName), true);
			bw = new BufferedWriter(fw);
			pw = new PrintWriter(bw);
			pw.write(content);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (pw != null) {
				pw.close();
			}
			if (bw != null) {
				try {
					bw.close();
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (fw != null) {
				try {
					fw.close();
				}
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
