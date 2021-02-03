import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Every PDFTreeNode contains one object in JSON.
 *
 * @author Yizhen Yuan
 */
public class PDFTreeNode {
    final static PDFTreeNode root = new PDFTreeNode();
    String title;
    int startPage;
    int endPage;
    /**
     * denote where in the start page, this section starts
     */
    int startPageSeparateIndex;
    /**
     * The corresponding PDOutlineItem for this Node
     *
     * @see PDOutlineItem
     */
    PDOutlineItem pdOutlineItem;
    PDFTreeNode parent;
    /**
     * the next children in list parent.children
     */
    PDFTreeNode next;
    List<PDFTreeNode> children;
    List<String> index;
    /**
     * A list contains all node
     * The result of preorder of the tree
     */
    static List<PDFTreeNode> all = new ArrayList<>();
    /**
     * The printer for output error message
     * When this is set to be new Myprinter(null)
     * the information will not be shown
     */
    static PrintStream myprinter =System.err;

    /**
     * Set the root for the PDDocument
     * The only way to initialized PDFTreeNode
     * All information except the content in each Node is initialized after this function finish
     *
     * @param items the highest level of TOC
     * @see PDFTreeNode#setEndPage()
     * @see PDFTreeNode#PDFTreeNode(PDOutlineItem, PDFTreeNode)
     */
    public static void setRoot(List<PDOutlineItem> items) {
        root.children = items.stream().map(e -> new PDFTreeNode(e, root)).collect(Collectors.toList());
        root.setEndPage();
    }

