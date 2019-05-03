/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
parser grammar TomlParser;

options { tokenVocab=TomlLexer; }

@header {
package org.apache.tuweni.toml.internal;
}

// Document parser
toml : NewLine* (expression (NewLine+ expression)* NewLine*)? EOF;

expression
  : keyval
  | table
  ;


// Key string parser
tomlKey : key EOF;


// Key-Value pairs
keyval : key Equals val;

key : simpleKey (Dot simpleKey)*;
simpleKey
  : quotedKey
  | unquotedKey
  ;

unquotedKey : UnquotedKey;
quotedKey
  : basicString
  | literalString
  ;

val
  : string
  | integer
  | floatValue
  | booleanValue
  | dateTime
  | array
  | inlineTable
  ;


// String
string
  : mlBasicString
  | basicString
  | mlLiteralString
  | literalString
  ;


// Basic String
basicString : QuotationMark basicChar* QuotationMark;
basicChar
  : basicUnescaped
  | escaped
  ;
basicUnescaped : StringChar;

escaped : EscapeSequence;


// Multiline Basic String
mlBasicString : TripleQuotationMark mlBasicChar* TripleQuotationMark;
mlBasicChar
  : mlBasicUnescaped
  | escaped;
mlBasicUnescaped : StringChar | NewLine;


// Literal String
literalString : Apostrophe literalBody Apostrophe;
literalBody : StringChar*;


// Multiline Literal String
mlLiteralString : TripleApostrophe mlLiteralBody TripleApostrophe;
mlLiteralBody : (StringChar | NewLine)*;


// Integer
integer
     : decInt
     | hexInt
     | octInt
     | binInt
     ;

decInt : DecimalInteger;
hexInt : HexInteger;
octInt : OctalInteger;
binInt : BinaryInteger;


// Float
floatValue
  : regularFloat
  | regularFloatInf
  | regularFloatNaN
  ;
regularFloat : FloatingPoint;
regularFloatInf : FloatingPointInf;
regularFloatNaN : FloatingPointNaN;


// Boolean
booleanValue
  : trueBool
  | falseBool
  ;

trueBool : TrueBoolean;
falseBool : FalseBoolean;


// Date and Time
dateTime
 : offsetDateTime
 | localDateTime
 | localDate
 | localTime
 ;

offsetDateTime : date TimeDelimiter time timeOffset;
localDateTime : date TimeDelimiter time;
localDate : date;
localTime : time;

date : year Dash month Dash day;
time : hour Colon minute Colon second (Dot secondFraction)?;
timeOffset
  : Z
  | hourOffset Colon minuteOffset
  ;
hourOffset : (Dash | Plus) hour;
minuteOffset : DateDigits;
secondFraction : DateDigits;
year : DateDigits;
month : DateDigits;
day : DateDigits;
hour : DateDigits;
minute : DateDigits;
second : DateDigits;


// Array
array : ArrayStart (arrayValues Comma?)? NewLine* ArrayEnd;
arrayValues : arrayValue (Comma arrayValue)*;
arrayValue : NewLine* val;


// Table
table
  : standardTable
  | arrayTable
  ;


// Standard Table
standardTable : TableKeyStart key? TableKeyEnd;


// Inline Table
inlineTable : InlineTableStart inlineTableValues? InlineTableEnd;
inlineTableValues : keyval (Comma keyval)*;


// Array Table
arrayTable : ArrayTableKeyStart key? ArrayTableKeyEnd;
