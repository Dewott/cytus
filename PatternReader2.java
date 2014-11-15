package cytus;

import java.io.*;
import java.util.*;

public class PatternReader2 {
	public static Pattern read(BufferedReader in) throws Exception {
		double beat = 0, pshift = 0;

		in.readLine();
		String off = in.readLine();
		off = off.substring(11);
		pshift = Double.parseDouble(off);
		String size = in.readLine();
		size = size.substring(10);
		beat = Double.parseDouble(size);

		Pattern pdata = new Pattern(beat, pshift);

		int count = 0, linkid = 0;
		String str = in.readLine();
		while (str != null) {
			if (str.indexOf("LINK") != -1) {
				str = str.substring(5);
				Scanner s = new Scanner(str);
				Pattern.Link l = pdata.new Link(linkid++);
				while (s.hasNext())
					l.add(s.nextInt());
				pdata.links.add(l);
				s.close();
				str = in.readLine();
				continue;
			}

			str = str.substring(5);
			Scanner s = new Scanner(str);
			s.nextInt();
			double time = s.nextDouble();
			double x = s.nextDouble();
			double hold = s.nextDouble();
			s.close();

			pdata.notes.add(pdata.new Note(count++, time, x, hold));
			str = in.readLine();
		}

		return pdata;
	}
}