package peterkim.wikilucene;

import java.util.*;

public class WikiArticle {
	public int revid;
	public int id;
	public String url;
	public String title;
	public String text;
	
	private boolean isNonParagraph(String s) {
		if (s.length() == 1 || !s.contains("."))
			return true;
		return false;
	}
	
	public ArrayList<String> splitParagraph() {
		ArrayList<String> pgs = new ArrayList<String>(Arrays.asList(text.split("\n\n")));
		
		Iterator<String> it = pgs.iterator();
		while (it.hasNext()) {
		    if (isNonParagraph(it.next())) {
		        it.remove();
		    }
		}
		
		return pgs;
	}
	
}