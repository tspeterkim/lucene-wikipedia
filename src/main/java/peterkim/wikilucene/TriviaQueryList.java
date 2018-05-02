package peterkim.wikilucene;

import java.util.List;

public class TriviaQueryList {
	
	public class TriviaQuery {
		public class TriviaAnswer {
			public List<String> Aliases;
			public String getAnswers() {
				String a = "";
				for (String answer : Aliases) {
					a += answer + "@@@";
				}
				return a.substring(0, a.length()-3);
			}
		}
		
		public TriviaAnswer Answer;
		public String Question;
		public String QuestionId;
	}
	
	public List<TriviaQuery> Data;
}
