---
id: string
title: 7.1 String functions
---

:::info Module dependency
This library is provided by `net.hasor:dataql-engine`. Add the dependency to your application, then import the library in your DataQL script.
:::

```javascript
import 'net.hasor.dataql.host.function.basic.StringUdfSource' as string;
return string.join(string.split('Alice,Bob', ','), ' / ');
// Alice / Bob
```

All examples below use DataQL syntax. Prepend the import above when running an individual example. In the signatures, `text` is the input text and `search` is the substring to find. String indices are zero-based; indices and lengths use Java UTF-16 code units. Square brackets denote optional arguments and are not written in calls.

## startsWith

`boolean startsWith(text, prefix)`: Checks whether text starts with prefix, respecting case. An empty prefix matches any non-null text. Two null arguments return true; exactly one null returns false.

```javascript
return string.startsWith('DataQL', 'Data');
// true
```

## startsWithIgnoreCase

`boolean startsWithIgnoreCase(text, prefix)`: Checks whether text starts with prefix, ignoring case. Empty-prefix and null handling match startsWith.

```javascript
return string.startsWithIgnoreCase('DataQL', 'data');
// true
```

## endsWith

`boolean endsWith(text, suffix)`: Checks whether text ends with suffix, respecting case. An empty suffix matches any non-null text; null handling matches startsWith.

```javascript
return string.endsWith('DataQL', 'QL');
// true
```

## endsWithIgnoreCase

`boolean endsWithIgnoreCase(text, suffix)`: Checks whether text ends with suffix, ignoring case. Empty-suffix and null handling match endsWith.

```javascript
return string.endsWithIgnoreCase('DataQL', 'ql');
// true
```

## lineToHump

`String lineToHump(text)`: Lowercases text, then removes an underscore before an English letter, digit, or another underscore, uppercasing the following letter. Intended for identifiers such as USER_NAME. A lone trailing underscore is retained; null returns null.

```javascript
return string.lineToHump('USER_NAME');
// userName
```

## humpToLine

`String humpToLine(text)`: Inserts an underscore before each uppercase English letter in text and lowercases it, collapses repeated underscores, then removes a leading underscore. Null returns null; an empty string stays empty.

```javascript
return string.humpToLine('UserName');
// user_name
```

## firstCharToUpperCase

`String firstCharToUpperCase(text)`: Uppercases the first letter of text that starts with an English letter, leaving the remainder unchanged. Null, empty, and whitespace-only strings are unchanged. The implementation uses a character-code offset; ensure the first character is an English letter.

```javascript
return string.firstCharToUpperCase('dataQL');
// DataQL
```

## firstCharToLowerCase

`String firstCharToLowerCase(text)`: Lowercases the first letter of text that starts with an English letter, leaving the remainder unchanged. Input restrictions and null/blank handling match firstCharToUpperCase.

```javascript
return string.firstCharToLowerCase('DataQL');
// dataQL
```

## toUpperCase

`String toUpperCase(text)`: Uppercases all of text using the Java default locale. Null returns null; an empty string stays empty.

```javascript
return string.toUpperCase('DataQL');
// DATAQL
```

## toLowerCase

`String toLowerCase(text)`: Lowercases all of text using the Java default locale. Null returns null; an empty string stays empty.

```javascript
return string.toLowerCase('DataQL');
// dataql
```

## indexOf

`int indexOf(text, search)`: Returns the zero-based position of the first occurrence of search in text. Returns -1 when no match exists or either argument is null; an empty search returns 0.

```javascript
return string.indexOf('abcabc', 'bc');
// 1
```

## indexOfWithStart

`int indexOfWithStart(text, search, start)`: Searches forward for search from the inclusive zero-based start position. The result is relative to the whole text. Negative start values act as 0; a missing match or null text argument returns -1.

```javascript
return string.indexOfWithStart('abcabc', 'bc', 2);
// 4
```

## indexOfIgnoreCase

`int indexOfIgnoreCase(text, search)`: Finds the first occurrence of search, ignoring case. Returns -1 when no match exists or either argument is null.

```javascript
return string.indexOfIgnoreCase('AbCaBc', 'bc');
// 1
```

## indexOfIgnoreCaseWithStart

`int indexOfIgnoreCaseWithStart(text, search, start)`: Searches forward for search from the inclusive start position, ignoring case. Negative start values act as 0; failure returns -1.

```javascript
return string.indexOfIgnoreCaseWithStart('AbCaBc', 'bc', 2);
// 4
```

## lastIndexOf

`int lastIndexOf(text, search)`: Returns the starting index of the last occurrence of search. Returns -1 when no match exists or either argument is null; an empty search returns the text length.

```javascript
return string.lastIndexOf('abcabc', 'bc');
// 4
```

## lastIndexOfWithStart

`int lastIndexOfWithStart(text, search, start)`: Searches backward for search whose starting index is at most start. Start is zero-based; a negative start returns -1, while a start beyond the text searches from the end.

```javascript
return string.lastIndexOfWithStart('abcabc', 'bc', 3);
// 1
```

## lastIndexOfIgnoreCase

`int lastIndexOfIgnoreCase(text, search)`: Returns the starting index of the last occurrence of search, ignoring case. A missing match or null argument returns -1.

```javascript
return string.lastIndexOfIgnoreCase('AbCaBc', 'ab');
// 3
```

