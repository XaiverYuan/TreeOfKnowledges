# some notes about the program
this is not a read me, you should only read this if you are facing some problem, or try to optimize my code
## library 
there are 3 library we are using here.
- tika
- pdfbox
- Pair

First two should be able to get from google, the version I use is tika-app-1.25.jar hopefully, al the version after that are available
I want to specifically talks about Pair here. I do not know why I can not use the javafx.util.Pair in the JDK 11. If you have same issue, please navigate to JDK 9

## Assumption made
### basic
- the input file is pdf, and it is XXX.pdf (have correct suffix), it is openable, readable...
- The pdf is not scanned, it should be something typed (I don`t have OCR here)
- PDF does not change during all process
### PDF to JSON
- there are available bookmarks in the PDF (i.e. You can see the table of content if you open the pdf with a browser except microsoft edge)
- if a section is in the higest level of TOC(Table Of Content), it should start in a new page (i.e. cover, copyright page, about author, preface...)
- if a section has children(subsection), it starts in a new page. (if the section is not in the bookmark, it is not counted as a section)
- assume the line splitor contains "\n" (no need to be "\n","\r\n" is ok, just it contains)
- in finding where a section begins (also called a "cut"):
    - if a line perfectly matches the title, first such line (hopefully there is only one such line) will be chosen to be the start place of the section (in another word, the "cut")
    - If we do not have a perfectly matching, then we will try to find the shortest line that contains the title (which means there is least useless information in the line)
    - If we still do not find any, we guess the title is so long such that it was split into multiple lines, we will find the longest line e, that title.contains(e)==true.
    - If still not find any, GG, we don`t have any idea, and just setting the half of the page to be the cut
- a section's endpage should be equal to its last subsection's endpage
- all page, lines belongs 1 section that has no subsection
- index
    - the deviation from pdf page and the page printed on the corner of the paper is fixed, and determined by where first section with subsection starts (Usually, this is the first useful section)
    - the index section is obtained by reverse transversing the list, and find the first section contains "index"
    - the index is processed by tokens, there is 3 types of token, String token, page token, page range token. Tokens are gotten by parsing the index section and split them by comma or new line. (which means something like see also, or see is ignored now)
    | previous token|current token|operation|
    | ---           |---         | ---     |
    |String Token|String Token|concat the String, since the index is not ended yet|
	|String Token|Number Token|The index is ended, now start getting pages and page range|
	|Number Token|String Token|The pages for previous index is ended, now start a new index|
	|Number Token|Number Token|Keep getting pages for current token|

	Number Token contains Page Token and Page Range Token
	- for Page Token: if only one smallest section (a section without subsection) contains it, then we add the index to that section; If multiple section contains it, we will check based where we think each section starts. and add all section between the section that contains the first existence of the index and the last section that contains the last existence of the index. (Usually this is not important, since usually a index only exist 1 or 2 times in a page and a page usually contains at most 2 sections)
	- for Page Range Token: find the first existence in the starting page and find last existence in the ending page, and find corresponding section that contains them, and add index to all section between them


