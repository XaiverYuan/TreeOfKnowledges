import java.util.ArrayList;
import java.util.List;

public class GeneralTools {
    public static String stringClean(String a) {
        return stringClean(a,true);
    }
    public static String stringClean(String a,boolean cleanSpace) {
        StringBuilder aa = new StringBuilder();
        a=a.toLowerCase();
        List<Integer> integers = new ArrayList<>();
        for (char c : a.toCharArray()) {
            integers.add((int) c);
        }
        integers.stream().filter(e -> e > (cleanSpace?32:31)).mapToInt(Integer::intValue).forEach(e -> aa.append((char) e));
        return aa.toString();
    }
    public static int characterCount(String a){
        a=stringClean(a);
        int count=0;
        for (char c : a.toCharArray()) {
            if(c>='a'&&c<='z')count++;
        }
        return count;
    }
    public static boolean superCompare(String a, String b) {
        return stringClean(a).equals(stringClean(b));
    }


}
