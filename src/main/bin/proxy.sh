#!/bin/sh

export CLASSPATH=../libs/*
export CLASSPATH=../conf/:${CLASSPATH}

java proxy.ProxyServiceController $1