## lastIndexOfIgnoreCaseWithStart

`int lastIndexOfIgnoreCaseWithStart(text, search, start)`: Searches backward from start for search, ignoring case. Start-position and return-value rules match lastIndexOfWithStart.

```javascript
return string.lastIndexOfIgnoreCaseWithStart('AbCaBc', 'ab', 2);
// 0
```

## contains

`boolean contains(text, search)`: Checks whether text contains the entire search substring, respecting case. A null argument returns false; an empty search matches any non-null text.

```javascript
return string.contains('DataQL', 'Data');
// true
```

## containsIgnoreCase

`boolean containsIgnoreCase(text, search)`: Checks whether text contains search, ignoring case. Null and empty-substring handling match contains.

```javascript
return string.containsIgnoreCase('DataQL', 'data');
// true
```

## containsAny

`boolean containsAny(text, searches)`: Searches is a list of strings; returns true if any entire substring matches. Empty/null text and a null/empty list return false. Null list elements do not match.

```javascript
return string.containsAny('DataQL', ['SQL', 'Data']);
// true
```

## containsAnyIgnoreCase

`boolean containsAnyIgnoreCase(text, searches)`: Matches any entire substring in searches, ignoring case. Empty and null input rules match containsAny.

```javascript
return string.containsAnyIgnoreCase('DataQL', ['sql', 'data']);
// true
```

## trim

`String trim(text)`: Removes leading and trailing characters with codes at most U+0020, including spaces, tabs, and line breaks. Interior whitespace is preserved; null returns null.

```javascript
return string.trim(' DataQL ');
// DataQL
```

## sub

`String sub(text, start, end)`: Extracts the interval including start and excluding end. Indices are zero-based; negative indices count from the end (-1 is the last character position). Out-of-range bounds are clamped; start greater than end returns an empty string, and null text returns null.

```javascript
return string.sub('DataQL', 0, 4);
// Data
```

```javascript
return string.sub('DataQL', -2, 6);
// QL
```

## left

`String left(text, length)`: Returns at most length characters from the left of text. A nonpositive length returns an empty string; a length beyond the text returns the whole text. Null text returns null.

```javascript
return string.left('DataQL', 4);
// Data
```

## right

`String right(text, length)`: Returns at most length characters from the right of text. A nonpositive length returns an empty string; a length beyond the text returns the whole text. Null text returns null.

```javascript
return string.right('DataQL', 2);
// QL
```

## alignRight

`String alignRight(text, padding, length)`: Pads the right side of text to a total length of length. Padding must be a non-null string; only its first character is used, or a space if it is empty. Text already at or beyond the target length is not truncated. With valid padding, null text returns null.

```javascript
return string.alignRight('ab', '0', 4);
// ab00
```

## alignLeft

`String alignLeft(text, padding, length)`: Pads the left side of text to a total length of length. Padding, overlong-text, and null-text rules match alignRight.

```javascript
return string.alignLeft('ab', '0', 4);
// 00ab
```

## alignCenter

`String alignCenter(text, padding, length)`: Pads both sides of text to a total length of length. If an odd number of padding characters is needed, the right receives one more. Other padding, overlong-text, and null-text rules match alignRight.

```javascript
return string.alignCenter('ab', '0', 4);
// 0ab0
```

```javascript
return string.alignCenter('ab', '0', 5);
// 0ab00
```

## compareString

`int compareString(left, right)`: Compares the two texts lexicographically. Returns a negative number, 0, or a positive number when left is less than, equal to, or greater than right; results are not limited to -1, 0, and 1. Null is treated as an empty string.

```javascript
return string.compareString('a', 'b');
// -1
```

## compareStringIgnoreCase

`int compareStringIgnoreCase(left, right)`: Compares two texts while ignoring case. Return-value meaning and null handling match compareString.

```javascript
return string.compareStringIgnoreCase('A', 'a');
// 0
```

## split

`List split(text[, separators])`: Splits text into a list of strings. Each character in separators is a delimiter; it is neither a regular expression nor a complete delimiter string. Adjacent delimiters collapse, and leading/trailing delimiters add no empty items. Omitted/null separators split on whitespace. Null text returns null; empty text returns an empty list.

```javascript
return string.split('a,,b;c', ',;');
// ["a", "b", "c"]
```

```javascript
return string.split('  Alice  Bob  ');
// ["Alice", "Bob"]
```

## join

`String join(values[, separator])`: Converts each element of values to text and joins them with separator. Omitted/null separator means an empty string. A null list returns null; an empty list returns an empty string. Null elements contribute empty text while retaining their delimiter positions.

```javascript
return string.join(['a', null, 'b'], ',');
// a,,b
```

```javascript
return string.join(['a', 'b']);
// ab
```

## isEmpty

`boolean isEmpty(text)`: Returns true if text is null or an empty string. A whitespace-only string returns false.

```javascript
return string.isEmpty(' ');
// false
```

## equalsIgnoreCase

`boolean equalsIgnoreCase(left, right)`: Checks whether the two texts are equal, ignoring case. Two nulls return true; exactly one null returns false.

```javascript
return string.equalsIgnoreCase('DataQL', 'dataql');
// true
```

CompareUdfSource is deprecated. Use this library's `compareString` and `compareStringIgnoreCase` for string comparisons.
