FROM gradle:jdk15
COPY ./* /app/
# 编译程序
WORKDIR /app/
RUN javac -d ./output ./main/java/miniplc0java/c0.java
# 将当前目录设为输出目录
WORKDIR /app/output
#FROM openjdk:12-alpine
## 向容器内复制文件
#COPY build.gradle gradle settings.gradle miniplc0-java.iml /
#COPY src /src
## 编译程序
#WORKDIR /app/
#RUN javac -d ./output ./main/java/miniplc0java/c0.java
## 将当前目录设为输出目录
#WORKDIR /app/output


#FROM gradle:jdk15
#WORKDIR /
#COPY build.gradle gradle settings.gradle miniplc0-java.iml /
#COPY src /src
#RUN gradle fatjar --no-daemon
#FROM openjdk:12-alpine
## 向容器内复制文件
#COPY ./* /app/
## 编译程序
#WORKDIR /app/
#RUN javac -d ./output ./c0.java
## 将当前目录设为输出目录
#WORKDIR /app/output