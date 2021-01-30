# Read You 
## Author
These codes is written by Yizhen Yuan (yuan226@purdue.edu) directed by Professor Benotman (hbenotma@purdue.edu). If you want to use any part of these programs, please contact us.

## Purpose of program
Convert PDF file(usually textbook) into JSON file containing Table Of Content, Index and popular words in each section
## How to use this program
### transfer PDF into JSON
``` 
javac *.java
java GetContents.java INPUT [OUTPUT] [-s]
```
the -s here means omit the content and the popular words, s stand for shorter.
if the output is not set, and the input is XXX.pdf then the output will automatically go to XXX_output.json
The reason we add "_output" is to avoid collision of name as much as possible (so we don`t mistakenly override originally existing file)

the formatting of the JSON file is:

```json
{
"bookTitle": "??"
"TOC": [
    ...
]
}
```
and each json item has following terms:
```json
{
"item": "???",
"pages": X,
"index": ["??","??"],
"item-text": "???(the new line will be replaced by \n, and replace " by ')" ???",
"popular words": ["??","??"]
}

```
### transfer PDF into txt
```
javac *.java
java GetContents.java INPUT [OUTPUT] -c
```
the -c will not generate a json file, it just transform all the text in the pdf into txt named XXX_output.txt if the output is not specified (assume INPUT is XXX.pdf)
