# logstash-output-scribe

用于logstash输出到scribe服务器

# 开发

插件主要原理为jruby调用java。

和scribe通信用java实现，具体见`bt.scribe.MultiScribeClient`

利用ruby对`MultiScribeClient`做一层薄封装，具体见`lib/logstash/output/scribe.rb`

# 使用

## 打包

```
mvn package
gem build logstash-output-loglens.gemspec
```

## 安装到logstash

```
/path/to/logstash/bin/logstash-plugin install logstash-output-scribe-1.0.1-java.gem
```

# 配置说明

样例一

适合用于事件本身自带日志类别的情况，如从filebeat收集过来时，写入fields

filebeat.conf
```
filebeat.prospectors:
  - input_type: log
    paths:
      - /path/to/your/log/**
    fields:
      log_source: test_category

output.logstash:
  hosts: ["127.0.0.1:5044"]
```

logstash.conf
```
input {
  beats {
    port => 5044
  }
}
output {
  scribe {
    host => "127.0.0.1"
    port => 1463
    maxSendOnce => 300 # 一次rpc调用最多合并的日志数量
    maxRetryTimes => 2 # 最大重试次数
    categoryParamName => "[fields][log_source]" # 事件中的日志类别获取层级
  }
}
```

样例二，配合grok

适合用于类别写在日志文本中的情况。如下配置适合用于格式为：(类别开头，空格隔开，日志内容)

`${CATEGORY} ${LOG_DATA}`

```
filter {
  grok {
    match => {
      "message" => "%{WORD:category} %{GREEDYDATA:content}"
    }
  }
}
output {
  scribe {
    host => "127.0.0.1"
    port => 1463
    maxSendOnce => 300 # 一次rpc调用最多合并的日志数量
    maxRetryTimes => 2 # 最大重试次数
    categoryParamName => "[category]" # 事件中的日志类别获取层级
  }
}
```

## 特别说明

1. 由于logstash本身有pipeline大小配置，决定批量交付给输出插件的日志条目数量N最大值，`maxSendOnce`的值M的作用在交付过来的日志条件数量N上做切分，所以需要合理配置这两个数值，避免出现如：`N=101, M=100`，这样插件会发送两次，一次为100条，另一次为1条，浪费资源。

2. `log4j2.properties`为用于记录错误日志和发送失败的日志文本，需要手动添加到logstash的log4j2.properties中。