    /**
     * Private function for recursion purpose
     * It initialized $endPage and the variable $next for each object.
     */
    private void setEndPage() {
        //Lowest level, stop
        if (children.isEmpty()) return;
        /*
            Transverse the children in reverse order
            The end page for last child is set to be
            the end page of their parent, which is "this"
         */
        int tempEndPage = endPage;
        PDFTreeNode next = null;
        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).endPage = tempEndPage;
            children.get(i).next = next;
            //recurse here, let children set their end page too.
            children.get(i).setEndPage();
            next = children.get(i);
            tempEndPage = children.get(i).startPage;
            /*
                if the children is not at the lowest level of the TOC tree
                we assume it is a chapter or something similar which usually
                does not separate in the middle of the page
             */
            if (children.get(i).startPageSeparateIndex == 0) tempEndPage--;
        }
    }

    /**
     * In package helper function
     * Note: the input page should be the page number
     * written on the book in the corner
     * This function WILL adapt to the offset between
     * pdf page number and print version page number
     * @param page the page number
     * @return a list of PDFTreeNode that startPage<=page<=endPage
     */
    static List<PDFTreeNode> findIndex(int page) {
        page+=GetContents.indexDeviation;
        final int Page=page;
        return all.stream().filter(e -> e.children.isEmpty()).filter(e -> e.startPage <= Page && e.endPage >= Page).collect(Collectors.toList());
    }

    /**
     * Show the tree in a JSON favor
     * @param content do we contains content in the result
     * @return the String contains JSON content
     * @see PDFTreeNode#iterate(int, StringBuilder,boolean)
     */
    public static String showTree(boolean content) {
        StringBuilder stringBuilder = new StringBuilder();
        root.iterate(0, stringBuilder,content);
        System.out.println("Tree information generated");
        return stringBuilder.toString();
    }

    /**
     * Private constructor, ONLY used for root
     * which is a special node
     */
    private PDFTreeNode() {
        this.children = new ArrayList<>();
        this.endPage=GetContents.pDDocument.getNumberOfPages();
    }

    /**
     * The constructor of normal node
     * This is a recursion constructor
     * which means it initialized the parent node and initialized all children nodes
     */
    private PDFTreeNode(PDOutlineItem pdOutlineItem, PDFTreeNode parent) {
        /*
            Add this node to the list, so it is in perorder
         */
        all.add(this);
        this.pdOutlineItem = pdOutlineItem;
        this.title = pdOutlineItem.getTitle();
        this.parent = parent;
        this.index = new ArrayList<>();
        try {
            /*
                Add one is needed
             */
            this.startPage = GetContents.pDDocument.getPages().indexOf(pdOutlineItem.findDestinationPage(GetContents.pDDocument)) + 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
        children = new ArrayList<>();
        if (pdOutlineItem.children().iterator().hasNext() || parent == root) {
            /*
               If it has children or it is the level right below the root, it should be
               a chapter or part, which means it should start at a new page
             */
            startPageSeparateIndex = 0;
        } else {
            try {
                /*
                    getting the content of the starting page
                 */
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(startPage);
                stripper.setEndPage(startPage);
                String text = stripper.getText(GetContents.pDDocument);
                /*
                    getting them in lines
                 */
                String[] raw = text.split("\n");
                /*
                    try to find a exact match of title
                 */
                Optional<String> target = Arrays.stream(raw).filter(s -> GeneralTools.superCompare(s, title)).findFirst();
                if (target.isPresent()) {
                    /*
                        accurate cut of the page is found, done
                     */
                    startPageSeparateIndex = text.indexOf(target.get());
                } else {
                    /*
                        try to find lines that contains the title, choose the shortest one(least useless information)
                     */
                    Optional<String> secondTarget = Arrays.stream(raw).filter(e -> e.contains(title)).min(Comparator.comparingInt(String::length));
                    if (secondTarget.isPresent()) {
                        /*
                            Print out the error message and set the cut, and done
                         */
                        myprinter.println("Can not find the accurate location of " + title + ", find ");
                        myprinter.println(GeneralTools.stringClean(secondTarget.get(),false) + " as its replacement");
                        startPageSeparateIndex = text.indexOf(secondTarget.get());
                    } else {
                        /*
                            Maybe the title is too long, so it gets split into 2 lines
                         */
                        Optional<String> thirdTarget = Arrays.stream(raw).filter(e -> title.contains(e)).max(Comparator.comparingInt(String::length));
                        if (thirdTarget.isPresent()) {
                            myprinter.println("Maybe the title is too long, and we find " + thirdTarget.get() + " as replacement");
                            startPageSeparateIndex = text.indexOf(thirdTarget.get());
                        } else {
                            /*
                                Can not find any, GG
                             */
                            myprinter.println("can not find any clue of where to cut the page,default set a half");
                            startPageSeparateIndex = text.length() / 2;
                        }
                    }
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        /*
            Recursively create children
         */
        children.addAll(StreamSupport.stream(pdOutlineItem.children().spliterator(), false).map(e -> new PDFTreeNode(e, this)).collect(Collectors.toList()));
    }

    /**
     * Print out the whole tree in a JSON form.
     * and also get the content of each node (but don`t put them into any variable/fields, just returned them in StringBuilder)
     * @param level the level of tab, used by recursion
     * @param stringBuilder the result String for the JSON output
     * @param content the boolean that do I need to contains the content in the output
     * @see WordCount
     */
    private void iterate(int level, StringBuilder stringBuilder,boolean content) {
        if (this == root) {
            /*
                Special handling for root node
             */
            children.forEach(e -> {
                e.iterate(1, stringBuilder,content);
            });
            return;
        }
        /*
            Count how many tabs we should have at this level and store it into a String
         */
        StringBuilder tempTab = new StringBuilder();
        for (int i = 0; i < level; i++) {
            tempTab.append("\t");
        }
        final String tab = tempTab.toString();
        /*
            Appending item title and pages for this node
         */
        stringBuilder.append(tab).append("{\n");
        stringBuilder.append(tab).append("\"item\": \"").append(title).append("\",\n");
        stringBuilder.append(tab).append("\"pages\": ").append(startPage).append(",\n");
        stringBuilder.append("\"index\": [ ");
        index.forEach(e-> stringBuilder.append("\"").append(e).append("\" ,"));
        stringBuilder.deleteCharAt(stringBuilder.length()-1);
        stringBuilder.append("]");
        String answer = null;
        if(content) {
            stringBuilder.append(tab).append(",\n\"item-text\": \"");
            try {
            /*
                Getting content of this node
             */
                PDFTextStripper stripper;
                stripper = new PDFTextStripper();
                stripper.setStartPage(startPage);

                if (startPage == endPage) {
                /*
                    If this section only contains one page
                 */
                    stripper.setEndPage(endPage);
                    answer = stripper.getText(GetContents.pDDocument);

                    answer = answer.substring(startPageSeparateIndex);
                    if (next != null && next.startPageSeparateIndex != 0) {
                    /*
                        In this if:
                            The next section also start in this page,
                            so we need to cut out the information for next section
                        Out this if:
                            This section ends perfectly at the end of this page.
                     */
                        answer = answer.substring(0, next.startPageSeparateIndex-startPageSeparateIndex);
                    }
                } else {
                    if (next != null && next.startPageSeparateIndex != 0) {
                    /*
                        This section has multiple pages, and it does not
                        end perfectly at the end of one page
                        so we separately getting its pages from 0~end-1, and end
                        since we also need to cut out the information belongs to
                        next section in the end page of this section
                     */
                        stripper.setEndPage(endPage - 1);
                        answer = stripper.getText(GetContents.pDDocument);
                        PDFTextStripper lastPageStripper = new PDFTextStripper();
                        lastPageStripper.setStartPage(endPage);
                        lastPageStripper.setEndPage(endPage);
                        answer += lastPageStripper.getText(GetContents.pDDocument).substring(0, next.startPageSeparateIndex);
                    } else {
                    /*
                        The section ends perfectly at the end of ending page
                     */
                        stripper.setEndPage(endPage);
                        answer = stripper.getText(GetContents.pDDocument);
                    }
                }
                answer=answer.replaceAll(System.lineSeparator(),"\\n");
                answer=answer.replaceAll("\"","'");
            } catch (IOException e) {
                e.printStackTrace();
            }
            stringBuilder.append(answer).append("\",\n");
        }
        /*
            Append popular words to this section
         */

        if(content) {
            stringBuilder.append("\"popular words\": [");
            assert answer != null;
            List<String> topWords = new WordCount(answer).getTop(20);
            topWords.forEach(e -> stringBuilder.append("\"").append(e).append("\","));
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            stringBuilder.append("]");
        }
        if (!children.isEmpty()) {
           /*
                If this item has children, then recursively print them too.
            */
            stringBuilder.append(",\n");
            stringBuilder.append(tab).append("\"subitems\": [\n");
            children.forEach(e -> e.iterate(level + 1, stringBuilder,content));
            stringBuilder.append(tab).append("]\n");
        } else {
            stringBuilder.append("\n");
        }
        stringBuilder.append(tab).append("}");
        if (pdOutlineItem.getNextSibling() != null)
            /*
                determine should there be comma or not based on is there following items
             */
            stringBuilder.append(",\n");
        else stringBuilder.append("\n");
    }

}
