#!/usr/bin/env bash
jjtree ccal_parser.jjt;
javacc ccal_parser.jj;
javac *.java
