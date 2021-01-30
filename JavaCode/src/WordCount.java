
import java.util.*;
import java.util.stream.Collectors;

/**
 * The class for doing word count.
 * @author Yizhen Yuan
 */
public class WordCount {
    HashMap<String, Integer> wordMap;
    final static String[] banList=new String[]{"myself","ours","ourselves","your","yours","yourself","yourselves","himself","hers","herself","itself","they","them","their","theirs","themselves","what","which","whom","this","that","these","those","were","been","being","have","having","does","doing","because","until","while","with","about","against","between","into","through","during","before","after","above","below","from","down","over","under","again","further","then","once","here","there","when","where","both","each","more","most","other","some","such","only","same","than","very","will","just","should","aren","couldn","didn","doesn","hadn","hasn","haven","mightn","mustn","needn","shan","shouldn","wasn","weren","wouldn"};

    /**
     *
     * @param raw the String we want to count the word
     */
    public WordCount(String raw){
        wordMap=new HashMap<>();
        Arrays.stream(raw.split(" ")).filter(e->e.length()>3).
                map(String::toLowerCase).filter(e->{
            for (int i = 0; i < e.length(); i++) {
                if(e.charAt(i)<'a'||e.charAt(i)>'z'){
                    return false;
                }
            }
            return true;
        }).filter(e->!Arrays.asList(banList).contains(e)).forEach(e->{
            if(wordMap.containsKey(e)){
                wordMap.put(e,wordMap.get(e)+1);
            }else {
                wordMap.put(e,1);
            }
        });
    }

    /**
     *
     * @param count how many word we want to get
     * @return top $count word in the list
     */
    public List<String> getTop(int count){
        List<Map.Entry<String,Integer>> answer= new ArrayList<>(wordMap.entrySet());
        answer.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        return answer.stream().limit(count).map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
