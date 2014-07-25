#!/bin/sh

export CLASSPATH=../libs/*
export CLASSPATH=../conf/:${CLASSPATH}

java benchmark.Bench $1 $2
